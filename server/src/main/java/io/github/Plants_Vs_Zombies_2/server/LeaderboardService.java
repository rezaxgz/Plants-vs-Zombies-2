package io.github.Plants_Vs_Zombies_2.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.github.Plants_Vs_Zombies_2.model.auth.UserRepository;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardEntry;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardPage;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardQuery;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardSortColumn;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardSortDirection;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

/** Sorts and paginates immutable repository snapshots outside repository locks. */
final class LeaderboardService {
    static final int MAX_PAGE_SIZE = 100;

    private final UserRepository repository;

    LeaderboardService(UserRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    LeaderboardPage getPage(String authenticatedUsername, LeaderboardQuery query)
            throws AccountServiceException {
        validate(query);
        List<LeaderboardEntry> ordered = new ArrayList<>(
                repository.snapshotLeaderboardEntries());
        ordered.sort(comparator(query.getSortColumn(), query.getSortDirection()));

        Integer authenticatedRank = null;
        List<LeaderboardEntry> ranked = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            LeaderboardEntry entry = ordered.get(index).withRank(index + 1);
            ranked.add(entry);
            if (entry.getUsername().equals(authenticatedUsername)) {
                authenticatedRank = entry.getRank();
            }
        }
        int from = Math.min(query.getOffset(), ranked.size());
        int to = Math.min(from + query.getLimit(), ranked.size());
        return new LeaderboardPage(ranked.subList(from, to), ranked.size(),
                authenticatedRank, query.getOffset(), query.getLimit());
    }

    private static void validate(LeaderboardQuery query)
            throws AccountServiceException {
        if (query == null || query.getSortColumn() == null
                || query.getSortDirection() == null) {
            fail("sort column and direction are required");
        }
        if (query.getOffset() < 0) fail("offset cannot be negative");
        if (query.getLimit() <= 0 || query.getLimit() > MAX_PAGE_SIZE) {
            fail("limit must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private static Comparator<LeaderboardEntry> comparator(
            LeaderboardSortColumn column, LeaderboardSortDirection direction) {
        Comparator<LeaderboardEntry> primary = switch (column) {
            case USERNAME -> Comparator.comparing(LeaderboardEntry::getUsername,
                    String.CASE_INSENSITIVE_ORDER);
            case LAST_LEVEL -> Comparator
                    .comparingInt(LeaderboardEntry::getLastCompletedChapter)
                    .thenComparingInt(LeaderboardEntry::getLastCompletedLevel);
            case MINIGAMES -> Comparator.comparingInt(
                    LeaderboardEntry::getCompletedMinigames);
            case DAILY_QUESTS -> Comparator.comparingInt(
                    LeaderboardEntry::getCompletedDailyQuests);
            case NON_DAILY_QUESTS -> Comparator.comparingInt(
                    LeaderboardEntry::getCompletedNonDailyQuests);
            case QUESTS -> Comparator.comparingInt(
                    LeaderboardEntry::getTotalCompletedQuests);
            case HIGH_SCORE -> Comparator.comparingInt(
                    LeaderboardEntry::getHighestScore);
        };
        if (direction == LeaderboardSortDirection.DESCENDING) {
            primary = primary.reversed();
        }
        Comparator<LeaderboardEntry> usernameTie = Comparator.comparing(
                LeaderboardEntry::getUsername, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(LeaderboardEntry::getUsername);
        if (column == LeaderboardSortColumn.USERNAME) {
            // The requested case-insensitive username order is primary. Exact
            // spelling remains ascending only when names compare equal.
            return primary.thenComparing(LeaderboardEntry::getUsername);
        }
        return primary.thenComparing(usernameTie);
    }

    private static void fail(String message) throws AccountServiceException {
        throw new AccountServiceException(ProtocolErrorCode.VALIDATION_FAILED,
                message);
    }
}
