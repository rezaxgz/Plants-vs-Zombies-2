package io.github.some_example_name.model.game.entities.plants.shooter;

final class ShooterUpgrade {
    static final ShooterUpgrade NONE = new ShooterUpgrade(
            0, 0, 0, 0.0f, 1.0f, 0.0, 0, 0.0, 0.0,
            0.0f, 0.0f, 0.0, false);

    private final int hitPointDelta;
    private final int damageDelta;
    private final int costDelta;
    private final float actionIntervalDeltaSeconds;
    private final float actionIntervalMultiplier;
    private final double chillDurationDeltaSeconds;
    private final int poisonDamagePerTickDelta;
    private final double rangeDeltaTiles;
    private final double lifespanDeltaSeconds;
    private final float rechargeDeltaSeconds;
    private final float familyBoostDurationDeltaSeconds;
    private final double plantFoodChanceDelta;
    private final boolean resetFamilyCooldowns;

    ShooterUpgrade(int hitPointDelta, int damageDelta, int costDelta,
            float actionIntervalDeltaSeconds, float actionIntervalMultiplier,
            double chillDurationDeltaSeconds, int poisonDamagePerTickDelta,
            double rangeDeltaTiles, double lifespanDeltaSeconds,
            float rechargeDeltaSeconds, float familyBoostDurationDeltaSeconds,
            double plantFoodChanceDelta, boolean resetFamilyCooldowns) {
        this.hitPointDelta = hitPointDelta;
        this.damageDelta = damageDelta;
        this.costDelta = costDelta;
        this.actionIntervalDeltaSeconds = actionIntervalDeltaSeconds;
        this.actionIntervalMultiplier = actionIntervalMultiplier;
        this.chillDurationDeltaSeconds = chillDurationDeltaSeconds;
        this.poisonDamagePerTickDelta = poisonDamagePerTickDelta;
        this.rangeDeltaTiles = rangeDeltaTiles;
        this.lifespanDeltaSeconds = lifespanDeltaSeconds;
        this.rechargeDeltaSeconds = rechargeDeltaSeconds;
        this.familyBoostDurationDeltaSeconds = familyBoostDurationDeltaSeconds;
        this.plantFoodChanceDelta = plantFoodChanceDelta;
        this.resetFamilyCooldowns = resetFamilyCooldowns;
    }

    int getHitPointDelta() {
        return hitPointDelta;
    }

    int getDamageDelta() {
        return damageDelta;
    }

    int getCostDelta() {
        return costDelta;
    }

    float getActionIntervalDeltaSeconds() {
        return actionIntervalDeltaSeconds;
    }

    float getActionIntervalMultiplier() {
        return actionIntervalMultiplier;
    }

    double getChillDurationDeltaSeconds() {
        return chillDurationDeltaSeconds;
    }

    int getPoisonDamagePerTickDelta() {
        return poisonDamagePerTickDelta;
    }

    double getRangeDeltaTiles() {
        return rangeDeltaTiles;
    }

    double getLifespanDeltaSeconds() {
        return lifespanDeltaSeconds;
    }

    float getRechargeDeltaSeconds() {
        return rechargeDeltaSeconds;
    }

    float getFamilyBoostDurationDeltaSeconds() {
        return familyBoostDurationDeltaSeconds;
    }

    double getPlantFoodChanceDelta() {
        return plantFoodChanceDelta;
    }

    boolean resetsFamilyCooldowns() {
        return resetFamilyCooldowns;
    }
}
