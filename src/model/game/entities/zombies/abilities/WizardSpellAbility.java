package model.game.entities.zombies.abilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import model.game.Board;
import model.game.entities.plants.BasePlant;
import model.game.entities.zombies.Zombie;

/**
 * Wizard periodically transforms the nearest active plant into a harmless
 * sheep. All surviving sheep created by this Wizard return to normal when the
 * Wizard dies.
 */
public class WizardSpellAbility extends ZombieAbility {
    private final Set<BasePlant> transformedPlants =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private BasePlant lastTarget;
    private boolean releasedAfterDeath;

    public WizardSpellAbility() {
        super(8.0);
    }

    @Override
    public boolean tryUse(Zombie wizard, Board board) {
        lastTarget = null;
        if (!canUse() || wizard == null || board == null
                || wizard.isDead() || wizard.isHypnotized()
                || wizard.isFrozen() || wizard.isStunned()) {
            return false;
        }

        BasePlant target = findNearestActivePlant(wizard, board);
        if (target == null || !target.transformToSheep()) {
            return false;
        }

        transformedPlants.add(target);
        lastTarget = target;
        resetCooldown();
        return true;
    }

    private BasePlant findNearestActivePlant(
            Zombie wizard, Board board) {
        BasePlant nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (BasePlant plant : board.getPlants()) {
            if (plant.isRemoved() || plant.isDestroyed()
                    || plant.isDisabled()
                    || plant.getEntityPosition() == null) {
                continue;
            }
            double rowDistance = wizard.getLane()
                    - plant.getEntityPosition().getRow();
            double columnDistance =
                    wizard.getColumnPosition()
                            - plant.getEntityPosition().getColumn();
            double distance = rowDistance * rowDistance
                    + columnDistance * columnDistance;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = plant;
            }
        }
        return nearest;
    }

    public int restoreTransformedPlants() {
        if (releasedAfterDeath) {
            return 0;
        }
        releasedAfterDeath = true;
        int restored = 0;
        for (BasePlant plant :
                new ArrayList<>(transformedPlants)) {
            if (!plant.isDestroyed()
                    && plant.restoreFromSheep()) {
                restored++;
            }
        }
        transformedPlants.clear();
        return restored;
    }

    public BasePlant getLastTarget() {
        return lastTarget;
    }

    public int getTransformedPlantCount() {
        return transformedPlants.size();
    }
}
