package io.github.some_example_name.commands;

import io.github.some_example_name.controller.GreenhouseMenuController;
import io.github.some_example_name.model.CommandResult;

public enum GreenhouseMenuCommand implements Command<CommandResult> {
    SHOW_GREENHOUSE("^show\\s+greenhouse$",
            GreenhouseMenuController::handleShowGreenhouse),
    PLANT_POT("^plant\\s+pot\\s+at\\s+\\(\\s*(?<x>[1-5])\\s*,"
            + "\\s*(?<y>[1-4])\\s*\\)$",
            GreenhouseMenuController::handlePlantPot),
    COLLECT_PLANT("^collect\\s+\\(\\s*(?<x>[1-5])\\s*,"
            + "\\s*(?<y>[1-4])\\s*\\)$",
            GreenhouseMenuController::handleCollect),
    GROW_PLANT("^grow\\s+\\(\\s*(?<x>[1-5])\\s*,"
            + "\\s*(?<y>[1-4])\\s*\\)$",
            GreenhouseMenuController::handleGrow),
    ENTER_SHOP("^enter\\s+shop$",
            GreenhouseMenuController::handleEnterShop);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    GreenhouseMenuCommand(String pattern,
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
