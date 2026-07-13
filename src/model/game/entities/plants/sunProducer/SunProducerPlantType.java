package model.game.entities.plants.sunProducer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import model.game.entities.plants.PlantTag;

public enum SunProducerPlantType {
    SUNFLOWER(
            1, "Sunflower", EnumSet.of(PlantTag.DAY), 50, 300, 0,
            "Produces 50 sun every 24 seconds.",
            "Immediately produces 150 sun.",
            "Production time -2s", "HP +150", "Double sun chance",
            24.0f, 5.0f, SunProductionBehavior.PERIODIC,
            new int[] {50}, new float[0]),

    TWIN_SUNFLOWER(
            2, "Twin Sunflower", EnumSet.of(PlantTag.DAY), 125, 300, 0,
            "Produces 100 sun every cycle.",
            "Immediately produces 250 sun.",
            "Production time -2s", "HP +150", "Cost -25",
            24.0f, 15.0f, SunProductionBehavior.PERIODIC,
            new int[] {100}, new float[0]),

    SUN_SHROOM(
            3, "Sun-shroom", EnumSet.of(PlantTag.SHROOM, PlantTag.WRAMP_UP, PlantTag.NIGHT), 25, 300, 0,
            "Grows through three stages and produces 25, 50, then 75 sun.",
            "Immediately reaches its final stage and produces 225 sun.",
            "Grow time -5s", "HP +150", "Double sun chance",
            24.0f, 5.0f, SunProductionBehavior.PERIODIC,
            new int[] {25, 50, 75}, new float[] {24.0f, 72.0f}),

    PRIMAL_SUNFLOWER(
            4, "Primal Sunflower", Collections.emptySet(), 75, 300, 0,
            "Produces 75 sun every 24 seconds.",
            "Immediately produces 225 sun.",
            "Production time -2s", "HP +150", "Cost -25",
            24.0f, 5.0f, SunProductionBehavior.PERIODIC,
            new int[] {75}, new float[0]),

    GOLD_BLOOM(
            5, "Gold Bloom", Collections.emptySet(), 0, 0, 0,
            "Immediately produces 375 sun once, then disappears.",
            "No plant food effect.",
            "Cooldown -5s", "Sun +50", "Cost -25",
            0.0f, 75.0f, SunProductionBehavior.INSTANT,
            new int[] {375}, new float[0]),

    ENLIGHTEN_MINT(
            61, "Enlighten-mint", Collections.emptySet(), 0, 0, 0,
            "Temporarily applies plant food to plants in its family.",
            "No plant food effect.",
            "Duration +1s", "Cooldown -5s", "Reset family cooldowns",
            0.0f, 85.0f, SunProductionBehavior.FAMILY_BOOST,
            new int[] {0}, new float[0]);

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

    SunProducerPlantType(int id, String displayName, Set<PlantTag> tags,
            int cost, int baseHP, int damage,
            String baseAbility, String plantFoodEffect,
            String levelTwoUpgrade, String levelThreeUpgrade, String levelFourUpgrade,
            float actionIntervalSeconds, float rechargeSeconds,
            SunProductionBehavior behavior, int[] sunAmounts, float[] stageThresholdSeconds) {
        if (sunAmounts.length != stageThresholdSeconds.length + 1) {
            throw new IllegalArgumentException("Each growth threshold must lead to one additional sun amount");
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
    }

    private static Set<PlantTag> immutableTags(Set<PlantTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(tags));
    }

    public int getSunAmountAt(double ageSeconds) {
        int stage = 0;
        while (stage < stageThresholdSeconds.length
                && ageSeconds >= stageThresholdSeconds[stage]) {
            stage++;
        }
        return sunAmounts[stage];
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
}
