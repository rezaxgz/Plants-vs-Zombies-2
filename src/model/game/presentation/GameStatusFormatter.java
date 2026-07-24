package model.game.presentation;

import model.game.Game;
import model.game.entities.EntityPosition;
import view.game.GameStatusView;

/**
 * Backward-compatible facade for the game status text view.
 */
public final class GameStatusFormatter {
    private GameStatusFormatter() {
    }

    public static String formatMap(Game game) {
        return GameStatusView.formatMap(game);
    }

    public static String formatPlantStatuses(Game game) {
        return GameStatusView.formatPlantStatuses(game);
    }

    public static String formatTileStatus(
            Game game, EntityPosition position) {
        return GameStatusView.formatTileStatus(game, position);
    }
}
