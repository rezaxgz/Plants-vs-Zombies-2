package io.github.Plants_Vs_Zombies_2.commands;

import io.github.Plants_Vs_Zombies_2.controller.CollectionMenuController;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;

public enum CollectionMenuCommand implements Command<CommandResult> {
    SHOW_PLANTS("^menu\\s+collection\\s+show-plants$",
            CollectionMenuController::handleShowPlants),
    SHOW_ALL_PLANTS("^menu\\s+collection\\s+show-all-plants$",
            CollectionMenuController::handleShowAllPlants),
    SHOW_ZOMBIES("^menu\\s+collection\\s+show-zombies$",
            CollectionMenuController::handleShowZombies),
    SHOW_ALL_ZOMBIES("^menu\\s+collection\\s+show-all-zombies$",
            CollectionMenuController::handleShowAllZombies),
    SHOW_PLANT("^menu\\s+collection\\s+show-plant\\s+-p\\s+(?<plant>.+?)$",
            CollectionMenuController::handleShowPlant),
    SHOW_ZOMBIE("^menu\\s+collection\\s+show-zombie\\s+-z\\s+(?<zombie>.+?)$",
            CollectionMenuController::handleShowZombie),
    UPGRADE_PLANT("^menu\\s+collection\\s+upgrade-plant\\s+-p\\s+(?<plant>.+?)$",
            CollectionMenuController::handleUpgradePlant),
    PURCHASE_PLANT("^menu\\s+collection\\s+purchase-plant\\s+-p\\s+(?<plant>.+?)$",
            CollectionMenuController::handlePurchasePlant);

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
