package model.game.entities.plants.wallnut;

final class WallnutUpgrade {
    static final WallnutUpgrade NONE = new WallnutUpgrade(0, 0, 0, 0.0f, 0, 0.0f, false);

    private final int hitPointDelta;
    private final int damageDelta;
    private final int costDelta;
    private final float rechargeDeltaSeconds;
    private final int sunPerHitDelta;
    private final float boostDurationDeltaSeconds;
    private final boolean resetFamilyCooldowns;

    WallnutUpgrade(int hitPointDelta, int damageDelta, int costDelta,
            float rechargeDeltaSeconds, int sunPerHitDelta,
            float boostDurationDeltaSeconds, boolean resetFamilyCooldowns) {
        this.hitPointDelta = hitPointDelta;
        this.damageDelta = damageDelta;
        this.costDelta = costDelta;
        this.rechargeDeltaSeconds = rechargeDeltaSeconds;
        this.sunPerHitDelta = sunPerHitDelta;
        this.boostDurationDeltaSeconds = boostDurationDeltaSeconds;
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

    int getSunPerHitDelta() {
        return sunPerHitDelta;
    }

    float getBoostDurationDeltaSeconds() {
        return boostDurationDeltaSeconds;
    }

    boolean resetsFamilyCooldowns() {
        return resetFamilyCooldowns;
    }
}
