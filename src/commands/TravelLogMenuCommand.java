package commands;

import controller.TravelLogMenuController;
import model.CommandResult;

public enum TravelLogMenuCommand implements Command<CommandResult> {
    PAGE(
            "^travel\\s+log\\s+page\\s+(?<page>\\d+)$",
            TravelLogMenuController::handlePage);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    TravelLogMenuCommand(String pattern, CommandAction<CommandResult> action) {
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