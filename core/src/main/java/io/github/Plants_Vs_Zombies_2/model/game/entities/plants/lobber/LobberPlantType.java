package io.github.Plants_Vs_Zombies_2.model.game.entities.plants.lobber;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantDefinition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantTag;

public enum LobberPlantType implements PlantDefinition {
    CABBAGE_PULT(
            25, "Cabbage-pult", Collections.emptySet(), 100, 300, 40,
            "Lobs a cabbage over obstacles onto the first zombie in its lane.",
            "Lobs giant cabbages at several zombies.",
            "Dmg +10", "Atk Speed +15%", "HP +150",
            2.9f, 5.0f, LobberBehavior.SINGLE_TARGET,
            0, 0.0, 0.0, 0, 0.0f,
            damageUpgrade(10), speedUpgrade(0.85f), hitPointUpgrade(150)),

    KERNEL_PULT(
            26, "Kernel-pult", Collections.emptySet(), 100, 300, 20,
            "Lobs corn kernels or butter that temporarily stops a zombie.",
            "Lobs butter onto every zombie on the board.",
            "Butter +5%", "Dmg +10", "HP +150",
            2.9f, 5.0f, LobberBehavior.KERNEL,
            20, 0.25, 0.0, 0, 0.0f,
            butterChanceUpgrade(0.05), damageUpgrade(10), hitPointUpgrade(150)),

    MELON_PULT(
            27, "Melon-pult", tags(PlantTag.AOE), 325, 300, 80,
            "Lobs a heavy melon that damages zombies around its landing target.",
            "Lobs giant melons at several zombies.",
            "Cost -25", "AoE Dmg +15", "Dmg +30",
            2.9f, 5.0f, LobberBehavior.AREA,
            0, 0.0, 40.0, 0, 0.0f,
            costUpgrade(-25), splashDamageUpgrade(15.0), damageUpgrade(30)),

    WINTER_MELON(
            28, "Winter Melon", tags(PlantTag.ICE, PlantTag.AOE), 500, 300, 80,
            "Lobs an icy melon that damages and chills zombies around its target.",
            "Lobs giant icy melons at several zombies.",
            "Cost -50", "AoE Dmg +15", "Cost -25",
            2.9f, 5.0f, LobberBehavior.ICE_AREA,
            0, 0.0, 40.0, 0, 0.0f,
            costUpgrade(-50), splashDamageUpgrade(15.0), costUpgrade(-25)),

    PEPPER_PULT(
            29, "Pepper-pult", tags(PlantTag.FIRE, PlantTag.AOE), 200, 300, 50,
            "Lobs a fiery pepper with area damage and warms nearby frozen tiles.",
            "Lobs large peppers at three zombies.",
            "Dmg +15", "Warmth Radius +1", "Cost -25",
            2.9f, 5.0f, LobberBehavior.FIRE_AREA,
            0, 0.0, 25.0, 1, 0.0f,
            damageUpgrade(15), warmthRadiusUpgrade(1), costUpgrade(-25)),

    ARMA_MINT(
            63, "Arma-mint", Collections.emptySet(), 0, 0, 0,
            "Applies plant food to every lobber-family plant.",
            "No direct plant food effect because it is consumed immediately.",
            "Duration +1s", "Cooldown -5s", "Reset family cooldowns",
            0.0f, 85.0f, LobberBehavior.FAMILY_BOOST,
            0, 0.0, 0.0, 0, 5.0f,
            familyDurationUpgrade(1.0f), rechargeUpgrade(-5.0f),
            resetCooldownUpgrade());

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;
    public static final double BUTTER_STUN_SECONDS = 4.0;
    public static final double WINTER_CHILL_SECONDS = 10.0;
    public static final double SPLASH_RADIUS_TILES = 1.0;

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
    private final LobberBehavior behavior;
    private final int butterDamageBonus;
    private final double butterChance;
    private final double splashDamage;
    private final int warmthRadius;
    private final float familyBoostDurationSeconds;
    private final LobberUpgrade[] upgrades;

    LobberPlantType(int id, String displayName, Set<PlantTag> tags,
            int cost, int baseHP, int damage, String baseAbility,
            String plantFoodEffect, String levelTwoUpgrade,
            String levelThreeUpgrade, String levelFourUpgrade,
            float actionIntervalSeconds, float rechargeSeconds,
            LobberBehavior behavior, int butterDamageBonus,
            double butterChance, double splashDamage, int warmthRadius,
            float familyBoostDurationSeconds, LobberUpgrade levelTwo,
            LobberUpgrade levelThree, LobberUpgrade levelFour) {
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
        this.butterDamageBonus = butterDamageBonus;
        this.butterChance = butterChance;
        this.splashDamage = splashDamage;
        this.warmthRadius = warmthRadius;
        this.familyBoostDurationSeconds = familyBoostDurationSeconds;
        this.upgrades = new LobberUpgrade[] {
                LobberUpgrade.NONE, levelTwo, levelThree, levelFour
        };
    }

