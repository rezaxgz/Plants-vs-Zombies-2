package io.github.some_example_name.commands;

import io.github.some_example_name.controller.MenuController;
import io.github.some_example_name.model.CommandResult;

public enum MenuCommand implements Command<CommandResult> {
    ENTER(
            "^menu\\s+enter\\s+(?<menuName>[A-Za-z][A-Za-z0-9_\\- ]*)$",
            MenuController::handleEnter),
    SHOW_CURRENT(
            "^menu\\s+show\\s+current$",
            MenuController::handleShowCurrent),
    EXIT(
            "^menu\\s+exit$",
            MenuController::handleExit);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    MenuCommand(String pattern, CommandAction<CommandResult> action) {
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
