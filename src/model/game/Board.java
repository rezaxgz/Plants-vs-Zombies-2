package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Constants;
import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.other.Sun;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.sunProducer.SunProducer;
import model.game.structure.BaseStructure;
import model.game.tile.Tile;

public class Board {
    private final int numberOfRows;
    private final int numberOfColumns;
    private final List<Tile> tiles;
    private final List<Entity> allEntities;
    private final List<BaseStructure> structures;
    private final List<String> pendingResults;

    public Board() {
        this(Constants.DEFAULT_BOARD_ROWS, Constants.DEFAULT_BOARD_COLUMNS);
    }

    public Board(int numberOfRows, int numberOfColumns) {
        if (numberOfRows <= 0 || numberOfColumns <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }
        this.numberOfRows = numberOfRows;
        this.numberOfColumns = numberOfColumns;
        this.tiles = new ArrayList<>();
        this.allEntities = new ArrayList<>();
        this.structures = new ArrayList<>();
        this.pendingResults = new ArrayList<>();
    }

    public void update(float deltaSeconds) {
        validateDeltaSeconds(deltaSeconds);

        List<Entity> entitiesToAdd = new ArrayList<>();
        List<Entity> updateSnapshot = new ArrayList<>(allEntities);

        for (Entity entity : updateSnapshot) {
            if (entity.isRemoved()) {
                continue;
            }

            entity.update(deltaSeconds);

            if (entity instanceof SunProducer) {
                SunProducer producer = (SunProducer) entity;
                List<Sun> producedSuns = producer.drainProducedSuns();
                entitiesToAdd.addAll(producedSuns);
                for (int i = 0; i < producedSuns.size(); i++) {
                    pendingResults.add(buildSunProductionResult(producer));
                }
            }
        }

        allEntities.removeIf(Entity::isRemoved);
        for (Entity entity : entitiesToAdd) {
            addEntity(entity);
        }
    }

    public List<String> drainResults() {
        if (pendingResults.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>(pendingResults);
        pendingResults.clear();
        return Collections.unmodifiableList(results);
    }

    private static String buildSunProductionResult(SunProducer producer) {
        return "plant " + producer.getType().getDisplayName() + " produced a sun at " + producer.getEntityPosition();
    }

    public void addEntity(Entity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null");
        }
        validatePosition(entity.getEntityPosition());
        allEntities.add(entity);
    }

    public boolean addPlant(BasePlant plant) {
        if (plant == null) {
            return false;
        }
        validatePosition(plant.getEntityPosition());

        EntityPosition position = plant.getEntityPosition();
        if (position != null) {
            for (BasePlant existingPlant : getPlants()) {
                if (!existingPlant.isRemoved() && position.equals(existingPlant.getEntityPosition())) {
                    return false;
                }
            }
        }

        addEntity(plant);
        return true;
    }

    public boolean removeEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        entity.markForRemoval();
        return allEntities.remove(entity);
    }

    public boolean containsEntity(Entity entity) {
        return entity != null && allEntities.contains(entity) && !entity.isRemoved();
    }

    public List<BasePlant> getPlants() {
        List<BasePlant> plants = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof BasePlant && !entity.isRemoved()) {
                plants.add((BasePlant) entity);
            }
        }
        return Collections.unmodifiableList(plants);
    }

    public List<Sun> getSuns() {
        List<Sun> suns = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof Sun && !entity.isRemoved()) {
                suns.add((Sun) entity);
            }
        }
        return Collections.unmodifiableList(suns);
    }

    public List<Sun> getSunsAt(EntityPosition position) {
        if (position == null) {
            return Collections.emptyList();
        }
        List<Sun> suns = new ArrayList<>();
        for (Sun sun : getSuns()) {
            if (position.equals(sun.getEntityPosition())) {
                suns.add(sun);
            }
        }
        return Collections.unmodifiableList(suns);
    }

    private void validatePosition(EntityPosition position) {
        if (position == null) {
            return;
        }
        if (position.getRow() >= numberOfRows || position.getColumn() >= numberOfColumns) {
            throw new IllegalArgumentException("Entity position is outside the board: " + position);
        }
    }

    private static void validateDeltaSeconds(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
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

    public List<Entity> getAllEntities() {
        return Collections.unmodifiableList(new ArrayList<>(allEntities));
    }

    public List<BaseStructure> getStructures() {
        return Collections.unmodifiableList(structures);
    }
}
