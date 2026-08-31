package io.github.Plants_Vs_Zombies_2.model.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;

class JsonUserRepositoryGameplayTest {
    @TempDir Path temporaryDirectory;

    @Test
    void acceptedUpdateIsAtomicAndRevisionSurvivesReload() throws Exception {
        Path database = temporaryDirectory.resolve("users.json");
        JsonUserRepository repository = repository(database);
        GameplayStateSnapshot initial = repository.findGameplayState("alice").orElseThrow();
        GameplayState update = withCore(initial.getState(), 350, 12,
                1, 2, 500, 4);

        GameplayStateSnapshot accepted = repository.updateGameplayState(
                "alice", 0L, update);
        assertEquals(1L, accepted.getRevision());
        assertEquals(350, accepted.getState().getCoins());

        JsonUserRepository reopened = new JsonUserRepository(database);
        GameplayStateSnapshot restored = reopened.findGameplayState("alice").orElseThrow();
        assertEquals(1L, restored.getRevision());
        assertEquals(update, restored.getState());
    }

    @Test
    void staleInvalidAndBackwardUpdatesLeaveStoredStateUnchanged() throws Exception {
        JsonUserRepository repository = repository(
                temporaryDirectory.resolve("users.json"));
        GameplayState initial = repository.findGameplayState("alice").orElseThrow().getState();
        GameplayState accepted = withCore(initial, 100, 5, 1, 1, 1000, 2);
        repository.updateGameplayState("alice", 0, accepted);

        GameplayUpdateException stale = assertThrows(GameplayUpdateException.class,
                () -> repository.updateGameplayState("alice", 0,
                        withCore(accepted, 999, 5, 1, 1, 1000, 2)));
        assertEquals(GameplayUpdateFailure.STALE_REVISION, stale.getFailure());
        GameplayUpdateException negative = assertThrows(GameplayUpdateException.class,
                () -> repository.updateGameplayState("alice", 1,
                        withCore(accepted, -1, 5, 1, 1, 1000, 2)));
        assertEquals(GameplayUpdateFailure.VALIDATION_FAILED, negative.getFailure());
        assertThrows(GameplayUpdateException.class,
                () -> repository.updateGameplayState("alice", 1,
                        withCore(accepted, 100, 5, 1, 1, 999, 2)));
        assertThrows(GameplayUpdateException.class,
                () -> repository.updateGameplayState("alice", 1,
                        withCore(accepted, 100, 5, 0, 0, 1000, 2)));
        assertThrows(GameplayUpdateException.class,
                () -> repository.updateGameplayState("alice", 1,
                        withCore(accepted, 100, 5, 1, 1, 1000, 1)));

        GameplayStateSnapshot unchanged = repository.findGameplayState("alice").orElseThrow();
        assertEquals(1L, unchanged.getRevision());
        assertEquals(accepted, unchanged.getState());
    }

