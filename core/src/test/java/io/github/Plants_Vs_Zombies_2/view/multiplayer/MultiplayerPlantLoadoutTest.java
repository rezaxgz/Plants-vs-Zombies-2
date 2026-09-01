package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollection;
import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;

class MultiplayerPlantLoadoutTest {
    @Test
    void requiresExactlyEightDistinctUnlockedPlants() {
        PlantCollection collection = new PlantCollection();
        List<PlantCollectionItem> plants = collection.getAllPlants();
        for (int index = 0; index < 9; index++) {
            plants.get(index).setUnlocked(true);
        }
        MultiplayerPlantLoadout loadout = new MultiplayerPlantLoadout(plants);

        for (int index = 0; index < MultiplayerPlantLoadout.SLOT_COUNT; index++) {
            assertEquals(MultiplayerPlantLoadout.ToggleResult.SELECTED,
                    loadout.toggle(plants.get(index)));
        }
        assertTrue(loadout.isComplete());
        assertEquals(8, loadout.selectedNames().size());
        assertEquals(MultiplayerPlantLoadout.ToggleResult.FULL,
                loadout.toggle(plants.get(8)));

        assertEquals(MultiplayerPlantLoadout.ToggleResult.REMOVED,
                loadout.toggle(plants.get(0)));
        assertFalse(loadout.isComplete());
        assertEquals(7, loadout.selectedCount());
    }

    @Test
    void lockedPlantsCannotEnterTheLoadout() {
        PlantCollection collection = new PlantCollection();
        PlantCollectionItem locked = collection.getLockedPlants().get(0);
        MultiplayerPlantLoadout loadout = new MultiplayerPlantLoadout(
                collection.getAllPlants());

        assertEquals(MultiplayerPlantLoadout.ToggleResult.LOCKED,
                loadout.toggle(locked));
        assertTrue(loadout.selectedNames().isEmpty());
    }
}
