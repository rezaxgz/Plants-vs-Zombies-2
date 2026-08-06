package io.github.some_example_name.commands;

import io.github.some_example_name.controller.SettingsMenuController;
import io.github.some_example_name.model.CommandResult;

public enum SettingsMenuCommand implements Command<CommandResult> {
    CHANGE_DIFFICULTY(
            "^menu\\s+settings\\s+change-difficulty\\s+-l\\s+(?<difficulty>-?\\d+)$",
            SettingsMenuController::handleChangeDifficulty);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    SettingsMenuCommand(String pattern,
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
