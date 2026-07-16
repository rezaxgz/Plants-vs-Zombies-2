package model.game.special;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import model.game.entities.EntityPosition;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantFactory;
import model.game.entities.plants.PlantFamily;

/**
 * Applies either a forced loadout or one representative per plant family.
 */
public final class LockedPlantsSystem {
    private final LockedPlantsMode mode;
    private final List<String> configuredPlantTypes;
    private final Set<String> normalizedConfiguredTypes;
    private final Map<PlantFamily, String>
            familyRepresentatives;

    private LockedPlantsSystem(
            LockedPlantsMode mode,
            List<String> plantTypes) {
        if (mode == null || plantTypes == null
                || plantTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Locked Plants mode and plant types are required");
        }

        this.mode = mode;
        configuredPlantTypes =
                canonicalizePlantTypes(plantTypes);
        normalizedConfiguredTypes =
                normalizeTypes(configuredPlantTypes);
        familyRepresentatives =
                createFamilyRepresentatives();
    }

    public static LockedPlantsSystem forcedLoadout(
            List<String> forcedPlantTypes) {
        return new LockedPlantsSystem(
                LockedPlantsMode.FORCED_LOADOUT,
                forcedPlantTypes);
    }

    public static LockedPlantsSystem familyRepresentatives(
            List<String> representativePlantTypes) {
        return new LockedPlantsSystem(
                LockedPlantsMode.FAMILY_REPRESENTATIVES,
                representativePlantTypes);
    }

    private static List<String> canonicalizePlantTypes(
            List<String> plantTypes) {
        Set<String> canonical =
                new LinkedHashSet<>();
        for (String plantType : plantTypes) {
            BasePlant plant = PlantFactory.createPlant(
                    plantType,
                    new EntityPosition(0, 0));
            if (plant == null) {
                throw new IllegalArgumentException(
                        "Unknown Locked Plants type: "
                                + plantType);
            }
            canonical.add(plant.getName());
        }
        if (canonical.isEmpty()) {
            throw new IllegalArgumentException(
                    "Locked Plants list cannot be empty");
        }
        return Collections.unmodifiableList(
                new ArrayList<>(canonical));
    }

    private static Set<String> normalizeTypes(
            List<String> plantTypes) {
        Set<String> normalized =
                new LinkedHashSet<>();
        for (String plantType : plantTypes) {
            normalized.add(normalize(plantType));
        }
        return Collections.unmodifiableSet(normalized);
    }

    private Map<PlantFamily, String>
            createFamilyRepresentatives() {
        Map<PlantFamily, String> representatives =
                new EnumMap<>(PlantFamily.class);
        if (mode
                != LockedPlantsMode.FAMILY_REPRESENTATIVES) {
            return Collections.unmodifiableMap(
                    representatives);
        }

        for (String plantType : configuredPlantTypes) {
            BasePlant plant = PlantFactory.createPlant(
                    plantType,
                    new EntityPosition(0, 0));
            PlantFamily family =
                    PlantFamily.findForPlant(plant);
            if (family == null) {
                throw new IllegalArgumentException(
                        plantType
                                + " does not belong to a lockable family");
            }
            if (representatives.put(
                    family, normalize(plantType)) != null) {
                throw new IllegalArgumentException(
                        "Only one representative is allowed per family");
            }
        }
        return Collections.unmodifiableMap(
                representatives);
    }

    public boolean isAllowed(BasePlant plant) {
        if (plant == null) {
            return false;
        }
        if (mode == LockedPlantsMode.FORCED_LOADOUT) {
            return normalizedConfiguredTypes.contains(
                    normalize(plant.getName()));
        }

        PlantFamily family =
                PlantFamily.findForPlant(plant);
        String representative =
                familyRepresentatives.get(family);
        return representative == null
                || representative.equals(
                        normalize(plant.getName()));
    }

    public LockedPlantsMode getMode() {
        return mode;
    }

    public List<String> getConfiguredPlantTypes() {
        return configuredPlantTypes;
    }

    public List<String> getForcedPlantTypes() {
        if (mode
                != LockedPlantsMode.FORCED_LOADOUT) {
            return Collections.emptyList();
        }
        return configuredPlantTypes;
    }

    public String describeRule() {
        if (mode == LockedPlantsMode.FORCED_LOADOUT) {
            return configuredPlantTypes.size()
                    + " plant slots are fixed; "
                    + "all other plants are locked.";
        }
        return "one representative is available from each "
                + "configured plant family; the remaining "
                + "members of those families are locked.";
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
