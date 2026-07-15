package model.game.entities.plants.lobber;

final class LobberUpgrade {
    static final LobberUpgrade NONE = new LobberUpgrade(
            0, 0, 0, 1.0f, 0.0, 0.0, 0, 0.0f, 0.0f, false);

    private final int hitPointDelta;
    private final int damageDelta;
    private final int costDelta;
    private final float actionIntervalMultiplier;
    private final double butterChanceDelta;
    private final double splashDamageDelta;
    private final int warmthRadiusDelta;
    private final float rechargeDeltaSeconds;
    private final float familyBoostDurationDeltaSeconds;
    private final boolean resetFamilyCooldowns;

    LobberUpgrade(int hitPointDelta, int damageDelta, int costDelta,
            float actionIntervalMultiplier, double butterChanceDelta,
            double splashDamageDelta, int warmthRadiusDelta,
            float rechargeDeltaSeconds, float familyBoostDurationDeltaSeconds,
            boolean resetFamilyCooldowns) {
        this.hitPointDelta = hitPointDelta;
        this.damageDelta = damageDelta;
        this.costDelta = costDelta;
        this.actionIntervalMultiplier = actionIntervalMultiplier;
        this.butterChanceDelta = butterChanceDelta;
        this.splashDamageDelta = splashDamageDelta;
        this.warmthRadiusDelta = warmthRadiusDelta;
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

    int getCostDelta() {
        return costDelta;
    }

    float getActionIntervalMultiplier() {
        return actionIntervalMultiplier;
    }

    double getButterChanceDelta() {
        return butterChanceDelta;
    }

    double getSplashDamageDelta() {
        return splashDamageDelta;
    }

    int getWarmthRadiusDelta() {
        return warmthRadiusDelta;
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
