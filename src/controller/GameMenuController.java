package controller;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.game.Game;
import model.game.PlantFoodResult;
import model.game.PlantPlacementResult;
import model.game.RewardCollectionResult;
import model.game.SunCollectionResult;
import model.game.entities.EntityPosition;
import model.game.entities.other.PlantFoodDrop;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantFactory;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.armor.Armor;
import model.game.special.ConveyorPlacementResult;
import model.game.special.ConveyorPlantPacket;
import model.game.special.ProtectedPlantStatus;
import model.menu.GameMenu;
import model.menu.Menu;
import model.roadmap.AdventureSession;
import model.user.User;

public final class GameMenuController {
    private GameMenuController() {
    }

    public static CommandResult handleAdvanceTime(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> preCommandResults = game.drainResults();
        int tickCount;
        try {
            tickCount = Integer.parseInt(matcher.group("count"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("tick count is too large!").addPreCommandResults(preCommandResults);
        }

        if (tickCount <= 0) {
            return CommandResult.error("tick count must be positive!").addPreCommandResults(preCommandResults);
        }

        game.advanceTicks(tickCount);
        List<String> progressResults =
                synchronizeAdventureProgress();
        return CommandResult.success(
                "time advanced by " + tickCount + " ticks")
                .addPreCommandResults(preCommandResults)
                .addPostCommandResults(game.drainResults())
                .addPostCommandResults(progressResults);
    }

    public static CommandResult handleCollectSun(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> preCommandResults = game.drainResults();
        int x;
        int y;
        try {
            x = Integer.parseInt(matcher.group("x"));
            y = Integer.parseInt(matcher.group("y"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("sun location is invalid!").addPreCommandResults(preCommandResults);
        }

        if (!isInsideBoard(game, x, y)) {
            return CommandResult.error("sun location is outside the board!").addPreCommandResults(preCommandResults);
        }

        SunCollectionResult collection = game.collectSunsAt(
                new EntityPosition(x, y));
        if (!collection.hasCollectedAnything()) {
            return CommandResult.error("there is no sun at (" + x + ", " + y + ")")
                    .addPreCommandResults(preCommandResults);
        }

        String message = "collected " + collection.getCollectedSunAmount()
                + " sun at (" + x + ", " + y + ")";
        if (collection.getRadioactiveExplosionCount() > 0) {
            message += "; " + collection.getRadioactiveExplosionCount()
                    + " radioactive sun exploded";
        }
        return CommandResult.success(message)
                .addPreCommandResults(preCommandResults).addPostCommandResults(game.drainResults());
    }

    public static CommandResult handleCollectPlantFood(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }
        List<String> preCommandResults = game.drainResults();
        EntityPosition position = parseBoardPosition(game, matcher);
        if (position == null) {
            return CommandResult.error("plant food location is invalid!")
                    .addPreCommandResults(preCommandResults);
        }
        boolean hasPlantFood = game.getBoard().getCollectibleDropsAt(position)
                .stream().anyMatch(drop -> drop instanceof PlantFoodDrop);
        if (!hasPlantFood) {
            return CommandResult.error("there is no plant food at " + position + "!")
                    .addPreCommandResults(preCommandResults);
        }
        if (game.getPlantFoodCount() >= game.getMaximumPlantFoodCount()) {
            return CommandResult.error("plant food storage is full!")
                    .addPreCommandResults(preCommandResults);
        }
        int collected = game.collectPlantFoodDropsAt(position);
        return CommandResult.success("collected " + collected
                + " plant food; you have " + game.getPlantFoodCount()
                + " plant foods now")
                .addPreCommandResults(preCommandResults)
                .addPostCommandResults(game.drainResults());
    }

    public static CommandResult handleCollectReward(Matcher matcher) {
        Game game = getCurrentGame();
        User user = App.getInstance().getLoggedInUser();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }
        if (user == null) {
            return CommandResult.error("you must be logged in to collect rewards!");
        }
        List<String> preCommandResults = game.drainResults();
        EntityPosition position = parseBoardPosition(game, matcher);
        if (position == null) {
            return CommandResult.error("reward location is invalid!")
                    .addPreCommandResults(preCommandResults);
        }
        boolean hasReward = game.getBoard().getCollectibleDropsAt(position).stream()
                .anyMatch(drop -> !(drop instanceof PlantFoodDrop));
        if (!hasReward) {
            return CommandResult.error("there is no reward at " + position + "!")
                    .addPreCommandResults(preCommandResults);
        }
        RewardCollectionResult result = game.collectRewardDropsAt(position, user);
        UserManager.saveAllUsers();
        String message = "collected " + result.getDropCount() + " reward drop(s): "
                + result.getCoins() + " coins, " + result.getDiamonds()
                + " diamonds, " + result.getPots() + " pots; totals: "
                + user.getCoins() + " coins, " + user.getDiamonds()
                + " diamonds, " + user.getPotCount() + " pots";
        return CommandResult.success(message)
                .addPreCommandResults(preCommandResults)
                .addPostCommandResults(game.drainResults());
    }

    public static CommandResult handleCheatAddSuns(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> preCommandResults = game.drainResults();
        int count;
        try {
            count = Integer.parseInt(matcher.group("count"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("sun count is too large!").addPreCommandResults(preCommandResults);
        }

        if (count <= 0) {
            return CommandResult.error("sun count must be positive!").addPreCommandResults(preCommandResults);
        }

        try {
            game.addSun(count);
        } catch (IllegalArgumentException exception) {
            return CommandResult.error("sun total is too large!").addPreCommandResults(preCommandResults);
        }

        return CommandResult.success("added " + count + " suns\ntotal suns: " + game.getSunCount())
                .addPreCommandResults(preCommandResults).addPostCommandResults(game.drainResults());
    }

    public static CommandResult handleCheatAddPlantFood(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> preCommandResults = game.drainResults();
        if (!game.addPlantFood()) {
            return CommandResult.error("plant food storage is full!")
                    .addPreCommandResults(preCommandResults);
        }
        return CommandResult.success("added one plant food; you have "
                + game.getPlantFoodCount() + " plant foods now")
                .addPreCommandResults(preCommandResults)
                .addPostCommandResults(game.drainResults());
    }

    public static CommandResult handleCheatRemoveCooldown(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }
        List<String> preCommandResults = game.drainResults();
        game.removePlantCooldowns();
        return CommandResult.success("all plant cooldowns were removed")
                .addPreCommandResults(preCommandResults)
                .addPostCommandResults(game.drainResults());
    }

    public static CommandResult handleFeedPlant(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> preCommandResults = game.drainResults();
        int x;
        int y;
        try {
            x = Integer.parseInt(matcher.group("x"));
            y = Integer.parseInt(matcher.group("y"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("plant location is invalid!")
                    .addPreCommandResults(preCommandResults);
        }
        if (!isInsideBoard(game, x, y)) {
            return CommandResult.error("plant location is outside the board!")
                    .addPreCommandResults(preCommandResults);
        }

        EntityPosition position = new EntityPosition(x, y);
        PlantFoodResult result = game.feedPlantAt(position);
        switch (result) {
        case NO_PLANT_FOOD:
            return CommandResult.error("you do not have any plant food!")
                    .addPreCommandResults(preCommandResults);
        case NO_PLANT:
            return CommandResult.error("there is no plant at " + position + "!")
                    .addPreCommandResults(preCommandResults);
        case NO_EFFECT:
            return CommandResult.error("the plant at " + position
                    + " has no plant food effect!")
                    .addPreCommandResults(preCommandResults);
        case SUCCESS:
            return CommandResult.success("fed the plant at " + position
                    + "; " + game.getPlantFoodCount() + " plant foods remaining")
                    .addPreCommandResults(preCommandResults)
                    .addPostCommandResults(game.drainResults());
        default:
            throw new IllegalStateException("unknown plant food result: " + result);
        }
    }

    public static CommandResult handlePlant(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> preCommandResults = game.drainResults();
        if (game.hasConveyorBelt()) {
            return CommandResult.error(
                    "this is a Conveyor Belt level; "
                            + "use plant from-conveyor instead!")
                    .addPreCommandResults(preCommandResults);
        }

        int x;
        int y;
        try {
            x = Integer.parseInt(matcher.group("x"));
            y = Integer.parseInt(matcher.group("y"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("plant location is invalid!").addPreCommandResults(preCommandResults);
        }

        if (!isInsideBoard(game, x, y)) {
            return CommandResult.error("plant location is outside the board!")
                    .addPreCommandResults(preCommandResults);
        }

        EntityPosition position = new EntityPosition(x, y);
        String requestedType = matcher.group("type").trim();
        if (PlantFactory.createPlant(requestedType, position) == null) {
            return CommandResult.error(
                    "plant type '" + requestedType
                            + "' does not exist or is not implemented!")
                    .addPreCommandResults(preCommandResults);
        }
        BasePlant plant = game.createPlantFromLoadout(
                requestedType, position);
        if (plant == null) {
            return CommandResult.error(
                    "plant type '" + requestedType
                            + "' was not selected for this level!")
                    .addPreCommandResults(preCommandResults);
        }

        PlantPlacementResult placementResult = game.plant(plant);
        switch (placementResult) {
            case NOT_ENOUGH_SUN:
                return CommandResult.error("not enough suns to plant " + plant.getName() + "! required: "
                        + plant.getCost() + ", available: " + game.getSunCount())
                        .addPreCommandResults(preCommandResults);
            case COOLDOWN_ACTIVE:
                return CommandResult.error(plant.getName() + " is recharging for "
                        + String.format(Locale.ROOT, "%.1f",
                                game.getPlantCooldownRemainingSeconds(plant))
                        + " more seconds!")
                        .addPreCommandResults(preCommandResults);
            case PLANT_LOCKED:
                return CommandResult.error(
                        plant.getName()
                                + " is locked in this level!")
                        .addPreCommandResults(preCommandResults);
            case PLANT_NOT_SELECTED:
                return CommandResult.error(
                        plant.getName()
                                + " was not selected for this level!")
                        .addPreCommandResults(preCommandResults);
            case POSITION_OCCUPIED:
                return CommandResult.error("there is already a plant at " + position + "!")
                        .addPreCommandResults(preCommandResults);
            case INVALID_POSITION:
                return CommandResult.error("plant location is outside the board!")
                        .addPreCommandResults(preCommandResults);
            case SUCCESS:
                return CommandResult.success("planted " + plant.getName() + " at " + position + "\nspent "
                        + plant.getCost() + " suns; " + game.getSunCount() + " suns remaining")
                        .addPreCommandResults(preCommandResults).addPostCommandResults(game.drainResults());
            default:
                throw new IllegalStateException("unknown plant placement result: " + placementResult);
        }
    }

    public static CommandResult handlePluckPlant(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> preCommandResults = game.drainResults();
        int x;
        int y;
        try {
            x = Integer.parseInt(matcher.group("x"));
            y = Integer.parseInt(matcher.group("y"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("plant location is invalid!").addPreCommandResults(preCommandResults);
        }

        if (!isInsideBoard(game, x, y)) {
            return CommandResult.error("plant location is outside the board!")
                    .addPreCommandResults(preCommandResults);
        }

        EntityPosition position = new EntityPosition(x, y);
        if (game.isProtectedSeedAt(position)) {
            return CommandResult.error(
                    "the protected plant at " + position
                            + " cannot be plucked!")
                    .addPreCommandResults(preCommandResults);
        }

        BasePlant removedPlant = game.pluckPlantAt(position);
        if (removedPlant == null) {
            return CommandResult.error("there is no plant at " + position + "!")
                    .addPreCommandResults(preCommandResults);
        }

        return CommandResult.success("plucked " + removedPlant.getName() + " from " + position)
                .addPreCommandResults(preCommandResults).addPostCommandResults(game.drainResults());
    }


    public static CommandResult handleShowSunAmount(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        return CommandResult.success("sun amount: " + game.getSunCount())
                .addPreCommandResults(game.drainResults())
                .addPostCommandResults(game.drainResults());
    }

    public static CommandResult handleShowAvailablePlants(
            Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> pending = game.drainResults();
        if (!game.hasLockedPlants()) {
            return CommandResult.success(
                    "all implemented plants are available in this level.")
                    .addPreCommandResults(pending);
        }

        StringBuilder output =
                new StringBuilder("available plants")
                        .append(System.lineSeparator())
                        .append("rule: ")
                        .append(game.getLockedPlantsRuleDescription());

        List<String> available =
                game.getLockedPlantTypes();
        for (String plantType : available) {
            output.append(System.lineSeparator())
                    .append("- ")
                    .append(plantType);
        }

        return CommandResult.success(output.toString())
                .addPreCommandResults(pending);
    }

    public static CommandResult handleShowForcedPlants(
            Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> pending = game.drainResults();
        if (!game.hasLockedPlants()) {
            return CommandResult.success(
                    "this level has no forced plants.")
                    .addPreCommandResults(pending);
        }

        List<String> forced =
                game.getForcedPlantTypes();
        StringBuilder output =
                new StringBuilder("forced plants");
        if (forced.isEmpty()) {
            output.append(System.lineSeparator())
                    .append("- none");
        } else {
            for (String plantType : forced) {
                output.append(System.lineSeparator())
                        .append("- ")
                        .append(plantType);
            }
        }

        return CommandResult.success(output.toString())
                .addPreCommandResults(pending);
    }

    public static CommandResult handleShowProtectedPlants(
            Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> pending = game.drainResults();
        if (!game.hasSaveOurSeeds()) {
            return CommandResult.error(
                    "this level has no protected plants!")
                    .addPreCommandResults(pending);
        }

        StringBuilder output =
                new StringBuilder("protected plants");
        for (ProtectedPlantStatus status
                : game.getProtectedPlantStatuses()) {
            output.append(System.lineSeparator())
                    .append("WARNING - RED DEFENSE LINE: row ")
                    .append(status.getDefenseRow())
                    .append(System.lineSeparator())
                    .append("- ")
                    .append(status.getPlantType())
                    .append(" | start: ")
                    .append(status.getOriginalPosition())
                    .append(" | current: ")
                    .append(status.getCurrentPosition())
                    .append(" | hp: ")
                    .append(status.getCurrentHitPoints())
                    .append('/')
                    .append(status.getMaximumHitPoints())
                    .append(" | ")
                    .append(status.isAlive()
                            ? "protected" : "lost");
        }

        return CommandResult.success(output.toString())
                .addPreCommandResults(pending);
    }

    public static CommandResult handleShowConveyorBelt(
            Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> pending = game.drainResults();
        if (!game.hasConveyorBelt()) {
            return CommandResult.error(
                    "this level does not use a Conveyor Belt!")
                    .addPreCommandResults(pending);
        }

        List<ConveyorPlantPacket> packets =
                game.getConveyorPackets();
        StringBuilder output =
                new StringBuilder("conveyor belt");
        if (packets.isEmpty()) {
            output.append(System.lineSeparator())
                    .append("- empty");
        } else {
            for (int index = 0;
                    index < packets.size(); index++) {
                ConveyorPlantPacket packet =
                        packets.get(index);
                output.append(System.lineSeparator())
                        .append(index + 1)
                        .append(". ")
                        .append(packet.getPlantType());
            }
        }
        output.append(System.lineSeparator())
                .append("next packet in: ")
                .append(String.format(
                        Locale.ROOT, "%.1fs",
                        game.getConveyorSecondsUntilNextPacket()));

        return CommandResult.success(output.toString())
                .addPreCommandResults(pending);
    }

    public static CommandResult handlePlantFromConveyor(
            Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> pending = game.drainResults();
        if (!game.hasConveyorBelt()) {
            return CommandResult.error(
                    "this level does not use a Conveyor Belt!")
                    .addPreCommandResults(pending);
        }

        int index;
        int row;
        int column;
        try {
            index = Integer.parseInt(
                    matcher.group("index"));
            row = Integer.parseInt(matcher.group("x"));
            column = Integer.parseInt(matcher.group("y"));
        } catch (NumberFormatException exception) {
            return CommandResult.error(
                    "conveyor index or plant location is invalid!")
                    .addPreCommandResults(pending);
        }

        EntityPosition position =
                new EntityPosition(row, column);
        ConveyorPlantPacket packet =
                game.getConveyorPacket(index);
        ConveyorPlacementResult result =
                game.plantFromConveyor(index, position);

        switch (result) {
            case NOT_CONVEYOR_LEVEL:
                return CommandResult.error(
                        "this level does not use a Conveyor Belt!")
                        .addPreCommandResults(pending);
            case INVALID_PACKET:
                return CommandResult.error(
                        "there is no conveyor packet at index "
                                + index + "!")
                        .addPreCommandResults(pending);
            case INVALID_POSITION:
                return CommandResult.error(
                        "plant location is outside the board!")
                        .addPreCommandResults(pending);
            case POSITION_OCCUPIED:
                return CommandResult.error(
                        "there is already a plant at "
                                + position + "!")
                        .addPreCommandResults(pending);
            case UNKNOWN_PLANT:
                return CommandResult.error(
                        "the conveyor packet contains an unknown plant!")
                        .addPreCommandResults(pending);
            case SUCCESS:
                return CommandResult.success(
                        "planted " + packet.getPlantType()
                                + " from conveyor slot "
                                + index + " at " + position
                                + "; no sun was spent")
                        .addPreCommandResults(pending)
                        .addPostCommandResults(
                                game.drainResults());
            default:
                throw new IllegalStateException(
                        "unknown conveyor placement result: "
                                + result);
        }
    }

    public static CommandResult handleZombiesInfo(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> preCommandResults = game.drainResults();
        List<Zombie> zombies = game.getBoard().getZombies();
        if (zombies.isEmpty()) {
            return CommandResult.success("no zombies on the board")
                    .addPreCommandResults(preCommandResults);
        }

        StringBuilder output = new StringBuilder();
        for (Zombie zombie : zombies) {
            if (output.length() > 0) {
                output.append(System.lineSeparator());
            }
            output.append(zombie.getName()).append(':').append(System.lineSeparator());
            output.append("position: ")
                    .append(String.format(Locale.ROOT, "%.2f", zombie.getColumnPosition()))
                    .append(", ").append(zombie.getLane()).append(System.lineSeparator());
            output.append("health: ").append(zombie.getHitPoints()).append('/')
                    .append(zombie.getMaximumHitPoints()).append(System.lineSeparator());
            appendArmorInfo(output, zombie);
            appendEffectInfo(output, zombie);
            output.append("wave: ").append(zombie.getWaveNumber());
        }
        return CommandResult.success(output.toString()).addPreCommandResults(preCommandResults);
    }

    private static void appendArmorInfo(StringBuilder output, Zombie zombie) {
        output.append("armor:").append(System.lineSeparator());
        Armor armor = zombie.getArmor();
        if (armor != null && !armor.isDestroyed()) {
            output.append(armor.getType().getDisplayName()).append(": ")
                    .append(armor.getCurrentHealth()).append('/')
                    .append(armor.getMaximumHealth()).append(System.lineSeparator());
        }
    }

    private static void appendEffectInfo(StringBuilder output, Zombie zombie) {
        output.append("effects:").append(System.lineSeparator());
        if (zombie.isGlowing()) {
            output.append("glowing").append(System.lineSeparator());
        }
        if (zombie.isFrozen()) {
            appendTimedEffect(output, "frozen", zombie.getFrozenDuration());
        }
        if (zombie.isChilled()) {
            appendTimedEffect(output, "chilled", zombie.getChilledDuration());
        }
        if (zombie.isStunned()) {
            appendTimedEffect(output, "stunned", zombie.getStunnedDuration());
        }
        if (zombie.isHypnotized()) {
            output.append("hypnotized").append(System.lineSeparator());
        }
        if (zombie.getPoisonDurationSeconds() > 0.0) {
            appendTimedEffect(output, "poisoned", zombie.getPoisonDurationSeconds());
        }
    }

    private static void appendTimedEffect(StringBuilder output, String name, double duration) {
        output.append(name).append(": ")
                .append(String.format(Locale.ROOT, "%.1fs", duration))
                .append(System.lineSeparator());
    }

    public static CommandResult handleReleaseNuke(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> preCommandResults = game.drainResults();
        if (game.isGameOver()) {
            return CommandResult.error("the game is already over!")
                    .addPreCommandResults(preCommandResults);
        }

        int zombieCount =
                game.getBoard().getZombies().size();
        game.releaseNuke();
        List<String> progressResults =
                synchronizeAdventureProgress();
        return CommandResult.success(
                "the nuke killed " + zombieCount + " zombies")
                .addPreCommandResults(preCommandResults)
                .addPostCommandResults(game.drainResults())
                .addPostCommandResults(progressResults);
    }

    public static CommandResult handleSpawnZombie(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }
        List<String> preCommandResults = game.drainResults();
        int x;
        int y;
        try {
            x = Integer.parseInt(matcher.group("x"));
            y = Integer.parseInt(matcher.group("y"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("zombie location is invalid!")
                    .addPreCommandResults(preCommandResults);
        }
        if (x < 0 || x >= game.getBoard().getNumberOfColumns()
                || y < 0 || y >= game.getBoard().getNumberOfRows()) {
            return CommandResult.error("zombie location is outside the board!")
                    .addPreCommandResults(preCommandResults);
        }
        Zombie zombie = game.spawnZombie(matcher.group("type").trim(), x, y);
        if (zombie == null) {
            return CommandResult.error("zombie type does not exist!")
                    .addPreCommandResults(preCommandResults);
        }
        UserManager.saveAllUsers();
        return CommandResult.success("spawned " + zombie.getName() + " at ("
                + x + ", " + y + ")"
                + (zombie.isGlowing() ? " as a glowing zombie" : ""))
                .addPreCommandResults(preCommandResults)
                .addPostCommandResults(game.drainResults());
    }

    private static List<String>
            synchronizeAdventureProgress() {
        Menu currentMenu =
                App.getInstance().getCurrentMenu();
        if (!(currentMenu instanceof GameMenu)) {
            return List.of();
        }
        ((GameMenu) currentMenu)
                .synchronizeAdventureProgress();
        return AdventureSession.getInstance()
                .drainNotifications();
    }

    private static boolean isInsideBoard(Game game, int x, int y) {
        return x >= 0 && y >= 0 && x < game.getBoard().getNumberOfRows()
                && y < game.getBoard().getNumberOfColumns();
    }

    private static EntityPosition parseBoardPosition(Game game, Matcher matcher) {
        int x;
        int y;
        try {
            x = Integer.parseInt(matcher.group("x"));
            y = Integer.parseInt(matcher.group("y"));
        } catch (NumberFormatException exception) {
            return null;
        }
        if (!isInsideBoard(game, x, y)) {
            return null;
        }
        return new EntityPosition(x, y);
    }

    private static Game getCurrentGame() {
        Menu currentMenu = App.getInstance().getCurrentMenu();
        if (!(currentMenu instanceof GameMenu)) {
            return null;
        }
        return ((GameMenu) currentMenu).getGame();
    }
}
