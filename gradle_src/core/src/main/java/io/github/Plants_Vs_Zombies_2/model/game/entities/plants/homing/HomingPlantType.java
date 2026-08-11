package io.github.Plants_Vs_Zombies_2.model.game.entities.plants.homing;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantDefinition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantTag;

public enum HomingPlantType implements PlantDefinition {
    CAULIPOWER(
            14, "Caulipower", tags(PlantTag.MAGIC, PlantTag.CHARGE),
            250, 300, 0,
            "Launches a magical shot at a random zombie anywhere on the lawn and hypnotizes it.",
            "Hypnotizes three random zombies on the lawn.",
            "Cooldown -2s", "HP +150", "Cost -50",
            12.0f, 15.0f, HomingBehavior.HYPNOTIZE,
            Double.POSITIVE_INFINITY, 3, 0.0f,
            actionIntervalUpgrade(-2.0f), hitPointUpgrade(150), costUpgrade(-50)),

    ELECTRIC_BLUEBERRY(
            15, "Electric Blueberry", tags(PlantTag.CHARGE),
            150, 300, 5000,
            "Calls lightning onto a random zombie anywhere on the lawn.",
            "Calls lightning onto three random zombies.",
            "Cooldown -2s", "Target Priority Up", "Cost -25",
            12.0f, 15.0f, HomingBehavior.LIGHTNING,
            Double.POSITIVE_INFINITY, 3, 0.0f,
            actionIntervalUpgrade(-2.0f), targetPriorityUpgrade(), costUpgrade(-25)),

    MAGNET_SHROOM(
            53, "Magnet-shroom", tags(PlantTag.SHROOM, PlantTag.MAGIC),
            100, 300, 0,
            "Pulls a metal bucket, crown, or shoulder guard from a zombie in range.",
            "Pulls every magnetizable armor piece in range at the same time.",
            "Range +1 Tile", "Cooldown -5s", "HP +200",
            10.0f, 15.0f, HomingBehavior.MAGNET,
            5.0, Integer.MAX_VALUE, 0.0f,
            rangeUpgrade(1.0), actionIntervalUpgrade(-5.0f), hitPointUpgrade(200)),

    CAT_TAIL(
            55, "Cat-tail", Collections.emptySet(),
            175, 300, 15,
            "Fires a guided spike at the closest zombie anywhere on the lawn.",
            "Fires a rapid barrage of guided spikes.",
            "Dmg +10", "HP +200", "Cost -25",
            1.5f, 20.0f, HomingBehavior.GUIDED_PROJECTILE,
            Double.POSITIVE_INFINITY, 10, 0.0f,
            damageUpgrade(10), hitPointUpgrade(200), costUpgrade(-25)),

    CAT_TAIL_MINT(
            69, "catTail-mint", Collections.emptySet(),
            0, 0, 0,
            "Applies plant food to every Homing-family plant.",
            "No direct plant food effect because it is consumed immediately.",
            "Duration +1s", "Cooldown -5s", "Reset family cooldowns",
            0.0f, 85.0f, HomingBehavior.FAMILY_BOOST,
            0.0, 0, 5.0f,
            familyDurationUpgrade(1.0f), rechargeUpgrade(-5.0f),
            resetCooldownUpgrade());

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;
    public static final double PROJECTILE_SPEED_TILES_PER_SECOND = 8.0;
    public static final double PROJECTILE_MAX_LIFETIME_SECONDS = 5.0;

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
    private final HomingBehavior behavior;
    private final double rangeTiles;
    private final int plantFoodTargetCount;
    private final float familyBoostDurationSeconds;
    private final HomingUpgrade[] upgrades;

    HomingPlantType(int id, String displayName, Set<PlantTag> tags,
            int cost, int baseHP, int damage, String baseAbility,
            String plantFoodEffect, String levelTwoUpgrade,
            String levelThreeUpgrade, String levelFourUpgrade,
            float actionIntervalSeconds, float rechargeSeconds,
            HomingBehavior behavior, double rangeTiles,
            int plantFoodTargetCount, float familyBoostDurationSeconds,
            HomingUpgrade levelTwo, HomingUpgrade levelThree,
            HomingUpgrade levelFour) {
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
        this.rangeTiles = rangeTiles;
        this.plantFoodTargetCount = plantFoodTargetCount;
        this.familyBoostDurationSeconds = familyBoostDurationSeconds;
        this.upgrades = new HomingUpgrade[] {
                HomingUpgrade.NONE, levelTwo, levelThree, levelFour
        };
    }

