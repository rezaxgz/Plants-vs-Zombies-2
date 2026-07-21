package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import model.game.entities.zombies.ZombieType;

/**
 * Immutable wave definition with exact default-difficulty wave budgeting.
 */
public final class ZombieWave {
    private final List<ZombieType> zombieTypes;
    private final List<ZombieType> availableTypes;
    private final int difficulty;
    private final boolean finalWave;
    private final int maximumHealth;
    private int remainingHealth;

    public ZombieWave(List<ZombieType> zombieTypes,
            int difficulty, boolean finalWave) {
        this(zombieTypes, distinctNonFlagTypes(zombieTypes),
                difficulty, finalWave);
    }

    private ZombieWave(List<ZombieType> zombieTypes,
            List<ZombieType> availableTypes,
            int difficulty, boolean finalWave) {
        if (zombieTypes == null || availableTypes == null
                || difficulty < 0) {
            throw new IllegalArgumentException(
                    "wave values are invalid");
        }
        this.zombieTypes = Collections.unmodifiableList(
                new ArrayList<>(zombieTypes));
        this.availableTypes = Collections.unmodifiableList(
                new ArrayList<>(availableTypes));
        this.difficulty = difficulty;
        this.finalWave = finalWave;
        this.maximumHealth = calculateTotalHealth();
        this.remainingHealth = maximumHealth;
    }

    private int calculateTotalHealth() {
        int total = 0;
        for (ZombieType type : zombieTypes) {
            total += type.getHitpoints();
            if (type.getDefaultArmor() != null) {
                total += type.getDefaultArmor().getBaseHealth();
            }
        }
        return total;
    }

    public static ZombieWave basicWave(
            int difficulty, boolean finalWave) {
        return buildWave(
                List.of(ZombieType.BASIC),
                ZombieType.FLAG, difficulty, finalWave,
                1.0, new Random());
    }

    public static ZombieWave themedWave(String chapter,
            int difficulty, boolean finalWave) {
        List<ZombieType> pool = List.of(getChapterZombies(chapter));
        return buildWave(pool, getFlagZombie(chapter),
                difficulty, finalWave, 1.0, new Random());
    }

    public static ZombieWave seededWave(
            List<ZombieType> sourcePool,
            int difficulty, boolean finalWave,
            int difficultyLevel, Random random) {
        if (random == null) {
            throw new IllegalArgumentException(
                    "random cannot be null");
        }
        double multiplier = DifficultyRules
                .forLevel(difficultyLevel)
                .getZombieWaveCostMultiplier();
        return buildWave(sourcePool, ZombieType.FLAG,
                difficulty, finalWave, multiplier, random);
    }

    private static ZombieWave buildWave(
            List<ZombieType> sourcePool, ZombieType flagType,
            int difficulty, boolean finalWave,
            double costMultiplier, Random random) {
        if (difficulty <= 0 || sourcePool == null
                || sourcePool.isEmpty() || random == null) {
            throw new IllegalArgumentException(
                    "wave difficulty, pool, and random are required");
        }

        List<ZombieType> pool =
                new ArrayList<>(distinctNonFlagTypes(sourcePool));
        Collections.shuffle(pool, random);

        List<ZombieType> result = new ArrayList<>();
        int remaining = difficulty;
        if (finalWave) {
            int flagCost = effectiveCost(flagType, costMultiplier);
            if (flagCost > remaining) {
                throw new IllegalArgumentException(
                        "final-wave difficulty is lower than flag cost");
            }
            result.add(flagType);
            remaining -= flagCost;
        }

        result.addAll(fillBudget(pool, remaining, costMultiplier));
        return new ZombieWave(result, pool, difficulty, finalWave);
    }

    private static List<ZombieType> fillBudget(
            List<ZombieType> pool, int budget,
            double costMultiplier) {
        if (budget == 0) {
            return List.of();
        }

        int[] previousType = new int[budget + 1];
        boolean[] reachable = new boolean[budget + 1];
        java.util.Arrays.fill(previousType, -1);
        reachable[0] = true;

        for (int spent = 0; spent <= budget; spent++) {
            if (!reachable[spent]) {
                continue;
            }
            for (int index = 0; index < pool.size(); index++) {
                int next = spent
                        + effectiveCost(pool.get(index), costMultiplier);
                if (next <= budget && !reachable[next]) {
                    reachable[next] = true;
                    previousType[next] = index;
                }
            }
        }

        if (!reachable[budget]
                && Math.abs(costMultiplier - 1.0) < 0.000001) {
            throw new IllegalStateException(
                    "no exact zombie combination fits default wave budget "
                            + budget);
        }

        int chosenBudget = budget;
        while (chosenBudget > 0 && !reachable[chosenBudget]) {
            chosenBudget--;
        }
        if (chosenBudget == 0) {
            throw new IllegalStateException(
                    "no zombie combination fits wave budget " + budget);
        }

        List<ZombieType> result = new ArrayList<>();
        int cursor = chosenBudget;
        while (cursor > 0) {
            int typeIndex = previousType[cursor];
            if (typeIndex < 0) {
                throw new IllegalStateException(
                        "wave budget reconstruction failed");
            }
            ZombieType type = pool.get(typeIndex);
            result.add(type);
            cursor -= effectiveCost(type, costMultiplier);
        }
        return result;
    }

