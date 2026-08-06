package io.github.some_example_name.model.game.entities.zombies.abilities;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.entities.zombies.Zombie;
import io.github.some_example_name.model.game.entities.zombies.ZombieType;

/**
 * Gargantuar ability that throws one Imp when its health reaches the configured
 * threshold.
 */
public class ImpThrowAbility extends ZombieAbility {
    private static final double THIRD_COLUMN_INDEX = 2.0;

    private final double healthThreshold;
    private final String impType;
    private boolean thrown;
    private Zombie spawnedImp;

    public ImpThrowAbility(double healthThreshold, String impType) {
        super(0.0);
        this.healthThreshold = healthThreshold;
        this.impType = impType;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (thrown || zombie == null || board == null || zombie.isDead()) {
            return false;
        }

        double healthPercent = (double) zombie.getHitPoints()
                / zombie.getMaximumHitPoints();
        if (healthPercent > healthThreshold) {
            return false;
        }

        ZombieType resolvedImpType = ZombieType.findByName(impType);
        if (resolvedImpType == null) {
            return false;
        }

        double targetColumn = Math.min(THIRD_COLUMN_INDEX,
                board.getNumberOfColumns() - 1.0);
        spawnedImp = new Zombie(resolvedImpType, zombie.getWaveNumber(),
                zombie.getLane(), targetColumn);
        board.addZombie(spawnedImp);
        thrown = true;
        return true;
    }

    public boolean hasThrown() {
        return thrown;
    }

    public String getImpType() {
        return impType;
    }

    public double getHealthThreshold() {
        return healthThreshold;
    }

    public Zombie getSpawnedImp() {
        return spawnedImp;
    }
}
