package io.github.some_example_name.commands;

import io.github.some_example_name.controller.PlantSelectionController;
import io.github.some_example_name.model.CommandResult;

public enum PlantSelectionMenuCommand implements Command<CommandResult> {
    SHOW_ALL_PLANTS("^show\\s+all\\s+plants$",
            PlantSelectionController::handleShowAllPlants),
    SHOW_AVAILABLE_PLANTS("^show\\s+available\\s+plants$",
            PlantSelectionController::handleShowAvailablePlants),
    ADD_PLANT("^add\\s+plant\\s+-t\\s+(?<type>.+?)$",
            PlantSelectionController::handleAddPlant),
    REMOVE_PLANT("^remove\\s+plant\\s+-t\\s+(?<type>.+?)$",
            PlantSelectionController::handleRemovePlant),
    BOOST_PLANT("^boost\\s+plant\\s+-t\\s+(?<type>.+?)$",
            PlantSelectionController::handleBoostPlant),
    START_GAME("^start\\s+game$",
            PlantSelectionController::handleStartGame);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    PlantSelectionMenuCommand(String pattern,
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
