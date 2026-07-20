package controller;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.game.Game;
import model.game.entities.EntityPosition;
import model.game.entities.other.VaseSeedPacket;
import model.game.minigame.VaseBreakResult;
import model.game.minigame.VaseBreaker;
import model.game.minigame.VaseSeedPlantingResult;
import model.game.structure.Vase;
import model.menu.GameMenu;
import model.menu.Menu;

/**
 * Commands that are only meaningful while Vase Breaker is active.
 */
public final class VaseBreakerCommandController {
    private VaseBreakerCommandController() {
    }

    public static CommandResult handleShowVases(Matcher matcher) {
        VaseBreaker game = getVaseBreaker();
        if (game == null) {
            return CommandResult.error("Vase Breaker is not active!");
        }
        List<String> pending = game.drainResults();
        List<Vase> vases = game.getVases().stream()
                .sorted(Comparator.comparingInt(
                        (Vase vase) -> vase.getPosition().getRow())
                        .thenComparingInt(vase ->
                                vase.getPosition().getColumn()))
                .toList();
        StringBuilder output = new StringBuilder("vases remaining: ")
                .append(vases.size());
        if (vases.isEmpty()) {
            output.append(System.lineSeparator()).append("- none");
        } else {
            for (Vase vase : vases) {
                output.append(System.lineSeparator()).append("- ")
                        .append(vase.getType().getDisplayName())
                        .append(" [")
                        .append(vase.getType().getMapSymbol())
                        .append("] at ").append(vase.getPosition());
            }
        }
        return CommandResult.success(output.toString())
                .addPreCommandResults(pending);
    }

    public static CommandResult handleShowSeeds(Matcher matcher) {
        VaseBreaker game = getVaseBreaker();
        if (game == null) {
            return CommandResult.error("Vase Breaker is not active!");
        }
        List<String> pending = game.drainResults();
        List<VaseSeedPacket> packets = game.getAvailableSeedPackets();
        StringBuilder output = new StringBuilder("vase seed packets: ")
                .append(packets.size());
        if (packets.isEmpty()) {
            output.append(System.lineSeparator()).append("- none");
        } else {
            for (VaseSeedPacket packet : packets) {
                output.append(System.lineSeparator()).append("- ")
                        .append(packet.getPlantType()).append(" at ")
                        .append(packet.getEntityPosition())
                        .append(" | disappears in ")
                        .append(String.format(Locale.ROOT, "%.1fs",
                                packet.getRemainingSeconds()));
            }
        }
        return CommandResult.success(output.toString())
                .addPreCommandResults(pending);
    }

    public static CommandResult handleBreakVase(Matcher matcher) {
        VaseBreaker game = getVaseBreaker();
        if (game == null) {
            return CommandResult.error("Vase Breaker is not active!");
        }
        List<String> pending = game.drainResults();
        EntityPosition position = parsePosition(game, matcher, "x", "y");
        if (position == null) {
            return CommandResult.error("vase location is invalid!")
                    .addPreCommandResults(pending);
        }

        VaseBreakResult result = game.breakVase(position);
        CommandResult commandResult = breakResult(result, position)
                .addPreCommandResults(pending)
                .addPostCommandResults(game.drainResults());
        if (result.name().startsWith("SUCCESS")) {
            UserManager.saveAllUsers();
            commandResult.addPostCommandResults(synchronizeProgress());
        }
        return commandResult;
    }

    private static CommandResult breakResult(VaseBreakResult result,
            EntityPosition position) {
        switch (result) {
            case SUCCESS_EMPTY:
            case SUCCESS_SEED_PACKET:
            case SUCCESS_ZOMBIE:
                return CommandResult.success("broke vase at " + position);
            case GAME_NOT_ACTIVE:
                return CommandResult.error("the Vase Breaker game is over!");
            case INVALID_POSITION:
                return CommandResult.error(
                        "vase location is outside the board!");
            case NO_VASE:
                return CommandResult.error(
                        "there is no unbroken vase at " + position + "!");
            default:
                throw new IllegalStateException(
                        "unknown vase break result: " + result);
        }
    }

    public static CommandResult handlePlantSeed(Matcher matcher) {
        VaseBreaker game = getVaseBreaker();
        if (game == null) {
            return CommandResult.error("Vase Breaker is not active!");
        }
        List<String> pending = game.drainResults();
        EntityPosition source = parsePosition(game, matcher, "sx", "sy");
        EntityPosition destination = parsePosition(game, matcher, "x", "y");
        if (source == null) {
            return CommandResult.error("seed packet location is invalid!")
                    .addPreCommandResults(pending);
        }
        if (destination == null) {
            return CommandResult.error("plant destination is invalid!")
                    .addPreCommandResults(pending);
        }

        VaseSeedPacket packet = game.getSeedPacketAt(source);
        String plantType = packet == null ? "plant" : packet.getPlantType();
        VaseSeedPlantingResult result = game.plantFromSeed(source, destination);
        return seedPlantingResult(result, source, destination, plantType)
                .addPreCommandResults(pending)
                .addPostCommandResults(game.drainResults())
                .addPostCommandResults(synchronizeProgress());
    }

    private static CommandResult seedPlantingResult(
            VaseSeedPlantingResult result, EntityPosition source,
            EntityPosition destination, String plantType) {
        switch (result) {
            case SUCCESS:
                return CommandResult.success("planted " + plantType
                        + " from vase seed at " + source + " to "
                        + destination);
            case GAME_NOT_ACTIVE:
                return CommandResult.error("the Vase Breaker game is over!");
            case INVALID_SOURCE:
                return CommandResult.error(
                        "seed packet location is outside the board!");
            case NO_SEED_PACKET:
                return CommandResult.error(
                        "there is no usable vase seed packet at "
                                + source + "!");
            case INVALID_DESTINATION:
                return CommandResult.error(
                        "plant destination is outside the board!");
            case DESTINATION_BLOCKED:
                return CommandResult.error("cannot plant at "
                        + destination + "; the tile is blocked or occupied!");
            case UNKNOWN_PLANT:
                return CommandResult.error(
                        "the seed packet contains an unknown plant!");
            default:
                throw new IllegalStateException(
                        "unknown vase seed planting result: " + result);
        }
    }

    private static EntityPosition parsePosition(VaseBreaker game,
            Matcher matcher, String rowGroup, String columnGroup) {
        try {
            int row = Integer.parseInt(matcher.group(rowGroup));
            int column = Integer.parseInt(matcher.group(columnGroup));
            EntityPosition position = new EntityPosition(row, column);
            return game.getBoard().isPositionInsideBoard(position)
                    ? position : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static List<String> synchronizeProgress() {
        Menu menu = App.getInstance().getCurrentMenu();
        if (!(menu instanceof GameMenu)) {
            return List.of();
        }
        GameMenu gameMenu = (GameMenu) menu;
        gameMenu.synchronizeProgress();
        return gameMenu.drainProgressResults();
    }

    private static VaseBreaker getVaseBreaker() {
        Menu menu = App.getInstance().getCurrentMenu();
        if (!(menu instanceof GameMenu)) {
            return null;
        }
        Game game = ((GameMenu) menu).getGame();
        return game instanceof VaseBreaker ? (VaseBreaker) game : null;
    }
}
