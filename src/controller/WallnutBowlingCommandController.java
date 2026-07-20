package controller;

import java.util.List;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.game.Game;
import model.game.minigame.BowlingWallnut;
import model.game.minigame.WallnutBowling;
import model.menu.GameMenu;
import model.menu.Menu;

/**
 * Commands that expose the moving pieces of Wall-nut Bowling.
 */
public final class WallnutBowlingCommandController {
    private WallnutBowlingCommandController() {
    }

    public static CommandResult handleShowRollingWallnuts(Matcher matcher) {
        WallnutBowling game = getCurrentWallnutBowling();
        if (game == null) {
            return CommandResult.error(
                    "Wall-nut Bowling is not active!");
        }

        List<String> pending = game.drainResults();
        StringBuilder output = new StringBuilder(
                "Wall-nut Bowling status");
        output.append(System.lineSeparator())
                .append("red line: launch columns 0 through ")
                .append(game.getRedLineColumn())
                .append(System.lineSeparator())
                .append("rolling Wall-nuts: ")
                .append(game.getRollingWallnuts().size());
        for (BowlingWallnut wallnut : game.getRollingWallnuts()) {
            output.append(System.lineSeparator())
                    .append("- ")
                    .append(game.describeRollingWallnut(wallnut));
        }
        if (game.getRollingWallnuts().isEmpty()) {
            output.append(System.lineSeparator())
                    .append("- none");
        }
        return CommandResult.success(output.toString())
                .addPreCommandResults(pending);
    }

    private static WallnutBowling getCurrentWallnutBowling() {
        Menu menu = App.getInstance().getCurrentMenu();
        if (!(menu instanceof GameMenu)) {
            return null;
        }
        Game game = ((GameMenu) menu).getGame();
        return game instanceof WallnutBowling
                ? (WallnutBowling) game : null;
    }
}
