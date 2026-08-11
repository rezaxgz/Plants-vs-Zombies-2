package io.github.Plants_Vs_Zombies_2.model.game.entities.plants.explosive;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantDefinition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantTag;

public enum ExplosivePlantType implements PlantDefinition {
    POTATO_MINE(
            30, "Potato Mine", tags(PlantTag.TRAP, PlantTag.CHARGE),
            25, 300, 1800,
            "Arms after 15 seconds and explodes when a zombie steps on it.",
            "Arms instantly and creates two armed clone mines.",
            "Arm Time -3s", "Cooldown -5s", "Dmg +600",
            25.0f, ExplosiveBehavior.CONTACT_MINE,
            15.0, 0.45, 1, 0, 0.0, 1, 0.0, 0.0f,
            upgrade(0, 0, 0, 0.0f, -3.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, -5.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 600, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false)),
    PRIMAL_POTATO_MINE(
            31, "Primal Potato Mine", tags(PlantTag.TRAP, PlantTag.CHARGE),
            50, 300, 2400,
            "Arms after 5 seconds and explodes in a 3x3 area on contact.",
            "Arms instantly and creates two armed clone mines.",
            "Arm Time -1s", "Cooldown -3s", "Dmg +400",
            5.0f, ExplosiveBehavior.AREA_CONTACT_MINE,
            5.0, 0.45, 1, 0, 0.0, 1, 0.0, 0.0f,
            upgrade(0, 0, 0, 0.0f, -1.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, -3.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 400, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false)),
    CHERRY_BOMB(
            32, "Cherry Bomb", Collections.emptySet(),
            150, 0, 1800,
            "Immediately explodes in a 3x3 area.",
            "No plant food effect because it is consumed immediately.",
            "Cooldown -5s", "Dmg +600", "Cost -25",
            35.0f, ExplosiveBehavior.INSTANT_AREA,
            0.0, 0.0, 1, 0, 0.0, 1, 0.0, 0.0f,
            upgrade(0, 0, 0, -5.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 600, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, -25, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false)),
    SQUASH(
            33, "Squash", tags(PlantTag.TRAP),
            50, 300, 1800,
            "Crushes the first adjacent zombie.",
            "Crushes two zombies on the lawn.",
            "Cooldown -3s", "Dmg +600", "Can crush 2x",
            20.0f, ExplosiveBehavior.SQUASH,
            0.0, 1.0, 1, 0, 0.0, 1, 0.0, 0.0f,
            upgrade(0, 0, 0, -3.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 600, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    true, false, 0.0f, false)),
    GRAPESHOT(
            34, "Grapeshot", Collections.emptySet(),
            150, 0, 1800,
            "Explodes in a 3x3 area and launches eight bouncing grapes for 5 seconds.",
            "No plant food effect because it is consumed immediately.",
            "Dmg +600", "Bounces +1", "Cost -25",
            35.0f, ExplosiveBehavior.GRAPESHOT,
            0.0, 0.0, 1, 1, 0.0, 1, 0.0, 0.0f,
            upgrade(0, 600, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, 0.0f, 0.0, 1, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, -25, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false)),
    JALAPENO(
            35, "Jalapeno", tags(PlantTag.FIRE),
            125, 0, 1800,
            "Immediately burns every zombie in its lane and melts ice.",
            "No plant food effect because it is consumed immediately.",
            "Cooldown -5s", "Dmg +600", "Cost -25",
            35.0f, ExplosiveBehavior.LANE_FIRE,
            0.0, 0.0, 1, 0, 0.0, 1, 0.0, 0.0f,
            upgrade(0, 0, 0, -5.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 600, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, -25, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false)),
    DOOM_SHROOM(
            36, "Doom-shroom", tags(PlantTag.SHROOM),
            125, 0, 1800,
            "Damages every zombie on the lawn and leaves an unplantable crater.",
            "No plant food effect because it is consumed immediately.",
            "Cooldown -5s", "Dmg +800", "Cost -50",
            15.0f, ExplosiveBehavior.WHOLE_BOARD,
            0.0, 0.0, 1, 0, 0.0, 1, 0.0, 0.0f,
            upgrade(0, 0, 0, -5.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 800, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, -50, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false)),
    TANGLE_KELP(
            37, "Tangle Kelp", tags(PlantTag.TRAP, PlantTag.WATER),
            25, 300, 0,
            "Pulls the first nearby water zombie underwater for an instant kill.",
            "Pulls several water zombies underwater.",
            "Cooldown -5s", "Targets +1", "Cost -25",
            15.0f, ExplosiveBehavior.WATER_TRAP,
            0.0, 0.65, 1, 0, 0.0, 1, 0.0, 0.0f,
            upgrade(0, 0, 0, -5.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, 0.0f, 0.0, 0, 1, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, -25, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false)),
    ICEBERG_LETTUCE(
            38, "Iceberg Lettuce", tags(PlantTag.TRAP, PlantTag.ICE),
            0, 300, 0,
            "Freezes the first zombie that steps on it.",
            "Freezes every zombie currently on the lawn.",
            "Cooldown -2s", "Freeze Time +2s", "Cost -0",
            20.0f, ExplosiveBehavior.FREEZE_TRAP,
            0.0, 0.45, 1, 0, 5.0, 1, 0.0, 0.0f,
            upgrade(0, 0, 0, -2.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, 0.0f, 0.0, 0, 0, 2.0, 0, 0,
                    false, false, 0.0f, false),
            ExplosiveUpgrade.NONE),
    ICE_SHROOM(
            57, "Ice-shroom", tags(PlantTag.SHROOM, PlantTag.ICE),
            75, 0, 0,
            "Freezes every zombie currently on the lawn.",
            "No plant food effect because it is consumed immediately.",
            "Freeze Time +2s", "Cooldown -5s", "Dmg +50",
            50.0f, ExplosiveBehavior.WHOLE_BOARD_FREEZE,
            0.0, 0.0, 1, 0, 5.0, 1, 0.0, 0.0f,
            upgrade(0, 0, 0, 0.0f, 0.0, 0, 0, 2.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, -5.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 50, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false)),
    HOT_POTATO(
            59, "Hot Potato", tags(PlantTag.FIRE),
            0, 0, 0,
            "Immediately melts the frozen tile on which it is planted.",
            "No plant food effect because it is consumed immediately.",
            "Cooldown -2s", "Melt Area 3x3", "Explode on Finish",
            5.0f, ExplosiveBehavior.MELT_ICE,
            0.0, 0.0, 1, 0, 0.0, 1, 0.0, 0.0f,
            upgrade(0, 0, 0, -2.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, 0.0f, 0.0, 0, 0, 0.0, 1, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, true, 0.0f, false)),
    GRAVE_BUSTER(
            60, "Grave Buster", Collections.emptySet(),
            0, 0, 0,
            "Consumes the grave on its tile.",
            "No plant food effect because it is consumed immediately.",
            "Eat Time -1s", "Cooldown -2s", "Explode on Finish",
            10.0f, ExplosiveBehavior.CONSUME_GRAVE,
            0.0, 0.0, 1, 0, 0.0, 1, 3.0, 0.0f,
            upgrade(0, 0, 0, 0.0f, 0.0, 0, 0, 0.0, 0, -1,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, -2.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, true, 0.0f, false)),
    BOMBARD_MINT(
            64, "Bombard-mint", Collections.emptySet(),
            0, 0, 0,
            "Applies plant food to every explosive-family plant.",
            "No direct plant food effect.",
            "Duration +1s", "Cooldown -5s", "Reset family cooldowns",
            85.0f, ExplosiveBehavior.FAMILY_BOOST,
            0.0, 0.0, 1, 0, 0.0, 1, 0.0, 5.0f,
            upgrade(0, 0, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 1.0f, false),
            upgrade(0, 0, 0, -5.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, false),
            upgrade(0, 0, 0, 0.0f, 0.0, 0, 0, 0.0, 0, 0,
                    false, false, 0.0f, true));

    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 4;
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
    private final float rechargeSeconds;
    private final ExplosiveBehavior behavior;
    private final double armTimeSeconds;
    private final double triggerRangeTiles;
    private final int targetCount;
    private final int grapeBounceCount;
    private final double freezeDurationSeconds;
    private final int meltRadius;
    private final double eatTimeSeconds;
    private final float familyBoostDurationSeconds;
    private final ExplosiveUpgrade[] upgrades;

