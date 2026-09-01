package io.github.Plants_Vs_Zombies_2.network.gameplay;

import java.util.Objects;

import io.github.Plants_Vs_Zombies_2.model.Settings;

/** Serializable per-account preferences synchronized by the multiplayer server. */
public final class SettingsGameplayState {
    private final int difficultyLevel;
    private final boolean debugMode;
    private final int gameSpeed;
    private final boolean showGameMapGrid;

    public SettingsGameplayState(int difficultyLevel, boolean debugMode,
            int gameSpeed, boolean showGameMapGrid) {
        this.difficultyLevel = difficultyLevel;
        this.debugMode = debugMode;
        this.gameSpeed = gameSpeed;
        this.showGameMapGrid = showGameMapGrid;
    }

    public static SettingsGameplayState fromSettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        return new SettingsGameplayState(settings.getDifficultyLevel(),
                settings.isDebugMode(), settings.getGameSpeed(),
                settings.isShowGameMapGrid());
    }

    public Settings toSettings() {
        return new Settings(difficultyLevel, debugMode, gameSpeed,
                showGameMapGrid);
    }

    public int getDifficultyLevel() { return difficultyLevel; }
    public boolean isDebugMode() { return debugMode; }
    public int getGameSpeed() { return gameSpeed; }
    public boolean isShowGameMapGrid() { return showGameMapGrid; }

    @Override public boolean equals(Object other) {
        return other instanceof SettingsGameplayState value
                && difficultyLevel == value.difficultyLevel
                && debugMode == value.debugMode
                && gameSpeed == value.gameSpeed
                && showGameMapGrid == value.showGameMapGrid;
    }

    @Override public int hashCode() {
        return Objects.hash(difficultyLevel, debugMode, gameSpeed,
                showGameMapGrid);
    }
}
