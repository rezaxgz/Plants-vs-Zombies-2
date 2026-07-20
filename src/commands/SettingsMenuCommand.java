package commands;

import controller.SettingsMenuController;
import model.CommandResult;

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
