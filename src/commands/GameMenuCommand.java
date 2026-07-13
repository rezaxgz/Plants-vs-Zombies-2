package commands;

import controller.GameMenuController;
import model.CommandResult;

public enum GameMenuCommand implements Command<CommandResult> {
    ADVANCE_TIME("^advance\\s+time\\s+-t\\s+(?<count>\\d+)\\s+ticks$", GameMenuController::handleAdvanceTime),
    COLLECT_SUN("^collect\\s+sun\\s+-l\\s+\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)$",
            GameMenuController::handleCollectSun);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    GameMenuCommand(String pattern, CommandAction<CommandResult> action) {
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
