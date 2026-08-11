package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFactory;
import io.github.Plants_Vs_Zombies_2.model.game.gameTypes.GameType;
import io.github.Plants_Vs_Zombies_2.model.game.special.ConveyorBeltSystem;
import io.github.Plants_Vs_Zombies_2.model.game.special.ConveyorPlacementResult;
import io.github.Plants_Vs_Zombies_2.model.game.special.ConveyorPlantPacket;
import io.github.Plants_Vs_Zombies_2.model.game.special.LockedPlantsMode;
import io.github.Plants_Vs_Zombies_2.model.game.special.LockedPlantsSystem;

abstract class GameLoadoutLogic extends GameRewardLogic {
    protected GameLoadoutLogic(Board board, GameType gameType,
            int initialSunCount, List<ZombieWave> zombieWaves,
            Random random, boolean startWavesImmediately,
            ChapterRuleset chapterRuleset, int difficultyLevel) {
        super(board, gameType, initialSunCount, zombieWaves, random,
                startWavesImmediately, chapterRuleset, difficultyLevel);
    }

    public void enableConveyorBelt(
            List<String> availablePlantTypes) {
        if (conveyorBeltSystem != null
                || lockedPlantsSystem != null) {
            throw new IllegalStateException(
                    "another plant-selection rule is already enabled");
        }
        conveyorBeltSystem = new ConveyorBeltSystem(
                availablePlantTypes);
        pendingResults.addAll(
                conveyorBeltSystem.drainMessages());
    }

    public void replaceConveyorPlantPool(
            List<String> availablePlantTypes) {
        if (conveyorBeltSystem == null) {
            throw new IllegalStateException(
                    "this game has no Conveyor Belt");
        }
        conveyorBeltSystem = new ConveyorBeltSystem(availablePlantTypes);
        pendingResults.add("Conveyor Belt pool updated from the "
                + "player's unlocked plants.");
        pendingResults.addAll(
                conveyorBeltSystem.drainMessages());
    }

    public boolean hasConveyorBelt() {
        return conveyorBeltSystem != null;
    }

    public List<ConveyorPlantPacket> getConveyorPackets() {
        if (conveyorBeltSystem == null) {
            return Collections.emptyList();
        }
        return conveyorBeltSystem.getPackets();
    }

    public ConveyorPlantPacket getConveyorPacket(
            int index) {
        if (conveyorBeltSystem == null) {
            return null;
        }
        return conveyorBeltSystem.getPacket(index);
    }

    protected final ConveyorPlantPacket consumeConveyorPacket(
            int index) {
        if (conveyorBeltSystem == null) {
            return null;
        }
        return conveyorBeltSystem.consumePacket(index);
    }

    public double getConveyorSecondsUntilNextPacket() {
        if (conveyorBeltSystem == null) {
            return 0.0;
        }
        return conveyorBeltSystem
                .getSecondsUntilNextPacket();
    }

    public ConveyorPlacementResult plantFromConveyor(
            int index,
            EntityPosition position) {
        if (conveyorBeltSystem == null) {
            return ConveyorPlacementResult.NOT_CONVEYOR_LEVEL;
        }
        ConveyorPlantPacket packet = conveyorBeltSystem.getPacket(index);
        if (packet == null) {
            return ConveyorPlacementResult.INVALID_PACKET;
        }
        if (!board.isPositionInsideBoard(position)) {
            return ConveyorPlacementResult.INVALID_POSITION;
        }

        BasePlant plant = PlantFactory.createPlant(
                packet.getPlantType(), position);
        if (plant == null) {
            return ConveyorPlacementResult.UNKNOWN_PLANT;
        }
        if (!board.canAddPlant(plant)
                || !board.addPlant(plant)) {
            return ConveyorPlacementResult.POSITION_OCCUPIED;
        }

        conveyorBeltSystem.consumePacket(index);
        pendingResults.add("Conveyor Belt plant "
                + plant.getName() + " was planted at "
                + position + ".");
        return ConveyorPlacementResult.SUCCESS;
    }

    public void enableLockedPlantsForcedLoadout(
            List<String> forcedPlantTypes) {
        if (lockedPlantsSystem != null
                || conveyorBeltSystem != null) {
            throw new IllegalStateException(
                    "another plant-selection rule is already enabled");
        }
        lockedPlantsSystem = LockedPlantsSystem.forcedLoadout(
                forcedPlantTypes);
        pendingResults.add(
                "Locked Plants level started. "
                        + lockedPlantsSystem.describeRule());
    }

    public void enableLockedPlantFamilyRepresentatives(
            List<String> representativePlantTypes) {
        if (lockedPlantsSystem != null
                || conveyorBeltSystem != null) {
            throw new IllegalStateException(
                    "another plant-selection rule is already enabled");
        }
        lockedPlantsSystem = LockedPlantsSystem.familyRepresentatives(
                representativePlantTypes);
        pendingResults.add(
                "Locked Plants level started. "
                        + lockedPlantsSystem.describeRule());
    }

