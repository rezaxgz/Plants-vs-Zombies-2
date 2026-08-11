package io.github.Plants_Vs_Zombies_2.model.game.minigame;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * One continuously rolling Wall-nut in the Wall-nut Bowling minigame.
 */
public final class BowlingWallnut {
    private static final double DIAGONAL_COMPONENT = Math.sqrt(0.5);

    private final long id;
    private final BowlingWallnutType type;
    private final Set<Zombie> hitZombies = Collections.newSetFromMap(
            new IdentityHashMap<>());

    private double rowPosition;
    private double columnPosition;
    private int verticalDirection;
    private int turnCount;
    private boolean removed;

    public BowlingWallnut(long id, BowlingWallnutType type,
            EntityPosition startingPosition) {
        if (id <= 0 || type == null || startingPosition == null) {
            throw new IllegalArgumentException(
                    "bowling Wall-nut values are invalid");
        }
        this.id = id;
        this.type = type;
        rowPosition = startingPosition.getRow();
        columnPosition = startingPosition.getColumn();
    }

    /**
     * Advances the Wall-nut and returns the number of degrees turned at a
     * vertical edge, or zero when no edge was hit.
     */
    public int advance(double deltaSeconds, int rowCount) {
        validateAdvance(deltaSeconds, rowCount);
        if (removed) {
            return 0;
        }
        double speed = type.getSpeedTilesPerSecond();
        if (type != BowlingWallnutType.NORMAL
                || verticalDirection == 0) {
            columnPosition += speed * deltaSeconds;
        } else {
            double component = speed * DIAGONAL_COMPONENT;
            columnPosition += component * deltaSeconds;
            rowPosition += verticalDirection * component * deltaSeconds;
        }
        return resolveVerticalBoundary(rowCount);
    }

    private static void validateAdvance(double deltaSeconds, int rowCount) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0
                || rowCount <= 0) {
            throw new IllegalArgumentException(
                    "bowling Wall-nut update values are invalid");
        }
    }

    private int resolveVerticalBoundary(int rowCount) {
        if (type != BowlingWallnutType.NORMAL) {
            return 0;
        }
        double lastRow = rowCount - 1.0;
        if (rowPosition < 0.0) {
            rowPosition = -rowPosition;
            return turnInward(1);
        }
        if (rowPosition > lastRow) {
            rowPosition = 2.0 * lastRow - rowPosition;
            return turnInward(-1);
        }
        return 0;
    }

    private int turnInward(int direction) {
        int degrees = turnCount == 0 ? 45 : 90;
        verticalDirection = direction;
        turnCount++;
        return degrees;
    }

    public int turnAfterZombieImpact(int rowCount, Random random) {
        if (type != BowlingWallnutType.NORMAL || random == null
                || rowCount <= 0) {
            return 0;
        }
        if (turnCount == 0) {
            verticalDirection = chooseFirstDirection(rowCount, random);
            turnCount++;
            return 45;
        }
        verticalDirection = verticalDirection == 0
                ? chooseFirstDirection(rowCount, random)
                : -verticalDirection;
        turnCount++;
        return 90;
    }

    private int chooseFirstDirection(int rowCount, Random random) {
        if (rowPosition <= 0.25) {
            return 1;
        }
        if (rowPosition >= rowCount - 1.25) {
            return -1;
        }
        return random.nextBoolean() ? 1 : -1;
    }

    public boolean hasHit(Zombie zombie) {
        return hitZombies.contains(zombie);
    }

    public void recordHit(Zombie zombie) {
        if (zombie != null) {
            hitZombies.add(zombie);
        }
    }

    public long getId() {
        return id;
    }

    public BowlingWallnutType getType() {
        return type;
    }

    public double getRowPosition() {
        return rowPosition;
    }

    public double getColumnPosition() {
        return columnPosition;
    }

    public int getVerticalDirection() {
        return verticalDirection;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void markForRemoval() {
        removed = true;
    }

    public String getDirectionDescription() {
        if (verticalDirection < 0) {
            return "up-right";
        }
        if (verticalDirection > 0) {
            return "down-right";
        }
        return "right";
    }
}
