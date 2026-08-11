package model.game.entities.plants.sunProducer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import model.game.entities.plants.PlantDefinition;
import model.game.entities.plants.PlantTag;

public enum SunProducerPlantType implements PlantDefinition {
    SUNFLOWER(
            1, "Sunflower", EnumSet.of(PlantTag.DAY), 50, 300, 0,
            "Produces 50 sun every 24 seconds.",
            "Immediately produces 150 sun.",
            "Production time -2s", "HP +150", "Double sun chance",
            24.0f, 5.0f, SunProductionBehavior.PERIODIC,
            new int[] {50}, new float[0], 150, 0.0f,
            upgrade(0, 0, -2.0f, 0.0f, 0.0f, 0, false, 0.0f, false),
            upgrade(150, 0, 0.0f, 0.0f, 0.0f, 0, false, 0.0f, false),
            upgrade(0, 0, 0.0f, 0.0f, 0.0f, 0, true, 0.0f, false)),

    TWIN_SUNFLOWER(
            2, "Twin Sunflower", EnumSet.of(PlantTag.DAY), 125, 300, 0,
            "Produces 100 sun every cycle.",
            "Immediately produces 250 sun.",
            "Production time -2s", "HP +150", "Cost -25",
            24.0f, 15.0f, SunProductionBehavior.PERIODIC,
            new int[] {100}, new float[0], 250, 0.0f,
            upgrade(0, 0, -2.0f, 0.0f, 0.0f, 0, false, 0.0f, false),
            upgrade(150, 0, 0.0f, 0.0f, 0.0f, 0, false, 0.0f, false),
            upgrade(0, -25, 0.0f, 0.0f, 0.0f, 0, false, 0.0f, false)),

    SUN_SHROOM(
            3, "Sun-shroom", EnumSet.of(PlantTag.SHROOM, PlantTag.WRAMP_UP, PlantTag.NIGHT),
            25, 300, 0,
            "Grows through three stages and produces 25, 50, then 75 sun.",
            "Immediately reaches its final stage and produces 225 sun.",
            "Grow time -5s", "HP +150", "Double sun chance",
            24.0f, 5.0f, SunProductionBehavior.PERIODIC,
            new int[] {25, 50, 75}, new float[] {24.0f, 72.0f}, 225, 0.0f,
            upgrade(0, 0, 0.0f, 0.0f, -5.0f, 0, false, 0.0f, false),
            upgrade(150, 0, 0.0f, 0.0f, 0.0f, 0, false, 0.0f, false),
            upgrade(0, 0, 0.0f, 0.0f, 0.0f, 0, true, 0.0f, false)),

    PRIMAL_SUNFLOWER(
            4, "Primal Sunflower", Collections.emptySet(), 75, 300, 0,
            "Produces 75 sun every 24 seconds.",
            "Immediately produces 225 sun.",
            "Production time -2s", "HP +150", "Cost -25",
            24.0f, 5.0f, SunProductionBehavior.PERIODIC,
            new int[] {75}, new float[0], 225, 0.0f,
            upgrade(0, 0, -2.0f, 0.0f, 0.0f, 0, false, 0.0f, false),
            upgrade(150, 0, 0.0f, 0.0f, 0.0f, 0, false, 0.0f, false),
            upgrade(0, -25, 0.0f, 0.0f, 0.0f, 0, false, 0.0f, false)),

    GOLD_BLOOM(
            5, "Gold Bloom", Collections.emptySet(), 0, 0, 0,
            "Immediately produces 375 sun once, then disappears.",
            "No plant food effect.",
            "Cooldown -5s", "Sun +50", "Cost -25",
            0.0f, 75.0f, SunProductionBehavior.INSTANT,
            new int[] {375}, new float[0], 0, 0.0f,
            upgrade(0, 0, 0.0f, -5.0f, 0.0f, 0, false, 0.0f, false),
            upgrade(0, 0, 0.0f, 0.0f, 0.0f, 50, false, 0.0f, false),
            upgrade(0, -25, 0.0f, 0.0f, 0.0f, 0, false, 0.0f, false)),

    ENLIGHTEN_MINT(
            61, "Enlighten-mint", Collections.emptySet(), 0, 0, 0,
            "Temporarily applies plant food to plants in its family.",
            "No plant food effect.",
            "Duration +1s", "Cooldown -5s", "Reset family cooldowns",
            0.0f, 85.0f, SunProductionBehavior.FAMILY_BOOST,
            new int[] {0}, new float[0], 0, 5.0f,
            upgrade(0, 0, 0.0f, 0.0f, 0.0f, 0, false, 1.0f, false),
            upgrade(0, 0, 0.0f, -5.0f, 0.0f, 0, false, 0.0f, false),
            upgrade(0, 0, 0.0f, 0.0f, 0.0f, 0, false, 0.0f, true));

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;
    public static final double DOUBLE_SUN_CHANCE = 0.5;

    private final int id;
    private final String displayName;
    private final Set<PlantTag> tags;
    private final int cost;
    private final int baseHP;
    private final int damage;
    private final String baseAbility;
    private final String plantFoodEffect;
    private final String levelTwoUpgrade;
    private final String levelThreeUpgrade;
    private final String levelFourUpgrade;
    private final float actionIntervalSeconds;
    private final float rechargeSeconds;
    private final SunProductionBehavior behavior;
    private final int[] sunAmounts;
    private final float[] stageThresholdSeconds;
    private final int plantFoodSunAmount;
    private final float familyBoostDurationSeconds;
    private final SunProducerUpgrade[] upgrades;

