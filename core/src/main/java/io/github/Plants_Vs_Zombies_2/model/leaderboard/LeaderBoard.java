package io.github.Plants_Vs_Zombies_2.model.leaderboard;

import java.util.Comparator;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/**
 * Global leaderboard over all registered users.
 */
public final class LeaderBoard {
    public enum SortColumn {
        USERNAME,
        LAST_LEVEL,
        MINIGAMES,
        DAILY_QUESTS,
        NON_DAILY_QUESTS,
        QUESTS,
        HIGH_SCORE
    }

    private LeaderBoard() {
    }

    public static List<User> getSortedLeaderboard(
            SortColumn column, boolean ascending) {
        List<User> users = UserManager.loadAllUsers();
        Comparator<User> comparator = comparatorFor(column)
                .thenComparing(
                        User::getUsername,
                        String.CASE_INSENSITIVE_ORDER);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        users.sort(comparator);
        return users;
    }

    private static Comparator<User> comparatorFor(
            SortColumn column) {
        if (column == null) {
            return Comparator.comparing(
                    User::getUsername,
                    String.CASE_INSENSITIVE_ORDER);
        }
        switch (column) {
            case LAST_LEVEL:
                return Comparator.comparingInt(
                        LeaderBoard::levelKey);
            case MINIGAMES:
                return Comparator.comparingInt(user -> user.getGameProgerss()
                        .getCompletedMinigames());
            case DAILY_QUESTS:
                return Comparator.comparingInt(user -> user.getQuestProgress()
                        .getCompletedDailyQuests());
            case NON_DAILY_QUESTS:
                return Comparator.comparingInt(user -> user.getQuestProgress()
                        .getCompletedNonDailyQuests());
            case QUESTS:
                return Comparator.comparingInt(user -> user.getQuestProgress()
                        .getTotalCompletedQuests());
            case HIGH_SCORE:
                return Comparator.comparingInt(user -> user.getGameProgerss()
                        .getHighestScore());
            case USERNAME:
            default:
                return Comparator.comparing(
                        User::getUsername,
                        String.CASE_INSENSITIVE_ORDER);
        }
    }

    private static int levelKey(User user) {
        return user.getGameProgerss()
                .getLastCompletedChapter() * 1000
                + user.getGameProgerss()
                        .getLastCompletedLevel();
    }
}
