package model.game.entities.plants.shooter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import model.game.entities.plants.PlantDefinition;
import model.game.entities.plants.PlantTag;

public enum ShooterPlantType implements PlantDefinition {
    PEASHOOTER(
            6, "Peashooter", EnumSet.of(PlantTag.PEA), 100, 300, 20,
            1.5f, 5.0f, ShooterBehavior.FORWARD, ShooterProjectileType.NORMAL,
            1, -1.0, 0.0, 0.0, 0, 10, 1,
            upgrade(0, 10, 0), upgrade(150, 0, 0), upgrade(0, 0, -25)),

    REPEATER(
            7, "Repeater", EnumSet.of(PlantTag.PEA), 200, 300, 20,
            1.5f, 5.0f, ShooterBehavior.FORWARD, ShooterProjectileType.NORMAL,
            2, -1.0, 0.0, 0.0, 0, 20, 20,
            upgrade(0, 10, 0), upgrade(200, 0, 0), upgrade(0, 0, -25)),

    THREEPEATER(
            8, "Threepeater", EnumSet.of(PlantTag.PEA), 300, 300, 20,
            1.5f, 5.0f, ShooterBehavior.THREE_LANES, ShooterProjectileType.NORMAL,
            1, -1.0, 0.0, 0.0, 0, 10, 1,
            upgrade(0, 0, -25), upgrade(0, 10, 0), upgrade(200, 0, 0)),

    SNOW_PEA(
            9, "Snow Pea", EnumSet.of(PlantTag.ICE, PlantTag.PEA), 150, 300, 20,
            1.5f, 5.0f, ShooterBehavior.FORWARD, ShooterProjectileType.ICE,
            1, -1.0, 0.0, 4.0, 0, 10, 1,
            upgrade(0, 10, 0), chillUpgrade(2.0), upgrade(0, 0, -25)),

    ROTOBAGA(
            10, "Rotobaga", Collections.emptySet(), 150, 300, 10,
            1.5f, 5.0f, ShooterBehavior.FOUR_DIAGONALS, ShooterProjectileType.NORMAL,
            3, -1.0, 0.0, 0.0, 0, 8, 1,
            upgrade(0, 10, 0), upgrade(150, 0, 0), upgrade(0, 0, -25)),

    PEA_POD(
            11, "Pea Pod", EnumSet.of(PlantTag.PEA, PlantTag.STACK), 125, 300, 20,
            1.5f, 5.0f, ShooterBehavior.FORWARD, ShooterProjectileType.NORMAL,
            1, -1.0, 0.0, 0.0, 0, 1, 20,
            upgrade(0, 10, 0), upgrade(200, 0, 0), upgrade(0, 0, -25)),

    SPLIT_PEA(
            12, "Split Pea", EnumSet.of(PlantTag.PEA), 125, 300, 20,
            1.5f, 5.0f, ShooterBehavior.SPLIT, ShooterProjectileType.NORMAL,
            1, -1.0, 0.0, 0.0, 0, 10, 1,
            upgrade(0, 10, 0), upgrade(200, 0, 0), upgrade(0, 0, -25)),

    CITRON(
            13, "Citron", EnumSet.of(PlantTag.CHARGE), 350, 300, 800,
            9.0f, 5.0f, ShooterBehavior.FORWARD, ShooterProjectileType.NORMAL,
            1, -1.0, 0.0, 0.0, 0, 1, 6,
            intervalUpgrade(-1.0f), upgrade(0, 150, 0), upgrade(0, 0, -50)),

    BOWLING_BULB(
            16, "Bowling Bulb", EnumSet.of(PlantTag.CHARGE), 200, 300, 40,
            2.0f, 5.0f, ShooterBehavior.BOWLING, ShooterProjectileType.NORMAL,
            1, -1.0, 0.0, 0.0, 0, 3, 1,
            intervalUpgrade(-1.0f), upgrade(0, 15, 0), upgrade(0, 0, -25)),

