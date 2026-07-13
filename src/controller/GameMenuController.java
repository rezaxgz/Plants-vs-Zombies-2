package controller;

import java.util.List;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.game.Game;
import model.game.PlantPlacementResult;
import model.game.entities.EntityPosition;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantFactory;
import model.menu.GameMenu;
import model.menu.Menu;

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
        return CommandResult.success("time advanced by " + tickCount + " ticks").addPreCommandResults(preCommandResults)
                .addPostCommandResults(game.drainResults());
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

        boolean hasDroppingSun = game.getBoard().getSunsAt(new EntityPosition(x, y)).stream()
                .anyMatch(sun -> sun.isDropping());
        int collectedAmount = game.collectSunAt(x, y);
        if (collectedAmount <= 0) {
            String message = hasDroppingSun
                    ? "sun at (" + x + ", " + y + ") has not reached the ground yet!"
                    : "there is no sun at (" + x + ", " + y + ")";
            return CommandResult.error(message).addPreCommandResults(preCommandResults);
        }

        return CommandResult.success("collected " + collectedAmount + " sun at (" + x + ", " + y + ")")
                .addPreCommandResults(preCommandResults).addPostCommandResults(game.drainResults());
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

    public static CommandResult handlePlant(Matcher matcher) {
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
        String requestedType = matcher.group("type").trim();
        BasePlant plant = PlantFactory.createPlant(requestedType, position);
        if (plant == null) {
            return CommandResult.error(
                    "plant type '" + requestedType + "' does not exist or is not implemented!")
                    .addPreCommandResults(preCommandResults);
        }

        PlantPlacementResult placementResult = game.plant(plant);
        switch (placementResult) {
            case NOT_ENOUGH_SUN:
                return CommandResult.error("not enough suns to plant " + plant.getName() + "! required: "
                        + plant.getCost() + ", available: " + game.getSunCount())
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

    private static boolean isInsideBoard(Game game, int x, int y) {
        return x >= 0 && y >= 0 && x < game.getBoard().getNumberOfRows()
                && y < game.getBoard().getNumberOfColumns();
    }

    private static Game getCurrentGame() {
        Menu currentMenu = App.getInstance().getCurrentMenu();
        if (!(currentMenu instanceof GameMenu)) {
            return null;
        }
        return ((GameMenu) currentMenu).getGame();
    }
}
