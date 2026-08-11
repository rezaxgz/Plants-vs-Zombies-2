package io.github.Plants_Vs_Zombies_2.model.game.entities.plants.strikeThrough;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantDefinition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantTag;

public enum StrikeThroughPlantType implements PlantDefinition {
    CACTUS(
            17, "Cactus", Collections.emptySet(), 175, 300, 30,
            "Shoots a spike that passes through the first three zombies in its lane.",
            "Shoots a high-damage electric spike with unlimited penetration.",
            "Pierce +1", "Dmg +10", "Cost -25",
            1.5f, 5.0f, StrikeThroughBehavior.LIMITED_PIERCE,
            Double.POSITIVE_INFINITY, 3, 0.0f,
            pierceUpgrade(1), damageUpgrade(10), costUpgrade(-25)),

    FUME_SHROOM(
            24, "Fume-shroom", tags(PlantTag.SHROOM), 125, 300, 20,
            "Fires a medium-range cloud that damages every zombie it passes through.",
            "Blasts a giant cloud that damages and pushes zombies backward.",
            "Range +1 Tile", "Dmg +10", "Cost -25",
            1.5f, 5.0f, StrikeThroughBehavior.RANGE_PIERCE,
            4.0, Integer.MAX_VALUE, 0.0f,
            rangeUpgrade(1.0), damageUpgrade(10), costUpgrade(-25)),

    PIERCE_MINT(
            68, "Pierce-mint", Collections.emptySet(), 0, 0, 0,
            "Applies plant food to every strike-through-family plant.",
            "No direct plant food effect because it is consumed immediately.",
            "Duration +1s", "Cooldown -5s", "Reset family cooldowns",
            0.0f, 85.0f, StrikeThroughBehavior.FAMILY_BOOST,
            0.0, 0, 5.0f,
            familyDurationUpgrade(1.0f), rechargeUpgrade(-5.0f),
            resetCooldownUpgrade());

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;
    public static final double PROJECTILE_SPEED_TILES_PER_SECOND = 6.0;
    public static final int PLANT_FOOD_DAMAGE_MULTIPLIER = 5;
    public static final double FUME_PLANT_FOOD_KNOCKBACK_TILES = 2.0;

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
    private final StrikeThroughBehavior behavior;
    private final double rangeTiles;
    private final int maximumTargets;
    private final float familyBoostDurationSeconds;
    private final StrikeThroughUpgrade[] upgrades;

    StrikeThroughPlantType(int id, String displayName, Set<PlantTag> tags,
            int cost, int baseHP, int damage, String baseAbility,
            String plantFoodEffect, String levelTwoUpgrade,
            String levelThreeUpgrade, String levelFourUpgrade,
            float actionIntervalSeconds, float rechargeSeconds,
            StrikeThroughBehavior behavior, double rangeTiles,
            int maximumTargets, float familyBoostDurationSeconds,
            StrikeThroughUpgrade levelTwo, StrikeThroughUpgrade levelThree,
            StrikeThroughUpgrade levelFour) {
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
        this.maximumTargets = maximumTargets;
        this.familyBoostDurationSeconds = familyBoostDurationSeconds;
        this.upgrades = new StrikeThroughUpgrade[] {
                StrikeThroughUpgrade.NONE, levelTwo, levelThree, levelFour
        };
    }

    private static StrikeThroughUpgrade damageUpgrade(int damage) {
        return upgrade(0, damage, 0, 0, 0.0, 0.0f, 0.0f, false);
    }

    private static StrikeThroughUpgrade costUpgrade(int cost) {
        return upgrade(0, 0, cost, 0, 0.0, 0.0f, 0.0f, false);
    }

    private static StrikeThroughUpgrade pierceUpgrade(int pierce) {
        return upgrade(0, 0, 0, pierce, 0.0, 0.0f, 0.0f, false);
    }

    private static StrikeThroughUpgrade rangeUpgrade(double tiles) {
        return upgrade(0, 0, 0, 0, tiles, 0.0f, 0.0f, false);
    }

    private static StrikeThroughUpgrade rechargeUpgrade(float seconds) {
        return upgrade(0, 0, 0, 0, 0.0, seconds, 0.0f, false);
    }

    private static StrikeThroughUpgrade familyDurationUpgrade(float seconds) {
        return upgrade(0, 0, 0, 0, 0.0, 0.0f, seconds, false);
    }

    private static StrikeThroughUpgrade resetCooldownUpgrade() {
        return upgrade(0, 0, 0, 0, 0.0, 0.0f, 0.0f, true);
    }

    private static StrikeThroughUpgrade upgrade(int hitPoints, int damage,
            int cost, int pierce, double range, float recharge,
            float familyDuration, boolean resetCooldowns) {
        return new StrikeThroughUpgrade(hitPoints, damage, cost, pierce,
                range, recharge, familyDuration, resetCooldowns);
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

    public static Optional<StrikeThroughPlantType> findByName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String normalizedName = normalizeName(rawName);
        for (StrikeThroughPlantType type : values()) {
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

    public int getMaximumTargets(int level) {
        validateLevel(level);
        if (maximumTargets == Integer.MAX_VALUE) {
            return maximumTargets;
        }
        return Math.max(1, maximumTargets + sumInt(level, UpgradeValue.PIERCE));
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

    public float getActionIntervalSeconds(int level) {
        validateLevel(level);
        return Math.max(0.0f, actionIntervalSeconds);
    }

    public float getRechargeSeconds(int level) {
        validateLevel(level);
        float recharge = rechargeSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            recharge += upgrades[currentLevel - 1].getRechargeDeltaSeconds();
        }
        return Math.max(0.0f, recharge);
    }

    public float getFamilyBoostDurationSeconds(int level) {
        validateLevel(level);
        float duration = familyBoostDurationSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            duration += upgrades[currentLevel - 1].getFamilyDurationDeltaSeconds();
        }
        return Math.max(0.0f, duration);
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
            StrikeThroughUpgrade upgrade = upgrades[currentLevel - 1];
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
                case PIERCE:
                    result += upgrade.getPierceDelta();
                    break;
                default:
                    throw new IllegalStateException("Unknown strike-through upgrade value: " + value);
            }
        }
        return result;
    }

    public static void validateLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("strike-through level must be between 1 and 4");
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

    public StrikeThroughBehavior getBehavior() {
        return behavior;
    }

    private enum UpgradeValue {
        HIT_POINTS,
        DAMAGE,
        COST,
        PIERCE
    }
}
