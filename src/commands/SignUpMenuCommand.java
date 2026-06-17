package commands;

import controller.SignupMenuController;
import model.CommandResult;

public enum SignUpMenuCommand implements Command<CommandResult> {
    REGISTER(".*", SignupMenuController::handleRegister);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    SignUpMenuCommand(String pattern, CommandAction<CommandResult> action) {
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
