package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;

public final class MatchEntitySnapshot {
    private final String entityId;
    private final String entityType;
    private final MatchRole ownerRole;
    private final int row;
    private final int column;

    public MatchEntitySnapshot(String entityId, String entityType,
            MatchRole ownerRole, int row, int column) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.ownerRole = ownerRole;
        this.row = row;
        this.column = column;
    }

    public String getEntityId() { return entityId; }
    public String getEntityType() { return entityType; }
    public MatchRole getOwnerRole() { return ownerRole; }
    public int getRow() { return row; }
    public int getColumn() { return column; }
}
