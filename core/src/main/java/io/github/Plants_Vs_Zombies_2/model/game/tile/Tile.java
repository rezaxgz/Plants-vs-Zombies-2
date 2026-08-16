package io.github.Plants_Vs_Zombies_2.model.game.tile;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;

public class Tile implements java.io.Serializable {
    private final EntityPosition position;
    private TileType tileType;
    private BasePlant plant;

    public Tile() {
        this(new EntityPosition(0, 0), TileType.NORMAL);
    }

    public Tile(EntityPosition position, TileType tileType) {
        if (position == null || tileType == null) {
            throw new IllegalArgumentException("tile position and type are required");
        }
        this.position = position;
        this.tileType = tileType;
    }

    public EntityPosition getPosition() {
        return position;
    }

    public TileType getTileType() {
        return tileType;
    }

    public void setTileType(TileType tileType) {
        if (tileType == null) {
            throw new IllegalArgumentException("tileType cannot be null");
        }
        this.tileType = tileType;
    }

    public boolean hasPlant() {
        return plant != null && !plant.isRemoved();
    }

    public BasePlant getPlant() {
        return hasPlant() ? plant : null;
    }

    public void setPlant(BasePlant plant) {
        this.plant = plant;
    }

    public void clearPlant() {
        plant = null;
    }

    public boolean isPlantableTerrain() {
        return tileType == TileType.NORMAL
                || tileType == TileType.LOW_BEACH
                || tileType == TileType.NECROMANCY;
    }
}
