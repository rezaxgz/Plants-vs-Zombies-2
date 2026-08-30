package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;

public final class MatchEntitySnapshot {
    private final String entityId;
    private final String entityType;
    private final MatchRole ownerRole;
    private final int row;
    private final int column;
    private final double columnPosition;
    private final int health;
    private final int maximumHealth;

    /** Stage 5-compatible constructor retained for older callers/tests. */
    public MatchEntitySnapshot(String entityId, String entityType,
            MatchRole ownerRole, int row, int column) {
        this(entityId, entityType, ownerRole, row, column,
                column, 0, 0);
    }

    public MatchEntitySnapshot(String entityId, String entityType,
            MatchRole ownerRole, int row, int column, double columnPosition,
            int health, int maximumHealth) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.ownerRole = ownerRole;
        this.row = row;
        this.column = column;
        this.columnPosition = columnPosition;
        this.health = health;
        this.maximumHealth = maximumHealth;
    }

    public String getEntityId() { return entityId; }
    public String getEntityType() { return entityType; }
    public MatchRole getOwnerRole() { return ownerRole; }
    public int getRow() { return row; }
    public int getColumn() { return column; }
    public double getColumnPosition() { return columnPosition; }
    public int getHealth() { return health; }
    public int getMaximumHealth() { return maximumHealth; }
}
