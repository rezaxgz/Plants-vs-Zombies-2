package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * Periodically covers the nearest active plant in the same lane with an
 * octopus. Covered plants cannot act and direct projectiles hit the octopus
 * before passing through that tile.
 */
public class OctopusThrowAbility extends ZombieAbility {
    private BasePlant lastTarget;

    public OctopusThrowAbility() {
        super(5.0);
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        lastTarget = null;
        if (!canUse() || zombie == null || board == null
                || zombie.isDead() || zombie.isHypnotized()
                || zombie.isFrozen() || zombie.isStunned()) {
            return false;
        }

        BasePlant target = findNearestActiveTarget(zombie, board);
        if (target == null || !target.attachOctopus()) {
            return false;
        }

        lastTarget = target;
        resetCooldown();
        return true;
    }

    private BasePlant findNearestActiveTarget(
            Zombie zombie, Board board) {
        BasePlant nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (BasePlant plant : board.getPlants()) {
            if (plant.isRemoved() || plant.isDisabled()
                    || plant.getEntityPosition() == null
                    || plant.getEntityPosition().getRow() != zombie.getLane()) {
                continue;
            }
            double distance = zombie.getColumnPosition()
                    - plant.getEntityPosition().getColumn();
            if (distance < 0.0 || distance >= nearestDistance) {
                continue;
            }
            nearest = plant;
            nearestDistance = distance;
        }
        return nearest;
    }

    public BasePlant getLastTarget() {
        return lastTarget;
    }
}
