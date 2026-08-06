package io.github.some_example_name.model.game.plantSelector;

import io.github.some_example_name.model.collections.plants.PlantCollectionItem;

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
