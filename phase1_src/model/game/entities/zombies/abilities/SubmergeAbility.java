package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.EntityPosition;
import model.game.entities.plants.BasePlant;
import model.game.entities.zombies.Zombie;
import model.game.tile.Tile;
import model.game.tile.TileType;

/**
 * Snorkel Zombie submerges while travelling through water. It surfaces when
 * it reaches a plant so that the plant can be eaten and can attack it back.
 */
public class SubmergeAbility extends ZombieAbility {
    private static final double SURFACE_DISTANCE_TILES =
            Zombie.ATTACK_REACH + 0.05;

    public SubmergeAbility() {
        super(0.0);
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (zombie == null || board == null
                || zombie.isDead() || zombie.isHypnotized()) {
            return false;
        }
        boolean inWater = isInWater(zombie, board);
        zombie.setSubmerged(inWater);
        return inWater;
    }

    public void updateState(Zombie zombie, Board board,
            BasePlant blockingPlant) {
        if (zombie == null || board == null
                || zombie.isDead() || zombie.isHypnotized()) {
            if (zombie != null) {
                zombie.setSubmerged(false);
            }
            return;
        }

        boolean inWater = isInWater(zombie, board);
        boolean mustSurface = inWater
                && isCloseEnoughToEat(zombie, blockingPlant);
        zombie.setSubmerged(inWater && !mustSurface);
    }

    private boolean isCloseEnoughToEat(Zombie zombie,
            BasePlant plant) {
        if (plant == null || plant.getEntityPosition() == null
                || plant.getEntityPosition().getRow() != zombie.getLane()) {
            return false;
        }
        double distance = zombie.getColumnPosition()
                - plant.getEntityPosition().getColumn();
        return distance >= 0.0
                && distance <= SURFACE_DISTANCE_TILES;
    }

    private boolean isInWater(Zombie zombie, Board board) {
        int column = Math.max(0, Math.min(
                board.getNumberOfColumns() - 1,
                (int) Math.floor(zombie.getColumnPosition())));
        Tile tile = board.getTileAt(
                new EntityPosition(zombie.getLane(), column));
        return tile != null && tile.getTileType() == TileType.WATER;
    }
}
