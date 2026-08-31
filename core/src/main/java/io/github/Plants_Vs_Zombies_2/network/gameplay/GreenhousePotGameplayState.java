package io.github.Plants_Vs_Zombies_2.network.gameplay;

import java.util.Objects;

/** Immutable credential-free state for one fixed greenhouse pot. */
public final class GreenhousePotGameplayState {
    private final int row;
    private final int column;
    private final boolean locked;
    private final String plantName;
    private final boolean marigold;
    private final long plantedTimeMillis;
    private final long durationMillis;

    public GreenhousePotGameplayState(int row, int column, boolean locked,
            String plantName, boolean marigold, long plantedTimeMillis,
            long durationMillis) {
        this.row = row;
        this.column = column;
        this.locked = locked;
        this.plantName = plantName == null ? "" : plantName;
        this.marigold = marigold;
        this.plantedTimeMillis = plantedTimeMillis;
        this.durationMillis = durationMillis;
    }

    public int getRow() { return row; }
    public int getColumn() { return column; }
    public boolean isLocked() { return locked; }
    public String getPlantName() { return plantName; }
    public boolean isMarigold() { return marigold; }
    public long getPlantedTimeMillis() { return plantedTimeMillis; }
    public long getDurationMillis() { return durationMillis; }
    public boolean isEmpty() { return plantName == null || plantName.isBlank(); }

    @Override public boolean equals(Object other) {
        if (!(other instanceof GreenhousePotGameplayState value)) return false;
        return row == value.row && column == value.column
                && locked == value.locked && marigold == value.marigold
                && plantedTimeMillis == value.plantedTimeMillis
                && durationMillis == value.durationMillis
                && Objects.equals(plantName, value.plantName);
    }

    @Override public int hashCode() {
        return Objects.hash(row, column, locked, plantName, marigold,
                plantedTimeMillis, durationMillis);
    }
}