    SunProducerPlantType(int id, String displayName, Set<PlantTag> tags,
            int cost, int baseHP, int damage,
            String baseAbility, String plantFoodEffect,
            String levelTwoUpgrade, String levelThreeUpgrade, String levelFourUpgrade,
            float actionIntervalSeconds, float rechargeSeconds,
            SunProductionBehavior behavior, int[] sunAmounts, float[] stageThresholdSeconds,
            int plantFoodSunAmount, float familyBoostDurationSeconds,
            SunProducerUpgrade levelTwo, SunProducerUpgrade levelThree,
            SunProducerUpgrade levelFour) {
        if (sunAmounts.length != stageThresholdSeconds.length + 1) {
            throw new IllegalArgumentException(
                    "Each growth threshold must lead to one additional sun amount");
        }
        this.id = id;
        this.displayName = displayName;
        this.tags = immutableTags(tags);
        this.cost = cost;
        this.baseHP = baseHP;
        this.damage = damage;
        this.baseAbility = baseAbility;
        this.plantFoodEffect = plantFoodEffect;
        this.levelTwoUpgrade = levelTwoUpgrade;
        this.levelThreeUpgrade = levelThreeUpgrade;
        this.levelFourUpgrade = levelFourUpgrade;
        this.actionIntervalSeconds = actionIntervalSeconds;
        this.rechargeSeconds = rechargeSeconds;
        this.behavior = behavior;
        this.sunAmounts = sunAmounts.clone();
        this.stageThresholdSeconds = stageThresholdSeconds.clone();
        this.plantFoodSunAmount = plantFoodSunAmount;
        this.familyBoostDurationSeconds = familyBoostDurationSeconds;
        this.upgrades = new SunProducerUpgrade[] {
            SunProducerUpgrade.NONE, levelTwo, levelThree, levelFour
        };
    }

    private static SunProducerUpgrade upgrade(int hp, int cost,
            float actionInterval, float recharge, float growthTime,
            int sunAmount, boolean doubleSunChance, float boostDuration,
            boolean resetCooldowns) {
        return new SunProducerUpgrade(hp, cost, actionInterval, recharge,
                growthTime, sunAmount, doubleSunChance, boostDuration, resetCooldowns);
    }

    private static Set<PlantTag> immutableTags(Set<PlantTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(tags));
    }

    public static Optional<SunProducerPlantType> findByName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }

        String normalizedName = normalizeName(rawName);
        for (SunProducerPlantType type : values()) {
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
        return Math.max(0, cost + sumCostDelta(level));
    }

    public int getBaseHP(int level) {
        return Math.max(0, baseHP + sumHitPointDelta(level));
    }

    public float getActionIntervalSeconds(int level) {
        return Math.max(0.0f, actionIntervalSeconds + sumActionIntervalDelta(level));
    }

    public float getRechargeSeconds(int level) {
        return Math.max(0.0f, rechargeSeconds + sumRechargeDelta(level));
    }

    public int getSunAmountAt(double ageSeconds, int level) {
        validateLevel(level);
        int stage = 0;
        float growthDelta = sumGrowthTimeDelta(level);
        while (stage < stageThresholdSeconds.length
                && ageSeconds >= Math.max(0.0f, stageThresholdSeconds[stage] + growthDelta)) {
            stage++;
        }
        return Math.max(0, sunAmounts[stage] + sumSunAmountDelta(level));
    }

    public int getFinalSunAmount(int level) {
        validateLevel(level);
        return Math.max(0, sunAmounts[sunAmounts.length - 1] + sumSunAmountDelta(level));
    }

    public boolean hasDoubleSunChance(int level) {
        validateLevel(level);
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            if (upgrades[currentLevel - 1].hasDoubleSunChance()) {
                return true;
            }
        }
        return false;
    }

    public float getFamilyBoostDurationSeconds(int level) {
        return Math.max(0.0f, familyBoostDurationSeconds + sumBoostDurationDelta(level));
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

    private int sumHitPointDelta(int level) {
        validateLevel(level);
        int result = 0;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getHitPointDelta();
        }
        return result;
    }

    private int sumCostDelta(int level) {
        validateLevel(level);
        int result = 0;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getCostDelta();
        }
        return result;
    }

    private float sumActionIntervalDelta(int level) {
        validateLevel(level);
        float result = 0.0f;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getActionIntervalDeltaSeconds();
        }
        return result;
    }

    private float sumRechargeDelta(int level) {
        validateLevel(level);
        float result = 0.0f;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getRechargeDeltaSeconds();
        }
        return result;
    }

    private float sumGrowthTimeDelta(int level) {
        validateLevel(level);
        float result = 0.0f;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getGrowthTimeDeltaSeconds();
        }
        return result;
    }

    private int sumSunAmountDelta(int level) {
        validateLevel(level);
        int result = 0;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getSunAmountDelta();
        }
        return result;
    }

    private float sumBoostDurationDelta(int level) {
        validateLevel(level);
        float result = 0.0f;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getBoostDurationDeltaSeconds();
        }
        return result;
    }

    public static void validateLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("sun-producer level must be between 1 and 4");
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

    public int getCost() {
        return cost;
    }

    public int getBaseHP() {
        return baseHP;
    }

    public int getDamage() {
        return damage;
    }

    public String getBaseAbility() {
        return baseAbility;
    }

    public String getPlantFoodEffect() {
        return plantFoodEffect;
    }

    public String getLevelTwoUpgrade() {
        return levelTwoUpgrade;
    }

    public String getLevelThreeUpgrade() {
        return levelThreeUpgrade;
    }

    public String getLevelFourUpgrade() {
        return levelFourUpgrade;
    }

    public float getActionIntervalSeconds() {
        return actionIntervalSeconds;
    }

    public float getRechargeSeconds() {
        return rechargeSeconds;
    }

    public SunProductionBehavior getBehavior() {
        return behavior;
    }

    public int getPlantFoodSunAmount() {
        return plantFoodSunAmount;
    }
}
