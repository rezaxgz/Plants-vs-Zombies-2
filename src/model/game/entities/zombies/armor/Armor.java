package model.game.entities.zombies.armor;

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

    public Armor(ArmorType type) {
        this.type = type;
        this.maximumHealth = type.getBaseHealth();
        this.currentHealth = maximumHealth;
        this.destroyed = false;
        this.dropped = false;
    }

    public Armor(ArmorType type, int healthMultiplier) {
        this.type = type;
        this.maximumHealth = type.getBaseHealth() * healthMultiplier;
        this.currentHealth = maximumHealth;
        this.destroyed = false;
        this.dropped = false;
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

    public ArmorType getType() { return type; }
    public int getCurrentHealth() { return currentHealth; }
    public int getMaximumHealth() { return maximumHealth; }
    public boolean isDestroyed() { return destroyed; }
    public boolean isDropped() { return dropped; }

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
