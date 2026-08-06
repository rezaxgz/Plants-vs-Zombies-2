package io.github.some_example_name.model.game.entities.other;

import io.github.some_example_name.model.game.entities.EntityPosition;

/**
 * A one-use plant packet released from a Vase Breaker vase.
 */
public final class VaseSeedPacket extends CollectibleDrop {
    private final String plantType;
    private boolean expirationReported;

    public VaseSeedPacket(EntityPosition position, String plantType,
            float lifeSpanSeconds) {
        super(position, lifeSpanSeconds);
        if (plantType == null || plantType.isBlank()) {
            throw new IllegalArgumentException("plantType cannot be blank");
        }
        this.plantType = plantType.trim();
    }

    public String getPlantType() {
        return plantType;
    }

    public double getRemainingSeconds() {
        return Math.max(0.0,
                getLifeSpanSeconds() - getElapsedSeconds());
    }

    public boolean consumeExpirationEvent() {
        if (!isRemoved() || isCollected() || expirationReported) {
            return false;
        }
        expirationReported = true;
        return true;
    }
}