    private static LobberUpgrade hitPointUpgrade(int hitPoints) {
        return upgrade(hitPoints, 0, 0, 1.0f, 0.0, 0.0, 0, 0.0f, 0.0f, false);
    }

    private static LobberUpgrade damageUpgrade(int damage) {
        return upgrade(0, damage, 0, 1.0f, 0.0, 0.0, 0, 0.0f, 0.0f, false);
    }

    private static LobberUpgrade costUpgrade(int cost) {
        return upgrade(0, 0, cost, 1.0f, 0.0, 0.0, 0, 0.0f, 0.0f, false);
    }

    private static LobberUpgrade speedUpgrade(float multiplier) {
        return upgrade(0, 0, 0, multiplier, 0.0, 0.0, 0, 0.0f, 0.0f, false);
    }

    private static LobberUpgrade butterChanceUpgrade(double chance) {
        return upgrade(0, 0, 0, 1.0f, chance, 0.0, 0, 0.0f, 0.0f, false);
    }

    private static LobberUpgrade splashDamageUpgrade(double damage) {
        return upgrade(0, 0, 0, 1.0f, 0.0, damage, 0, 0.0f, 0.0f, false);
    }

    private static LobberUpgrade warmthRadiusUpgrade(int radius) {
        return upgrade(0, 0, 0, 1.0f, 0.0, 0.0, radius, 0.0f, 0.0f, false);
    }

    private static LobberUpgrade rechargeUpgrade(float seconds) {
        return upgrade(0, 0, 0, 1.0f, 0.0, 0.0, 0, seconds, 0.0f, false);
    }

    private static LobberUpgrade familyDurationUpgrade(float seconds) {
        return upgrade(0, 0, 0, 1.0f, 0.0, 0.0, 0, 0.0f, seconds, false);
    }

    private static LobberUpgrade resetCooldownUpgrade() {
        return upgrade(0, 0, 0, 1.0f, 0.0, 0.0, 0, 0.0f, 0.0f, true);
    }

    private static LobberUpgrade upgrade(int hitPoints, int damage, int cost,
            float intervalMultiplier, double butterChance,
            double splashDamage, int warmthRadius, float recharge,
            float familyDuration, boolean resetCooldowns) {
        return new LobberUpgrade(hitPoints, damage, cost, intervalMultiplier,
                butterChance, splashDamage, warmthRadius, recharge,
                familyDuration, resetCooldowns);
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

    public static Optional<LobberPlantType> findByName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String normalizedName = normalizeName(rawName);
        for (LobberPlantType type : values()) {
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

    public int getButterDamage(int level) {
        return Math.max(0, getDamage(level) + butterDamageBonus);
    }

    public float getActionIntervalSeconds(int level) {
        validateLevel(level);
        float interval = actionIntervalSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            interval *= upgrades[currentLevel - 1].getActionIntervalMultiplier();
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

    public double getButterChance(int level) {
        validateLevel(level);
        double chance = butterChance;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            chance += upgrades[currentLevel - 1].getButterChanceDelta();
        }
        return Math.max(0.0, Math.min(1.0, chance));
    }

    public int getSplashDamage(int level) {
        validateLevel(level);
        double value = splashDamage;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getSplashDamageDelta();
        }
        return Math.max(0, (int) Math.round(value));
    }

    public int getWarmthRadius(int level) {
        validateLevel(level);
        int radius = warmthRadius;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            radius += upgrades[currentLevel - 1].getWarmthRadiusDelta();
        }
        return Math.max(0, radius);
    }

    public float getFamilyBoostDurationSeconds(int level) {
        validateLevel(level);
        float duration = familyBoostDurationSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            duration += upgrades[currentLevel - 1].getFamilyBoostDurationDeltaSeconds();
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
            LobberUpgrade upgrade = upgrades[currentLevel - 1];
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
                    throw new IllegalStateException("Unknown lobber upgrade value: " + value);
            }
        }
        return result;
    }

    public static void validateLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("lobber level must be between 1 and 4");
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

    public LobberBehavior getBehavior() {
        return behavior;
    }

    private enum UpgradeValue {
        HIT_POINTS,
        DAMAGE,
        COST
    }
}
