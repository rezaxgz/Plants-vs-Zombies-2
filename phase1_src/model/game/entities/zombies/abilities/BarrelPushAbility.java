package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.other.RollingBarrel;
import model.game.entities.zombies.Zombie;

/**
 * Creates and keeps a rolling barrel immediately in front of its pusher.
 */
public class BarrelPushAbility extends ZombieAbility {
    private static final double BARREL_OFFSET_TILES = 0.75;

    private final int impCount;

    private RollingBarrel barrel;
    private boolean spawnedThisUse;

    public BarrelPushAbility(int impCount) {
        super(0.0);
        if (impCount <= 0) {
            throw new IllegalArgumentException(
                    "impCount must be positive");
        }
        this.impCount = impCount;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        spawnedThisUse = false;
        if (zombie == null || board == null
                || zombie.isDead() || zombie.isHypnotized()) {
            return false;
        }

        if (barrel == null) {
            barrel = new RollingBarrel(
                    zombie.getLane(),
                    Math.max(0.0,
                            zombie.getColumnPosition()
                                    - BARREL_OFFSET_TILES),
                    zombie.getWaveNumber(), impCount);
            board.addEntity(barrel);
            spawnedThisUse = true;
        }
        if (barrel.isDestroyed()) {
            return false;
        }

        barrel.moveTo(zombie.getLane(),
                zombie.getColumnPosition()
                        - BARREL_OFFSET_TILES,
                board.getNumberOfColumns());
        return true;
    }

    public RollingBarrel getBarrel() {
        return barrel;
    }

    public boolean didSpawnThisUse() {
        return spawnedThisUse;
    }

    public int getImpCount() {
        return impCount;
    }
}