    private static int effectiveCost(
            ZombieType type, double multiplier) {
        return Math.max(1,
                (int) Math.round(
                        type.getWavePointCost() * multiplier));
    }

    public ZombieWave forDifficulty(int difficultyLevel) {
        DifficultyRules rules =
                DifficultyRules.forLevel(difficultyLevel);
        ZombieType flag = findFlagType();
        return buildWave(availableTypes, flag, difficulty,
                finalWave,
                rules.getZombieWaveCostMultiplier(),
                new Random());
    }

    private ZombieType findFlagType() {
        for (ZombieType type : zombieTypes) {
            if (type == ZombieType.FLAG) {
                return type;
            }
        }
        return ZombieType.FLAG;
    }

    private static List<ZombieType> distinctNonFlagTypes(
            List<ZombieType> types) {
        Set<ZombieType> result = new LinkedHashSet<>();
        if (types != null) {
            for (ZombieType type : types) {
                if (type != null && type != ZombieType.FLAG
                        && !type.isBoss()) {
                    result.add(type);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private static ZombieType getFlagZombie(String chapter) {
        return ZombieType.FLAG;
    }

    private static ZombieType[] getChapterZombies(String chapter) {
        switch (chapter.toLowerCase()) {
            case "egypt":
                return new ZombieType[]{
                    ZombieType.MUMMY, ZombieType.MUMMY_CONEHEAD,
                    ZombieType.MUMMY_BUCKETHEAD, ZombieType.RA,
                    ZombieType.EXPLORER, ZombieType.TOMB_RAISER,
                    ZombieType.CAMEL
                };
            case "iceage":
                return new ZombieType[]{
                    ZombieType.ICEAGE, ZombieType.ICEAGE_CONEHEAD,
                    ZombieType.ICEAGE_BUCKETHEAD,
                    ZombieType.ICEAGE_BLOCKHEAD,
                    ZombieType.HUNTER, ZombieType.TROGLOBITE,
                    ZombieType.DODO, ZombieType.WEASEL_HOARDER
                };
            case "beach":
                return new ZombieType[]{
                    ZombieType.BEACH, ZombieType.BEACH_CONEHEAD,
                    ZombieType.BEACH_BUCKETHEAD, ZombieType.SNORKEL,
                    ZombieType.SURFER, ZombieType.FISHERMAN,
                    ZombieType.OCTOPUS, ZombieType.FAST_SWIMMER
                };
            case "dark":
                return new ZombieType[]{
                    ZombieType.DARK, ZombieType.DARK_CONEHEAD,
                    ZombieType.DARK_BUCKETHEAD,
                    ZombieType.DARK_SHOULDER_ARMOR,
                    ZombieType.DARK_BRICKHEAD, ZombieType.WIZARD,
                    ZombieType.JUGGLER, ZombieType.DARK_KING
                };
            default:
                return new ZombieType[]{
                    ZombieType.BASIC, ZombieType.CONEHEAD,
                    ZombieType.BUCKETHEAD
                };
        }
    }

    public ZombieWave copy() {
        return new ZombieWave(zombieTypes, availableTypes,
                difficulty, finalWave);
    }

    public List<ZombieType> getZombieTypes() {
        return zombieTypes;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public boolean isFinalWave() {
        return finalWave;
    }

    public int getMaximumHealth() {
        return maximumHealth;
    }

    public int getRemainingHealth() {
        return remainingHealth;
    }

    public int getEffectiveWaveCost(int difficultyLevel) {
        double multiplier = DifficultyRules.forLevel(difficultyLevel)
                .getZombieWaveCostMultiplier();
        int total = 0;
        for (ZombieType type : zombieTypes) {
            total += effectiveCost(type, multiplier);
        }
        return total;
    }

    public void recordDamage(int damage) {
        remainingHealth = Math.max(0, remainingHealth - damage);
    }

    public boolean isDamagedEnough(double threshold) {
        return remainingHealth * 4
                <= maximumHealth * (4 - threshold);
    }
}
