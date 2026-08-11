package io.github.Plants_Vs_Zombies_2.model.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Aggregate statistics used by the profile, travel log, and leaderboard.
 *
 * <p>
 * The class name is kept for compatibility with the existing codebase.
 * </p>
 */
public class GameProgerss {
    private int lastCompletedChapter;
    private int lastCompletedLevel;
    private int completedMinigames;
    private int highestScore;
    private int gamesPlayed;
    private final Map<String, Integer> highestUnlockedMinigameLevels = new LinkedHashMap<>();
    private final Set<String> completedMinigameLevels = new LinkedHashSet<>();

    public static GameProgerss fromStoredData(
            int lastCompletedChapter, int lastCompletedLevel,
            int completedMinigames, int highestScore,
            int gamesPlayed) {
        return fromStoredData(lastCompletedChapter, lastCompletedLevel,
                completedMinigames, highestScore, gamesPlayed,
                Collections.emptyMap(), Collections.emptyList());
    }

    public static GameProgerss fromStoredData(
            int lastCompletedChapter, int lastCompletedLevel,
            int completedMinigames, int highestScore,
            int gamesPlayed,
            Map<String, Integer> unlockedMinigameLevels,
            List<String> completedMinigameLevelKeys) {
        GameProgerss progress = new GameProgerss();
        progress.lastCompletedChapter = Math.max(0, lastCompletedChapter);
        progress.lastCompletedLevel = Math.max(0, lastCompletedLevel);
        progress.completedMinigames = Math.max(0, completedMinigames);
        progress.highestScore = Math.max(0, highestScore);
        progress.gamesPlayed = Math.max(0, gamesPlayed);
        progress.restoreMinigameProgress(unlockedMinigameLevels,
                completedMinigameLevelKeys);
        progress.completedMinigames = Math.max(progress.completedMinigames,
                progress.completedMinigameLevels.size());
        return progress;
    }

    private void restoreMinigameProgress(
            Map<String, Integer> unlockedMinigameLevels,
            List<String> completedMinigameLevelKeys) {
        if (unlockedMinigameLevels != null) {
            for (Map.Entry<String, Integer> entry : unlockedMinigameLevels.entrySet()) {
                String id = normalizeMinigameId(entry.getKey());
                int level = entry.getValue() == null
                        ? 1
                        : Math.max(1, entry.getValue());
                if (!id.isBlank()) {
                    highestUnlockedMinigameLevels.put(id, level);
                }
            }
        }
        if (completedMinigameLevelKeys != null) {
            for (String key : completedMinigameLevelKeys) {
                String normalized = normalizeStoredLevelKey(key);
                if (!normalized.isBlank()) {
                    completedMinigameLevels.add(normalized);
                    deriveUnlockFromCompletedKey(normalized);
                }
            }
        }
    }

    private void deriveUnlockFromCompletedKey(String key) {
        int separator = key.lastIndexOf(':');
        if (separator <= 0 || separator == key.length() - 1) {
            return;
        }
        try {
            int completedLevel = Integer.parseInt(
                    key.substring(separator + 1));
            String id = key.substring(0, separator);
            highestUnlockedMinigameLevels.merge(id,
                    completedLevel + 1, Math::max);
        } catch (NumberFormatException ignored) {
            // Invalid legacy entries are ignored rather than breaking login.
        }
    }

    public int getLastCompletedChapter() {
        return lastCompletedChapter;
    }

    public void setLastCompletedChapter(int lastCompletedChapter) {
        this.lastCompletedChapter = Math.max(0, lastCompletedChapter);
    }

    public int getLastCompletedLevel() {
        return lastCompletedLevel;
    }

    public void setLastCompletedLevel(int lastCompletedLevel) {
        this.lastCompletedLevel = Math.max(0, lastCompletedLevel);
    }

    public void recordCompletedLevel(int chapterNumber, int levelNumber) {
        if (chapterNumber < 1 || levelNumber < 1) {
            return;
        }
        boolean isFurther = chapterNumber > lastCompletedChapter
                || chapterNumber == lastCompletedChapter
                        && levelNumber > lastCompletedLevel;
        if (isFurther) {
            lastCompletedChapter = chapterNumber;
            lastCompletedLevel = levelNumber;
        }
    }

    public int getCompletedMinigames() {
        return completedMinigames;
    }

    public void addCompletedMinigame() {
        completedMinigames++;
    }

    public boolean recordCompletedMinigameLevel(String minigameId,
            int levelNumber, int maximumLevel) {
        if (levelNumber < 1 || maximumLevel < 1
                || levelNumber > maximumLevel
                || !isMinigameLevelUnlocked(minigameId, levelNumber,
                        maximumLevel)) {
            return false;
        }
        String id = normalizeMinigameId(minigameId);
        if (id.isBlank()) {
            return false;
        }
        String levelKey = levelKey(id, levelNumber);
        if (!completedMinigameLevels.add(levelKey)) {
            return false;
        }
        completedMinigames++;
        int nextLevel = Math.min(maximumLevel, levelNumber + 1);
        highestUnlockedMinigameLevels.merge(id, nextLevel, Math::max);
        return true;
    }

    public boolean isMinigameLevelUnlocked(String minigameId,
            int levelNumber, int maximumLevel) {
        if (levelNumber < 1 || maximumLevel < 1
                || levelNumber > maximumLevel) {
            return false;
        }
        String id = normalizeMinigameId(minigameId);
        if (id.isBlank()) {
            return false;
        }
        int highestUnlocked = highestUnlockedMinigameLevels
                .getOrDefault(id, 1);
        return levelNumber <= Math.min(maximumLevel, highestUnlocked);
    }

    public boolean isMinigameLevelCompleted(String minigameId,
            int levelNumber) {
        String id = normalizeMinigameId(minigameId);
        return !id.isBlank()
                && completedMinigameLevels.contains(levelKey(id, levelNumber));
    }

    public int getHighestUnlockedMinigameLevel(String minigameId,
            int maximumLevel) {
        if (maximumLevel < 1) {
            return 0;
        }
        String id = normalizeMinigameId(minigameId);
        if (id.isBlank()) {
            return 0;
        }
        return Math.min(maximumLevel,
                highestUnlockedMinigameLevels.getOrDefault(id, 1));
    }

    public Map<String, Integer> getHighestUnlockedMinigameLevelsForStorage() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(highestUnlockedMinigameLevels));
    }

    public List<String> getCompletedMinigameLevelsForStorage() {
        return Collections.unmodifiableList(
                new ArrayList<>(completedMinigameLevels));
    }

    public int getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(int highestScore) {
        if (highestScore > this.highestScore) {
            this.highestScore = highestScore;
        }
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void recordGameStarted() {
        gamesPlayed++;
    }

    private static String levelKey(String minigameId, int levelNumber) {
        return minigameId + ":" + levelNumber;
    }

    private static String normalizeStoredLevelKey(String key) {
        if (key == null) {
            return "";
        }
        int separator = key.lastIndexOf(':');
        if (separator <= 0 || separator == key.length() - 1) {
            return "";
        }
        String id = normalizeMinigameId(key.substring(0, separator));
        try {
            int level = Integer.parseInt(key.substring(separator + 1));
            return level < 1 || id.isBlank() ? "" : levelKey(id, level);
        } catch (NumberFormatException exception) {
            return "";
        }
    }

    private static String normalizeMinigameId(String minigameId) {
        if (minigameId == null) {
            return "";
        }
        return minigameId.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