    ExplosivePlantType(int id, String displayName, Set<PlantTag> tags,
            int cost, int baseHP, int damage, String baseAbility,
            String plantFoodEffect, String levelTwoUpgrade,
            String levelThreeUpgrade, String levelFourUpgrade,
            float rechargeSeconds, ExplosiveBehavior behavior,
            double armTimeSeconds, double triggerRangeTiles,
            int targetCount, int grapeBounceCount,
            double freezeDurationSeconds, int meltRadius,
            double eatTimeSeconds, float familyBoostDurationSeconds,
            ExplosiveUpgrade levelTwo, ExplosiveUpgrade levelThree,
            ExplosiveUpgrade levelFour) {
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
        this.rechargeSeconds = rechargeSeconds;
        this.behavior = behavior;
        this.armTimeSeconds = armTimeSeconds;
        this.triggerRangeTiles = triggerRangeTiles;
        this.targetCount = targetCount;
        this.grapeBounceCount = grapeBounceCount;
        this.freezeDurationSeconds = freezeDurationSeconds;
        this.meltRadius = meltRadius;
        this.eatTimeSeconds = eatTimeSeconds;
        this.familyBoostDurationSeconds = familyBoostDurationSeconds;
        this.upgrades = new ExplosiveUpgrade[] {
                ExplosiveUpgrade.NONE, levelTwo, levelThree, levelFour
        };
    }