    private static HomingUpgrade hitPointUpgrade(int hitPoints) {
        return upgrade(hitPoints, 0, 0, 0.0f, 0.0f, 0.0, 0.0f, false, false);
    }

    private static HomingUpgrade damageUpgrade(int damage) {
        return upgrade(0, damage, 0, 0.0f, 0.0f, 0.0, 0.0f, false, false);
    }

    private static HomingUpgrade costUpgrade(int cost) {
        return upgrade(0, 0, cost, 0.0f, 0.0f, 0.0, 0.0f, false, false);
    }

    private static HomingUpgrade actionIntervalUpgrade(float seconds) {
        return upgrade(0, 0, 0, seconds, 0.0f, 0.0, 0.0f, false, false);
    }

    private static HomingUpgrade rechargeUpgrade(float seconds) {
        return upgrade(0, 0, 0, 0.0f, seconds, 0.0, 0.0f, false, false);
    }

    private static HomingUpgrade rangeUpgrade(double tiles) {
        return upgrade(0, 0, 0, 0.0f, 0.0f, tiles, 0.0f, false, false);
    }

    private static HomingUpgrade targetPriorityUpgrade() {
        return upgrade(0, 0, 0, 0.0f, 0.0f, 0.0, 0.0f, true, false);
    }

    private static HomingUpgrade familyDurationUpgrade(float seconds) {
        return upgrade(0, 0, 0, 0.0f, 0.0f, 0.0, seconds, false, false);
    }

    private static HomingUpgrade resetCooldownUpgrade() {
        return upgrade(0, 0, 0, 0.0f, 0.0f, 0.0, 0.0f, false, true);
    }

    private static HomingUpgrade upgrade(int hitPoints, int damage,
            int cost, float actionInterval, float recharge, double range,
            float familyDuration, boolean targetPriority, boolean resetCooldowns) {
        return new HomingUpgrade(hitPoints, damage, cost, actionInterval,
                recharge, range, familyDuration, targetPriority, resetCooldowns);
    }

    private static Set<PlantTag> tags(PlantTag first, PlantTag... rest) {
        return Collections.unmodifiableSet(EnumSet.of(first, rest));
    }

    private static Set<PlantTag> immutableTags(Set<PlantTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(tags));
    }

    public static Optional<HomingPlantType> findByName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String normalizedName = normalizeName(rawName);
        for (HomingPlantType type : values()) {
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

    public int getBaseHP(int level) {
        return Math.max(0, baseHP + sumInt(level, UpgradeValue.HIT_POINTS));
    }

    public int getDamage(int level) {
        return Math.max(0, damage + sumInt(level, UpgradeValue.DAMAGE));
    }

    public float getActionIntervalSeconds(int level) {
        validateLevel(level);
        float interval = actionIntervalSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            interval += upgrades[currentLevel - 1].getActionIntervalDeltaSeconds();
        }
        return Math.max(0.0f, interval);
    }

    public float getRechargeSeconds(int level) {
        validateLevel(level);
        float recharge = rechargeSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            recharge += upgrades[currentLevel - 1].getRechargeDeltaSeconds();
        }
        return Math.max(0.0f, recharge);
    }

    public double getRangeTiles(int level) {
        validateLevel(level);
        if (Double.isInfinite(rangeTiles)) {
            return rangeTiles;
        }
        double range = rangeTiles;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            range += upgrades[currentLevel - 1].getRangeDeltaTiles();
        }
        return Math.max(0.0, range);
    }

    public float getFamilyBoostDurationSeconds(int level) {
        validateLevel(level);
        float duration = familyBoostDurationSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            duration += upgrades[currentLevel - 1].getFamilyDurationDeltaSeconds();
        }
        return Math.max(0.0f, duration);
    }

    public boolean hasTargetPriorityUp(int level) {
        validateLevel(level);
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            if (upgrades[currentLevel - 1].hasTargetPriorityUp()) {
                return true;
            }
        }
        return false;
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
            HomingUpgrade upgrade = upgrades[currentLevel - 1];
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
                    throw new IllegalStateException("Unknown homing upgrade value: " + value);
            }
        }
        return result;
    }

    public static void validateLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("homing level must be between 1 and 4");
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

    public HomingBehavior getBehavior() {
        return behavior;
    }

    public int getPlantFoodTargetCount() {
        return plantFoodTargetCount;
    }

    private enum UpgradeValue {
        HIT_POINTS,
        DAMAGE,
        COST
    }
}
