package model.game.entities.plants.strikeThrough;

final class StrikeThroughUpgrade {
    static final StrikeThroughUpgrade NONE = new StrikeThroughUpgrade(
            0, 0, 0, 0, 0.0, 0.0f, 0.0f, false);

    private final int hitPointDelta;
    private final int damageDelta;
    private final int costDelta;
    private final int pierceDelta;
    private final double rangeDeltaTiles;
    private final float rechargeDeltaSeconds;
    private final float familyDurationDeltaSeconds;
    private final boolean resetFamilyCooldowns;

    StrikeThroughUpgrade(int hitPointDelta, int damageDelta, int costDelta,
            int pierceDelta, double rangeDeltaTiles,
            float rechargeDeltaSeconds, float familyDurationDeltaSeconds,
            boolean resetFamilyCooldowns) {
        this.hitPointDelta = hitPointDelta;
        this.damageDelta = damageDelta;
        this.costDelta = costDelta;
        this.pierceDelta = pierceDelta;
        this.rangeDeltaTiles = rangeDeltaTiles;
        this.rechargeDeltaSeconds = rechargeDeltaSeconds;
        this.familyDurationDeltaSeconds = familyDurationDeltaSeconds;
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

    int getPierceDelta() {
        return pierceDelta;
    }

    double getRangeDeltaTiles() {
        return rangeDeltaTiles;
    }

    float getRechargeDeltaSeconds() {
        return rechargeDeltaSeconds;
    }

    float getFamilyDurationDeltaSeconds() {
        return familyDurationDeltaSeconds;
    }

    boolean resetsFamilyCooldowns() {
        return resetFamilyCooldowns;
    }
}
