package io.github.Plants_Vs_Zombies_2.model.game.plantSelector;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;

public class NormalSelector extends PlantSelector {
    @Override
    protected void update(float deltaSeconds) {
        // Recharge timers will use deltaSeconds when implemented.
    }

    @Override
    protected PlantCollectionItem getPlant() {
        return null;
    }
}
