package model.collections.plants;

import java.util.ArrayList;
import java.util.List;

import model.App;

public class PlantCollection {
    private List<PlantCollectionItem> allPlants;

    public List<PlantCollectionItem> getUnlockedPlants() {
        if (allPlants == null)
            return new ArrayList<>();
        List<PlantCollectionItem> unlocked = new ArrayList<>();
        for (PlantCollectionItem item : allPlants) {
            if (item.isUnlocked()) {
                unlocked.add(item);
            }
        }
        return unlocked;
    }

    public boolean isPlantUnlocked(String name) {
        if (allPlants == null)
            return false;
        for (PlantCollectionItem item : allPlants) {
            if (item.getName().equalsIgnoreCase(name)) {
                return item.isUnlocked();
            }
        }
        return false;
    }

    public void addSeeds(String name, int count) {
        if (allPlants == null)
            return;
        for (PlantCollectionItem item : allPlants) {
            if (item.getName().equalsIgnoreCase(name)) {
                item.addCards(count);
                return;
            }
        }
    }

    public List<PlantCollectionItem> getLockedPlants() {
        if (allPlants == null)
            return new ArrayList<>();
        List<PlantCollectionItem> locked = new ArrayList<>();
        for (PlantCollectionItem item : allPlants) {
            if (!item.isUnlocked()) {
                locked.add(item);
            }
        }
        return locked;
    }

    public void unlockPlant(PlantCollectionItem plant) {
        if (!plant.isUnlocked()) {
            plant.setUnlocked(true);

            // Dispatch notification to user
            if (App.getInstance() != null && App.getInstance().getLoggedInUser() != null) {
                App.getInstance().getLoggedInUser().addNews(
                        "New Plant Unlocked!",
                        "You have successfully unlocked the " + plant.getName() + ".");
            }
        }
    }
}