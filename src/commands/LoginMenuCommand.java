package commands;

import controller.LoginMenuController;
import model.CommandResult;

public enum LoginMenuCommand implements Command<CommandResult> {
    ANSWER("^answer\\s+-a\\s+(?<answer>.+)$", LoginMenuController::handleAnswer),
    FORGET_PASSWORD("^forget\\s+password\\s+-u\\s+(?<username>.+)\\s+-e\\s+(?<email>.+)$",
            LoginMenuController::handleForgetPassword),
    LOGIN("^login\\s+-u\\s+(?<username>.+)\\s+-p\\s+(?<password>.+)(?:\\s+-stay-logged-in)?$",
            LoginMenuController::handleLogin);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    LoginMenuCommand(String pattern, CommandAction<CommandResult> action) {
        this.pattern = pattern;
        this.action = action;
    }

    @Override
    public String getPattern() {
        return this.pattern;
    }

    @Override
    public CommandAction<CommandResult> getAction() {
        return action;
    }
}
