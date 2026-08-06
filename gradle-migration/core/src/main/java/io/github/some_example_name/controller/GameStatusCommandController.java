package io.github.some_example_name.controller;

import java.util.List;
import java.util.regex.Matcher;

import io.github.some_example_name.model.App;
import io.github.some_example_name.model.CommandResult;
import io.github.some_example_name.model.game.Game;
import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.menu.GameMenu;
import io.github.some_example_name.model.menu.Menu;
import io.github.some_example_name.view.game.GameStatusView;

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
                GameStatusView.formatMap(game))
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
                GameStatusView.formatPlantStatuses(game))
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

        EntityPosition position = new EntityPosition(row, column);
        if (!game.getBoard().isPositionInsideBoard(position)) {
            return CommandResult.error(
                    "tile location is outside the board!")
                    .addPreCommandResults(pendingResults);
        }

        return CommandResult.success(
                GameStatusView.formatTileStatus(
                        game, position))
                .addPreCommandResults(pendingResults);
    }

    private static Game getCurrentGame() {
        Menu currentMenu = App.getInstance().getCurrentMenu();
        if (!(currentMenu instanceof GameMenu)) {
            return null;
        }
        return ((GameMenu) currentMenu).getGame();
    }
}
