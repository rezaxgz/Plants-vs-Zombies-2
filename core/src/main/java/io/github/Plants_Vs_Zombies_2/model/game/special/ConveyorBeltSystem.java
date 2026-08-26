package io.github.Plants_Vs_Zombies_2.model.game.special;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Produces free plant packets at a configurable interval. Normal conveyor
 * levels retain the original one-packet/twelve-second behavior; boss levels
 * can opt into a slightly faster supply without affecting other minigames.
 */
public final class ConveyorBeltSystem implements java.io.Serializable {
    public static final double PACKET_INTERVAL_SECONDS = 12.0;
    public static final int MAXIMUM_PACKET_COUNT = 8;

    private final List<String> availablePlantTypes;
    private final List<ConveyorPlantPacket> packets;
    private final List<String> pendingMessages;
    private final Random random;
    private double packetIntervalSeconds;
    private int initialPacketCount;

    private double secondsSinceLastPacket;
    private long nextSequenceNumber = 1;

    public ConveyorBeltSystem(
            List<String> availablePlantTypes) {
        this(availablePlantTypes, PACKET_INTERVAL_SECONDS, 1, new Random());
    }

    public ConveyorBeltSystem(
            List<String> availablePlantTypes,
            double packetIntervalSeconds, int initialPacketCount) {
        this(availablePlantTypes, packetIntervalSeconds, initialPacketCount,
                new Random());
    }

    ConveyorBeltSystem(
            List<String> availablePlantTypes,
            Random random) {
        this(availablePlantTypes, PACKET_INTERVAL_SECONDS, 1, random);
    }

    ConveyorBeltSystem(
            List<String> availablePlantTypes,
            double packetIntervalSeconds, int initialPacketCount,
            Random random) {
        if (availablePlantTypes == null
                || availablePlantTypes.isEmpty()
                || !Double.isFinite(packetIntervalSeconds)
                || packetIntervalSeconds <= 0.0
                || initialPacketCount < 1
                || initialPacketCount > MAXIMUM_PACKET_COUNT
                || random == null) {
            throw new IllegalArgumentException(
                    "conveyor plants and random are required");
        }

        this.availablePlantTypes = validatePlantTypes(
                availablePlantTypes);
        this.random = random;
        this.packetIntervalSeconds = packetIntervalSeconds;
        this.initialPacketCount = initialPacketCount;
        packets = new ArrayList<>();
        pendingMessages = new ArrayList<>();
        for (int index = 0; index < initialPacketCount; index++) {
            producePacket();
        }
    }

    private static List<String> validatePlantTypes(
            List<String> plantTypes) {
        List<String> result = new ArrayList<>();
        for (String plantType : plantTypes) {
            if (plantType == null
                    || plantType.isBlank()) {
                throw new IllegalArgumentException(
                        "conveyor plant type cannot be blank");
            }
            result.add(plantType.trim());
        }
        return Collections.unmodifiableList(result);
    }

    public void update(double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds)
                || deltaSeconds < 0.0) {
            throw new IllegalArgumentException(
                    "deltaSeconds must be finite and non-negative");
        }

        if (packets.size() >= MAXIMUM_PACKET_COUNT) {
            return;
        }

        secondsSinceLastPacket += deltaSeconds;
        double interval = getPacketIntervalSeconds();
        while (secondsSinceLastPacket >= interval
                && packets.size() < MAXIMUM_PACKET_COUNT) {
            secondsSinceLastPacket -= interval;
            producePacket();
        }
    }

    private void producePacket() {
        String plantType = availablePlantTypes.get(
                random.nextInt(
                        availablePlantTypes.size()));
        ConveyorPlantPacket packet = new ConveyorPlantPacket(
                nextSequenceNumber++,
                plantType);
        packets.add(packet);
        pendingMessages.add(
                "Conveyor Belt produced "
                        + plantType
                        + " in slot "
                        + packets.size() + ".");
    }

    public ConveyorPlantPacket getPacket(
            int oneBasedIndex) {
        if (oneBasedIndex < 1
                || oneBasedIndex > packets.size()) {
            return null;
        }
        return packets.get(oneBasedIndex - 1);
    }

    public ConveyorPlantPacket consumePacket(
            int oneBasedIndex) {
        if (oneBasedIndex < 1
                || oneBasedIndex > packets.size()) {
            return null;
        }
        return packets.remove(oneBasedIndex - 1);
    }

    public List<ConveyorPlantPacket> getPackets() {
        return Collections.unmodifiableList(
                new ArrayList<>(packets));
    }

    public double getSecondsUntilNextPacket() {
        if (packets.size() >= MAXIMUM_PACKET_COUNT) {
            return 0.0;
        }
        return Math.max(
                0.0,
                getPacketIntervalSeconds()
                        - secondsSinceLastPacket);
    }

    public double getPacketIntervalSeconds() {
        // Old serialized games do not have this field. Preserve their
        // original twelve-second conveyor behavior.
        return packetIntervalSeconds > 0.0
                ? packetIntervalSeconds : PACKET_INTERVAL_SECONDS;
    }

    public int getInitialPacketCount() {
        return initialPacketCount >= 1
                ? initialPacketCount : 1;
    }

    public List<String> drainMessages() {
        if (pendingMessages.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(pendingMessages);
        pendingMessages.clear();
        return Collections.unmodifiableList(result);
    }

    public List<String> getAvailablePlantTypes() {
        return availablePlantTypes;
    }
}
