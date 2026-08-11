package io.github.Plants_Vs_Zombies_2.commands;

import io.github.Plants_Vs_Zombies_2.controller.LeaderboardMenuController;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;

public enum LeaderboardMenuCommand
        implements Command<CommandResult> {
    SHOW("^show\s+leaderboard$",
            LeaderboardMenuController::handleShow),
    SORT(
            "^sort\s+-c\s+"
                    + "(?<column>username|last_level|minigames|"
                    + "daily_quests|non_daily_quests|quests|high_score)"
                    + "\s+-o\s+(?<order>asc|desc)$",
            LeaderboardMenuController::handleSort);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    LeaderboardMenuCommand(String pattern,
            CommandAction<CommandResult> action) {
        this.pattern = pattern;
        this.action = action;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public CommandAction<CommandResult> getAction() {
        return action;
    }
}
