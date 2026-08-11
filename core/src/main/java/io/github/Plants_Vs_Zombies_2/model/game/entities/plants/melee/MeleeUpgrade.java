package io.github.Plants_Vs_Zombies_2.model.game.entities.plants.melee;

final class MeleeUpgrade {
    static final MeleeUpgrade NONE = new MeleeUpgrade(
            0, 0, 1.0f, 0.0, 0.0, 0, 0.0f, 0.0f, false);

    private final int hitPointDelta;
    private final int damageDelta;
    private final float actionIntervalMultiplier;
    private final double digestTimeDeltaSeconds;
    private final double rangeDeltaTiles;
    private final int maximumGrowthStageDelta;
    private final float rechargeDeltaSeconds;
    private final float familyBoostDurationDeltaSeconds;
    private final boolean resetFamilyCooldowns;

    MeleeUpgrade(int hitPointDelta, int damageDelta,
            float actionIntervalMultiplier, double digestTimeDeltaSeconds,
            double rangeDeltaTiles, int maximumGrowthStageDelta,
            float rechargeDeltaSeconds, float familyBoostDurationDeltaSeconds,
            boolean resetFamilyCooldowns) {
        this.hitPointDelta = hitPointDelta;
        this.damageDelta = damageDelta;
        this.actionIntervalMultiplier = actionIntervalMultiplier;
        this.digestTimeDeltaSeconds = digestTimeDeltaSeconds;
        this.rangeDeltaTiles = rangeDeltaTiles;
        this.maximumGrowthStageDelta = maximumGrowthStageDelta;
        this.rechargeDeltaSeconds = rechargeDeltaSeconds;
        this.familyBoostDurationDeltaSeconds = familyBoostDurationDeltaSeconds;
        this.resetFamilyCooldowns = resetFamilyCooldowns;
    }

    int getHitPointDelta() {
        return hitPointDelta;
    }

    int getDamageDelta() {
        return damageDelta;
    }

    float getActionIntervalMultiplier() {
        return actionIntervalMultiplier;
    }

    double getDigestTimeDeltaSeconds() {
        return digestTimeDeltaSeconds;
    }

    double getRangeDeltaTiles() {
        return rangeDeltaTiles;
    }

    int getMaximumGrowthStageDelta() {
        return maximumGrowthStageDelta;
    }

    float getRechargeDeltaSeconds() {
        return rechargeDeltaSeconds;
    }

    float getFamilyBoostDurationDeltaSeconds() {
        return familyBoostDurationDeltaSeconds;
    }

    boolean resetsFamilyCooldowns() {
        return resetFamilyCooldowns;
    }
}
