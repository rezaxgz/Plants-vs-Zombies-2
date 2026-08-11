package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFamily;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantTag;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.structure.BaseStructure;
import io.github.Plants_Vs_Zombies_2.model.game.structure.Grave;
import io.github.Plants_Vs_Zombies_2.model.game.structure.GraveReward;
import io.github.Plants_Vs_Zombies_2.model.game.tile.Tile;
import io.github.Plants_Vs_Zombies_2.model.game.tile.TileType;

abstract class BoardChapterLogic extends BoardEntityLogic {
    protected BoardChapterLogic() {
        super();
    }

    protected BoardChapterLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }

    public int getNumberOfColumns() {
        return numberOfColumns;
    }

    public List<Tile> getTiles() {
        return Collections.unmodifiableList(tiles);
    }

    public Tile getTileAt(EntityPosition position) {
        if (!isPositionInsideBoard(position)) {
            return null;
        }
        return tiles.get(position.getRow() * numberOfColumns + position.getColumn());
    }

    public void configureBigWaveBeach(int initialWaterColumns,
            int maximumWaterColumns,
            List<EntityPosition> lowBeachPositions) {
        validateBeachWaterColumns(initialWaterColumns,
                maximumWaterColumns);
        if (lowBeachPositions == null) {
            throw new IllegalArgumentException(
                    "lowBeachPositions cannot be null");
        }
        bigWaveBeachRules = true;
        maximumWaterColumnCount = maximumWaterColumns;
        waterColumnCount = 0;
        lowBeachTiles.clear();
        for (EntityPosition position : lowBeachPositions) {
            if (!isPositionInsideBoard(position)) {
                throw new IllegalArgumentException(
                        "low-beach position is outside the board");
            }
            lowBeachTiles.add(position);
        }
        setBeachWaterColumns(initialWaterColumns, false);
    }

    void validateBeachWaterColumns(int initialWaterColumns,
            int maximumWaterColumns) {
        if (initialWaterColumns < 0
                || maximumWaterColumns < initialWaterColumns
                || maximumWaterColumns > numberOfColumns) {
            throw new IllegalArgumentException(
                    "beach water columns are invalid");
        }
    }

    public List<EntityPosition> raiseBigWaveBeachTide() {
        if (!bigWaveBeachRules
                || waterColumnCount >= maximumWaterColumnCount) {
            return Collections.emptyList();
        }
        return setBeachWaterColumns(waterColumnCount + 1, true);
    }

    List<EntityPosition> setBeachWaterColumns(
            int requestedWaterColumns, boolean drownPlants) {
        int oldWaterColumns = waterColumnCount;
        int newWaterColumns = Math.min(
                requestedWaterColumns, maximumWaterColumnCount);
        int oldWaterStart = numberOfColumns - oldWaterColumns;
        int newWaterStart = numberOfColumns - newWaterColumns;
        List<EntityPosition> newlyFloodedLowBeach = new ArrayList<>();
        for (int row = 0; row < numberOfRows; row++) {
            for (int column = 0; column < numberOfColumns; column++) {
                EntityPosition position = new EntityPosition(row, column);
                boolean isWater = column >= newWaterStart;
                boolean wasWater = column >= oldWaterStart;
                updateBeachTile(position, isWater);
                if (isWater && !wasWater) {
                    recordNewlyFloodedTile(position, drownPlants,
                            newlyFloodedLowBeach);
                }
            }
        }
        waterColumnCount = newWaterColumns;
        if (drownPlants) {
            cleanupRemovedEntities();
        }
        return Collections.unmodifiableList(newlyFloodedLowBeach);
    }

    void updateBeachTile(EntityPosition position,
            boolean water) {
        Tile tile = getTileAt(position);
        if (water) {
            tile.setTileType(TileType.WATER);
        } else if (lowBeachTiles.contains(position)) {
            tile.setTileType(TileType.LOW_BEACH);
        } else {
            tile.setTileType(TileType.NORMAL);
        }
    }

    void recordNewlyFloodedTile(EntityPosition position,
            boolean drownPlants,
            List<EntityPosition> newlyFloodedLowBeach) {
        if (lowBeachTiles.contains(position)) {
            newlyFloodedLowBeach.add(position);
        }
        if (drownPlants) {
            drownUnsupportedPlants(position,
                    " when the tide rose.");
        }
    }

    void drownUnsupportedPlants(EntityPosition position,
            String reason) {
        List<BasePlant> plantsAtPosition = new ArrayList<>(getPlantsAt(position));
        boolean hasLilyPad = plantsAtPosition.stream()
                .anyMatch(Board::isLilyPad);
        for (BasePlant plant : plantsAtPosition) {
            if (isLilyPad(plant)
                    || plant.hasTag(PlantTag.WATER)
                    || hasLilyPad) {
                continue;
            }
            plant.takeDamage(Integer.MAX_VALUE);
            pendingResults.add("Plant " + plant.getName()
                    + " at " + position + " drowned" + reason);
        }
    }

    public boolean isBigWaveBeachRulesEnabled() {
        return bigWaveBeachRules;
    }

    public int getWaterColumnCount() {
        return waterColumnCount;
    }

    public int getMaximumWaterColumnCount() {
        return maximumWaterColumnCount;
    }

    public int getWaterBoundaryColumn() {
        return numberOfColumns - maximumWaterColumnCount;
    }

    public boolean isLowBeachTile(EntityPosition position) {
        return position != null && lowBeachTiles.contains(position);
    }

    public boolean isSubmergedLowBeachTile(EntityPosition position) {
        Tile tile = getTileAt(position);
        return isLowBeachTile(position)
                && tile != null
                && tile.getTileType() == TileType.WATER;
    }

    public void enableFrostbiteCavesRules() {
        frostbiteCavesRules = true;
        for (Zombie zombie : getZombies()) {
            zombie.setChapterColdImmune(true);
        }
    }

    public boolean isFrostbiteCavesRulesEnabled() {
        return frostbiteCavesRules;
    }

    public int applyIcyWind(List<Integer> lanes) {
        if (!frostbiteCavesRules || lanes == null || lanes.isEmpty()) {
            return 0;
        }
        Set<Integer> affectedLanes = new LinkedHashSet<>();
        for (Integer lane : lanes) {
            if (lane != null && lane >= 0 && lane < numberOfRows) {
                affectedLanes.add(lane);
            }
        }
        int affectedPlants = 0;
        for (BasePlant plant : getPlants()) {
            EntityPosition position = plant.getEntityPosition();
            if (position == null || plant.isDestroyed()
                    || plant.isFrozen()
                    || plant.hasTag(PlantTag.FIRE)
                    || !affectedLanes.contains(position.getRow())) {
                continue;
            }
            boolean frozenNow = plant.increaseFreezeLevel();
            affectedPlants++;
            pendingResults.add("Icy wind raised " + plant.getName()
                    + " at " + position + " to freeze level "
                    + plant.getFreezeLevel() + "/"
                    + BasePlant.MAX_FREEZE_LEVEL + "."
                    + (frozenNow
                            ? " The plant is frozen inside a "
                                    + BasePlant.ICE_SHELL_HIT_POINTS
                                    + " HP ice shell."
                            : ""));
        }
        return affectedPlants;
    }

    public void setSliderTile(EntityPosition position, int laneDelta) {
        if (!isPositionInsideBoard(position)) {
            throw new IllegalArgumentException(
                    "slider position is outside the board");
        }
        if (laneDelta != -1 && laneDelta != 1) {
            throw new IllegalArgumentException(
                    "slider lane delta must be -1 or 1");
        }
        setTileType(position, laneDelta < 0
                ? TileType.SLIDER_UP
                : TileType.SLIDER_DOWN);
    }

    public boolean addGrave(EntityPosition position) {
        return addGrave(position, GraveReward.NONE);
    }

    public boolean addGrave(EntityPosition position, GraveReward reward) {
        if (!canAddGraveAt(position) || reward == null) {
            return false;
        }
        Tile tile = getTileAt(position);
        return addStructure(new Grave(position, reward,
                tile.getTileType()));
    }

    public boolean canAddGraveAt(EntityPosition position) {
        if (!isPositionInsideBoard(position)
                || getStructureAt(position) != null
                || !getPlantsAt(position).isEmpty()
                || hasZombieAt(position)) {
            return false;
        }
        Tile tile = getTileAt(position);
        return tile != null
                && (tile.getTileType() == TileType.NORMAL
                        || tile.getTileType() == TileType.NECROMANCY);
    }

    public List<Grave> getGraves() {
        List<Grave> graves = new ArrayList<>();
        for (BaseStructure structure : structures) {
            if (structure instanceof Grave
                    && !structure.isRemoved()) {
                graves.add((Grave) structure);
            }
        }
        return Collections.unmodifiableList(graves);
    }

    public boolean hasZombieAt(EntityPosition position) {
        for (Zombie zombie : getZombies()) {
            if (zombie.getLane() == position.getRow()
                    && (int) Math.floor(zombie.getColumnPosition()) == position.getColumn()) {
                return true;
            }
        }
        return false;
    }

    public Zombie addFrozenZombie(ZombieType type,
            EntityPosition position) {
        if (type == null || !isPositionInsideBoard(position)) {
            throw new IllegalArgumentException(
                    "frozen zombie type and position are required");
        }
        if (hasZombieAt(position)
                || getStructureAt(position) != null) {
            throw new IllegalArgumentException(
                    "frozen zombie position is occupied");
        }
        Zombie zombie = new Zombie(type, 0,
                position.getRow(), position.getColumn(), false);
        zombie.encaseInIce();
        addZombie(zombie);
        setTileType(position, TileType.FROZEN);
        pendingResults.add("Frozen " + zombie.getName()
                + " is encased at " + position + " with "
                + zombie.getFrozenShellHitPoints() + " ice HP.");
        return zombie;
    }

    public void setTileType(EntityPosition position, TileType tileType) {
        Tile tile = getTileAt(position);
        if (tile == null) {
            throw new IllegalArgumentException("tile position is outside the board");
        }
        tile.setTileType(tileType);
    }

    public boolean addStructure(BaseStructure structure) {
        if (structure == null || !isPositionInsideBoard(structure.getPosition())) {
            return false;
        }
        if (getStructureAt(structure.getPosition()) != null) {
            return false;
        }
        structures.add(structure);
        if (structure instanceof Grave) {
            setTileType(structure.getPosition(), TileType.GRAVESTONE);
        }
        return true;
    }

    public BaseStructure getStructureAt(EntityPosition position) {
        for (BaseStructure structure : structures) {
            if (!structure.isRemoved() && position != null
                    && position.equals(structure.getPosition())) {
                return structure;
            }
        }
        return null;
    }

    public boolean hasGraveAt(EntityPosition position) {
        return getStructureAt(position) instanceof Grave;
    }

    public BaseStructure removeStructureAt(EntityPosition position) {
        BaseStructure structure = getStructureAt(position);
        if (structure == null) {
            return null;
        }
        structure.markForRemoval();
        if (structure instanceof Grave) {
            destroyGrave((Grave) structure);
        } else {
            structures.remove(structure);
        }
        return structure;
    }

    void removeGraveAt(EntityPosition position) {
        removeStructureAt(position);
    }

    public List<Zombie> drainSpawnedZombies() {
        if (pendingSpawnedZombies.isEmpty()) {
            return Collections.emptyList();
        }
        List<Zombie> result = new ArrayList<>(pendingSpawnedZombies);
        pendingSpawnedZombies.clear();
        return Collections.unmodifiableList(result);
    }

    public List<PlantFamily> drainPlantCooldownResetRequests() {
        if (pendingPlantCooldownResets.isEmpty()) {
            return Collections.emptyList();
        }
        List<PlantFamily> result = new ArrayList<>(pendingPlantCooldownResets);
        pendingPlantCooldownResets.clear();
        return Collections.unmodifiableList(result);
    }

    public List<Entity> getAllEntities() {
        return Collections.unmodifiableList(new ArrayList<>(allEntities));
    }

    public List<BaseStructure> getStructures() {
        List<BaseStructure> activeStructures = new ArrayList<>();
        for (BaseStructure structure : structures) {
            if (!structure.isRemoved()) {
                activeStructures.add(structure);
            }
        }
        return Collections.unmodifiableList(activeStructures);
    }
}
