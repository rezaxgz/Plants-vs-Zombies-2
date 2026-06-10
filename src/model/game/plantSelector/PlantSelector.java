package model.game.plantSelector;

import java.util.List;

import model.collections.plants.PlantCollectionItem;

public abstract class PlantSelector {

    private List<PlantCollectionItem> plantsCollection;

    protected abstract PlantCollectionItem getPlant();

    protected abstract void tick();
}
