package io.github.some_example_name.view.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.entities.zombies.Zombie;
import io.github.some_example_name.model.game.entities.zombies.armor.Armor;

/**
 * Builds zombie-related text shown by game commands and game results.
 */
public final class ZombieView {
    private ZombieView() {
    }

    public static void appendExactPositions(
            StringBuilder output, Board board) {
        List<Zombie> zombies = board.getZombies();
        output.append("exact zombie positions:");
        if (zombies.isEmpty()) {
            output.append(" none");
            return;
        }
        for (Zombie zombie : zombies) {
            output.append(System.lineSeparator())
                    .append("- ")
                    .append(zombie.getName())
                    .append(" at (")
                    .append(zombie.getLane())
                    .append(", ")
                    .append(String.format(
                            Locale.ROOT,
                            "%.2f",
                            zombie.getColumnPosition()))
                    .append(')');
        }
    }

    public static void appendDetails(
            StringBuilder output,
            List<Zombie> zombies) {
        output.append("zombies:");
        if (zombies.isEmpty()) {
            output.append(" none")
                    .append(System.lineSeparator());
            return;
        }
        output.append(System.lineSeparator());
        for (Zombie zombie : zombies) {
            output.append("- ")
                    .append(zombie.getName())
                    .append(" | type: ")
                    .append(zombie.getType())
                    .append(" | exact position: (")
                    .append(zombie.getLane())
                    .append(", ")
                    .append(String.format(
                            Locale.ROOT,
                            "%.2f",
                            zombie.getColumnPosition()))
                    .append(") | hp: ")
                    .append(zombie.getHitPoints())
                    .append('/')
                    .append(zombie.getMaximumHitPoints())
                    .append(" | wave: ")
                    .append(zombie.getWaveNumber())
                    .append(" | armor: ")
                    .append(armorState(zombie))
                    .append(" | effects: ")
                    .append(zombieEffects(zombie))
                    .append(System.lineSeparator());
        }
    }

    private static String armorState(Zombie zombie) {
        Armor armor = zombie.getArmor();
        if (armor == null || armor.isDestroyed()) {
            return "none";
        }
        return armor.getType().getDisplayName()
                + " " + armor.getCurrentHealth()
                + "/" + armor.getMaximumHealth();
    }

    private static String zombieEffects(Zombie zombie) {
        List<String> effects = new ArrayList<>();
        if (zombie.isEncasedInIce()) {
            effects.add("encased in ice "
                    + zombie.getFrozenShellHitPoints()
                    + "/"
                    + zombie.getFrozenShellMaximumHitPoints()
                    + " HP");
        }
        if (zombie.isFrozen()) {
            effects.add("frozen "
                    + formatSeconds(
                            zombie.getFrozenDuration()));
        }
        if (zombie.isChilled()) {
            effects.add("chilled "
                    + formatSeconds(
                            zombie.getChilledDuration()));
        }
        if (zombie.isStunned()) {
            effects.add("stunned "
                    + formatSeconds(
                            zombie.getStunnedDuration()));
        }
        if (zombie.isHypnotized()) {
            effects.add("hypnotized");
        }
        if (zombie.isGlowing()) {
            effects.add("glowing");
        }
        if (zombie.getPoisonDurationSeconds() > 0.0) {
            effects.add("poisoned "
                    + formatSeconds(
                            zombie
                                    .getPoisonDurationSeconds()));
        }
        return effects.isEmpty()
                ? "none"
                : String.join(", ", effects);
    }

    public static List<Zombie> getZombiesAt(
            Board board, int row, int column) {
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : board.getZombies()) {
            if (zombie.getLane() != row) {
                continue;
            }
            int zombieColumn = (int) Math.floor(
                    zombie.getColumnPosition());
            if (zombieColumn == column) {
                result.add(zombie);
            }
        }
        return result;
    }

    public static String buildSpawnMessage(
            Zombie zombie, int tornadoAdvance) {
        if (tornadoAdvance > 0) {
            return "Zombie " + zombie.getName()
                    + " arrived by tornado at wave "
                    + zombie.getWaveNumber() + " in lane "
                    + zombie.getLane() + ", " + tornadoAdvance
                    + " columns ahead, which costed "
                    + zombie.getType().getWavePointCost() + ".";
        }
        return "Zombie " + zombie.getName() + " spawned at wave "
                + zombie.getWaveNumber() + " in lane " + zombie.getLane()
                + " which costed " + zombie.getType().getWavePointCost() + ".";
    }

    public static String buildDeathMessage(Zombie zombie) {
        return "Zombie of type " + zombie.getName() + " is dead at ("
                + formatColumn(zombie.getColumnPosition()) + ", "
                + zombie.getLane() + ")";
    }

    private static String formatColumn(double column) {
        return String.format(Locale.ROOT, "%.2f", column);
    }

    private static String formatSeconds(double seconds) {
        return String.format(Locale.ROOT, "%.1fs", seconds);
    }
}
