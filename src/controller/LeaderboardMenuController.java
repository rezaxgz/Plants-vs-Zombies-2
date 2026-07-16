package controller;

import java.util.List;
import java.util.regex.Matcher;
import model.CommandResult;
import model.leaderboard.LeaderBoard;
import model.user.User;

public class LeaderboardMenuController {

    public static CommandResult handleSort(Matcher matcher) {
        String columnStr = matcher.group("column").toUpperCase();
        String orderStr = matcher.group("order").toLowerCase();

        LeaderBoard.SortColumn column;
        try {
            column = LeaderBoard.SortColumn.valueOf(columnStr);
        } catch (IllegalArgumentException e) {
            return CommandResult.error("Invalid column for sorting.");
        }

        boolean isAscending = orderStr.equals("asc");
        List<User> sortedUsers = LeaderBoard.getSortedLeaderboard(column, isAscending);

        CommandResult result = CommandResult.success("Leaderboard Sorted by " + columnStr + " (" + orderStr + "):");

        for (int i = 0; i < sortedUsers.size(); i++) {
            User u = sortedUsers.get(i);
            String row = String.format("%d. %s | Lvl: %d-%d | Minigames: %d | Quests: %d | Score: %d",
                    i + 1, u.getUsername(),
                    u.getGameProgerss().getLastCompletedChapter(), u.getGameProgerss().getLastCompletedLevel(),
                    u.getGameProgerss().getCompletedMinigames(),
                    u.getQuestProgress().getTotalCompletedQuests(),
                    u.getGameProgerss().getHighestScore());
            result.addPostCommandResult(row);
        }

        return result;
    }
}