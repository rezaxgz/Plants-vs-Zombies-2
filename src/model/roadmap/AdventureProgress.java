package model.roadmap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Session-only chapter and level unlock state.
 */
public final class AdventureProgress {
    private final Map<String, Integer>
            highestUnlockedLevel;
    private final Set<String> completedLevels;

    public AdventureProgress() {
        highestUnlockedLevel = new HashMap<>();
        completedLevels = new HashSet<>();
        reset();
    }

    public void reset() {
        highestUnlockedLevel.clear();
        completedLevels.clear();
        for (Chapter chapter :
                ChapterCatalog.getChapters()) {
            highestUnlockedLevel.put(
                    chapter.getId(), 0);
        }
        Chapter first =
                ChapterCatalog.getFirstChapter();
        highestUnlockedLevel.put(
                first.getId(), 1);
    }

    public boolean isChapterUnlocked(
            Chapter chapter) {
        return chapter != null
                && highestUnlockedLevel.getOrDefault(
                        chapter.getId(), 0) > 0;
    }

    public boolean isLevelUnlocked(
            Chapter chapter, int levelNumber) {
        return chapter != null
                && levelNumber >= 1
                && levelNumber
                        <= highestUnlockedLevel
                                .getOrDefault(
                                        chapter.getId(), 0);
    }

    public boolean isLevelCompleted(
            Chapter chapter, int levelNumber) {
        return chapter != null
                && completedLevels.contains(
                        levelKey(
                                chapter, levelNumber));
    }

    public boolean completeLevel(
            Chapter chapter, int levelNumber) {
        if (!isLevelUnlocked(
                chapter, levelNumber)
                || chapter.getLevel(levelNumber)
                        == null) {
            return false;
        }

        String key =
                levelKey(chapter, levelNumber);
        if (!completedLevels.add(key)) {
            return false;
        }

        unlockFollowingContent(
                chapter, levelNumber);
        return true;
    }

    private void unlockFollowingContent(
            Chapter chapter, int levelNumber) {
        if (levelNumber
                < chapter.getLevelCount()) {
            int nextLevel = levelNumber + 1;
            int current = highestUnlockedLevel
                    .getOrDefault(
                            chapter.getId(), 0);
            highestUnlockedLevel.put(
                    chapter.getId(),
                    Math.max(current, nextLevel));
            return;
        }

        Chapter nextChapter =
                ChapterCatalog.getNextChapter(
                        chapter);
        if (nextChapter != null) {
            highestUnlockedLevel.put(
                    nextChapter.getId(),
                    Math.max(1,
                            highestUnlockedLevel
                                    .getOrDefault(
                                            nextChapter
                                                    .getId(),
                                            0)));
        }
    }

    public int getHighestUnlockedLevel(
            Chapter chapter) {
        if (chapter == null) {
            return 0;
        }
        return highestUnlockedLevel
                .getOrDefault(chapter.getId(), 0);
    }

    public int getCompletedLevelCount(
            Chapter chapter) {
        if (chapter == null) {
            return 0;
        }
        int count = 0;
        for (int level = 1;
                level <= chapter.getLevelCount();
                level++) {
            if (isLevelCompleted(
                    chapter, level)) {
                count++;
            }
        }
        return count;
    }

    public int getRecommendedLevel(
            Chapter chapter) {
        int highest =
                getHighestUnlockedLevel(chapter);
        for (int level = 1;
                level <= highest; level++) {
            if (!isLevelCompleted(
                    chapter, level)) {
                return level;
            }
        }
        return Math.max(1, highest);
    }

    private static String levelKey(
            Chapter chapter, int levelNumber) {
        return chapter.getId()
                + ":" + levelNumber;
    }
}
