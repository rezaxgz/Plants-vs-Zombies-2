package controller;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.game.Game;
import model.menu.GameMenu;
import model.menu.Menu;

/**
 * Commands shared by Timed War, Night Ops, Dead Line,
 * Love Your Plants, and Plant What You Get.
 */
public final class SpecialLevelCommandController {
    private SpecialLevelCommandController() {
    }

    public static CommandResult
            handleShowSpecialLevelStatus(
                    Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error(
                    "game is not active!");
        }

        List<String> pending = game.drainResults();
        StringBuilder output =
                new StringBuilder(
                        "special level status");
        boolean found = false;

        if (game.hasTimedWar()) {
            found = true;
            output.append(System.lineSeparator())
                    .append("- Timed War: ")
                    .append(game.getTimedWarObjective())
                    .append(" ")
                    .append(game.getTimedWarProgress())
                    .append('/')
                    .append(game.getTimedWarTarget())
                    .append(" | remaining: ")
                    .append(formatSeconds(
                            game.getTimedWarRemainingSeconds()));
        }
        if (game.areSkySunsDisabled()) {
            found = true;
            output.append(System.lineSeparator())
                    .append("- sky suns: disabled");
            if (!game.getSkySunDisabledReason().isBlank()) {
                output.append(" | reason: ")
                        .append(game
                                .getSkySunDisabledReason());
            }
        }
        if (game.hasDeadLine()) {
            found = true;
            output.append(System.lineSeparator())
                    .append("- Dead Line column: ")
                    .append(String.format(
                            Locale.ROOT, "%.1f",
                            game.getDeadLineColumn()));
        }
        if (game.hasLoveYourPlants()) {
            found = true;
            output.append(System.lineSeparator())
                    .append("- lost plants: ")
                    .append(game.getLostPlantCount())
                    .append('/')
                    .append(game.getMaximumLostPlants());
        }
        if (game.hasPlantWhatYouGet()) {
            found = true;
            output.append(System.lineSeparator())
                    .append("- Plant What You Get: ")
                    .append(game.haveZombieWavesStarted()
                            ? "waves started"
                            : "setup phase");
        }

        if (!found) {
            output.append(System.lineSeparator())
                    .append("- no active special rule");
        }

        return CommandResult.success(output.toString())
                .addPreCommandResults(pending);
    }

    public static CommandResult handleStartZombieWaves(
            Matcher matcher) {
        Game game = getCurrentGame();
        if (game == null) {
            return CommandResult.error(
                    "game is not active!");
        }

        List<String> pending = game.drainResults();
        if (!game.hasPlantWhatYouGet()) {
            return CommandResult.error(
                    "this level does not have a setup phase!")
                    .addPreCommandResults(pending);
        }
        if (game.haveZombieWavesStarted()) {
            return CommandResult.error(
                    "zombie waves have already started!")
                    .addPreCommandResults(pending);
        }
        if (!game.startZombieWaves()) {
            return CommandResult.error(
                    "zombie waves could not be started!")
                    .addPreCommandResults(pending);
        }

        return CommandResult.success(
                "zombie waves started")
                .addPreCommandResults(pending)
                .addPostCommandResults(
                        game.drainResults());
    }

    private static String formatSeconds(
            double seconds) {
        return String.format(
                Locale.ROOT, "%.1fs", seconds);
    }

    private static Game getCurrentGame() {
        Menu menu = App.getInstance().getCurrentMenu();
        if (!(menu instanceof GameMenu)) {
            return null;
        }
        return ((GameMenu) menu).getGame();
    }
}
