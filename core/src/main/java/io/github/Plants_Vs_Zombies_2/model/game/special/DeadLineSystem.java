package io.github.Plants_Vs_Zombies_2.model.game.special;

import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * Finds the first hostile zombie that reaches a vertical loss line.
 */
public final class DeadLineSystem implements java.io.Serializable {
    private static final double POSITION_EPSILON = 0.000001;

    private final double lineColumn;

    public DeadLineSystem(double lineColumn) {
        if (!Double.isFinite(lineColumn)
                || lineColumn < 0.0) {
            throw new IllegalArgumentException(
                    "Dead Line column is invalid");
        }
        this.lineColumn = lineColumn;
    }

    public Zombie findBreacher(
            List<Zombie> zombies) {
        if (zombies == null) {
            throw new IllegalArgumentException(
                    "zombies cannot be null");
        }
        for (Zombie zombie : zombies) {
            if (zombie != null
                    && !zombie.isDead()
                    && !zombie.isHypnotized()
                    && zombie.getColumnPosition() <= lineColumn
                            + POSITION_EPSILON) {
                return zombie;
            }
        }
        return null;
    }

    public double getLineColumn() {
        return lineColumn;
    }
}
