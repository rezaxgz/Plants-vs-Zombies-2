package io.github.Plants_Vs_Zombies_2.model;

/**
 * Per-user game preferences.
 */
public final class Settings {
    public static final int MIN_DIFFICULTY = 1;
    public static final int MAX_DIFFICULTY = 5;
    public static final int DEFAULT_DIFFICULTY = 3;

    public static final int MIN_GAME_SPEED = 1;
    public static final int MAX_GAME_SPEED = 3;
    public static final int DEFAULT_GAME_SPEED = 1;

    private int difficultyLevel;
    private boolean debugMode;
    private int gameSpeed;
    private boolean showGameMapGrid;

    public Settings() {
        this(DEFAULT_DIFFICULTY, false, DEFAULT_GAME_SPEED, false);
    }

    public Settings(int difficultyLevel) {
        this(difficultyLevel, false, DEFAULT_GAME_SPEED, false);
    }

    public Settings(int difficultyLevel, boolean debugMode) {
        this(difficultyLevel, debugMode, DEFAULT_GAME_SPEED, false);
    }

    public Settings(int difficultyLevel, boolean debugMode,
            int gameSpeed, boolean showGameMapGrid) {
        setDifficultyLevel(difficultyLevel);
        setDebugMode(debugMode);
        setGameSpeed(gameSpeed);
        setShowGameMapGrid(showGameMapGrid);
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

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public int getGameSpeed() {
        return gameSpeed;
    }

    public void setGameSpeed(int gameSpeed) {
        if (gameSpeed < MIN_GAME_SPEED || gameSpeed > MAX_GAME_SPEED) {
            throw new IllegalArgumentException(
                    "game speed must be between 1 and 3");
        }
        this.gameSpeed = gameSpeed;
    }

    public boolean isShowGameMapGrid() {
        return showGameMapGrid;
    }

    public void setShowGameMapGrid(boolean showGameMapGrid) {
        this.showGameMapGrid = showGameMapGrid;
    }
}
