package commands;

import controller.MainController;
import model.CommandResult;

public enum MainMenuCommand implements Command<CommandResult> {
    LOGOUT("^menu\\s+logout$", MainController::handleLogout);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    MainMenuCommand(String pattern, CommandAction<CommandResult> action) {
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
