package commands;

import model.CommandResult;

public enum CollectionMenuCommand implements Command<CommandResult> {
    ;
    private final String pattern;
    private final CommandAction<CommandResult> action;

    CollectionMenuCommand(String pattern, CommandAction<CommandResult> action) {
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
