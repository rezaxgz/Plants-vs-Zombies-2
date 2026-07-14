package model.game.entities.plants.sunProducer;

final class SunProducerUpgrade {
    static final SunProducerUpgrade NONE = new SunProducerUpgrade(
            0, 0, 0.0f, 0.0f, 0.0f, 0, false, 0.0f, false);

    private final int hitPointDelta;
    private final int costDelta;
    private final float actionIntervalDeltaSeconds;
    private final float rechargeDeltaSeconds;
    private final float growthTimeDeltaSeconds;
    private final int sunAmountDelta;
    private final boolean doubleSunChance;
    private final float boostDurationDeltaSeconds;
    private final boolean resetFamilyCooldowns;

    SunProducerUpgrade(int hitPointDelta, int costDelta,
            float actionIntervalDeltaSeconds, float rechargeDeltaSeconds,
            float growthTimeDeltaSeconds, int sunAmountDelta,
            boolean doubleSunChance, float boostDurationDeltaSeconds,
            boolean resetFamilyCooldowns) {
        this.hitPointDelta = hitPointDelta;
        this.costDelta = costDelta;
        this.actionIntervalDeltaSeconds = actionIntervalDeltaSeconds;
        this.rechargeDeltaSeconds = rechargeDeltaSeconds;
        this.growthTimeDeltaSeconds = growthTimeDeltaSeconds;
        this.sunAmountDelta = sunAmountDelta;
        this.doubleSunChance = doubleSunChance;
        this.boostDurationDeltaSeconds = boostDurationDeltaSeconds;
        this.resetFamilyCooldowns = resetFamilyCooldowns;
    }

    int getHitPointDelta() {
        return hitPointDelta;
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

    float getGrowthTimeDeltaSeconds() {
        return growthTimeDeltaSeconds;
    }

    int getSunAmountDelta() {
        return sunAmountDelta;
    }

    boolean hasDoubleSunChance() {
        return doubleSunChance;
    }

    float getBoostDurationDeltaSeconds() {
        return boostDurationDeltaSeconds;
    }

    boolean resetsFamilyCooldowns() {
        return resetFamilyCooldowns;
    }
}