    public boolean hasLockedPlants() {
        return lockedPlantsSystem != null;
    }

    public boolean isPlantAllowed(BasePlant plant) {
        return lockedPlantsSystem == null
                || lockedPlantsSystem.isAllowed(plant);
    }

    public LockedPlantsMode getLockedPlantsMode() {
        return lockedPlantsSystem == null
                ? null
                : lockedPlantsSystem.getMode();
    }

    public List<String> getLockedPlantTypes() {
        if (lockedPlantsSystem == null) {
            return Collections.emptyList();
        }
        return lockedPlantsSystem.getConfiguredPlantTypes();
    }

    public List<String> getForcedPlantTypes() {
        if (lockedPlantsSystem == null) {
            return Collections.emptyList();
        }
        return lockedPlantsSystem.getForcedPlantTypes();
    }

    public String getLockedPlantsRuleDescription() {
        if (lockedPlantsSystem == null) {
            return "none";
        }
        return lockedPlantsSystem.describeRule();
    }

    public void configurePlantLoadout(
            Map<String, Integer> selectedPlantLevels,
            List<String> boostedPlantNames) {
        configurePlantLoadout(selectedPlantLevels,
                boostedPlantNames, Collections.emptyList());
    }

    public void configurePlantLoadout(
            Map<String, Integer> selectedPlantLevels,
            List<String> boostedPlantNames,
            List<String> greenhouseBoostNames) {
        if (selectedPlantLevels == null
                || selectedPlantLevels.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one selected plant is required");
        }
        plantLoadoutLevels.clear();
        plantLoadoutNames.clear();
        boostedPlantTypes.clear();
        greenhouseBoostTypes.clear();
        consumedGreenhouseBoosts.clear();
        for (Map.Entry<String, Integer> entry : selectedPlantLevels.entrySet()) {
            addPlantToLoadout(entry.getKey(), entry.getValue());
        }
        addConfiguredBoosts(boostedPlantNames,
                boostedPlantTypes);
        addConfiguredBoosts(greenhouseBoostNames,
                greenhouseBoostTypes);
        boostedPlantTypes.addAll(greenhouseBoostTypes);
        plantLoadoutConfigured = true;
    }

    void addConfiguredBoosts(List<String> plantNames,
            Set<String> destination) {
        if (plantNames == null) {
            return;
        }
        for (String plantName : plantNames) {
            String key = requestedPlantKey(plantName);
            if (plantLoadoutLevels.containsKey(key)) {
                destination.add(key);
            }
        }
    }

    void addPlantToLoadout(String plantName, int level) {
        BasePlant plant = PlantFactory.createPlant(
                plantName, level, new EntityPosition(0, 0));
        if (plant == null) {
            throw new IllegalArgumentException(
                    "unknown plant in loadout: " + plantName);
        }
        String key = getLoadoutKey(plant);
        plantLoadoutLevels.put(key, Math.max(1, level));
        plantLoadoutNames.put(key, plant.getName());
    }

    public BasePlant createPlantFromLoadout(
            String requestedType, EntityPosition position) {
        if (!plantLoadoutConfigured) {
            return PlantFactory.createPlant(requestedType, position);
        }
        Integer level = plantLoadoutLevels.get(
                requestedPlantKey(requestedType));
        if (level == null) {
            return null;
        }
        return PlantFactory.createPlant(requestedType, level, position);
    }

    public boolean isPlantInLoadout(BasePlant plant) {
        return !plantLoadoutConfigured
                || plantLoadoutLevels.containsKey(getLoadoutKey(plant));
    }

    public boolean hasConfiguredPlantLoadout() {
        return plantLoadoutConfigured;
    }

    public List<BasePlant> getPlantLoadoutPrototypes() {
        if (!plantLoadoutConfigured) {
            return Collections.emptyList();
        }
        List<BasePlant> plants = new ArrayList<>();
        for (Map.Entry<String, String> entry : plantLoadoutNames.entrySet()) {
            BasePlant plant = PlantFactory.createPlant(
                    entry.getValue(), plantLoadoutLevels.get(entry.getKey()),
                    new EntityPosition(0, 0));
            if (plant != null) {
                plants.add(plant);
            }
        }
        return Collections.unmodifiableList(plants);
    }

    void applyLoadoutBoost(BasePlant plant) {
        String key = getLoadoutKey(plant);
        if (!boostedPlantTypes.contains(key)) {
            return;
        }
        board.usePlantFoodAt(plant.getEntityPosition());
        pendingResults.addAll(board.drainResults());
        if (greenhouseBoostTypes.remove(key)) {
            String plantName = plantLoadoutNames.getOrDefault(
                    key, plant.getName());
            consumedGreenhouseBoosts.add(plantName);
            pendingResults.add("Stored greenhouse boost for "
                    + plantName + " was consumed on first use.");
        }
    }

    public List<String> drainConsumedGreenhouseBoosts() {
        List<String> drained = new ArrayList<>(
                consumedGreenhouseBoosts);
        consumedGreenhouseBoosts.clear();
        return drained;
    }
}
