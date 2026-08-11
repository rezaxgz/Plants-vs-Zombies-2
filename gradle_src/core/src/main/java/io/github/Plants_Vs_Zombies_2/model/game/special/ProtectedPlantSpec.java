package io.github.Plants_Vs_Zombies_2.model.game.special;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;

/**
 * Immutable initial plant and position for a Save Our Seeds level.
 */
public final class ProtectedPlantSpec {
    private final String plantType;
    private final EntityPosition position;

    public ProtectedPlantSpec(
            String plantType,
            EntityPosition position) {
        if (plantType == null
                || plantType.isBlank()
                || position == null) {
            throw new IllegalArgumentException(
                    "protected plant type and position are required");
        }
        this.plantType = plantType.trim();
        this.position = position;
    }

    public String getPlantType() {
        return plantType;
    }

    public EntityPosition getPosition() {
        return position;
    }
}
