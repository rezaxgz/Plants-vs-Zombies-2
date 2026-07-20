package commands;

import controller.GameMenuController;
import controller.GameStatusCommandController;
import controller.SpecialLevelCommandController;
import model.CommandResult;

public enum GameMenuCommand implements Command<CommandResult> {
    ADVANCE_TIME("^advance\\s+time\\s+-t\\s+(?<count>\\d+)\\s+ticks$",
            GameMenuController::handleAdvanceTime),
    COLLECT_SUN("^collect\\s+sun\\s+-l\\s+\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)$",
            GameMenuController::handleCollectSun),
    COLLECT_PLANT_FOOD(
            "^collect\\s+plant(?:-|\\s+)food\\s+-l\\s+"
                    + "\\(\\s*(?<x>-?\\d+)\\s*,\\s*(?<y>-?\\d+)\\s*\\)$",
            GameMenuController::handleCollectPlantFood),
    COLLECT_REWARD(
            "^collect\\s+(?:reward|drop)\\s+-l\\s+"
                    + "\\(\\s*(?<x>-?\\d+)\\s*,\\s*(?<y>-?\\d+)\\s*\\)$",
            GameMenuController::handleCollectReward),
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
    SHOW_SUN_AMOUNT("^show\\s+sun\\s+amount$",
            GameMenuController::handleShowSunAmount),
    SHOW_MAP("^show\\s+map$",
            GameStatusCommandController::handleShowMap),
    SHOW_PLANTS_STATUS("^show\\s+plants\\s+status$",
            GameStatusCommandController::handleShowPlantsStatus),
    SHOW_TILE_STATUS(
            "^show\\s+tile\\s+status\\s+-l\\s+"
                    + "\\(\\s*(?<x>-?\\d+)\\s*,"
                    + "\\s*(?<y>-?\\d+)\\s*\\)$",
            GameStatusCommandController::handleShowTileStatus),
    SHOW_AVAILABLE_PLANTS("^show\\s+available\\s+plants$",
            GameMenuController::handleShowAvailablePlants),
    SHOW_FORCED_PLANTS("^show\\s+forced\\s+plants$",
            GameMenuController::handleShowForcedPlants),
    SHOW_PROTECTED_PLANTS("^show\\s+protected\\s+plants$",
            GameMenuController::handleShowProtectedPlants),
    SHOW_SPECIAL_LEVEL_STATUS(
            "^show\\s+special\\s+level\\s+status$",
            SpecialLevelCommandController
                    ::handleShowSpecialLevelStatus),
    START_ZOMBIE_WAVES("^start\\s+zombie\\s+waves$",
            SpecialLevelCommandController
                    ::handleStartZombieWaves),
    SHOW_CONVEYOR_BELT("^show\\s+conveyor\\s+belt$",
            GameMenuController::handleShowConveyorBelt),
    PLANT_FROM_CONVEYOR(
            "^plant\\s+from-conveyor\\s+-i\\s+"
                    + "(?<index>\\d+)\\s+-l\\s+"
                    + "\\(\\s*(?<x>-?\\d+)\\s*,"
                    + "\\s*(?<y>-?\\d+)\\s*\\)$",
            GameMenuController::handlePlantFromConveyor),
    ZOMBIES_INFO("^zombies\\s+info$",
            GameMenuController::handleZombiesInfo),
    CHEAT_SPAWN_ZOMBIE(
            "^cheat\\s+spawn-zombie\\s+-t\\s+(?<type>.+?)\\s+-l\\s+"
                    + "(?:\\(\\s*)?(?<x>-?\\d+)\\s*,\\s*(?<y>-?\\d+)"
                    + "(?:\\s*\\))?$",
            GameMenuController::handleSpawnZombie),
    RELEASE_NUKE("^release\\s+the\\s+nuke$",
            GameMenuController::handleReleaseNuke);

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
