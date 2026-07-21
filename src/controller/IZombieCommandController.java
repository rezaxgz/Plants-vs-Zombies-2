package controller;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.game.entities.EntityPosition;
import model.game.minigame.IZombie;
import model.game.minigame.IZombieCard;
import model.game.minigame.IZombiePlacementResult;
import model.menu.GameMenu;
import model.menu.Menu;

/**
 * Game-menu commands for the I, Zombie reverse-defense minigame.
 */
public final class IZombieCommandController {
    private IZombieCommandController() {
    }

    public static CommandResult handleShowStatus(Matcher matcher) {
        IZombie game = getCurrentGame();
        if (game == null) {
            return CommandResult.error(
                    "I, Zombie is not active!");
        }
        List<String> pending = game.drainResults();
        StringBuilder output = new StringBuilder(
                "I, Zombie status");
        output.append(System.lineSeparator())
                .append("level: ")
                .append(game.getLevel().getNumber())
                .append(" - ").append(game.getLevel().getName())
                .append(System.lineSeparator())
                .append("sun: ").append(game.getSunCount())
                .append(System.lineSeparator())
                .append("red line after column: ")
                .append(game.getRedLineColumn())
                .append(System.lineSeparator())
                .append("brains eaten: ")
                .append(game.getEatenBrainCount()).append('/5')
                .append(System.lineSeparator())
                .append("sun producers alive: ")
                .append(game.getLivingSunProducerCount()).append('/5')
                .append(System.lineSeparator())
                .append("sun-production interval: ")
                .append(String.format(Locale.ROOT, "%.1fs",
                        game.getCurrentProductionIntervalSeconds()))
                .append(System.lineSeparator())
                .append("remaining plants: ")
                .append(game.getRemainingPlantCount());
        appendBrains(output, game);
        return CommandResult.success(output.toString())
                .addPreCommandResults(pending);
    }

    private static void appendBrains(
            StringBuilder output, IZombie game) {
        output.append(System.lineSeparator()).append("brains:");
        for (int row = 0;
                row < game.getBoard().getNumberOfRows(); row++) {
            output.append(System.lineSeparator())
                    .append("- row ").append(row).append(": ")
                    .append(game.isBrainAvailable(row)
                            ? "available" : "eaten");
        }
    }

    public static CommandResult handleShowCards(Matcher matcher) {
        IZombie game = getCurrentGame();
        if (game == null) {
            return CommandResult.error(
                    "I, Zombie is not active!");
        }
        List<String> pending = game.drainResults();
        StringBuilder output = new StringBuilder(
                "I, Zombie zombie cards");
        for (IZombieCard card :
                game.getLevel().getZombieCards()) {
            output.append(System.lineSeparator())
                    .append("- ").append(card.getType().name())
                    .append(" | alias: ")
                    .append(card.getType().getAlias())
                    .append(" | cost: ").append(card.getCost())
                    .append(" | affordable: ")
                    .append(game.getSunCount() >= card.getCost()
                            ? "yes" : "no");
        }
        return CommandResult.success(output.toString())
                .addPreCommandResults(pending);
    }

    public static CommandResult handlePlaceZombie(Matcher matcher) {
        IZombie game = getCurrentGame();
        if (game == null) {
            return CommandResult.error(
                    "I, Zombie is not active!");
        }
        List<String> pending = game.drainResults();
        int row;
        int column;
        try {
            row = Integer.parseInt(matcher.group("x"));
            column = Integer.parseInt(matcher.group("y"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("zombie location is invalid!")
                    .addPreCommandResults(pending);
        }

        String type = matcher.group("type").trim();
        IZombiePlacementResult result = game.placeZombie(
                type, new EntityPosition(row, column));
        return placementResult(result, type, row, column)
                .addPreCommandResults(pending)
                .addPostCommandResults(game.drainResults());
    }

    private static CommandResult placementResult(
            IZombiePlacementResult result, String type,
            int row, int column) {
        switch (result) {
            case SUCCESS:
                return CommandResult.success(
                        "placed " + type + " at ("
                                + row + ", " + column + ")");
            case GAME_NOT_ACTIVE:
                return CommandResult.error("the game is not active!");
            case UNKNOWN_ZOMBIE:
                return CommandResult.error(
                        "that zombie is not one of this level's five cards!");
            case BOSS_NOT_ALLOWED:
                return CommandResult.error(
                        "Zomboss enemies are not enabled in levels yet!");
            case NOT_ENOUGH_SUN:
                return CommandResult.error("not enough zombie sun!");
            case INVALID_POSITION:
                return CommandResult.error(
                        "zombie location is outside the board!");
            case LEFT_OF_RED_LINE:
                return CommandResult.error(
                        "zombies must be placed to the right of the red line!");
            case POSITION_OCCUPIED:
                return CommandResult.error(
                        "another zombie already occupies that position!");
            default:
                return CommandResult.error("zombie placement failed!");
        }
    }

    private static IZombie getCurrentGame() {
        Menu menu = App.getInstance().getCurrentMenu();
        if (!(menu instanceof GameMenu)) {
            return null;
        }
        if (!(((GameMenu) menu).getGame()
                instanceof IZombie)) {
            return null;
        }
        return (IZombie) ((GameMenu) menu).getGame();
    }
}
