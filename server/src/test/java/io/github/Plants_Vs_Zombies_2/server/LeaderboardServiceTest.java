package io.github.Plants_Vs_Zombies_2.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.Plants_Vs_Zombies_2.model.auth.JsonUserRepository;
import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardEntry;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardPage;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardQuery;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardSortColumn;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardSortDirection;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;

class LeaderboardServiceTest {
    @TempDir Path temporaryDirectory;

    @Test
    void emptyAndSinglePlayerPagesHaveGlobalMetadata() throws Exception {
        JsonUserRepository repository = repository();
        LeaderboardService service = new LeaderboardService(repository);
        LeaderboardPage empty = service.getPage("nobody", query(
                LeaderboardSortColumn.HIGH_SCORE,
                LeaderboardSortDirection.DESCENDING, 0, 100));
        assertTrue(empty.getEntries().isEmpty());
        assertEquals(0, empty.getTotalPlayers());

        repository.addIfUsernameAvailable(user("alice", 2, 3, 4, 5, 6, 700));
        LeaderboardPage one = service.getPage("alice", query(
                LeaderboardSortColumn.HIGH_SCORE,
                LeaderboardSortDirection.DESCENDING, 0, 100));
        assertEquals(1, one.getTotalPlayers());
        assertEquals(1, one.getEntries().get(0).getRank());
        assertEquals(1, one.getAuthenticatedUserRank());
    }

    @Test
    void everyColumnSupportsBothDirectionsAndStableUsernameTies()
            throws Exception {
        JsonUserRepository repository = repository();
        repository.addIfUsernameAvailable(user("charlie", 1, 1, 1, 1, 1, 10));
        repository.addIfUsernameAvailable(user("Alice", 2, 1, 2, 2, 2, 20));
        repository.addIfUsernameAvailable(user("alice", 2, 2, 3, 3, 3, 30));
        repository.addIfUsernameAvailable(user("bob", 3, 1, 4, 4, 4, 40));
        LeaderboardService service = new LeaderboardService(repository);

        for (LeaderboardSortColumn column : LeaderboardSortColumn.values()) {
            LeaderboardPage ascending = service.getPage("alice", query(column,
                    LeaderboardSortDirection.ASCENDING, 0, 100));
            LeaderboardPage descending = service.getPage("alice", query(column,
                    LeaderboardSortDirection.DESCENDING, 0, 100));
            assertEquals(4, ascending.getEntries().size(), column.name());
            assertEquals(4, descending.getEntries().size(), column.name());
            assertSequential(ascending.getEntries());
            assertSequential(descending.getEntries());
            List<String> expectedAscending = column == LeaderboardSortColumn.USERNAME
                    ? List.of("Alice", "alice", "bob", "charlie")
                    : List.of("charlie", "Alice", "alice", "bob");
            List<String> expectedDescending = column == LeaderboardSortColumn.USERNAME
                    ? List.of("charlie", "bob", "Alice", "alice")
                    : List.of("bob", "alice", "Alice", "charlie");
            assertEquals(expectedAscending, usernames(ascending),
                    column + " ascending");
            assertEquals(expectedDescending, usernames(descending),
                    column + " descending");
        }

        // Give all rows the same score in a second repository to isolate ties.
        JsonUserRepository tieRepository = new JsonUserRepository(
                temporaryDirectory.resolve("ties.json"));
        tieRepository.addIfUsernameAvailable(user("alice", 1, 1, 0, 0, 0, 9));
        tieRepository.addIfUsernameAvailable(user("Alice", 1, 1, 0, 0, 0, 9));
        tieRepository.addIfUsernameAvailable(user("Bob", 1, 1, 0, 0, 0, 9));
        LeaderboardPage tied = new LeaderboardService(tieRepository).getPage("alice", query(
                LeaderboardSortColumn.HIGH_SCORE,
                LeaderboardSortDirection.DESCENDING, 0, 100));
        assertEquals(List.of("Alice", "alice", "Bob"), usernames(tied));
    }

    @Test
    void chapterThenLevelOrderingDoesNotUseAnAmbiguousMultiplier()
            throws Exception {
        JsonUserRepository repository = repository();
        repository.addIfUsernameAvailable(user("laterChapter", 2, 1, 0, 0, 0, 0));
        repository.addIfUsernameAvailable(user("largeLevel", 1, 50_000, 0, 0, 0, 0));
        LeaderboardPage page = new LeaderboardService(repository).getPage(
                "laterChapter", query(LeaderboardSortColumn.LAST_LEVEL,
                        LeaderboardSortDirection.ASCENDING, 0, 100));
        assertEquals(List.of("largeLevel", "laterChapter"), usernames(page));
    }

    @Test
    void paginationRankAndLimitValidationUseTheGlobalOrdering()
            throws Exception {
        JsonUserRepository repository = repository();
        for (int index = 0; index < 12; index++) {
            repository.addIfUsernameAvailable(user("player" + index,
                    1, 1, 0, 0, 0, index));
        }
        LeaderboardService service = new LeaderboardService(repository);
        LeaderboardPage page = service.getPage("player11", query(
                LeaderboardSortColumn.HIGH_SCORE,
                LeaderboardSortDirection.DESCENDING, 3, 4));
        assertEquals(12, page.getTotalPlayers());
        assertEquals(4, page.getEntries().size());
        assertEquals(4, page.getEntries().get(0).getRank());
        assertEquals(1, page.getAuthenticatedUserRank());

        AccountServiceException tooLarge = assertThrows(
                AccountServiceException.class, () -> service.getPage("player0",
                        query(LeaderboardSortColumn.USERNAME,
                                LeaderboardSortDirection.ASCENDING, 0, 101)));
        assertEquals(ProtocolErrorCode.VALIDATION_FAILED,
                tooLarge.getErrorCode());
        assertThrows(AccountServiceException.class, () -> service.getPage(
                "player0", query(LeaderboardSortColumn.USERNAME,
                        LeaderboardSortDirection.ASCENDING, -1, 1)));
    }

