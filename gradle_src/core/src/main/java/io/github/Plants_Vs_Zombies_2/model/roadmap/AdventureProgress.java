package io.github.Plants_Vs_Zombies_2.model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-user chapter and level unlock state.
 */
public final class AdventureProgress {
    private final Map<String, Integer> highestUnlockedLevel;
    private final Set<String> completedLevels;

    public AdventureProgress() {
        highestUnlockedLevel = new HashMap<>();
        completedLevels = new HashSet<>();
        reset();
    }

    public static AdventureProgress fromStoredData(
            Map<String, Integer> unlockedLevels,
            Set<String> storedCompletedLevels) {
        AdventureProgress progress = new AdventureProgress();
        progress.restoreUnlockedLevels(unlockedLevels);
        progress.restoreCompletedLevels(storedCompletedLevels);
        return progress;
    }

    public void reset() {
        highestUnlockedLevel.clear();
        completedLevels.clear();
        for (Chapter chapter : ChapterCatalog.getChapters()) {
            highestUnlockedLevel.put(chapter.getId(), 0);
        }
        Chapter first = ChapterCatalog.getFirstChapter();
        highestUnlockedLevel.put(first.getId(), 1);
    }

    private void restoreUnlockedLevels(Map<String, Integer> storedLevels) {
        if (storedLevels == null) {
            return;
        }
        for (Chapter chapter : ChapterCatalog.getChapters()) {
            Integer storedLevel = storedLevels.get(chapter.getId());
            if (storedLevel == null) {
                continue;
            }
            int clamped = Math.max(0,
                    Math.min(chapter.getLevelCount(), storedLevel));
            highestUnlockedLevel.put(chapter.getId(), clamped);
        }
        Chapter first = ChapterCatalog.getFirstChapter();
        highestUnlockedLevel.put(first.getId(), Math.max(1,
                highestUnlockedLevel.getOrDefault(first.getId(), 0)));
    }

    private void restoreCompletedLevels(Set<String> storedLevels) {
        if (storedLevels == null) {
            return;
        }
        for (String key : storedLevels) {
            StoredLevel storedLevel = parseLevelKey(key);
            if (storedLevel == null) {
                continue;
            }
            completedLevels.add(levelKey(
                    storedLevel.chapter(), storedLevel.levelNumber()));
            ensureCompletionUnlocks(
                    storedLevel.chapter(), storedLevel.levelNumber());
        }
    }

    private void ensureCompletionUnlocks(Chapter chapter, int levelNumber) {
        int current = highestUnlockedLevel.getOrDefault(chapter.getId(), 0);
        highestUnlockedLevel.put(chapter.getId(), Math.max(current, levelNumber));
        unlockFollowingContent(chapter, levelNumber);
    }

    private static StoredLevel parseLevelKey(String key) {
        if (key == null) {
            return null;
        }
        int separator = key.lastIndexOf(':');
        if (separator <= 0 || separator + 1 >= key.length()) {
            return null;
        }
        Chapter chapter = ChapterCatalog.findById(key.substring(0, separator));
        if (chapter == null) {
            return null;
        }
        try {
            int levelNumber = Integer.parseInt(key.substring(separator + 1));
            if (chapter.getLevel(levelNumber) == null) {
                return null;
            }
            return new StoredLevel(chapter, levelNumber);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public boolean isChapterUnlocked(Chapter chapter) {
        return chapter != null
                && highestUnlockedLevel.getOrDefault(chapter.getId(), 0) > 0;
    }

    public boolean isLevelUnlocked(Chapter chapter, int levelNumber) {
        return chapter != null
                && levelNumber >= 1
                && levelNumber <= highestUnlockedLevel
                        .getOrDefault(chapter.getId(), 0);
    }

    public boolean isLevelCompleted(Chapter chapter, int levelNumber) {
        return chapter != null
                && completedLevels.contains(levelKey(chapter, levelNumber));
    }

    public boolean completeLevel(Chapter chapter, int levelNumber) {
        if (!isLevelUnlocked(chapter, levelNumber)
                || chapter.getLevel(levelNumber) == null) {
            return false;
        }

        String key = levelKey(chapter, levelNumber);
        if (!completedLevels.add(key)) {
            return false;
        }

        unlockFollowingContent(chapter, levelNumber);
        return true;
    }

    private void unlockFollowingContent(Chapter chapter, int levelNumber) {
        if (levelNumber < chapter.getLevelCount()) {
            int nextLevel = levelNumber + 1;
            int current = highestUnlockedLevel
                    .getOrDefault(chapter.getId(), 0);
            highestUnlockedLevel.put(chapter.getId(),
                    Math.max(current, nextLevel));
            return;
        }

        Chapter nextChapter = ChapterCatalog.getNextChapter(chapter);
        if (nextChapter != null) {
            highestUnlockedLevel.put(nextChapter.getId(),
                    Math.max(1, highestUnlockedLevel
                            .getOrDefault(nextChapter.getId(), 0)));
        }
    }

    public int getHighestUnlockedLevel(Chapter chapter) {
        if (chapter == null) {
            return 0;
        }
        return highestUnlockedLevel.getOrDefault(chapter.getId(), 0);
    }

    public int getCompletedLevelCount(Chapter chapter) {
        if (chapter == null) {
            return 0;
        }
        int count = 0;
        for (int level = 1; level <= chapter.getLevelCount(); level++) {
            if (isLevelCompleted(chapter, level)) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCompletedLevelCount() {
        return completedLevels.size();
    }

    public int getRecommendedLevel(Chapter chapter) {
        int highest = getHighestUnlockedLevel(chapter);
        for (int level = 1; level <= highest; level++) {
            if (!isLevelCompleted(chapter, level)) {
                return level;
            }
        }
        return Math.max(1, highest);
    }

    public Map<String, Integer> getHighestUnlockedLevelsForStorage() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Chapter chapter : ChapterCatalog.getChapters()) {
            result.put(chapter.getId(),
                    highestUnlockedLevel.getOrDefault(chapter.getId(), 0));
        }
        return Collections.unmodifiableMap(result);
    }

    public List<String> getCompletedLevelsForStorage() {
        List<String> result = new ArrayList<>(completedLevels);
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    private static String levelKey(Chapter chapter, int levelNumber) {
        return chapter.getId() + ":" + levelNumber;
    }

    private record StoredLevel(Chapter chapter, int levelNumber) {
    }
}
