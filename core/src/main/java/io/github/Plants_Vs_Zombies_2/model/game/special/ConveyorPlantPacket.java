package io.github.Plants_Vs_Zombies_2.model.game.special;

/**
 * One plant packet currently waiting on the Conveyor Belt.
 */
public final class ConveyorPlantPacket {
    private final long sequenceNumber;
    private final String plantType;

    ConveyorPlantPacket(
            long sequenceNumber, String plantType) {
        if (sequenceNumber <= 0
                || plantType == null
                || plantType.isBlank()) {
            throw new IllegalArgumentException(
                    "conveyor packet values are invalid");
        }
        this.sequenceNumber = sequenceNumber;
        this.plantType = plantType;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public String getPlantType() {
        return plantType;
    }
}
