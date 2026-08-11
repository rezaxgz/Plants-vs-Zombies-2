package io.github.Plants_Vs_Zombies_2.model.game.structure;

/**
 * Visible vase variants used by Vase Breaker.
 */
public enum VaseType {
    NORMAL("normal vase", "?"),
    PLANT("plant vase", "P"),
    GIANT("giant vase", "G");

    private final String displayName;
    private final String mapSymbol;

    VaseType(String displayName, String mapSymbol) {
        this.displayName = displayName;
        this.mapSymbol = mapSymbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMapSymbol() {
        return mapSymbol;
    }
}
