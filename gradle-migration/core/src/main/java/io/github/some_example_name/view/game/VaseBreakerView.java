package io.github.some_example_name.view.game;

import java.util.Locale;

import io.github.some_example_name.model.game.structure.Vase;

/**
 * Builds Vase Breaker result text.
 */
public final class VaseBreakerView {
    private VaseBreakerView() {
    }

    public static String formatBrokenVase(Vase vase) {
        return vase.getType().getDisplayName() + " at "
                + vase.getPosition() + " broke.";
    }

    public static String formatSeconds(float seconds) {
        return String.format(Locale.ROOT, "%.1f", seconds);
    }
}
