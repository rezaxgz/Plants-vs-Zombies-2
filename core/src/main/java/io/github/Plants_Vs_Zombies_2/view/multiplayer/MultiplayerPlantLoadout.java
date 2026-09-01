package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;

/** Eight-slot plant loadout prepared locally before multiplayer readiness. */
public final class MultiplayerPlantLoadout {
    public static final int SLOT_COUNT = 8;

    public enum ToggleResult {
        SELECTED,
        REMOVED,
        LOCKED,
        FULL
    }

    private final List<PlantCollectionItem> plants;
    private final Set<PlantCollectionItem> selected = new LinkedHashSet<>();

    public MultiplayerPlantLoadout(List<PlantCollectionItem> plants) {
        if (plants == null) {
            throw new IllegalArgumentException("plants are required");
        }
        this.plants = List.copyOf(plants);
    }

    public List<PlantCollectionItem> plants() {
        return plants;
    }

    public List<PlantCollectionItem> selectedPlants() {
        return List.copyOf(selected);
    }

    public List<String> selectedNames() {
        List<String> names = new ArrayList<>();
        for (PlantCollectionItem plant : selected) {
            names.add(plant.getName());
        }
        return List.copyOf(names);
    }

    public boolean isSelected(PlantCollectionItem plant) {
        return plant != null && selected.contains(plant);
    }

    public boolean isComplete() {
        return selected.size() == SLOT_COUNT;
    }

    public int selectedCount() {
        return selected.size();
    }

    public long unlockedCount() {
        return plants.stream().filter(PlantCollectionItem::isUnlocked).count();
    }

    public ToggleResult toggle(PlantCollectionItem plant) {
        if (plant == null || !plants.contains(plant) || !plant.isUnlocked()) {
            return ToggleResult.LOCKED;
        }
        if (selected.remove(plant)) {
            return ToggleResult.REMOVED;
        }
        if (selected.size() >= SLOT_COUNT) {
            return ToggleResult.FULL;
        }
        selected.add(plant);
        return ToggleResult.SELECTED;
    }
}
