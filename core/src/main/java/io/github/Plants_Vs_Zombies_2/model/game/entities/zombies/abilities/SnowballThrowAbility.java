package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * Hunter throws a three-snowball barrage at the nearest plant in its lane.
 * Three accumulated ice hits freeze the plant.
 */
public class SnowballThrowAbility extends ZombieAbility {
    private final int snowballsPerBarrage;
    private final double farRange;
    private final double nearRange;

    private BasePlant lastTarget;
    private int lastSnowballCount;
    private boolean lastBarrageFrozeTarget;

    public SnowballThrowAbility(int snowballsPerBarrage,
            double farRange, double nearRange) {
        super(4.0);
        if (snowballsPerBarrage <= 0
                || !Double.isFinite(farRange)
                || !Double.isFinite(nearRange)
                || farRange <= 0.0
                || nearRange < 0.0
                || nearRange > farRange) {
            throw new IllegalArgumentException(
                    "invalid snowball barrage configuration");
        }
        this.snowballsPerBarrage = snowballsPerBarrage;
        this.farRange = farRange;
        this.nearRange = nearRange;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        lastTarget = null;
        lastSnowballCount = 0;
        lastBarrageFrozeTarget = false;
        if (!canUse() || zombie == null || board == null
                || zombie.isDead() || zombie.isHypnotized()) {
            return false;
        }

        BasePlant target = findNearestTarget(zombie, board);
        if (target == null) {
            return false;
        }

        lastTarget = target;
        for (int i = 0; i < snowballsPerBarrage && !target.isFrozen(); i++) {
            lastSnowballCount++;
            if (target.applyIceHit()) {
                lastBarrageFrozeTarget = true;
            }
        }
        resetCooldown();
        return lastSnowballCount > 0;
    }

    private BasePlant findNearestTarget(Zombie zombie, Board board) {
        BasePlant nearest = null;
        int nearestColumn = Integer.MIN_VALUE;
        for (BasePlant plant : board.getPlants()) {
            if (plant.isRemoved() || plant.isFrozen()
                    || plant.getEntityPosition() == null
                    || plant.getEntityPosition().getRow() != zombie.getLane()) {
                continue;
            }
            int column = plant.getEntityPosition().getColumn();
            double distance = zombie.getColumnPosition() - column;
            if (distance + 0.000001 < nearRange
                    || distance - 0.000001 > farRange) {
                continue;
            }
            if (column > nearestColumn) {
                nearest = plant;
                nearestColumn = column;
            }
        }
        return nearest;
    }

    public BasePlant getLastTarget() {
        return lastTarget;
    }

    public int getLastSnowballCount() {
        return lastSnowballCount;
    }

    public boolean didLastBarrageFreezeTarget() {
        return lastBarrageFrozeTarget;
    }

    public int getSnowballsPerBarrage() {
        return snowballsPerBarrage;
    }

    public double getFarRange() {
        return farRange;
    }

    public double getNearRange() {
        return nearRange;
    }
}
