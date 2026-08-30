package io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer;

import io.github.Plants_Vs_Zombies_2.model.Constants;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.IZombieLevel;

/** Centralized Stage 5 balancing and deterministic board configuration. */
public final class MultiplayerIZombieConfig {
    public static final int DEFAULT_PLANT_RESOURCE = 500;
    public static final int DEFAULT_ZOMBIE_RESOURCE = 300;

    private final IZombieLevel level;
    private final int boardRows;
    private final int boardColumns;
    private final int initialPlantResource;
    private final int initialZombieResource;

    public static MultiplayerIZombieConfig firstBiteDefaults() {
        return new MultiplayerIZombieConfig(IZombieLevel.FIRST_BITE,
                Constants.DEFAULT_BOARD_ROWS, Constants.DEFAULT_BOARD_COLUMNS,
                DEFAULT_PLANT_RESOURCE, DEFAULT_ZOMBIE_RESOURCE);
    }

    public MultiplayerIZombieConfig(IZombieLevel level, int boardRows,
            int boardColumns, int initialPlantResource,
            int initialZombieResource) {
        if (level == null || boardRows <= 0 || boardColumns <= 0
                || level.getRedLineColumn() < 0
                || level.getRedLineColumn() >= boardColumns
                || initialPlantResource < 0 || initialZombieResource < 0) {
            throw new IllegalArgumentException("Invalid multiplayer I, Zombie configuration");
        }
        this.level = level;
        this.boardRows = boardRows;
        this.boardColumns = boardColumns;
        this.initialPlantResource = initialPlantResource;
        this.initialZombieResource = initialZombieResource;
    }

    public IZombieLevel getLevel() { return level; }
    public int getBoardRows() { return boardRows; }
    public int getBoardColumns() { return boardColumns; }
    public int getRedLineColumn() { return level.getRedLineColumn(); }
    public int getInitialPlantResource() { return initialPlantResource; }
    public int getInitialZombieResource() { return initialZombieResource; }
}
