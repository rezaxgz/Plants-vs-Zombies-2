package io.github.some_example_name.commands;

import io.github.some_example_name.controller.GameMenuController;
import io.github.some_example_name.controller.GameStatusCommandController;
import io.github.some_example_name.controller.IZombieCommandController;
import io.github.some_example_name.controller.ScoredGameCommandController;
import io.github.some_example_name.controller.SpecialLevelCommandController;
import io.github.some_example_name.controller.VaseBreakerCommandController;
import io.github.some_example_name.controller.WalletController;
import io.github.some_example_name.controller.WallnutBowlingCommandController;
import io.github.some_example_name.model.CommandResult;

public enum GameMenuCommand implements Command<CommandResult> {
    OPEN_LEADERBOARD("^menu\\s+leaderboard$",
            GameMenuController::handleOpenLeaderboard),
    SHOW_COIN_WALLET("^menu\\s+coin(?:-|\\s+)wallet$",
            WalletController::handleShowCoins),
    SHOW_GEM_WALLET("^menu\\s+gem(?:-|\\s+)wallet$",
            WalletController::handleShowDiamonds),
    CHEAT_ADD_CURRENCY(
            "^menu\\s+cheat\\s+add\\s+(?<count>\\d+)\\s+"
                    + "(?<currency>coin(?:s)?|diamond(?:s)?)$",
            WalletController::handleCheatAddCurrency),
    SHOW_SCORED_GAME_SCORE(
            "^show\\s+(?:scored(?:-|\\s+)game\\s+)?score$",
            ScoredGameCommandController::handleShowScore),
    SHOW_SCORED_GAME_RULES(
            "^show\\s+(?:scored(?:-|\\s+)game\\s+)?score\\s+rules$",
            ScoredGameCommandController::handleShowRules),
    OPEN_GREENHOUSE("^menu\\s+greenhouse$",
            GameMenuController::handleOpenGreenhouse),
    OPEN_TRAVEL_LOG("^menu\\s+travel(?:-|\\s+)log$",
            GameMenuController::handleOpenTravelLog),
    SHOW_I_ZOMBIE_STATUS(
            "^show\\s+i(?:-|\\s*)zombie(?:\\s+status)?$",
            IZombieCommandController::handleShowStatus),
    SHOW_I_ZOMBIE_CARDS(
            "^show\\s+(?:i(?:-|\\s*)zombie\\s+)?zombie\\s+cards$",
            IZombieCommandController::handleShowCards),
    SHOW_I_ZOMBIE_BRAINS("^show\\s+brains$",
            IZombieCommandController::handleShowStatus),
    PLACE_I_ZOMBIE(
            "^place\\s+zombie\\s+-t\\s+(?<type>.+?)\\s+-l\\s+"
                    + "\\(\\s*(?<x>-?\\d+)\\s*,\\s*"
                    + "(?<y>-?\\d+)\\s*\\)$",
            IZombieCommandController::handlePlaceZombie),
    SHOW_VASES("^show\\s+vases$",
            VaseBreakerCommandController::handleShowVases),
    SHOW_BOWLING_WALLNUTS(
            "^show\\s+(?:bowling|rolling)\\s+wall(?:-|\\s*)nuts$",
            WallnutBowlingCommandController::handleShowRollingWallnuts),
    SHOW_VASE_SEEDS("^show\\s+vase(?:-|\\s+)seeds$",
            VaseBreakerCommandController::handleShowSeeds),
    BREAK_VASE(
            "^break\\s+vase\\s+-l\\s+"
                    + "\\(\\s*(?<x>-?\\d+)\\s*,\\s*(?<y>-?\\d+)\\s*\\)$",
            VaseBreakerCommandController::handleBreakVase),
    PLANT_VASE_SEED(
            "^plant\\s+(?:vase(?:-|\\s+)?seed|seed)\\s+-s\\s+"
                    + "\\(\\s*(?<sx>-?\\d+)\\s*,\\s*(?<sy>-?\\d+)\\s*\\)\\s+"
                    + "-l\\s+\\(\\s*(?<x>-?\\d+)\\s*,\\s*(?<y>-?\\d+)\\s*\\)$",
            VaseBreakerCommandController::handlePlantSeed),
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
            SpecialLevelCommandController::handleShowSpecialLevelStatus),
    START_ZOMBIE_WAVES("^start\\s+zombie\\s+waves$",
            SpecialLevelCommandController::handleStartZombieWaves),
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