    FIRE_PEASHOOTER(
            18, "Fire Peashooter", EnumSet.of(PlantTag.FIRE, PlantTag.PEA), 175, 300, 40,
            1.5f, 5.0f, ShooterBehavior.FORWARD, ShooterProjectileType.FIRE,
            1, -1.0, 0.0, 0.0, 0, 10, 1,
            upgrade(0, 10, 0), upgrade(200, 0, 0), upgrade(0, 0, -25)),

    STARFRUIT(
            19, "Starfruit", Collections.emptySet(), 150, 300, 20,
            1.5f, 5.0f, ShooterBehavior.FIVE_WAY, ShooterProjectileType.NORMAL,
            1, -1.0, 0.0, 0.0, 0, 8, 1,
            speedUpgrade(0.9f), upgrade(0, 10, 0), upgrade(0, 0, -25)),

    GOO_PEASHOOTER(
            20, "Goo Peashooter", EnumSet.of(PlantTag.POISON), 125, 300, 20,
            1.5f, 5.0f, ShooterBehavior.FORWARD, ShooterProjectileType.POISON,
            1, -1.0, 0.0, 0.0, 5, 10, 1,
            poisonUpgrade(5), upgrade(150, 0, 0), upgrade(0, 0, -25)),

    MEGA_GATLING_PEA(
            21, "Mega Gatling Pea", EnumSet.of(PlantTag.PEA), 400, 300, 20,
            1.5f, 5.0f, ShooterBehavior.FORWARD, ShooterProjectileType.NORMAL,
            4, -1.0, 0.0, 0.0, 0, 20, 20,
            upgrade(0, 10, 0), plantFoodChanceUpgrade(0.05), upgrade(0, 0, -50)),

    SEA_SHROOM(
            22, "Sea-shroom", EnumSet.of(PlantTag.SHROOM, PlantTag.WATER), 0, 300, 20,
            1.5f, 15.0f, ShooterBehavior.SHORT_RANGE, ShooterProjectileType.NORMAL,
            1, 3.0, 60.0, 0.0, 0, 10, 1,
            rangeUpgrade(1.0), upgrade(0, 5, 0), lifespanUpgrade(10.0)),

    PUFF_SHROOM(
            23, "Puff-shroom", EnumSet.of(PlantTag.SHROOM), 0, 300, 20,
            1.5f, 5.0f, ShooterBehavior.SHORT_RANGE, ShooterProjectileType.NORMAL,
            1, 3.0, 60.0, 0.0, 0, 10, 1,
            lifespanUpgrade(10.0), upgrade(0, 10, 0), rangeUpgrade(1.0)),

    APPEASE_MINT(
            62, "Appease-mint", Collections.emptySet(), 0, 0, 0,
            0.0f, 85.0f, ShooterBehavior.FAMILY_BOOST, ShooterProjectileType.NORMAL,
            0, -1.0, 0.0, 0.0, 0, 0, 1,
            familyDurationUpgrade(1.0f), rechargeUpgrade(-5.0f), resetCooldownUpgrade());

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;
    public static final double POISON_DURATION_SECONDS = 5.0;
    public static final double POISON_TICK_INTERVAL_SECONDS = 1.0;
    public static final float BASE_FAMILY_BOOST_DURATION_SECONDS = 5.0f;

    private final int id;
    private final String displayName;
    private final Set<PlantTag> tags;
    private final int cost;
    private final int baseHP;
    private final int damage;
    private final float actionIntervalSeconds;
    private final float rechargeSeconds;
    private final ShooterBehavior behavior;
    private final ShooterProjectileType projectileType;
    private final int shotsPerDirection;
    private final double rangeTiles;
    private final double lifespanSeconds;
    private final double chillDurationSeconds;
    private final int poisonDamagePerTick;
    private final int plantFoodShotCount;
    private final int plantFoodDamageMultiplier;
    private final ShooterUpgrade[] upgrades;

