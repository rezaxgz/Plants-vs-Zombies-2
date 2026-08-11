package io.github.Plants_Vs_Zombies_2.model.collections.plants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantCategory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantDefinition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.explosive.ExplosivePlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.homing.HomingPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.lobber.LobberPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.melee.MeleePlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.modifier.ModifierPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.shooter.ShooterPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.strikeThrough.StrikeThroughPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.sunProducer.SunProducerPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.wallnut.WallnutPlantType;

/**
 * Builds the complete collection catalog from the plant definitions that mirror
 * {@code plants.csv}. Keeping the catalog tied to those enums prevents the
 * collection menu from silently omitting a newly implemented plant.
 */
final class PlantCatalog {
    private static final int EXPECTED_PLANT_COUNT = 69;

    private PlantCatalog() {
    }

    static List<PlantCollectionItem> createItems() {
        List<PlantCollectionItem> items = new ArrayList<>();
        addAll(items, PlantCategory.SUN_PRODUCER, SunProducerPlantType.values());
        addAll(items, PlantCategory.SHOOTER, ShooterPlantType.values());
        addAll(items, PlantCategory.HOMING, HomingPlantType.values());
        addAll(items, PlantCategory.LOBBER, LobberPlantType.values());
        addAll(items, PlantCategory.STRIKE_THROUGH, StrikeThroughPlantType.values());
        addAll(items, PlantCategory.EXPLOSIVE, ExplosivePlantType.values());
        addAll(items, PlantCategory.MELEE, MeleePlantType.values());
        addAll(items, PlantCategory.MODIFIER, ModifierPlantType.values());
        addAll(items, PlantCategory.WALL_NUT, WallnutPlantType.values());
        items.sort(Comparator.comparingInt(PlantCollectionItem::getId));
        validateCatalog(items);
        return items;
    }

    private static void addAll(List<PlantCollectionItem> items,
            PlantCategory category, PlantDefinition[] definitions) {
        for (PlantDefinition definition : definitions) {
            items.add(new PlantCollectionItem(definition, category));
        }
    }

    private static void validateCatalog(List<PlantCollectionItem> items) {
        if (items.size() != EXPECTED_PLANT_COUNT) {
            throw new IllegalStateException("plant catalog must contain "
                    + EXPECTED_PLANT_COUNT + " plants, but contains " + items.size());
        }
        Set<Integer> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (PlantCollectionItem item : items) {
            if (!ids.add(item.getId())) {
                throw new IllegalStateException("duplicate plant id: " + item.getId());
            }
            String normalizedName = PlantCollection.normalizeName(item.getName());
            if (!names.add(normalizedName)) {
                throw new IllegalStateException("duplicate plant name: " + item.getName());
            }
        }
    }
}
