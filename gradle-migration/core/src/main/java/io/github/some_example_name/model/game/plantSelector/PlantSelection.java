package io.github.some_example_name.model.game.plantSelector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.github.some_example_name.model.collections.plants.PlantCollection;
import io.github.some_example_name.model.collections.plants.PlantCollectionItem;
import io.github.some_example_name.model.game.entities.plants.PlantCategory;
import io.github.some_example_name.model.roadmap.Level;
import io.github.some_example_name.model.roadmap.SpecialLevelType;

/**
 * Mutable loadout being prepared before an adventure level starts.
 */
public final class PlantSelection {
    private final PlantCollection collection;
    private final Level level;
    private final int slotCount;
    private final List<PlantCollectionItem> availablePlants;
    private final List<PlantCollectionItem> selectedPlants = new ArrayList<>();
    private final Set<String> boostedPlantKeys = new LinkedHashSet<>();

    public PlantSelection(PlantCollection collection, Level level) {
        if (collection == null || level == null) {
            throw new IllegalArgumentException(
                    "plant collection and level are required");
        }
        this.collection = collection;
        this.level = level;
        this.slotCount = level.getPlantSlotCount();
        this.availablePlants = Collections.unmodifiableList(
                findAvailablePlants(collection, level));
    }

    private static List<PlantCollectionItem> findAvailablePlants(
            PlantCollection collection, Level level) {
        List<PlantCollectionItem> available = new ArrayList<>();
        for (PlantCollectionItem plant : collection.getUnlockedPlants()) {
            if (isAllowedForLevel(plant, level)) {
                available.add(plant);
            }
        }
        return available;
    }

    private static boolean isAllowedForLevel(
            PlantCollectionItem plant, Level level) {
        if (level.getSpecialLevelType() == SpecialLevelType.PLANT_WHAT_YOU_GET) {
            return plant.getCategory() != PlantCategory.SUN_PRODUCER;
        }
        if (level.getSpecialLevelType() == SpecialLevelType.LOCKED_PLANTS) {
            for (String allowed : level.getSpecialConfig().getPlantPool()) {
                if (normalize(allowed).equals(normalize(plant.getName()))) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public PlantCollectionItem findPlant(String name) {
        return collection.findPlant(name);
    }

    public List<PlantCollectionItem> getAllPlants() {
        return collection.getAllPlants();
    }

    public List<PlantCollectionItem> getAvailablePlants() {
        return availablePlants;
    }

    public List<PlantCollectionItem> getSelectedPlants() {
        return List.copyOf(selectedPlants);
    }

    public int getSlotCount() {
        return slotCount;
    }

    public boolean isAvailable(PlantCollectionItem plant) {
        return plant != null && availablePlants.contains(plant);
    }

    public boolean isSelected(PlantCollectionItem plant) {
        return plant != null && selectedPlants.contains(plant);
    }

    public boolean addPlant(PlantCollectionItem plant) {
        if (!isAvailable(plant) || isSelected(plant)
                || selectedPlants.size() >= slotCount) {
            return false;
        }
        selectedPlants.add(plant);
        return true;
    }

    public boolean removePlant(PlantCollectionItem plant) {
        return plant != null && selectedPlants.remove(plant);
    }

    public boolean boostPlant(PlantCollectionItem plant) {
        if (!isSelected(plant)) {
            return false;
        }
        return boostedPlantKeys.add(normalize(plant.getName()));
    }

    public boolean isBoosted(PlantCollectionItem plant) {
        return plant != null
                && boostedPlantKeys.contains(normalize(plant.getName()));
    }

    public void selectAllAvailable() {
        selectedPlants.clear();
        int count = Math.min(slotCount, availablePlants.size());
        selectedPlants.addAll(availablePlants.subList(0, count));
    }

    public boolean shouldStartAutomatically() {
        return availablePlants.size() < slotCount;
    }

    public Map<String, Integer> getSelectedPlantLevels() {
        Map<String, Integer> levels = new LinkedHashMap<>();
        for (PlantCollectionItem plant : selectedPlants) {
            levels.put(plant.getName(), plant.getCurrentLevel());
        }
        return Collections.unmodifiableMap(levels);
    }

    public List<String> getBoostedPlantNames() {
        List<String> names = new ArrayList<>();
        for (PlantCollectionItem plant : selectedPlants) {
            if (isBoosted(plant)) {
                names.add(plant.getName());
            }
        }
        return Collections.unmodifiableList(names);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
