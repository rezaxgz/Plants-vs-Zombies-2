package io.github.some_example_name.model.game.entities.plants.melee;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import io.github.some_example_name.model.game.entities.plants.PlantDefinition;
import io.github.some_example_name.model.game.entities.plants.PlantTag;

public enum MeleePlantType implements PlantDefinition {
    BONK_CHOY(
            39, "Bonk Choy", Collections.emptySet(), 150, 300, 15,
            "Punches the first zombie in the tile in front and behind it.",
            "Rapidly punches every zombie in the surrounding 3x3 area.",
            "Dmg +5", "Atk Speed +10%", "HP +200",
            0.25f, 5.0f, MeleeBehavior.FRONT_AND_BACK,
            1.0, 0.0, 1, 0.0f,
            damageUpgrade(5), speedUpgrade(0.9f), hitPointUpgrade(200)),
    PHAT_BEET(
            40, "Phat Beet", tags(PlantTag.AOE), 150, 300, 15,
            "Damages every zombie in the surrounding 3x3 area with a sonic pulse.",
            "Releases a much stronger sonic pulse around itself.",
            "Dmg +10", "Atk Speed +10%", "HP +200",
            2.0f, 5.0f, MeleeBehavior.AREA,
            1.0, 0.0, 1, 0.0f,
            damageUpgrade(10), speedUpgrade(0.9f), hitPointUpgrade(200)),
    CHOMPER(
            41, "Chomper", Collections.emptySet(), 150, 300, 0,
            "Instantly swallows one nearby zombie, then digests for 40 seconds.",
            "Swallows up to three zombies from long range.",
            "Digest -2s", "HP +200", "Digest -3s",
            40.0f, 5.0f, MeleeBehavior.CHOMPER,
            1.0, 40.0, 1, 0.0f,
            digestUpgrade(-2.0), hitPointUpgrade(200), digestUpgrade(-3.0)),
    WASABI_WHIP(
            42, "Wasabi Whip", tags(PlantTag.FIRE), 150, 300, 40,
            "Whips the first zombie in front and behind it and warms nearby tiles.",
            "Spins its fiery whip through the surrounding 3x3 area.",
            "Dmg +10", "Range +1 Tile", "HP +200",
            2.0f, 5.0f, MeleeBehavior.FRONT_AND_BACK,
            1.0, 0.0, 1, 0.0f,
            damageUpgrade(10), rangeUpgrade(1.0), hitPointUpgrade(200)),
    KIWIBEAST(
            43, "Kiwibeast", tags(PlantTag.AOE, PlantTag.WRAMP_UP), 175, 300, 15,
            "Emits an area pulse and grows at 24 and 72 seconds, increasing damage and range.",
            "Jumps and slams the ground for heavy area damage.",
            "HP +200", "Dmg +15", "Max Size +1",
            2.0f, 5.0f, MeleeBehavior.GROWING_AREA,
            1.0, 0.0, 3, 0.0f,
            hitPointUpgrade(200), damageUpgrade(15), growthStageUpgrade(1)),
    ENFORCE_MINT(
            65, "Enforce-mint", Collections.emptySet(), 0, 0, 0,
            "Applies plant food to every melee-family plant.",
            "No direct plant food effect because it is consumed immediately.",
            "Duration +1s", "Cooldown -5s", "Reset family cooldowns",
            0.0f, 85.0f, MeleeBehavior.FAMILY_BOOST,
            0.0, 0.0, 1, 5.0f,
            familyDurationUpgrade(1.0f), rechargeUpgrade(-5.0f),
            resetCooldownUpgrade());

    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 4;
    private static final int GROWTH_DAMAGE_STEP = 15;

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
    private final MeleeBehavior behavior;
    private final double attackRangeTiles;
    private final double digestTimeSeconds;
    private final int maximumGrowthStage;
    private final float familyBoostDurationSeconds;
    private final MeleeUpgrade[] upgrades;

