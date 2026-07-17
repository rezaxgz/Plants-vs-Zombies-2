package model.game.entities.plants.modifier;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import model.game.entities.plants.PlantDefinition;
import model.game.entities.plants.PlantTag;

public enum ModifierPlantType implements PlantDefinition {
    TORCHWOOD(
            52, "Torchwood", tags(PlantTag.FIRE), 175, 300, 0,
            "Turns pea projectiles that pass through it into fire projectiles.",
            "Creates a blue flame that makes passing pea projectiles deal triple damage.",
            "HP +300", "AoE on Death", "Cost -25", 5.0f,
            ModifierBehavior.TORCHWOOD,
            hpUpgrade(300), deathAreaUpgrade(), costUpgrade(-25)),

    HYPNO_SHROOM(
            54, "Hypno-shroom", tags(PlantTag.SHROOM, PlantTag.MAGIC),
            125, 300, 0,
            "The zombie that finishes eating it becomes hypnotized and fights for the player.",
            "The eating zombie becomes an allied Gargantuar.",
            "Cost -25", "Zombie HP Buff", "Zombie Dmg Buff", 20.0f,
            ModifierBehavior.HYPNO_SHROOM,
            costUpgrade(-25), hypnotizedHealthUpgrade(0.5),
            hypnotizedDamageUpgrade(0.5)),

    IMITATER(
            56, "Imitater", Collections.emptySet(), 0, 0, 0,
            "Copies another plant and provides a second seed packet for it.",
            "Uses the copied plant's plant food effect.",
            "Cooldown -2s", "Cost -25", "plant food on enterance", 0.0f,
            ModifierBehavior.IMITATER,
            rechargeUpgrade(-2.0f), costUpgrade(-25), plantFoodEntranceUpgrade()),

    LILY_PAD(
            58, "Lily Pad", tags(PlantTag.WATER, PlantTag.STACK), 25, 300, 0,
            "Provides a platform for non-water plants on water tiles.",
            "Creates copies of itself on empty water tiles.",
            "Cost -25", "HP +200", "Cooldown -2s", 5.0f,
            ModifierBehavior.LILY_PAD,
            costUpgrade(-25), hpUpgrade(200), rechargeUpgrade(-2.0f)),

    ENCHANT_MINT(
            67, "Enchant-mint", Collections.emptySet(), 0, 0, 0,
            "Applies plant food to all Modifier-family plants on the lawn.",
            "Instant consumable family effect.",
            "Duration +1s", "Cooldown -5s", "reset family cooldowns", 85.0f,
            ModifierBehavior.FAMILY_BOOST,
            familyDurationUpgrade(1.0f), rechargeUpgrade(-5.0f),
            resetCooldownUpgrade());

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;
    public static final int TORCHWOOD_DEATH_DAMAGE = 450;
    public static final int LILY_PAD_PLANT_FOOD_COPIES = 4;
    public static final float BASE_FAMILY_BOOST_DURATION_SECONDS = 5.0f;

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
    private final ModifierBehavior behavior;
    private final ModifierUpgrade[] upgrades;

    ModifierPlantType(int id, String displayName, Set<PlantTag> tags,
            int cost, int baseHP, int damage, String baseAbility,
            String plantFoodEffect, String levelTwoUpgrade,
            String levelThreeUpgrade, String levelFourUpgrade,
            float rechargeSeconds, ModifierBehavior behavior,
            ModifierUpgrade levelTwo, ModifierUpgrade levelThree,
            ModifierUpgrade levelFour) {
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
        this.upgrades = new ModifierUpgrade[] {
            ModifierUpgrade.NONE, levelTwo, levelThree, levelFour
        };
    }

    private static ModifierUpgrade hpUpgrade(int amount) {
        return new ModifierUpgrade(amount, 0, 0.0f, 0.0f,
                false, 0.0, 0.0, false, false);
    }

    private static ModifierUpgrade costUpgrade(int amount) {
        return new ModifierUpgrade(0, amount, 0.0f, 0.0f,
                false, 0.0, 0.0, false, false);
    }

    private static ModifierUpgrade rechargeUpgrade(float seconds) {
        return new ModifierUpgrade(0, 0, seconds, 0.0f,
                false, 0.0, 0.0, false, false);
    }

