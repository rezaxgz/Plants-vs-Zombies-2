package model.game.entities.zombies.armor;

/**
 * Enum representing different types of zombie armor.
 * Each armor type has specific properties like health, damageability, and droppability.
 */
public enum ArmorType {
    NONE("None", 0, false, false, false, false),
    CONE("Cone", 370, true, true, false, true),
    BUCKET("Bucket", 1100, true, true, true, true),
    BRICK("Brick", 2200, true, true, false, true),
    SHOULDER_ARMOR("ShoulderArmor", 1600, true, false, false, false),
    CROWN("Crown", 1600, true, true, true, true),
    KNIGHT("KnightArmor", 3200, true, true, true, true),
    NEWSPAPER("Newspaper", 190, true, false, false, false),
    ICE_BLOCK("IceBlock", 1600, true, true, false, false),
    SARCOPHAGUS("Sarcophagus", 2400, true, true, false, false),
    SURFBOARD("Surfboard", 600, true, true, false, false);

    private final String displayName;
    private final int baseHealth;
    private final boolean damageable;
    private final boolean droppable;
    private final boolean metallic;
    private final boolean helm;

    ArmorType(String displayName, int baseHealth, boolean damageable, 
              boolean droppable, boolean metallic, boolean helm) {
        this.displayName = displayName;
        this.baseHealth = baseHealth;
        this.damageable = damageable;
        this.droppable = droppable;
        this.metallic = metallic;
        this.helm = helm;
    }

    public String getDisplayName() { return displayName; }
    public int getBaseHealth() { return baseHealth; }
    public boolean isDamageable() { return damageable; }
    public boolean isDroppable() { return droppable; }
    public boolean isMetallic() { return metallic; }
    public boolean isHelm() { return helm; }

    public boolean isMagnetizable() {
        return metallic || this == SHOULDER_ARMOR;
    }

    /**
     * Gets the health threshold for each damage layer.
     * Returns array of health percentages where layers change.
     */
    public double[] getLayerHealthThresholds() {
        return new double[]{0.666, 0.333};
    }
}
