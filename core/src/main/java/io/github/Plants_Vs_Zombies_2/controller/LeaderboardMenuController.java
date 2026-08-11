package io.github.Plants_Vs_Zombies_2.controller;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.leaderboard.LeaderBoard;
import io.github.Plants_Vs_Zombies_2.model.user.User;

public final class LeaderboardMenuController {
    private LeaderboardMenuController() {
    }

    public static CommandResult handleShow(
            Matcher matcher) {
        return buildLeaderboard(
                LeaderBoard.SortColumn.HIGH_SCORE,
                false);
    }

    public static CommandResult handleSort(
            Matcher matcher) {
        LeaderBoard.SortColumn column;
        try {
            column = LeaderBoard.SortColumn.valueOf(
                    matcher.group("column")
                            .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return CommandResult.error(
                    "invalid leaderboard column!");
        }
        boolean ascending = "asc".equalsIgnoreCase(
                matcher.group("order"));
        return buildLeaderboard(column, ascending);
    }

    private static CommandResult buildLeaderboard(
            LeaderBoard.SortColumn column,
            boolean ascending) {
        List<User> users = LeaderBoard.getSortedLeaderboard(
                column, ascending);
        String order = ascending ? "ascending" : "descending";
        CommandResult result = CommandResult.success(
                "Global leaderboard | sorted by "
                        + column + " | " + order);
        for (int index = 0; index < users.size(); index++) {
            result.addPostCommandResult(
                    formatRow(index + 1, users.get(index)));
        }
        if (users.isEmpty()) {
            result.addPostCommandResult(
                    "No registered users were found.");
        }
        return result;
    }

    private static String formatRow(int rank, User user) {
        return rank + ". " + user.getUsername()
                + " | chapter-level: "
                + user.getGameProgerss()
                        .getLastCompletedChapter()
                + "-"
                + user.getGameProgerss()
                        .getLastCompletedLevel()
                + " | minigames: "
                + user.getGameProgerss()
                        .getCompletedMinigames()
                + " | daily quests: "
                + user.getQuestProgress()
                        .getCompletedDailyQuests()
                + " | non-daily quests: "
                + user.getQuestProgress()
                        .getCompletedNonDailyQuests()
                + " | high score: "
                + user.getGameProgerss()
                        .getHighestScore();
    }
}
