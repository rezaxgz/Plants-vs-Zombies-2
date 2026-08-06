package io.github.some_example_name.model.game.entities.other;

import io.github.some_example_name.model.game.entities.zombies.armor.ArmorType;

/**
 * A movable ice block pushed by a Troglobite.
 */
public final class IceBlock extends PushedObstacle {
    public static final int DEFAULT_HIT_POINTS = ArmorType.ICE_BLOCK.getBaseHealth();
    public static final double COLLISION_RADIUS_TILES = PushedObstacle.COLLISION_RADIUS_TILES;

    private final int formationIndex;

    public IceBlock(int lane, double columnPosition,
            int formationIndex) {
        super("Troglobite ice block",
                DEFAULT_HIT_POINTS, lane, columnPosition);
        if (formationIndex < 0) {
            throw new IllegalArgumentException(
                    "formationIndex cannot be negative");
        }
        this.formationIndex = formationIndex;
    }

    public int getFormationIndex() {
        return formationIndex;
    }
}
