package model.collections.plants;

import java.util.ArrayList;
import java.util.List;

public class PlantCollection {
    private List<PlantCollectionItem> allPlants;

    public List<PlantCollectionItem> getUnlockedPlants() {
        return allPlants == null ? new ArrayList<>() : new ArrayList<>(allPlants);
    }

    public List<PlantCollectionItem> getLockedPlants() {
        return null;
    }

    public void unlockPlant(PlantCollectionItem plant) {

    }
}