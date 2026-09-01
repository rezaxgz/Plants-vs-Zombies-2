package io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFactory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.IZombieCard;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;

/**
 * Deterministic, headless two-player I, Zombie placement rules. It composes
 * the existing board and entity factories but deliberately has no simulation
 * tick; movement and combat synchronization belong to Stage 6.
 */
public final class MultiplayerIZombieGame {
    private final MultiplayerIZombieConfig config;
    private final long seed;
    private final Board board;
    private final Map<String, OwnedEntity> entities = new LinkedHashMap<>();
    private final List<Boolean> brainsAvailable;
    private int plantResource;
    private int zombieResource;
    private long nextEntityNumber = 1;

    public MultiplayerIZombieGame(MultiplayerIZombieConfig config, long seed) {
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        this.config = config;
        this.seed = seed;
        this.board = new Board(config.getBoardRows(), config.getBoardColumns());
        this.plantResource = config.getInitialPlantResource();
        this.zombieResource = config.getInitialZombieResource();
        this.brainsAvailable = new ArrayList<>();
        for (int row = 0; row < config.getBoardRows(); row++) {
            brainsAvailable.add(Boolean.TRUE);
        }
    }

    public String placePlant(String requestedType, int row, int column)
            throws MultiplayerRuleException {
        EntityPosition position = validatePosition(row, column);
        if (column > config.getRedLineColumn()) {
            throw failure(MultiplayerRuleError.INVALID_POSITION,
                    "Plants must be placed on or left of the red line");
        }
        BasePlant plant = PlantFactory.createPlant(requestedType, position);
        if (plant == null) {
            throw failure(MultiplayerRuleError.UNKNOWN_PLANT,
                    "The requested plant type is unknown");
        }
        if (occupied(MatchRole.PLANTS, row, column)
                || !board.canAddPlant(plant)) {
            throw failure(MultiplayerRuleError.POSITION_OCCUPIED,
                    "The plant position is occupied");
        }
        if (plantResource < plant.getCost()) {
            throw failure(MultiplayerRuleError.INSUFFICIENT_RESOURCE,
                    "Not enough plant resource");
        }
        if (!board.addPlant(plant)) {
            throw failure(MultiplayerRuleError.POSITION_OCCUPIED,
                    "The plant position is occupied");
        }
        plantResource -= plant.getCost();
        return register(plant.getName(), MatchRole.PLANTS, row, column, plant);
    }

    /** Plant removal has no refund in the Stage 5 balancing rules. */
    public String removePlant(String entityId) throws MultiplayerRuleException {
        OwnedEntity owned = entities.get(entityId);
        if (owned == null) {
            throw failure(MultiplayerRuleError.ENTITY_NOT_FOUND,
                    "The entity does not exist");
        }
        if (owned.role() != MatchRole.PLANTS) {
            throw failure(MultiplayerRuleError.NOT_ENTITY_OWNER,
                    "The entity does not belong to the plant side");
        }
        if (!board.removeEntity(owned.entity())) {
            throw failure(MultiplayerRuleError.ACTION_NOT_ALLOWED,
                    "The plant cannot be removed");
        }
        entities.remove(entityId);
        return entityId;
    }

    public String placeZombie(String requestedType, int row, int column)
            throws MultiplayerRuleException {
        EntityPosition position = validatePosition(row, column);
        if (column <= config.getRedLineColumn()) {
            throw failure(MultiplayerRuleError.INVALID_POSITION,
                    "Zombies must be placed strictly right of the red line");
        }
        IZombieCard card = config.getLevel().findCard(requestedType);
        if (card == null) {
            throw failure(MultiplayerRuleError.UNKNOWN_ZOMBIE,
                    "The requested zombie type is unknown for this level");
        }
        if (occupied(MatchRole.ZOMBIES, row, column)) {
            throw failure(MultiplayerRuleError.POSITION_OCCUPIED,
                    "The zombie position is occupied");
        }
        if (zombieResource < card.getCost()) {
            throw failure(MultiplayerRuleError.INSUFFICIENT_RESOURCE,
                    "Not enough zombie resource");
        }
        Zombie zombie = new Zombie(card.getType(), 0, row, column, false);
        board.addZombie(zombie);
        zombieResource -= card.getCost();
        return register(card.getType().name(), MatchRole.ZOMBIES,
                row, column, zombie);
    }

    private EntityPosition validatePosition(int row, int column)
            throws MultiplayerRuleException {
        if (row < 0 || column < 0 || row >= config.getBoardRows()
                || column >= config.getBoardColumns()) {
            throw failure(MultiplayerRuleError.INVALID_POSITION,
                    "The position is outside the board");
        }
        return new EntityPosition(row, column);
    }

    private boolean occupied(MatchRole role, int row, int column) {
        return entities.values().stream().anyMatch(entity -> entity.role() == role
                && entity.row() == row && entity.column() == column);
    }

    private String register(String type, MatchRole role, int row,
            int column, Entity entity) {
        String prefix = role == MatchRole.PLANTS ? "plant-" : "zombie-";
        String id = prefix + nextEntityNumber++;
        entities.put(id, new OwnedEntity(id, type, role, row, column, entity));
        return id;
    }

    public List<PlacedMatchEntity> getPlants() {
        return snapshots(MatchRole.PLANTS);
    }

    public List<PlacedMatchEntity> getZombies() {
        return snapshots(MatchRole.ZOMBIES);
    }

    private List<PlacedMatchEntity> snapshots(MatchRole role) {
        List<PlacedMatchEntity> result = new ArrayList<>();
        for (OwnedEntity entity : entities.values()) {
            if (entity.role() == role) {
                result.add(new PlacedMatchEntity(entity.id(), entity.type(),
                        entity.role(), entity.row(), entity.column()));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Mirrors automatic Stage 6 removals back into the Stage 5 placement board
     * without changing either player resource balance.
     */
    public void removeEntityAfterSimulation(String entityId) {
        OwnedEntity owned = entities.remove(entityId);
        if (owned != null) {
            board.removeEntity(owned.entity());
        }
    }

    /** Keeps placement occupancy aligned with authoritative zombie movement. */
    public void synchronizeZombiePosition(String entityId, int row,
            double columnPosition) {
        OwnedEntity owned = entities.get(entityId);
        if (owned == null || owned.role() != MatchRole.ZOMBIES
                || !(owned.entity() instanceof Zombie)) {
            return;
        }
        Zombie zombie = (Zombie) owned.entity();
        // The authoritative simulation lets a zombie travel from column zero
        // to the brain at -0.25. EntityPosition is an older board-only type
        // that deliberately rejects negative columns, so keep that mirror at
        // the lawn edge while the simulation and wire snapshot retain the
        // precise sub-zero position.
        double boardColumn = Math.max(0.0, columnPosition);
        zombie.moveToLane(row);
        zombie.moveTo(boardColumn);
        entities.put(entityId, new OwnedEntity(owned.id(), owned.type(),
                owned.role(), row, (int) Math.floor(boardColumn), zombie));
    }

    public MultiplayerIZombieConfig getConfig() { return config; }
    public long getSeed() { return seed; }
    public int getPlantResource() { return plantResource; }
    public int getZombieResource() { return zombieResource; }
    public List<Boolean> getBrainsAvailable() { return List.copyOf(brainsAvailable); }

    private static MultiplayerRuleException failure(
            MultiplayerRuleError error, String message) {
        return new MultiplayerRuleException(error, message);
    }

    private record OwnedEntity(String id, String type, MatchRole role,
            int row, int column, Entity entity) { }
}
