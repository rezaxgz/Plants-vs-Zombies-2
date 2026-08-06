package io.github.some_example_name.model.game.entities.zombies.armor;

/**
 * Represents armor worn by a zombie. Armor absorbs damage before
 * the zombie's base health is affected.
 */
public class Armor {
    private final ArmorType type;
    private int currentHealth;
    private int maximumHealth;
    private boolean destroyed;
    private boolean dropped;
    private boolean magnetLayerRemoved;

    public Armor(ArmorType type) {
        this.type = type;
        this.maximumHealth = type.getBaseHealth();
        this.currentHealth = maximumHealth;
        this.destroyed = false;
        this.dropped = false;
    }

    public Armor(ArmorType type, int healthMultiplier) {
        this(type, (double) healthMultiplier);
    }

    public Armor(ArmorType type, double healthMultiplier) {
        if (type == null || !Double.isFinite(healthMultiplier)
                || healthMultiplier <= 0.0) {
            throw new IllegalArgumentException(
                    "armor type and multiplier are invalid");
        }
        this.type = type;
        this.maximumHealth = Math.max(1,
                (int) Math.round(
                        type.getBaseHealth() * healthMultiplier));
        this.currentHealth = maximumHealth;
        this.destroyed = false;
        this.dropped = false;
    }

    public void rescaleHealth(
            double oldMultiplier, double newMultiplier) {
        if (!Double.isFinite(oldMultiplier) || oldMultiplier <= 0.0
                || !Double.isFinite(newMultiplier)
                || newMultiplier <= 0.0) {
            throw new IllegalArgumentException(
                    "armor multipliers must be positive");
        }
        double healthRatio = maximumHealth == 0
                ? 0.0
                : (double) currentHealth / maximumHealth;
        maximumHealth = Math.max(1,
                (int) Math.round(
                        maximumHealth / oldMultiplier
                                * newMultiplier));
        currentHealth = Math.max(0,
                (int) Math.round(maximumHealth * healthRatio));
        destroyed = currentHealth == 0;
    }

    /**
     * Applies damage to armor. Returns remaining damage that should
     * pass through to the zombie's base health.
     */
    public int takeDamage(int damage) {
        if (destroyed || !type.isDamageable()) {
            return damage; // All damage passes through
        }

        if (currentHealth > damage) {
            currentHealth -= damage;
            return 0; // All absorbed
        } else {
            int remaining = damage - currentHealth;
            currentHealth = 0;
            destroyed = true;
            if (type.isDroppable()) {
                dropped = true;
            }
            return remaining; // Remaining damage passes through
        }
    }

    public boolean isMagnetizable() {
        if (destroyed) {
            return false;
        }
        if (type == ArmorType.KNIGHT) {
            return !magnetLayerRemoved;
        }
        return type.isMagnetizable();
    }

    public boolean removeByMagnet() {
        if (!isMagnetizable()) {
            return false;
        }
        if (type == ArmorType.KNIGHT) {
            currentHealth = Math.max(0,
                    currentHealth
                            - ArmorType.CROWN.getBaseHealth());
            magnetLayerRemoved = true;
            dropped = true;
            if (currentHealth == 0) {
                destroyed = true;
            }
            return true;
        }
        currentHealth = 0;
        destroyed = true;
        dropped = true;
        return true;
    }

    public ArmorType getType() {
        return type;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getMaximumHealth() {
        return maximumHealth;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public boolean isDropped() {
        return dropped;
    }

    public boolean isMagnetLayerRemoved() {
        return magnetLayerRemoved;
    }

    public double getHealthPercent() {
        return maximumHealth > 0 ? (double) currentHealth / maximumHealth : 0;
    }

    /**
     * Gets the current visual layer based on damage state.
     */
    public int getCurrentLayer() {
        double healthPercent = getHealthPercent();
        double[] thresholds = type.getLayerHealthThresholds();

        if (healthPercent > thresholds[0]) {
            return 0; // Normal
        } else if (healthPercent > thresholds[1]) {
            return 1; // Damaged
        } else {
            return 2; // Very damaged
        }
    }

    @Override
    public String toString() {
        return type.getDisplayName() + "(" + currentHealth + "/" + maximumHealth + ")";
    }
}
