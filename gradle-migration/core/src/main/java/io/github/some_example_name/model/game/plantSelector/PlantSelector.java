package io.github.some_example_name.model.game.plantSelector;

import java.util.List;

import io.github.some_example_name.model.collections.plants.PlantCollectionItem;

public abstract class PlantSelector {
    private List<PlantCollectionItem> plantsCollection;

    protected abstract PlantCollectionItem getPlant();

    protected abstract void update(float deltaSeconds);
}