    MeleePlantType(int id, String displayName, Set<PlantTag> tags,
            int cost, int baseHP, int damage, String baseAbility,
            String plantFoodEffect, String levelTwoUpgrade,
            String levelThreeUpgrade, String levelFourUpgrade,
            float actionIntervalSeconds, float rechargeSeconds,
            MeleeBehavior behavior, double attackRangeTiles,
            double digestTimeSeconds, int maximumGrowthStage,
            float familyBoostDurationSeconds, MeleeUpgrade levelTwo,
            MeleeUpgrade levelThree, MeleeUpgrade levelFour) {
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
        this.attackRangeTiles = attackRangeTiles;
        this.digestTimeSeconds = digestTimeSeconds;
        this.maximumGrowthStage = maximumGrowthStage;
        this.familyBoostDurationSeconds = familyBoostDurationSeconds;
        this.upgrades = new MeleeUpgrade[] {
                MeleeUpgrade.NONE, levelTwo, levelThree, levelFour
        };
    }

    private static MeleeUpgrade hitPointUpgrade(int hitPoints) {
        return upgrade(hitPoints, 0, 1.0f, 0.0, 0.0, 0, 0.0f, 0.0f, false);
    }

    private static MeleeUpgrade damageUpgrade(int damage) {
        return upgrade(0, damage, 1.0f, 0.0, 0.0, 0, 0.0f, 0.0f, false);
    }

    private static MeleeUpgrade speedUpgrade(float multiplier) {
        return upgrade(0, 0, multiplier, 0.0, 0.0, 0, 0.0f, 0.0f, false);
    }

    private static MeleeUpgrade digestUpgrade(double seconds) {
        return upgrade(0, 0, 1.0f, seconds, 0.0, 0, 0.0f, 0.0f, false);
    }

    private static MeleeUpgrade rangeUpgrade(double tiles) {
        return upgrade(0, 0, 1.0f, 0.0, tiles, 0, 0.0f, 0.0f, false);
    }

    private static MeleeUpgrade growthStageUpgrade(int stages) {
        return upgrade(0, 0, 1.0f, 0.0, 0.0, stages, 0.0f, 0.0f, false);
    }

    private static MeleeUpgrade rechargeUpgrade(float seconds) {
        return upgrade(0, 0, 1.0f, 0.0, 0.0, 0, seconds, 0.0f, false);
    }

    private static MeleeUpgrade familyDurationUpgrade(float seconds) {
        return upgrade(0, 0, 1.0f, 0.0, 0.0, 0, 0.0f, seconds, false);
    }

    private static MeleeUpgrade resetCooldownUpgrade() {
        return upgrade(0, 0, 1.0f, 0.0, 0.0, 0, 0.0f, 0.0f, true);
    }

    private static MeleeUpgrade upgrade(int hitPoints, int damage,
            float intervalMultiplier, double digestTime, double range,
            int maximumGrowthStage, float recharge, float familyDuration,
            boolean resetCooldowns) {
        return new MeleeUpgrade(hitPoints, damage, intervalMultiplier,
                digestTime, range, maximumGrowthStage, recharge,
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

    public static Optional<MeleePlantType> findByName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String normalizedName = normalizeName(rawName);
        for (MeleePlantType type : values()) {
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
        validateLevel(level);
        return cost;
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

    public double getAttackRangeTiles(int level) {
        validateLevel(level);
        double range = attackRangeTiles;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            range += upgrades[currentLevel - 1].getRangeDeltaTiles();
        }
        return Math.max(0.0, range);
    }

    public double getDigestTimeSeconds(int level) {
        validateLevel(level);
        double digestTime = digestTimeSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            digestTime += upgrades[currentLevel - 1].getDigestTimeDeltaSeconds();
        }
        return Math.max(0.0, digestTime);
    }

    public int getMaximumGrowthStage(int level) {
        validateLevel(level);
        int stage = maximumGrowthStage;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            stage += upgrades[currentLevel - 1].getMaximumGrowthStageDelta();
        }
        return Math.max(1, stage);
    }

    public int getGrowthStageDamage(int level, int stage) {
        int maximumStage = getMaximumGrowthStage(level);
        if (stage < 1 || stage > maximumStage) {
            throw new IllegalArgumentException("growth stage is outside this plant's level range");
        }
        return getDamage(level) + (stage - 1) * GROWTH_DAMAGE_STEP;
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
            MeleeUpgrade upgrade = upgrades[currentLevel - 1];
            if (value == UpgradeValue.HIT_POINTS) {
                result += upgrade.getHitPointDelta();
            } else {
                result += upgrade.getDamageDelta();
            }
        }
        return result;
    }

    public static void validateLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("melee level must be between 1 and 4");
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

    public MeleeBehavior getBehavior() {
        return behavior;
    }

    private enum UpgradeValue {
        HIT_POINTS,
        DAMAGE
    }
}
