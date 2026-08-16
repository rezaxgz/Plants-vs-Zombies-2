package io.github.Plants_Vs_Zombies_2.model.game.special;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;

/**
 * Counts every tracked plant that is removed or destroyed.
 */
public final class LoveYourPlantsSystem implements java.io.Serializable {
    private final int maximumLostPlants;
    private final Set<BasePlant> observedPlants;
    private final Set<BasePlant> lostPlants;

    public LoveYourPlantsSystem(
            int maximumLostPlants) {
        if (maximumLostPlants <= 0) {
            throw new IllegalArgumentException(
                    "maximumLostPlants must be positive");
        }
        this.maximumLostPlants = maximumLostPlants;
        observedPlants = Collections.newSetFromMap(
                new IdentityHashMap<>());
        lostPlants = Collections.newSetFromMap(
                new IdentityHashMap<>());
    }

    public void observePlants(Board board) {
        if (board == null) {
            throw new IllegalArgumentException(
                    "board cannot be null");
        }
        observedPlants.addAll(board.getPlants());
    }

    public void updateLosses(Board board) {
        if (board == null) {
            throw new IllegalArgumentException(
                    "board cannot be null");
        }
        for (BasePlant plant : observedPlants) {
            if ((plant.isDestroyed()
                    || plant.isRemoved()
                    || !board.containsEntity(plant))
                    && lostPlants.add(plant)) {
                continue;
            }
        }
    }

    public boolean hasFailed() {
        return lostPlants.size() >= maximumLostPlants;
    }

    public int getLostPlantCount() {
        return lostPlants.size();
    }

    public int getMaximumLostPlants() {
        return maximumLostPlants;
    }
}
