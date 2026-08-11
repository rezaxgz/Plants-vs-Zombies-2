package io.github.Plants_Vs_Zombies_2.model.game.plantSelector;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;

public class ConveyorBeltSelector extends PlantSelector {
    @Override
    protected PlantCollectionItem getPlant() {
        return null;
    }

    @Override
    protected void update(float deltaSeconds) {
        // Conveyor timers will use deltaSeconds when implemented.
    }
}