    private static ExplosiveUpgrade upgrade(int hp, int damage, int cost,
            float recharge, double armTime, int bounces, int targets,
            double freezeTime, int meltRadius, int eatTime,
            boolean extraSquashUse, boolean explodeOnFinish,
            float boostDuration, boolean resetCooldowns) {
        return new ExplosiveUpgrade(hp, damage, cost, recharge, armTime,
                bounces, targets, freezeTime, meltRadius, eatTime,
                extraSquashUse, explodeOnFinish, boostDuration,
                resetCooldowns);
    }

    private static Set<PlantTag> tags(PlantTag first, PlantTag... rest) {
        EnumSet<PlantTag> result = EnumSet.of(first, rest);
        return Collections.unmodifiableSet(result);
    }

    private static Set<PlantTag> immutableTags(Set<PlantTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(tags));
    }

    public static Optional<ExplosivePlantType> findByName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String normalizedName = normalizeName(rawName);
        for (ExplosivePlantType type : values()) {
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

    public float getRechargeSeconds(int level) {
        validateLevel(level);
        float value = rechargeSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getRechargeDeltaSeconds();
        }
        return Math.max(0.0f, value);
    }

    public double getArmTimeSeconds(int level) {
        validateLevel(level);
        double value = armTimeSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getArmTimeDeltaSeconds();
        }
        return Math.max(0.0, value);
    }

    public int getGrapeBounceCount(int level) {
        validateLevel(level);
        int value = grapeBounceCount;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getGrapeBounceDelta();
        }
        return Math.max(0, value);
    }

    public int getTargetCount(int level) {
        validateLevel(level);
        int value = targetCount;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getTargetCountDelta();
        }
        return Math.max(1, value);
    }

    public double getFreezeDurationSeconds(int level) {
        validateLevel(level);
        double value = freezeDurationSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getFreezeDurationDeltaSeconds();
        }
        return Math.max(0.0, value);
    }

    public int getMeltRadius(int level) {
        validateLevel(level);
        int value = meltRadius;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value = Math.max(value, upgrades[currentLevel - 1].getMeltRadius());
        }
        return value;
    }

    public double getEatTimeSeconds(int level) {
        validateLevel(level);
        double value = eatTimeSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getEatTimeDeltaSeconds();
        }
        return Math.max(0.0, value);
    }

    public int getMaximumActivations(int level) {
        validateLevel(level);
        int activations = 1;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            if (upgrades[currentLevel - 1].hasExtraSquashUse()) {
                activations++;
            }
        }
        return activations;
    }

    public boolean explodesOnFinish(int level) {
        validateLevel(level);
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            if (upgrades[currentLevel - 1].explodesOnFinish()) {
                return true;
            }
        }
        return false;
    }

    public float getFamilyBoostDurationSeconds(int level) {
        validateLevel(level);
        float value = familyBoostDurationSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            value += upgrades[currentLevel - 1].getFamilyBoostDurationDeltaSeconds();
        }
        return Math.max(0.0f, value);
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
            ExplosiveUpgrade upgrade = upgrades[currentLevel - 1];
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
            throw new IllegalArgumentException("explosive level must be between 1 and 4");
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

    public float getRechargeSeconds() {
        return rechargeSeconds;
    }

    public ExplosiveBehavior getBehavior() {
        return behavior;
    }

    public double getTriggerRangeTiles() {
        return triggerRangeTiles;
    }

    private enum UpgradeValue {
        HIT_POINTS,
        DAMAGE,
        COST
    }
}
