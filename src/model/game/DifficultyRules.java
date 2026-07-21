package model.game;

import model.Settings;

/**
 * Converts the saved difficulty level into the five gameplay multipliers
 * required by the project specification.
 */
public final class DifficultyRules {
    private static final double DEFAULT_LEVEL = 3.0;

    private final int level;
    private final double increaseMultiplier;
    private final double decreaseMultiplier;

    private DifficultyRules(int level) {
        if (level < Settings.MIN_DIFFICULTY
                || level > Settings.MAX_DIFFICULTY) {
            throw new IllegalArgumentException(
                    "difficulty level must be between 1 and 5");
        }
        this.level = level;
        this.increaseMultiplier = level / DEFAULT_LEVEL;
        this.decreaseMultiplier = DEFAULT_LEVEL / level;
    }

    public static DifficultyRules forLevel(int level) {
        return new DifficultyRules(level);
    }

    public int getLevel() {
        return level;
    }

    public double getZombieHealthMultiplier() {
        return increaseMultiplier;
    }

    public double getZombieDamageMultiplier() {
        return increaseMultiplier;
    }

    public double getGameSpeedMultiplier() {
        return increaseMultiplier;
    }

    public double getSkySunIntervalMultiplier() {
        return increaseMultiplier;
    }

    public double getZombieWaveCostMultiplier() {
        return decreaseMultiplier;
    }

    public float scaleGameDelta(float deltaSeconds) {
        return (float) (deltaSeconds * getGameSpeedMultiplier());
    }

    public double scaleSkySunInterval(double baseInterval) {
        return baseInterval * getSkySunIntervalMultiplier();
    }
}
