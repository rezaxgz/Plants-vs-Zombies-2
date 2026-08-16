package io.github.Plants_Vs_Zombies_2.commands;

import io.github.Plants_Vs_Zombies_2.controller.SettingsMenuController;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;

public enum SettingsMenuCommand implements Command<CommandResult> {
    CHANGE_DIFFICULTY(
            "^menu\\s+settings\\s+change-difficulty\\s+-l\\s+(?<difficulty>-?\\d+)$",
            SettingsMenuController::handleChangeDifficulty),
    SET_DEBUG_MODE(
            "^menu\\s+settings\\s+set-debug\\s+-v\\s+(?<enabled>true|false)$",
            SettingsMenuController::handleSetDebugMode);

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
