package io.github.Plants_Vs_Zombies_2.model.game.entities.plants.explosive;

final class ExplosiveUpgrade {
    static final ExplosiveUpgrade NONE = new ExplosiveUpgrade(
            0, 0, 0, 0.0f, 0.0, 0, 0, 0.0,
            0, 0, false, false, 0.0f, false);

    private final int hitPointDelta;
    private final int damageDelta;
    private final int costDelta;
    private final float rechargeDeltaSeconds;
    private final double armTimeDeltaSeconds;
    private final int grapeBounceDelta;
    private final int targetCountDelta;
    private final double freezeDurationDeltaSeconds;
    private final int meltRadius;
    private final int eatTimeDeltaSeconds;
    private final boolean extraSquashUse;
    private final boolean explodeOnFinish;
    private final float familyBoostDurationDeltaSeconds;
    private final boolean resetFamilyCooldowns;

    ExplosiveUpgrade(int hitPointDelta, int damageDelta, int costDelta,
            float rechargeDeltaSeconds, double armTimeDeltaSeconds,
            int grapeBounceDelta, int targetCountDelta,
            double freezeDurationDeltaSeconds, int meltRadius,
            int eatTimeDeltaSeconds, boolean extraSquashUse,
            boolean explodeOnFinish, float familyBoostDurationDeltaSeconds,
            boolean resetFamilyCooldowns) {
        this.hitPointDelta = hitPointDelta;
        this.damageDelta = damageDelta;
        this.costDelta = costDelta;
        this.rechargeDeltaSeconds = rechargeDeltaSeconds;
        this.armTimeDeltaSeconds = armTimeDeltaSeconds;
        this.grapeBounceDelta = grapeBounceDelta;
        this.targetCountDelta = targetCountDelta;
        this.freezeDurationDeltaSeconds = freezeDurationDeltaSeconds;
        this.meltRadius = meltRadius;
        this.eatTimeDeltaSeconds = eatTimeDeltaSeconds;
        this.extraSquashUse = extraSquashUse;
        this.explodeOnFinish = explodeOnFinish;
        this.familyBoostDurationDeltaSeconds = familyBoostDurationDeltaSeconds;
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

    float getRechargeDeltaSeconds() {
        return rechargeDeltaSeconds;
    }

    double getArmTimeDeltaSeconds() {
        return armTimeDeltaSeconds;
    }

    int getGrapeBounceDelta() {
        return grapeBounceDelta;
    }

    int getTargetCountDelta() {
        return targetCountDelta;
    }

    double getFreezeDurationDeltaSeconds() {
        return freezeDurationDeltaSeconds;
    }

    int getMeltRadius() {
        return meltRadius;
    }

    int getEatTimeDeltaSeconds() {
        return eatTimeDeltaSeconds;
    }

    boolean hasExtraSquashUse() {
        return extraSquashUse;
    }

    boolean explodesOnFinish() {
        return explodeOnFinish;
    }

    float getFamilyBoostDurationDeltaSeconds() {
        return familyBoostDurationDeltaSeconds;
    }

    boolean resetsFamilyCooldowns() {
        return resetFamilyCooldowns;
    }
}
