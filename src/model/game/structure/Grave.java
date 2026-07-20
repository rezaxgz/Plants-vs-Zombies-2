package model.game.structure;

import model.game.entities.EntityPosition;
import model.game.tile.TileType;

public class Grave extends BaseStructure {
    public static final int DEFAULT_HIT_POINTS = 700;

    private final GraveReward reward;
    private final TileType underlyingTileType;
    private int hitPoints;

    public Grave() {
        this(null);
    }

    public Grave(EntityPosition position) {
        this(position, GraveReward.NONE, TileType.NORMAL);
    }

    public Grave(EntityPosition position, GraveReward reward,
            TileType underlyingTileType) {
        super(position);
        if (reward == null || underlyingTileType == null) {
            throw new IllegalArgumentException(
                    "grave reward and underlying terrain are required");
        }
        this.reward = reward;
        this.underlyingTileType = underlyingTileType;
        hitPoints = DEFAULT_HIT_POINTS;
    }

    public int getHitPoints() {
        return hitPoints;
    }

    public GraveReward getReward() {
        return reward;
    }

    public TileType getUnderlyingTileType() {
        return underlyingTileType;
    }

    public boolean isNecromancyGrave() {
        return underlyingTileType == TileType.NECROMANCY;
    }

    public void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        hitPoints = Math.max(0, hitPoints - damage);
        if (hitPoints == 0) {
            markForRemoval();
        }
    }
}
