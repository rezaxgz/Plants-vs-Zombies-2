package io.github.some_example_name.commands;

import io.github.some_example_name.controller.TravelLogMenuController;
import io.github.some_example_name.model.CommandResult;

public enum TravelLogMenuCommand implements Command<CommandResult> {
    MINIGAMES_PAGE(
            "^travel\\s+log\\s+page\\s+mini(?:-|\\s*)game(?:s)?$",
            TravelLogMenuController::handleMinigamesPage),
    SHOW_MINIGAMES(
            "^show\\s+mini(?:-|\\s*)games$",
            TravelLogMenuController::handleMinigamesPage),
    SHOW_VASE_BREAKER_LEVELS(
            "^show\\s+vase(?:-|\\s*)breaker\\s+levels$",
            TravelLogMenuController::handleShowVaseBreakerLevels),
    SHOW_WALLNUT_BOWLING_LEVELS(
            "^show\\s+wall(?:-|\\s*)nut\\s+bowling\\s+levels$",
            TravelLogMenuController::handleShowWallnutBowlingLevels),
    SHOW_I_ZOMBIE_LEVELS(
            "^show\\s+i(?:-|\\s*)zombie\\s+levels$",
            TravelLogMenuController::handleShowIZombieLevels),
    START_VASE_BREAKER(
            "^(?:start\\s+vase(?:-|\\s*)breaker|"
                    + "start\\s+minigame\\s+-m\\s+vase(?:-|\\s*)breaker)"
                    + "\\s+-l\\s+(?<level>\\d+)$",
            TravelLogMenuController::handleStartVaseBreaker),
    START_WALLNUT_BOWLING(
            "^(?:start\\s+wall(?:-|\\s*)nut\\s+bowling|"
                    + "start\\s+minigame\\s+-m\\s+"
                    + "wall(?:-|\\s*)nut(?:-|\\s*)bowling)"
                    + "\\s+-l\\s+(?<level>\\d+)$",
            TravelLogMenuController::handleStartWallnutBowling),
    START_I_ZOMBIE(
            "^(?:start\\s+i(?:-|\\s*)zombie|"
                    + "start\\s+minigame\\s+-m\\s+i(?:-|\\s*)zombie)"
                    + "\\s+-l\\s+(?<level>\\d+)$",
            TravelLogMenuController::handleStartIZombie),
    NUMBERED_PAGE(
            "^travel\\s+log\\s+page\\s+(?<pageNumber>\\d+)$",
            TravelLogMenuController::handleNumberedPage),
    PAGE(
            "^travel\\s+log\\s+page\\s+"
                    + "(?<page>[A-Za-z][A-Za-z0-9_\\- ]*)$",
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
