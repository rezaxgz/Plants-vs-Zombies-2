package io.github.Plants_Vs_Zombies_2.model.game.minigame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;

/**
 * The three progressively harder Vase Breaker levels required by the spec.
 */
public enum VaseBreakerLevel {
    LEVEL_ONE(
            1,
            "Questionable Beginnings",
            2, 2, 3, 2, 1,
            20.0f,
            List.of("Peashooter", "Repeater", "Snow Pea",
                    "Wall-nut", "Potato Mine", "Cherry Bomb"),
            List.of(ZombieType.BASIC, ZombieType.CONEHEAD,
                    ZombieType.IMP)),
    LEVEL_TWO(
            2,
            "Cracks in the Plan",
            2, 5, 3, 3, 2,
            16.0f,
            List.of("Repeater", "Snow Pea", "Threepeater",
                    "Wall-nut", "Tall-nut", "Cherry Bomb",
                    "Jalapeno", "Kernel-pult"),
            List.of(ZombieType.BASIC, ZombieType.CONEHEAD,
                    ZombieType.BUCKETHEAD, ZombieType.IMP,
                    ZombieType.NEWSPAPER)),
    LEVEL_THREE(
            3,
            "Giant Surprise",
            2, 8, 4, 3, 3,
            12.0f,
            List.of("Repeater", "Threepeater", "Snow Pea",
                    "Fire Peashooter", "Tall-nut", "Cherry Bomb",
                    "Jalapeno", "Melon-pult", "Winter Melon"),
            List.of(ZombieType.CONEHEAD, ZombieType.BUCKETHEAD,
                    ZombieType.BRICKHEAD, ZombieType.IMP,
                    ZombieType.ALL_STAR, ZombieType.NEWSPAPER));

    public static final int LEVEL_COUNT = 3;

    private final int number;
    private final String name;
    private final int normalEmptyVases;
    private final int normalZombieVases;
    private final int normalSeedVases;
    private final int plantVases;
    private final int giantVases;
    private final float seedPacketLifeSpanSeconds;
    private final List<String> plantPool;
    private final List<ZombieType> zombiePool;

    VaseBreakerLevel(int number, String name,
            int normalEmptyVases, int normalZombieVases,
            int normalSeedVases, int plantVases, int giantVases,
            float seedPacketLifeSpanSeconds,
            List<String> plantPool, List<ZombieType> zombiePool) {
        this.number = number;
        this.name = name;
        this.normalEmptyVases = normalEmptyVases;
        this.normalZombieVases = normalZombieVases;
        this.normalSeedVases = normalSeedVases;
        this.plantVases = plantVases;
        this.giantVases = giantVases;
        this.seedPacketLifeSpanSeconds = seedPacketLifeSpanSeconds;
        this.plantPool = Collections.unmodifiableList(plantPool);
        this.zombiePool = Collections.unmodifiableList(zombiePool);
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public int getNormalEmptyVases() {
        return normalEmptyVases;
    }

    public int getNormalZombieVases() {
        return normalZombieVases;
    }

    public int getNormalSeedVases() {
        return normalSeedVases;
    }

    public int getPlantVases() {
        return plantVases;
    }

    public int getGiantVases() {
        return giantVases;
    }

    public int getTotalVaseCount() {
        return normalEmptyVases + normalZombieVases
                + normalSeedVases + plantVases + giantVases;
    }

    public float getSeedPacketLifeSpanSeconds() {
        return seedPacketLifeSpanSeconds;
    }

    public List<String> getPlantPool() {
        return plantPool;
    }

    public List<ZombieType> getZombiePool() {
        return zombiePool;
    }

    public static VaseBreakerLevel find(int number) {
        return Arrays.stream(values())
                .filter(level -> level.number == number)
                .findFirst()
                .orElse(null);
    }
}
