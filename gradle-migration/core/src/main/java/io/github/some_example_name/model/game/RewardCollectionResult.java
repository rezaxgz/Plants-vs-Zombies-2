package io.github.some_example_name.model.game;

/**
 * Currency and pot rewards collected from one board tile.
 */
public final class RewardCollectionResult {
    private final int dropCount;
    private final int coins;
    private final int diamonds;
    private final int pots;

    public RewardCollectionResult(int dropCount, int coins, int diamonds, int pots) {
        this.dropCount = dropCount;
        this.coins = coins;
        this.diamonds = diamonds;
        this.pots = pots;
    }

    public int getDropCount() {
        return dropCount;
    }

    public int getCoins() {
        return coins;
    }

    public int getDiamonds() {
        return diamonds;
    }

    public int getPots() {
        return pots;
    }
}
