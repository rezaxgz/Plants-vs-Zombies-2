package model.game.entities.plants.wallnut;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import model.game.entities.plants.PlantDefinition;
import model.game.entities.plants.PlantTag;

public enum WallnutPlantType implements PlantDefinition {
    WALL_NUT(
            44, "Wall-nut", Collections.emptySet(), 50, 4000, 0,
            "A durable defensive barrier that stops zombies.",
            "Gains 4000 permanent armor.",
            "HP +1000", "Cooldown -5s", "HP +1500",
            20.0f, WallnutBehavior.BLOCKER, 4000, 0, 0, 0.0f,
            upgrade(1000, 0, 0, 0.0f, 0, 0.0f, false),
            upgrade(0, 0, 0, -5.0f, 0, 0.0f, false),
            upgrade(1500, 0, 0, 0.0f, 0, 0.0f, false)),

    TALL_NUT(
            45, "Tall-nut", Collections.emptySet(), 125, 8000, 0,
            "A tall barrier that also prevents zombies from jumping over it.",
            "Gains 8000 permanent armor.",
            "HP +2000", "Cooldown -5s", "HP +3000",
            20.0f, WallnutBehavior.TALL_BLOCKER, 8000, 0, 0, 0.0f,
            upgrade(2000, 0, 0, 0.0f, 0, 0.0f, false),
            upgrade(0, 0, 0, -5.0f, 0, 0.0f, false),
            upgrade(3000, 0, 0, 0.0f, 0, 0.0f, false)),

    ENDURIAN(
            46, "Endurian", Collections.emptySet(), 100, 3000, 20,
            "A defensive barrier that reflects damage to the attacking zombie.",
            "Gains metal armor and doubles its reflected damage.",
            "Reflect Dmg +5", "HP +1000", "Cost -25",
            15.0f, WallnutBehavior.REFLECTIVE, 3000, 20, 0, 0.0f,
            upgrade(0, 5, 0, 0.0f, 0, 0.0f, false),
            upgrade(1000, 0, 0, 0.0f, 0, 0.0f, false),
            upgrade(0, 0, -25, 0.0f, 0, 0.0f, false)),

    GARLIC(
            47, "Garlic", EnumSet.of(PlantTag.MOVE_ZOMBIES), 50, 300, 0,
            "A zombie that bites it is diverted into an adjacent lane.",
            "Diverts every zombie in its lane into adjacent lanes.",
            "HP +150", "Cooldown -3s", "HP +250",
            20.0f, WallnutBehavior.LANE_DIVERSION, 300, 0, 0, 0.0f,
            upgrade(150, 0, 0, 0.0f, 0, 0.0f, false),
            upgrade(0, 0, 0, -3.0f, 0, 0.0f, false),
            upgrade(250, 0, 0, 0.0f, 0, 0.0f, false)),

    SWEET_POTATO(
            48, "Sweet Potato", EnumSet.of(PlantTag.MOVE_ZOMBIES), 150, 3000, 0,
            "Pulls nearby zombies from adjacent lanes into its own lane.",
            "Pulls all nearby zombies and restores all of its health.",
            "HP +1000", "Cooldown -5s", "HP +1500",
            20.0f, WallnutBehavior.LANE_ATTRACTOR, 3000, 0, 0, 0.0f,
            upgrade(1000, 0, 0, 0.0f, 0, 0.0f, false),
            upgrade(0, 0, 0, -5.0f, 0, 0.0f, false),
            upgrade(1500, 0, 0, 0.0f, 0, 0.0f, false)),

    EXPLODE_O_NUT(
            49, "Explode-o-nut", EnumSet.of(PlantTag.EXPLOSIVE), 50, 4000, 1800,
            "Explodes in a 3x3 area when destroyed.",
            "Gains metal armor that also explodes when destroyed.",
            "HP +1000", "Explode Dmg +200", "Cost -25",
            20.0f, WallnutBehavior.EXPLOSIVE, 4000, 0, 0, 0.0f,
            upgrade(1000, 0, 0, 0.0f, 0, 0.0f, false),
            upgrade(0, 200, 0, 0.0f, 0, 0.0f, false),
            upgrade(0, 0, -25, 0.0f, 0, 0.0f, false)),

    PUMPKIN(
            50, "Pumpkin", EnumSet.of(PlantTag.STACK), 150, 4000, 0,
            "A protective cover that can be planted over another plant.",
            "Gains powerful metal armor.",
            "HP +1000", "Cooldown -5s", "HP +1500",
            20.0f, WallnutBehavior.COVER, 4000, 0, 0, 0.0f,
            upgrade(1000, 0, 0, 0.0f, 0, 0.0f, false),
            upgrade(0, 0, 0, -5.0f, 0, 0.0f, false),
            upgrade(1500, 0, 0, 0.0f, 0, 0.0f, false)),