    @Test
    void snapshotsAreImmutableAndDatabaseRestartPreservesValues()
            throws Exception {
        Path database = temporaryDirectory.resolve("restart.json");
        JsonUserRepository repository = new JsonUserRepository(database);
        User alice = user("alice", 3, 4, 5, 6, 7, 800);
        repository.addIfUsernameAvailable(alice);
        List<LeaderboardEntry> snapshot = repository.snapshotLeaderboardEntries();
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(snapshot.get(0)));
        alice.getGameProgerss().setHighestScore(999);
        assertEquals(800, snapshot.get(0).getHighestScore());

        // Persist the mutated compatibility object through the existing save
        // boundary by adding another user, then reload the database.
        repository.addIfUsernameAvailable(user("bob", 1, 1, 0, 0, 0, 1));
        LeaderboardPage restarted = new LeaderboardService(
                new JsonUserRepository(database)).getPage("alice", query(
                        LeaderboardSortColumn.HIGH_SCORE,
                        LeaderboardSortDirection.DESCENDING, 0, 100));
        assertEquals(999, restarted.getEntries().get(0).getHighestScore());
        assertEquals(6, restarted.getEntries().get(0).getCompletedDailyQuests());
        assertEquals(7, restarted.getEntries().get(0).getCompletedNonDailyQuests());
        assertEquals(13, restarted.getEntries().get(0).getTotalCompletedQuests());
    }

    @Test
    void concurrentGameplayUpdatesNeverPublishPartiallyAppliedRows()
            throws Exception {
        JsonUserRepository repository = repository();
        repository.addIfUsernameAvailable(user("alice", 1, 1, 0, 0, 0, 0));
        LeaderboardService service = new LeaderboardService(repository);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var writer = executor.submit(() -> {
                start.await();
                GameplayStateSnapshot current = repository.findGameplayState("alice")
                        .orElseThrow();
                for (int value = 1; value <= 50; value++) {
                    current = repository.updateGameplayState("alice",
                            current.getRevision(), withMetrics(current.getState(), value));
                }
                return null;
            });
            var reader = executor.submit(() -> {
                start.await();
                for (int attempt = 0; attempt < 500; attempt++) {
                    LeaderboardEntry entry = service.getPage("alice", query(
                            LeaderboardSortColumn.HIGH_SCORE,
                            LeaderboardSortDirection.DESCENDING, 0, 100))
                            .getEntries().get(0);
                    assertEquals(entry.getHighestScore(),
                            entry.getCompletedDailyQuests());
                    assertEquals(entry.getCompletedDailyQuests() * 2,
                            entry.getCompletedNonDailyQuests());
                    assertEquals(entry.getCompletedDailyQuests() * 3,
                            entry.getTotalCompletedQuests());
                }
                return null;
            });
            start.countDown();
            writer.get(10, TimeUnit.SECONDS);
            reader.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private JsonUserRepository repository() {
        return new JsonUserRepository(temporaryDirectory.resolve("users.json"));
    }

    private static User user(String username, int chapter, int level,
            int minigames, int daily, int nonDaily, int score) {
        User user = new User(username, "GoodPass1!", "Player",
                username + "@example.com", Gender.MALE);
        user.getGameProgerss().setLastCompletedChapter(chapter);
        user.getGameProgerss().setLastCompletedLevel(level);
        for (int index = 0; index < minigames; index++) {
            user.getGameProgerss().addCompletedMinigame();
        }
        user.getGameProgerss().setHighestScore(score);
        user.getQuestProgress().restoreCompletedCountsForStorage(daily, nonDaily);
        return user;
    }

    private static LeaderboardQuery query(LeaderboardSortColumn column,
            LeaderboardSortDirection direction, int offset, int limit) {
        return new LeaderboardQuery(column, direction, offset, limit);
    }

    private static List<String> usernames(LeaderboardPage page) {
        return page.getEntries().stream().map(LeaderboardEntry::getUsername).toList();
    }

    private static void assertSequential(List<LeaderboardEntry> entries) {
        for (int index = 0; index < entries.size(); index++) {
            assertEquals(index + 1, entries.get(index).getRank());
        }
    }

    private static GameplayState withMetrics(GameplayState source, int value) {
        return new GameplayState(source.getCoins(), source.getDiamonds(),
                source.getSprouts(), source.getPlantFoodCount(),
                source.getPotCount(), source.getGreenhousePotsUnlocked(),
                source.getLastCompletedChapter(), source.getLastCompletedLevel(),
                source.getCompletedMinigames(), value, source.getGamesPlayed(),
                source.getAdventureUnlockedLevels(),
                source.getCompletedAdventureLevels(),
                source.getMinigameUnlockedLevels(),
                source.getCompletedMinigameLevels(), source.getPlants(),
                source.getZombies(), source.getPlantBoosts(),
                source.getDailyOfferDate(), source.getDailyOfferPlant(),
                source.isDailyOfferPurchased(), value, value * 2);
    }
}
