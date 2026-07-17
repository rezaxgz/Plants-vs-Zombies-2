package model.collections.plants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import model.App;

public class PlantCollection {
    private final List<PlantCollectionItem> allPlants;

    public PlantCollection() {
        allPlants = PlantCatalog.createItems();
    }

    public List<PlantCollectionItem> getAllPlants() {
        return List.copyOf(allPlants);
    }

    public List<PlantCollectionItem> getUnlockedPlants() {
        List<PlantCollectionItem> unlocked = new ArrayList<>();
        for (PlantCollectionItem item : allPlants) {
            if (item.isUnlocked()) {
                unlocked.add(item);
            }
        }
        return List.copyOf(unlocked);
    }

    public List<PlantCollectionItem> getLockedPlants() {
        List<PlantCollectionItem> locked = new ArrayList<>();
        for (PlantCollectionItem item : allPlants) {
            if (!item.isUnlocked()) {
                locked.add(item);
            }
        }
        return List.copyOf(locked);
    }

    public PlantCollectionItem findPlant(String name) {
        String normalized = normalizeName(name);
        if (normalized.isEmpty()) {
            return null;
        }
        for (PlantCollectionItem item : allPlants) {
            if (normalizeName(item.getName()).equals(normalized)) {
                return item;
            }
        }
        return null;
    }

    public boolean isPlantUnlocked(String name) {
        PlantCollectionItem item = findPlant(name);
        return item != null && item.isUnlocked();
    }

    public boolean addCards(String name, int count) {
        PlantCollectionItem item = findPlant(name);
        if (item == null) {
            return false;
        }
        item.addCards(count);
        return true;
    }

    public void addSeeds(String name, int count) {
        addCards(name, count);
    }

    public boolean unlockPlant(String name) {
        PlantCollectionItem item = findPlant(name);
        return item != null && unlockPlant(item);
    }

    public boolean unlockPlant(PlantCollectionItem plant) {
        if (plant == null || plant.isUnlocked() || !allPlants.contains(plant)) {
            return false;
        }
        plant.setUnlocked(true);
        dispatchUnlockNews(plant.getName());
        return true;
    }

    public boolean restorePlantState(String name, boolean unlocked, int level, int cards) {
        PlantCollectionItem item = findPlant(name);
        if (item == null) {
            return false;
        }
        item.restoreState(unlocked, level, cards);
        return true;
    }

    static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static void dispatchUnlockNews(String plantName) {
        if (App.getInstance().getLoggedInUser() == null) {
            return;
        }
        App.getInstance().getLoggedInUser().addNews(
                "New Plant Unlocked!",
                "You have successfully unlocked the " + plantName + ".");
    }
}