    ShooterPlantType(int id, String displayName, Set<PlantTag> tags,
            int cost, int baseHP, int damage, float actionIntervalSeconds,
            float rechargeSeconds, ShooterBehavior behavior,
            ShooterProjectileType projectileType, int shotsPerDirection,
            double rangeTiles, double lifespanSeconds, double chillDurationSeconds,
            int poisonDamagePerTick, int plantFoodShotCount,
            int plantFoodDamageMultiplier, ShooterUpgrade levelTwo,
            ShooterUpgrade levelThree, ShooterUpgrade levelFour) {
        this.id = id;
        this.displayName = displayName;
        this.tags = immutableTags(tags);
        this.cost = cost;
        this.baseHP = baseHP;
        this.damage = damage;
        this.actionIntervalSeconds = actionIntervalSeconds;
        this.rechargeSeconds = rechargeSeconds;
        this.behavior = behavior;
        this.projectileType = projectileType;
        this.shotsPerDirection = shotsPerDirection;
        this.rangeTiles = rangeTiles;
        this.lifespanSeconds = lifespanSeconds;
        this.chillDurationSeconds = chillDurationSeconds;
        this.poisonDamagePerTick = poisonDamagePerTick;
        this.plantFoodShotCount = plantFoodShotCount;
        this.plantFoodDamageMultiplier = plantFoodDamageMultiplier;
        this.upgrades = new ShooterUpgrade[] {ShooterUpgrade.NONE, levelTwo, levelThree, levelFour};
    }

    private static ShooterUpgrade upgrade(int hp, int damage, int cost) {
        return new ShooterUpgrade(hp, damage, cost, 0.0f, 1.0f,
                0.0, 0, 0.0, 0.0, 0.0f, 0.0f, 0.0, false);
    }

    private static ShooterUpgrade intervalUpgrade(float seconds) {
        return new ShooterUpgrade(0, 0, 0, seconds, 1.0f,
                0.0, 0, 0.0, 0.0, 0.0f, 0.0f, 0.0, false);
    }

    private static ShooterUpgrade speedUpgrade(float multiplier) {
        return new ShooterUpgrade(0, 0, 0, 0.0f, multiplier,
                0.0, 0, 0.0, 0.0, 0.0f, 0.0f, 0.0, false);
    }

    private static ShooterUpgrade chillUpgrade(double seconds) {
        return new ShooterUpgrade(0, 0, 0, 0.0f, 1.0f,
                seconds, 0, 0.0, 0.0, 0.0f, 0.0f, 0.0, false);
    }

    private static ShooterUpgrade poisonUpgrade(int damagePerTick) {
        return new ShooterUpgrade(0, 0, 0, 0.0f, 1.0f,
                0.0, damagePerTick, 0.0, 0.0, 0.0f, 0.0f, 0.0, false);
    }

    private static ShooterUpgrade rangeUpgrade(double tiles) {
        return new ShooterUpgrade(0, 0, 0, 0.0f, 1.0f,
                0.0, 0, tiles, 0.0, 0.0f, 0.0f, 0.0, false);
    }

    private static ShooterUpgrade lifespanUpgrade(double seconds) {
        return new ShooterUpgrade(0, 0, 0, 0.0f, 1.0f,
                0.0, 0, 0.0, seconds, 0.0f, 0.0f, 0.0, false);
    }

    private static ShooterUpgrade rechargeUpgrade(float seconds) {
        return new ShooterUpgrade(0, 0, 0, 0.0f, 1.0f,
                0.0, 0, 0.0, 0.0, seconds, 0.0f, 0.0, false);
    }

    private static ShooterUpgrade familyDurationUpgrade(float seconds) {
        return new ShooterUpgrade(0, 0, 0, 0.0f, 1.0f,
                0.0, 0, 0.0, 0.0, 0.0f, seconds, 0.0, false);
    }

    private static ShooterUpgrade plantFoodChanceUpgrade(double chance) {
        return new ShooterUpgrade(0, 0, 0, 0.0f, 1.0f,
                0.0, 0, 0.0, 0.0, 0.0f, 0.0f, chance, false);
    }

    private static ShooterUpgrade resetCooldownUpgrade() {
        return new ShooterUpgrade(0, 0, 0, 0.0f, 1.0f,
                0.0, 0, 0.0, 0.0, 0.0f, 0.0f, 0.0, true);
    }

