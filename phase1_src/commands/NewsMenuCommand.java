package commands;

import controller.NewsMenuController;
import model.CommandResult;

public enum NewsMenuCommand implements Command<CommandResult> {
    SHOW_UNREAD(
            "^menu\\s+news\\s+show-unread$",
            NewsMenuController::handleShowUnread),
    SHOW_ALL(
            "^menu\\s+news\\s+show-all$",
            NewsMenuController::handleShowAll);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    NewsMenuCommand(String pattern, CommandAction<CommandResult> action) {
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