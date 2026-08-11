package model.game.plantSelector;

import model.collections.plants.PlantCollectionItem;

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
