package io.github.Plants_Vs_Zombies_2.model.game;

/**
 * Result of collecting every sun currently located on one board tile.
 */
public final class SunCollectionResult {
    private final int collectedSunCount;
    private final int collectedSunAmount;
    private final int radioactiveExplosionCount;

    public SunCollectionResult(int collectedSunCount, int collectedSunAmount,
            int radioactiveExplosionCount) {
        this.collectedSunCount = collectedSunCount;
        this.collectedSunAmount = collectedSunAmount;
        this.radioactiveExplosionCount = radioactiveExplosionCount;
    }

    public boolean hasCollectedAnything() {
        return collectedSunCount > 0;
    }

    public int getCollectedSunCount() {
        return collectedSunCount;
    }

    public int getCollectedSunAmount() {
        return collectedSunAmount;
    }

    public int getRadioactiveExplosionCount() {
        return radioactiveExplosionCount;
    }
}
