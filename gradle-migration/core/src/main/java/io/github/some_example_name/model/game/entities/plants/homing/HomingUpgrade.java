package io.github.some_example_name.model.game.entities.plants.homing;

final class HomingUpgrade {
    static final HomingUpgrade NONE = new HomingUpgrade(
            0, 0, 0, 0.0f, 0.0f, 0.0, 0.0f, false, false);

    private final int hitPointDelta;
    private final int damageDelta;
    private final int costDelta;
    private final float actionIntervalDeltaSeconds;
    private final float rechargeDeltaSeconds;
    private final double rangeDeltaTiles;
    private final float familyDurationDeltaSeconds;
    private final boolean targetPriorityUp;
    private final boolean resetFamilyCooldowns;

    HomingUpgrade(int hitPointDelta, int damageDelta, int costDelta,
            float actionIntervalDeltaSeconds, float rechargeDeltaSeconds,
            double rangeDeltaTiles, float familyDurationDeltaSeconds,
            boolean targetPriorityUp,
            boolean resetFamilyCooldowns) {
        this.hitPointDelta = hitPointDelta;
        this.damageDelta = damageDelta;
        this.costDelta = costDelta;
        this.actionIntervalDeltaSeconds = actionIntervalDeltaSeconds;
        this.rechargeDeltaSeconds = rechargeDeltaSeconds;
        this.rangeDeltaTiles = rangeDeltaTiles;
        this.familyDurationDeltaSeconds = familyDurationDeltaSeconds;
        this.targetPriorityUp = targetPriorityUp;
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

    float getRechargeDeltaSeconds() {
        return rechargeDeltaSeconds;
    }

    double getRangeDeltaTiles() {
        return rangeDeltaTiles;
    }

    float getFamilyDurationDeltaSeconds() {
        return familyDurationDeltaSeconds;
    }

    boolean hasTargetPriorityUp() {
        return targetPriorityUp;
    }

    boolean resetsFamilyCooldowns() {
        return resetFamilyCooldowns;
    }
}
