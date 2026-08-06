package io.github.some_example_name.view.game;

import java.util.List;

import io.github.some_example_name.model.game.entities.zombies.Zombie;

/**
 * Builds lawn-mower result text.
 */
public final class LawnMowerView {
    private LawnMowerView() {
    }

    public static String buildTriggerMessage(
            int row, List<Zombie> killed) {
        StringBuilder message = new StringBuilder();
        message.append("The lawn mower in the row ")
                .append(row)
                .append(" is triggered and killed these zombies:");

        if (killed.isEmpty()) {
            message.append(System.lineSeparator())
                    .append("- none");
            return message.toString();
        }

        for (Zombie zombie : killed) {
            message.append(System.lineSeparator())
                    .append("- ")
                    .append(zombie.getName());
        }
        return message.toString();
    }
}
