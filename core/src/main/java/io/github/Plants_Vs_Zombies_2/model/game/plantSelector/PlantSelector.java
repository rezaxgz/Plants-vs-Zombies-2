package io.github.Plants_Vs_Zombies_2.model.game.plantSelector;

import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;

public abstract class PlantSelector {
    private List<PlantCollectionItem> plantsCollection;

    protected abstract PlantCollectionItem getPlant();

    protected abstract void update(float deltaSeconds);
}
