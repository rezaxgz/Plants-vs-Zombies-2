package model.game.entities.plants.modifier;

public final class ModifierUpgrade {
    public static final ModifierUpgrade NONE = new ModifierUpgrade(
            0, 0, 0.0f, 0.0f, false, 0.0, 0.0, false, false);

    private final int hitPointDelta;
    private final int costDelta;
    private final float rechargeDeltaSeconds;
    private final float familyDurationDeltaSeconds;
    private final boolean deathAreaEffect;
    private final double hypnotizedHealthMultiplierDelta;
    private final double hypnotizedDamageMultiplierDelta;
    private final boolean plantFoodOnEntrance;
    private final boolean resetFamilyCooldowns;

    public ModifierUpgrade(int hitPointDelta, int costDelta,
            float rechargeDeltaSeconds, float familyDurationDeltaSeconds,
            boolean deathAreaEffect, double hypnotizedHealthMultiplierDelta,
            double hypnotizedDamageMultiplierDelta, boolean plantFoodOnEntrance,
            boolean resetFamilyCooldowns) {
        this.hitPointDelta = hitPointDelta;
        this.costDelta = costDelta;
        this.rechargeDeltaSeconds = rechargeDeltaSeconds;
        this.familyDurationDeltaSeconds = familyDurationDeltaSeconds;
        this.deathAreaEffect = deathAreaEffect;
        this.hypnotizedHealthMultiplierDelta = hypnotizedHealthMultiplierDelta;
        this.hypnotizedDamageMultiplierDelta = hypnotizedDamageMultiplierDelta;
        this.plantFoodOnEntrance = plantFoodOnEntrance;
        this.resetFamilyCooldowns = resetFamilyCooldowns;
    }

    public int getHitPointDelta() {
        return hitPointDelta;
    }

    public int getCostDelta() {
        return costDelta;
    }

    public float getRechargeDeltaSeconds() {
        return rechargeDeltaSeconds;
    }

    public float getFamilyDurationDeltaSeconds() {
        return familyDurationDeltaSeconds;
    }

    public boolean hasDeathAreaEffect() {
        return deathAreaEffect;
    }

    public double getHypnotizedHealthMultiplierDelta() {
        return hypnotizedHealthMultiplierDelta;
    }

    public double getHypnotizedDamageMultiplierDelta() {
        return hypnotizedDamageMultiplierDelta;
    }

    public boolean hasPlantFoodOnEntrance() {
        return plantFoodOnEntrance;
    }

    public boolean resetsFamilyCooldowns() {
        return resetFamilyCooldowns;
    }
}
