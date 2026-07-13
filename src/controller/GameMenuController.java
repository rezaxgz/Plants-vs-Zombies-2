package controller;

import java.util.List;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.game.Game;
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

        if (x >= game.getBoard().getNumberOfRows() || y >= game.getBoard().getNumberOfColumns()) {
            return CommandResult.error("sun location is outside the board!").addPreCommandResults(preCommandResults);
        }

        int collectedAmount = game.collectSunAt(x, y);
        if (collectedAmount <= 0) {
            return CommandResult.error("there is no sun at (" + x + ", " + y + ")")
                    .addPreCommandResults(preCommandResults);
        }

        return CommandResult.success("collected " + collectedAmount + " sun at (" + x + ", " + y + ")")
                .addPreCommandResults(preCommandResults).addPostCommandResults(game.drainResults());
    }

    private static Game getCurrentGame() {
        Menu currentMenu = App.getInstance().getCurrentMenu();
        if (!(currentMenu instanceof GameMenu)) {
            return null;
        }
        return ((GameMenu) currentMenu).getGame();
    }
}
