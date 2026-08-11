package view.game;

import java.util.Locale;

import model.game.minigame.BowlingWallnut;

/**
 * Builds Wall-nut Bowling result and status text.
 */
public final class WallnutBowlingView {
    private WallnutBowlingView() {
    }

    public static String formatWallnut(BowlingWallnut wallnut) {
        return wallnut.getType().getDisplayName() + " #"
                + wallnut.getId();
    }

    public static String describeRollingWallnut(
            BowlingWallnut wallnut) {
        return wallnut.getType().getDisplayName() + " #" + wallnut.getId()
                + " | position: ("
                + String.format(Locale.ROOT, "%.2f",
                        wallnut.getRowPosition())
                + ", " + String.format(Locale.ROOT, "%.2f",
                        wallnut.getColumnPosition())
                + ") | direction: " + wallnut.getDirectionDescription()
                + " | turns: " + wallnut.getTurnCount();
    }
}
