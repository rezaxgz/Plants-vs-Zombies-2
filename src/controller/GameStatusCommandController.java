package controller;

import java.util.List;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.game.Game;
import model.game.entities.EntityPosition;
import model.game.presentation.GameStatusFormatter;
import model.menu.GameMenu;
import model.menu.Menu;

/**
 * Read-only game commands required for map and status inspection.
 */
public final class GameStatusCommandController {
    private GameStatusCommandController() {
    }

    public static CommandResult handleShowMap(Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> pendingResults = game.drainResults();
        return CommandResult.success(
                GameStatusFormatter.formatMap(game))
                .addPreCommandResults(pendingResults);
    }

    public static CommandResult handleShowPlantsStatus(
            Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> pendingResults = game.drainResults();
        return CommandResult.success(
                GameStatusFormatter.formatPlantStatuses(game))
                .addPreCommandResults(pendingResults);
    }

    public static CommandResult handleShowTileStatus(
            Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error("game is not active!");
        }

        List<String> pendingResults = game.drainResults();
        int row;
        int column;
        try {
            row = Integer.parseInt(matcher.group("x"));
            column = Integer.parseInt(matcher.group("y"));
        } catch (NumberFormatException exception) {
            return CommandResult.error(
                    "tile location is invalid!")
                    .addPreCommandResults(pendingResults);
        }

        EntityPosition position =
                new EntityPosition(row, column);
        if (!game.getBoard().isPositionInsideBoard(position)) {
            return CommandResult.error(
                    "tile location is outside the board!")
                    .addPreCommandResults(pendingResults);
        }

        return CommandResult.success(
                GameStatusFormatter.formatTileStatus(
                        game, position))
                .addPreCommandResults(pendingResults);
    }

    private static Game getCurrentGame() {
        Menu currentMenu =
                App.getInstance().getCurrentMenu();
        if (!(currentMenu instanceof GameMenu)) {
            return null;
        }
        return ((GameMenu) currentMenu).getGame();
    }
}
