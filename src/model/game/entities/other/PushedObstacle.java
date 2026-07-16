package model.game.entities.other;

import model.game.entities.Entity;
import model.game.entities.EntityPosition;

/**
 * Shared movable obstacle pushed in front of a zombie. Direct projectiles hit
 * the obstacle before zombies behind it, while zombies can pass through it.
 */
public abstract class PushedObstacle extends Entity {
    public static final double COLLISION_RADIUS_TILES = 0.42;

    private final String displayName;
    private final int maximumHitPoints;

    private int currentHitPoints;
    private int lane;
    private double columnPosition;

    protected PushedObstacle(String displayName,
            int maximumHitPoints, int lane,
            double columnPosition) {
        super(toEntityPosition(lane, columnPosition));
        if (displayName == null || displayName.isBlank()
                || maximumHitPoints <= 0 || lane < 0
                || !Double.isFinite(columnPosition)
                || columnPosition < 0.0) {
            throw new IllegalArgumentException(
                    "pushed-obstacle values are invalid");
        }
        this.displayName = displayName;
        this.maximumHitPoints = maximumHitPoints;
        this.currentHitPoints = maximumHitPoints;
        this.lane = lane;
        this.columnPosition = columnPosition;
    }

    public final void moveTo(int newLane,
            double newColumnPosition, int boardColumnCount) {
        if (newLane < 0 || boardColumnCount <= 0
                || !Double.isFinite(newColumnPosition)) {
            throw new IllegalArgumentException(
                    "pushed-obstacle movement is invalid");
        }
        lane = newLane;
        columnPosition = Math.max(0.0,
                Math.min(boardColumnCount - 0.001,
                        newColumnPosition));
        setEntityPosition(toEntityPosition(
                lane, columnPosition));
    }

    public final void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException(
                    "damage cannot be negative");
        }
        if (isDestroyed() || damage == 0) {
            return;
        }
        currentHitPoints = Math.max(
                0, currentHitPoints - damage);
        if (currentHitPoints == 0) {
            markForRemoval();
        }
    }

    public final boolean isDestroyed() {
        return currentHitPoints <= 0 || isRemoved();
    }

    public final String getDisplayName() {
        return displayName;
    }

    public final int getCurrentHitPoints() {
        return currentHitPoints;
    }

    public final int getMaximumHitPoints() {
        return maximumHitPoints;
    }

    public final int getLane() {
        return lane;
    }

    public final double getColumnPosition() {
        return columnPosition;
    }

    private static EntityPosition toEntityPosition(
            int lane, double columnPosition) {
        return new EntityPosition(
                Math.max(0, lane),
                Math.max(0,
                        (int) Math.floor(columnPosition)));
    }
}