    SUN_BEAN(
            51, "Sun Bean", EnumSet.of(PlantTag.SUN), 50, 1000, 0,
            "Acts as a barrier and creates 5 sun whenever it is damaged.",
            "Gains powerful metal armor.",
            "Sun Drop +5", "HP +150", "Cost -25",
            20.0f, WallnutBehavior.SUN_ON_HIT, 1000, 0, 5, 0.0f,
            upgrade(0, 0, 0, 0.0f, 5, 0.0f, false),
            upgrade(150, 0, 0, 0.0f, 0, 0.0f, false),
            upgrade(0, 0, -25, 0.0f, 0, 0.0f, false)),

    REINFORCE_MINT(
            66, "Reinforce-mint", Collections.emptySet(), 0, 0, 0,
            "Applies plant food to every Wall-nut family plant, then disappears.",
            "No plant food effect.",
            "Duration +1s", "Cooldown -5s", "Reset family cooldowns",
            85.0f, WallnutBehavior.FAMILY_BOOST, 0, 0, 0, 5.0f,
            upgrade(0, 0, 0, 0.0f, 0, 1.0f, false),
            upgrade(0, 0, 0, -5.0f, 0, 0.0f, false),
            upgrade(0, 0, 0, 0.0f, 0, 0.0f, true));

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;

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
    private final WallnutBehavior behavior;
    private final int plantFoodArmor;
    private final int plantFoodReflectBonus;
    private final int sunPerHit;
    private final float familyBoostDurationSeconds;
    private final WallnutUpgrade[] upgrades;

    WallnutPlantType(int id, String displayName, Set<PlantTag> tags,
            int cost, int baseHP, int damage, String baseAbility,
            String plantFoodEffect, String levelTwoUpgrade,
            String levelThreeUpgrade, String levelFourUpgrade,
            float rechargeSeconds, WallnutBehavior behavior,
            int plantFoodArmor, int plantFoodReflectBonus,
            int sunPerHit, float familyBoostDurationSeconds,
            WallnutUpgrade levelTwo, WallnutUpgrade levelThree,
            WallnutUpgrade levelFour) {
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
        this.plantFoodArmor = plantFoodArmor;
        this.plantFoodReflectBonus = plantFoodReflectBonus;
        this.sunPerHit = sunPerHit;
        this.familyBoostDurationSeconds = familyBoostDurationSeconds;
        this.upgrades = new WallnutUpgrade[] {WallnutUpgrade.NONE, levelTwo, levelThree, levelFour};
    }

    private static WallnutUpgrade upgrade(int hp, int damage, int cost,
            float recharge, int sun, float duration, boolean resetCooldowns) {
        return new WallnutUpgrade(hp, damage, cost, recharge, sun, duration, resetCooldowns);
    }

    private static Set<PlantTag> immutableTags(Set<PlantTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(tags));
    }

    public static Optional<WallnutPlantType> findByName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String normalizedName = normalizeName(rawName);
        for (WallnutPlantType type : values()) {
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

    public int getCost() {
        return cost;
    }

    public int getCost(int level) {
        return Math.max(0, cost + sumCostDelta(level));
    }

    public int getBaseHP() {
        return baseHP;
    }

    public int getBaseHP(int level) {
        return Math.max(0, baseHP + sumHitPointDelta(level));
    }

    public int getDamage() {
        return damage;
    }

    public int getDamage(int level) {
        return Math.max(0, damage + sumDamageDelta(level));
    }

    public float getRechargeSeconds() {
        return rechargeSeconds;
    }

    public float getRechargeSeconds(int level) {
        return Math.max(0.0f, rechargeSeconds + sumRechargeDelta(level));
    }

    public int getSunPerHit() {
        return sunPerHit;
    }

    public int getSunPerHit(int level) {
        return Math.max(0, sunPerHit + sumSunPerHitDelta(level));
    }

    public float getFamilyBoostDurationSeconds(int level) {
        return Math.max(0.0f, familyBoostDurationSeconds + sumBoostDurationDelta(level));
    }

    public boolean resetsFamilyCooldowns(int level) {
        validateLevel(level);
        for (int currentLevel = MIN_LEVEL; currentLevel <= level; currentLevel++) {
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

    private int sumDamageDelta(int level) {
        validateLevel(level);
        int result = 0;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getDamageDelta();
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

    private float sumRechargeDelta(int level) {
        validateLevel(level);
        float result = 0.0f;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getRechargeDeltaSeconds();
        }
        return result;
    }

    private int sumSunPerHitDelta(int level) {
        validateLevel(level);
        int result = 0;
        for (int currentLevel = 2; currentLevel <= level; currentLevel++) {
            result += upgrades[currentLevel - 1].getSunPerHitDelta();
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
            throw new IllegalArgumentException("wall-nut level must be between 1 and 4");
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

    public WallnutBehavior getBehavior() {
        return behavior;
    }

    public int getPlantFoodArmor() {
        return plantFoodArmor;
    }

    public int getPlantFoodReflectBonus() {
        return plantFoodReflectBonus;
    }
}