    private static Set<PlantTag> immutableTags(Set<PlantTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(tags));
    }

    public static Optional<ShooterPlantType> findByName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String normalizedName = normalizeName(rawName);
        for (ShooterPlantType type : values()) {
            if (normalizeName(type.name()).equals(normalizedName)
                    || normalizeName(type.displayName).equals(normalizedName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public int getCost(int level) {
        return Math.max(0, cost + sumInt(level, UpgradeValue.COST));
    }

    public int getCost() {
        return cost;
    }

    public int getBaseHP(int level) {
        return Math.max(0, baseHP + sumInt(level, UpgradeValue.HIT_POINTS));
    }

    public int getBaseHP() {
        return baseHP;
    }

    public int getDamage(int level) {
        return Math.max(0, damage + sumInt(level, UpgradeValue.DAMAGE));
    }

    public int getDamage() {
        return damage;
    }

    public float getActionIntervalSeconds(int level) {
        validateLevel(level);
        float interval = actionIntervalSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            ShooterUpgrade current = upgrades[currentLevel - 1];
            interval += current.getActionIntervalDeltaSeconds();
            interval *= current.getActionIntervalMultiplier();
        }
        return Math.max(0.0f, interval);
    }

    public float getActionIntervalSeconds() {
        return actionIntervalSeconds;
    }

    public float getRechargeSeconds(int level) {
        validateLevel(level);
        float value = rechargeSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getRechargeDeltaSeconds();
        }
        return Math.max(0.0f, value);
    }

    public float getRechargeSeconds() {
        return rechargeSeconds;
    }

    public double getChillDurationSeconds(int level) {
        validateLevel(level);
        double value = chillDurationSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getChillDurationDeltaSeconds();
        }
        return Math.max(0.0, value);
    }

    public int getPoisonDamagePerTick(int level) {
        validateLevel(level);
        int value = poisonDamagePerTick;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getPoisonDamagePerTickDelta();
        }
        return Math.max(0, value);
    }

    public double getRangeTiles(int level) {
        validateLevel(level);
        if (rangeTiles < 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        double value = rangeTiles;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getRangeDeltaTiles();
        }
        return Math.max(0.0, value);
    }

    public double getLifespanSeconds(int level) {
        validateLevel(level);
        double value = lifespanSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getLifespanDeltaSeconds();
        }
        return Math.max(0.0, value);
    }

    public float getFamilyBoostDurationSeconds(int level) {
        validateLevel(level);
        float value = behavior == ShooterBehavior.FAMILY_BOOST
                ? BASE_FAMILY_BOOST_DURATION_SECONDS : 0.0f;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getFamilyBoostDurationDeltaSeconds();
        }
        return Math.max(0.0f, value);
    }

    public double getPlantFoodChance(int level) {
        validateLevel(level);
        double value = 0.0;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getPlantFoodChanceDelta();
        }
        return Math.max(0.0, value);
    }

    public boolean resetsFamilyCooldowns(int level) {
        validateLevel(level);
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            if (upgrades[currentLevel - 1].resetsFamilyCooldowns()) {
                return true;
            }
        }
        return false;
    }

    private int sumInt(int level, UpgradeValue value) {
        validateLevel(level);
        int result = 0;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            ShooterUpgrade upgrade = upgrades[currentLevel - 1];
            switch (value) {
            case HIT_POINTS:
                result += upgrade.getHitPointDelta();
                break;
            case DAMAGE:
                result += upgrade.getDamageDelta();
                break;
            case COST:
                result += upgrade.getCostDelta();
                break;
            default:
                throw new IllegalStateException("Unknown upgrade value: " + value);
            }
        }
        return result;
    }

    public static void validateLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("shooter level must be between 1 and 4");
        }
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<PlantTag> getTags() {
        return tags;
    }

    public ShooterBehavior getBehavior() {
        return behavior;
    }

    public ShooterProjectileType getProjectileType() {
        return projectileType;
    }

    public int getShotsPerDirection() {
        return shotsPerDirection;
    }

    public int getPlantFoodShotCount() {
        return plantFoodShotCount;
    }

    public int getPlantFoodDamageMultiplier() {
        return plantFoodDamageMultiplier;
    }

    private enum UpgradeValue {
        HIT_POINTS,
        DAMAGE,
        COST
    }
}
