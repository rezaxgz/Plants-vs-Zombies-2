package model.game.entities.other;

import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.zombies.armor.ArmorType;

/**
 * A movable ice block pushed by a Troglobite. It absorbs direct projectile
 * damage and instantly crushes plants or hypnotized zombies it touches.
 */
public final class IceBlock extends Entity {
    public static final int DEFAULT_HIT_POINTS =
            ArmorType.ICE_BLOCK.getBaseHealth();
    public static final double COLLISION_RADIUS_TILES = 0.42;

    private final int formationIndex;
    private final int maximumHitPoints;
    private int currentHitPoints;
    private int lane;
    private double columnPosition;

    public IceBlock(int lane, double columnPosition, int formationIndex) {
        super(toEntityPosition(lane, columnPosition));
        if (lane < 0 || !Double.isFinite(columnPosition)
                || columnPosition < 0.0 || formationIndex < 0) {
            throw new IllegalArgumentException(
                    "ice block position and formation index are invalid");
        }
        this.lane = lane;
        this.columnPosition = columnPosition;
        this.formationIndex = formationIndex;
        this.maximumHitPoints = DEFAULT_HIT_POINTS;
        this.currentHitPoints = maximumHitPoints;
    }

    public void moveTo(int lane, double columnPosition,
            int boardColumnCount) {
        if (lane < 0 || boardColumnCount <= 0
                || !Double.isFinite(columnPosition)) {
            throw new IllegalArgumentException(
                    "ice block movement values are invalid");
        }
        this.lane = lane;
        this.columnPosition = Math.max(0.0,
                Math.min(boardColumnCount - 0.001, columnPosition));
        setEntityPosition(toEntityPosition(lane, this.columnPosition));
    }

    public void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        if (isDestroyed() || damage == 0) {
            return;
        }
        currentHitPoints = Math.max(0, currentHitPoints - damage);
        if (currentHitPoints == 0) {
            markForRemoval();
        }
    }

    public boolean isDestroyed() {
        return currentHitPoints <= 0 || isRemoved();
    }

    public int getCurrentHitPoints() {
        return currentHitPoints;
    }

    public int getMaximumHitPoints() {
        return maximumHitPoints;
    }

    public int getFormationIndex() {
        return formationIndex;
    }

    public int getLane() {
        return lane;
    }

    public double getColumnPosition() {
        return columnPosition;
    }

    private static EntityPosition toEntityPosition(
            int lane, double columnPosition) {
        return new EntityPosition(
                Math.max(0, lane),
                Math.max(0, (int) Math.floor(columnPosition)));
    }
}