    private static ModifierUpgrade familyDurationUpgrade(float seconds) {
        return new ModifierUpgrade(0, 0, 0.0f, seconds,
                false, 0.0, 0.0, false, false);
    }

    private static ModifierUpgrade deathAreaUpgrade() {
        return new ModifierUpgrade(0, 0, 0.0f, 0.0f,
                true, 0.0, 0.0, false, false);
    }

    private static ModifierUpgrade hypnotizedHealthUpgrade(double multiplierDelta) {
        return new ModifierUpgrade(0, 0, 0.0f, 0.0f,
                false, multiplierDelta, 0.0, false, false);
    }

    private static ModifierUpgrade hypnotizedDamageUpgrade(double multiplierDelta) {
        return new ModifierUpgrade(0, 0, 0.0f, 0.0f,
                false, 0.0, multiplierDelta, false, false);
    }

    private static ModifierUpgrade plantFoodEntranceUpgrade() {
        return new ModifierUpgrade(0, 0, 0.0f, 0.0f,
                false, 0.0, 0.0, true, false);
    }

    private static ModifierUpgrade resetCooldownUpgrade() {
        return new ModifierUpgrade(0, 0, 0.0f, 0.0f,
                false, 0.0, 0.0, false, true);
    }

    private static Set<PlantTag> tags(PlantTag... tags) {
        if (tags == null || tags.length == 0) {
            return Collections.emptySet();
        }
        return EnumSet.of(tags[0], tags);
    }

    private static Set<PlantTag> immutableTags(Set<PlantTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(tags));
    }

    public static Optional<ModifierPlantType> findByName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizeName(rawName);
        for (ModifierPlantType type : values()) {
            if (normalizeName(type.name()).equals(normalized)
                    || normalizeName(type.displayName).equals(normalized)) {
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
        validateLevel(level);
        return damage;
    }

    public float getRechargeSeconds(int level) {
        validateLevel(level);
        float result = rechargeSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getRechargeDeltaSeconds();
        }
        return Math.max(0.0f, result);
    }

    public int getImitatedCost(int level, int copiedPlantCost) {
        validateLevel(level);
        return Math.max(0, copiedPlantCost + sumInt(level, UpgradeValue.COST));
    }

    public float getImitatedRechargeSeconds(int level, float copiedRechargeSeconds) {
        validateLevel(level);
        float result = copiedRechargeSeconds;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getRechargeDeltaSeconds();
        }
        return Math.max(0.0f, result);
    }

    public boolean hasDeathAreaEffect(int level) {
        validateLevel(level);
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            if (upgrades[currentLevel - 1].hasDeathAreaEffect()) {
                return true;
            }
        }
        return false;
    }

    public double getHypnotizedHealthMultiplier(int level) {
        validateLevel(level);
        double result = 1.0;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getHypnotizedHealthMultiplierDelta();
        }
        return result;
    }

    public double getHypnotizedDamageMultiplier(int level) {
        validateLevel(level);
        double result = 1.0;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getHypnotizedDamageMultiplierDelta();
        }
        return result;
    }

    public boolean appliesPlantFoodOnEntrance(int level) {
        validateLevel(level);
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            if (upgrades[currentLevel - 1].hasPlantFoodOnEntrance()) {
                return true;
            }
        }
        return false;
    }

    public float getFamilyBoostDurationSeconds(int level) {
        validateLevel(level);
        float result = behavior == ModifierBehavior.FAMILY_BOOST
                ? BASE_FAMILY_BOOST_DURATION_SECONDS : 0.0f;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getFamilyDurationDeltaSeconds();
        }
        return Math.max(0.0f, result);
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
            ModifierUpgrade upgrade = upgrades[currentLevel - 1];
            result += value == UpgradeValue.HIT_POINTS
                    ? upgrade.getHitPointDelta() : upgrade.getCostDelta();
        }
        return result;
    }

    public static void validateLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("modifier level must be between 1 and 4");
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

    public ModifierBehavior getBehavior() {
        return behavior;
    }

    private enum UpgradeValue {
        HIT_POINTS,
        COST
    }
}
