package commands;

import controller.SignupMenuController;
import model.CommandResult;

public enum SignUpMenuCommand implements Command<CommandResult> {
    REGISTER(
            "^register\\s+-u\\s+(?<username>.+)\\s+-p\\s+(<password>.+)\\s+(?<passwordConfirm>.+)\\s+-n\\s+(?<nickname>.+)\\s+-e\\s+(?<email>.+)\\s+g\\s+(?<gender>.+)$",
            SignupMenuController::handleRegister),
    PICK_QUESTION(
            "^pick\\s+question\\s+-q\\s+(?<questionNumber>\\d+)\\s+-a\\s+(?<answer>\\.+)\\s+-c\\s+(?<answerConfirm>\\.+)$",
            SignupMenuController::handlePickQuestion);

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
