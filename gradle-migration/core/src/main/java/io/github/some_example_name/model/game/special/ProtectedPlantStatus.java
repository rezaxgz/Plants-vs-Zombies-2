package io.github.some_example_name.model.game.special;

import io.github.some_example_name.model.game.entities.EntityPosition;

/**
 * Read-only status of one plant that must survive Save Our Seeds.
 */
public final class ProtectedPlantStatus {
    private final String plantType;
    private final EntityPosition originalPosition;
    private final EntityPosition currentPosition;
    private final int currentHitPoints;
    private final int maximumHitPoints;
    private final boolean alive;

    ProtectedPlantStatus(
            String plantType,
            EntityPosition originalPosition,
            EntityPosition currentPosition,
            int currentHitPoints,
            int maximumHitPoints,
            boolean alive) {
        this.plantType = plantType;
        this.originalPosition = originalPosition;
        this.currentPosition = currentPosition;
        this.currentHitPoints = currentHitPoints;
        this.maximumHitPoints = maximumHitPoints;
        this.alive = alive;
    }

    public String getPlantType() {
        return plantType;
    }

    public EntityPosition getOriginalPosition() {
        return originalPosition;
    }

    public EntityPosition getCurrentPosition() {
        return currentPosition;
    }

    public int getDefenseRow() {
        return originalPosition.getRow();
    }

    public int getCurrentHitPoints() {
        return currentHitPoints;
    }

    public int getMaximumHitPoints() {
        return maximumHitPoints;
    }

    public boolean isAlive() {
        return alive;
    }
}