    @Test
    void concurrentSameRevisionAllowsExactlyOneAtomicUpdate() throws Exception {
        JsonUserRepository repository = repository(
                temporaryDirectory.resolve("users.json"));
        GameplayState initial = repository.findGameplayState("alice").orElseThrow().getState();
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = () -> attempt(repository,
                    withCore(initial, 10, 0, 0, 0, 0, 0));
            Callable<Boolean> second = () -> attempt(repository,
                    withCore(initial, 20, 0, 0, 0, 0, 0));
            var results = executor.invokeAll(new ArrayList<>(java.util.List.of(first, second)));
            int successes = 0;
            for (var result : results) if (result.get()) successes++;
            assertEquals(1, successes);
            assertEquals(1L, repository.findGameplayState("alice")
                    .orElseThrow().getRevision());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void persistenceFailureRestoresInMemoryAccount() throws Exception {
        Path database = temporaryDirectory.resolve("users.json");
        JsonUserRepository repository = repository(database);
        GameplayState initial = repository.findGameplayState("alice").orElseThrow().getState();
        Files.delete(database);
        Files.createDirectory(database);

        assertThrows(RuntimeException.class, () -> repository.updateGameplayState(
                "alice", 0, withCore(initial, 77, 0, 0, 0, 0, 0)));
        GameplayStateSnapshot unchanged = repository.findGameplayState("alice").orElseThrow();
        assertEquals(0L, unchanged.getRevision());
        assertEquals(initial, unchanged.getState());
        assertTrue(Files.isDirectory(database));
    }

    @Test
    void questCountersPersistAreBoundedMonotonicAndDerived() throws Exception {
        Path database = temporaryDirectory.resolve("quests.json");
        JsonUserRepository repository = repository(database);
        GameplayState initial = repository.findGameplayState("alice")
                .orElseThrow().getState();
        GameplayState accepted = withQuests(initial, 3, 7);
        GameplayStateSnapshot saved = repository.updateGameplayState(
                "alice", 0, accepted);
        assertEquals(3, saved.getState().getCompletedDailyQuests());
        assertEquals(7, saved.getState().getCompletedNonDailyQuests());
        assertEquals(10, repository.snapshotLeaderboardEntries().get(0)
                .getTotalCompletedQuests());

        assertThrows(GameplayUpdateException.class, () ->
                repository.updateGameplayState("alice", 1,
                        withQuests(accepted, 2, 7)));
        assertThrows(GameplayUpdateException.class, () ->
                repository.updateGameplayState("alice", 1,
                        withQuests(accepted, -1, 7)));
        assertThrows(GameplayUpdateException.class, () ->
                repository.updateGameplayState("alice", 1,
                        withQuests(accepted, 3, 1_000_000_001)));
        assertThrows(GameplayUpdateException.class, () ->
                repository.updateGameplayState("alice", 0,
                        withQuests(accepted, 4, 8)));

        GameplayStateSnapshot unchanged = new JsonUserRepository(database)
                .findGameplayState("alice").orElseThrow();
        assertEquals(1L, unchanged.getRevision());
        assertEquals(3, unchanged.getState().getCompletedDailyQuests());
        assertEquals(7, unchanged.getState().getCompletedNonDailyQuests());
    }

    @Test
    void legacyJsonWithoutQuestProgressDefaultsCountersSafely() throws Exception {
        Path database = temporaryDirectory.resolve("legacy.json");
        User user = new User("legacy", "GoodPass1!", "Legacy",
                "legacy@example.com", Gender.MALE);
        UserJsonDatabase.save(database, List.of(user));
        JsonObject root = JsonParser.parseString(Files.readString(database))
                .getAsJsonObject();
        root.getAsJsonArray("users").get(0).getAsJsonObject()
                .remove("questProgress");
        Files.writeString(database, new Gson().toJson(root));

        User restored = UserJsonDatabase.load(database).get(0);
        assertEquals(0, restored.getQuestProgress().getCompletedDailyQuests());
        assertEquals(0,
                restored.getQuestProgress().getCompletedNonDailyQuests());
    }

    private static JsonUserRepository repository(Path database) {
        JsonUserRepository repository = new JsonUserRepository(database);
        User user = new User("alice", "GoodPass1!", "Alice",
                "alice@example.com", Gender.FEMALE);
        user.setSecurityQuestion(1, "answer");
        assertTrue(repository.addIfUsernameAvailable(user));
        return repository;
    }

    private static boolean attempt(JsonUserRepository repository,
            GameplayState state) {
        try {
            repository.updateGameplayState("alice", 0, state);
            return true;
        } catch (GameplayUpdateException expected) {
            return false;
        }
    }

    private static GameplayState withCore(GameplayState source,
            int coins, int diamonds, int chapter, int level,
            int highestScore, int gamesPlayed) {
        return new GameplayState(coins, diamonds, source.getSprouts(),
                source.getPlantFoodCount(), source.getPotCount(),
                source.getGreenhousePotsUnlocked(), chapter, level,
                source.getCompletedMinigames(), highestScore, gamesPlayed,
                source.getAdventureUnlockedLevels(),
                source.getCompletedAdventureLevels(),
                source.getMinigameUnlockedLevels(),
                source.getCompletedMinigameLevels(), source.getPlants(),
                source.getZombies(), source.getPlantBoosts(),
                source.getDailyOfferDate(), source.getDailyOfferPlant(),
                source.isDailyOfferPurchased(),
                source.getCompletedDailyQuests(),
                source.getCompletedNonDailyQuests());
    }

    private static GameplayState withQuests(GameplayState source,
            int daily, int nonDaily) {
        return new GameplayState(source.getCoins(), source.getDiamonds(),
                source.getSprouts(), source.getPlantFoodCount(),
                source.getPotCount(), source.getGreenhousePotsUnlocked(),
                source.getLastCompletedChapter(), source.getLastCompletedLevel(),
                source.getCompletedMinigames(), source.getHighestScore(),
                source.getGamesPlayed(), source.getAdventureUnlockedLevels(),
                source.getCompletedAdventureLevels(),
                source.getMinigameUnlockedLevels(),
                source.getCompletedMinigameLevels(), source.getPlants(),
                source.getZombies(), source.getPlantBoosts(),
                source.getDailyOfferDate(), source.getDailyOfferPlant(),
                source.isDailyOfferPurchased(), daily, nonDaily);
    }
}
