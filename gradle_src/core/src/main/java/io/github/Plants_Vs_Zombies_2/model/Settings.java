package io.github.Plants_Vs_Zombies_2.model;

/**
 * Per-user game preferences.
 */
public final class Settings {
    public static final int MIN_DIFFICULTY = 1;
    public static final int MAX_DIFFICULTY = 5;
    public static final int DEFAULT_DIFFICULTY = 3;

    private int difficultyLevel;

    public Settings() {
        this(DEFAULT_DIFFICULTY);
    }

    public Settings(int difficultyLevel) {
        setDifficultyLevel(difficultyLevel);
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        if (difficultyLevel < MIN_DIFFICULTY
                || difficultyLevel > MAX_DIFFICULTY) {
            throw new IllegalArgumentException(
                    "difficulty level must be between 1 and 5");
        }
        this.difficultyLevel = difficultyLevel;
    }
}
