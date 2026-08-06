package io.github.some_example_name.model.game.entities.zombies.abilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.zombies.Zombie;

/**
 * Wizard periodically transforms an active plant into a harmless cat. Cats
 * cannot act and are ignored by eating zombies until their Wizard dies.
 */
public class WizardSpellAbility extends ZombieAbility {
    private final Set<BasePlant> transformedPlants = Collections.newSetFromMap(
            new IdentityHashMap<>());

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
        if (!transformPlant(target)) {
            return false;
        }
        resetCooldown();
        return true;
    }

    public boolean transformReachedPlant(
            Zombie wizard, BasePlant plant) {
        lastTarget = null;
        if (wizard == null || plant == null
                || wizard.isDead()
                || wizard.isHypnotized()
                || wizard.isFrozen()
                || wizard.isStunned()) {
            return false;
        }
        if (!transformPlant(plant)) {
            return false;
        }
        resetCooldown();
        return true;
    }

    private boolean transformPlant(BasePlant plant) {
        if (plant == null
                || !plant.transformToSheep()) {
            return false;
        }
        transformedPlants.add(plant);
        lastTarget = plant;
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
            double columnDistance = wizard.getColumnPosition()
                    - plant.getEntityPosition()
                            .getColumn();
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
        for (BasePlant plant : new ArrayList<>(transformedPlants)) {
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
