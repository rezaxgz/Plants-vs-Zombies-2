package model.leaderboard;

import java.util.Comparator;
import java.util.List;
import model.auth.UserManager;
import model.user.User;

public class LeaderBoard {
    public enum SortColumn {
        USERNAME, LAST_LEVEL, MINIGAMES, QUESTS, HIGH_SCORE
    }

    public static List<User> getSortedLeaderboard(SortColumn column, boolean ascending) {
        List<User> users = UserManager.loadAllUsers();
        Comparator<User> comparator;

        switch (column) {
            case LAST_LEVEL:
                comparator = Comparator.comparingInt(u -> (u.getGameProgerss().getLastCompletedChapter() * 1000)
                        + u.getGameProgerss().getLastCompletedLevel());
                break;
            case MINIGAMES:
                comparator = Comparator.comparingInt(u -> u.getGameProgerss().getCompletedMinigames());
                break;
            case QUESTS:
                comparator = Comparator.comparingInt(u -> u.getQuestProgress().getTotalCompletedQuests());
                break;
            case HIGH_SCORE:
                comparator = Comparator.comparingInt(u -> u.getGameProgerss().getHighestScore());
                break;
            case USERNAME:
            default:
                comparator = Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER);
                break;
        }

        if (!ascending) {
            comparator = comparator.reversed();
        }

        users.sort(comparator);
        return users;
    }
}