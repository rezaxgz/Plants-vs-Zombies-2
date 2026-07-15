package commands;

import controller.GameMenuController;
import model.CommandResult;

public enum GameMenuCommand implements Command<CommandResult> {
    ADVANCE_TIME("^advance\\s+time\\s+-t\\s+(?<count>\\d+)\\s+ticks$",
            GameMenuController::handleAdvanceTime),
    COLLECT_SUN("^collect\\s+sun\\s+-l\\s+\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)$",
            GameMenuController::handleCollectSun),
    CHEAT_ADD_SUNS("^cheat\\s+add\\s+-n\\s+(?<count>-?\\d+)\\s+suns$",
            GameMenuController::handleCheatAddSuns),
    CHEAT_ADD_PLANT_FOOD("^cheat\\s+add-plant-food$",
            GameMenuController::handleCheatAddPlantFood),
    CHEAT_REMOVE_COOLDOWN("^cheat\\s+remove-cooldown$",
            GameMenuController::handleCheatRemoveCooldown),
    FEED_PLANT("^feed\\s+plant\\s+-l\\s+\\(\\s*(?<x>-?\\d+)\\s*,\\s*(?<y>-?\\d+)\\s*\\)$",
            GameMenuController::handleFeedPlant),
    PLANT_PLANT(
            "^plant\\s+plant\\s+-t\\s+(?<type>.+?)\\s+-l\\s+\\(\\s*(?<x>-?\\d+)\\s*,\\s*(?<y>-?\\d+)\\s*\\)$",
            GameMenuController::handlePlant),
    PLUCK_PLANT("^pluck\\s+plant\\s+-l\\s+\\(\\s*(?<x>-?\\d+)\\s*,\\s*(?<y>-?\\d+)\\s*\\)$",
            GameMenuController::handlePluckPlant),
    SHOW_SUN_AMOUNT("^show\\s+sun\\s+amount$", GameMenuController::handleShowSunAmount),
    ZOMBIES_INFO("^zombies\\s+info$", GameMenuController::handleZombiesInfo),
    RELEASE_NUKE("^release\\s+the\\s+nuke$", GameMenuController::handleReleaseNuke);

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
