package io.github.some_example_name.model.game.entities.zombies.abilities;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.zombies.Zombie;
import io.github.some_example_name.model.game.tile.Tile;
import io.github.some_example_name.model.game.tile.TileType;

/**
 * Fast Swimmer uses its configured zombie speed in water and moves at forty
 * percent of that speed on dry land.
 */
public class FastSwimAbility extends ZombieAbility {
    private static final double LAND_SPEED_SCALE = 0.4;

    private boolean inWater;

    public FastSwimAbility() {
        super(0.0);
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (zombie == null || board == null || zombie.isDead()) {
            inWater = false;
            return false;
        }

        int column = Math.max(0, Math.min(
                board.getNumberOfColumns() - 1,
                (int) Math.floor(zombie.getColumnPosition())));
        Tile tile = board.getTileAt(
                new EntityPosition(zombie.getLane(), column));
        inWater = tile != null
                && tile.getTileType() == TileType.WATER;
        return inWater;
    }

    public double getEffectiveSpeed(double baseSpeed) {
        return inWater ? baseSpeed : baseSpeed * LAND_SPEED_SCALE;
    }

    public boolean isInWater() {
        return inWater;
    }

    public double getLandSpeedScale() {
        return LAND_SPEED_SCALE;
    }
}
