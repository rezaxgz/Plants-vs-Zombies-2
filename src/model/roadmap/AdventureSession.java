package model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Current in-memory adventure selection and unlock notifications.
 */
public final class AdventureSession {
    private static final AdventureSession INSTANCE =
            new AdventureSession();

    private final AdventureProgress progress;
    private final List<String> pendingNotifications;
    private String selectedChapterId;

    private AdventureSession() {
        progress = new AdventureProgress();
        pendingNotifications =
                new ArrayList<>();
        selectedChapterId =
                ChapterCatalog.getFirstChapter()
                        .getId();
    }

    public static AdventureSession getInstance() {
        return INSTANCE;
    }

    public void reset() {
        progress.reset();
        pendingNotifications.clear();
        selectedChapterId =
                ChapterCatalog.getFirstChapter()
                        .getId();
    }

    public boolean selectChapter(Chapter chapter) {
        if (!progress.isChapterUnlocked(chapter)) {
            return false;
        }
        selectedChapterId = chapter.getId();
        return true;
    }

    public Chapter getSelectedChapter() {
        Chapter selected =
                ChapterCatalog.findById(
                        selectedChapterId);
        return selected == null
                ? ChapterCatalog.getFirstChapter()
                : selected;
    }

    public boolean completeLevel(
            String chapterId, int levelNumber) {
        Chapter chapter =
                ChapterCatalog.findById(chapterId);
        if (!progress.completeLevel(
                chapter, levelNumber)) {
            return false;
        }

        addCompletionNotifications(
                chapter, levelNumber);
        return true;
    }

    private void addCompletionNotifications(
            Chapter chapter, int levelNumber) {
        pendingNotifications.add(
                "completed " + chapter.getDisplayName()
                        + " level " + levelNumber + ".");

        if (levelNumber
                < chapter.getLevelCount()) {
            pendingNotifications.add(
                    "unlocked "
                            + chapter.getDisplayName()
                            + " level "
                            + (levelNumber + 1) + ".");
            return;
        }

        Chapter next =
                ChapterCatalog.getNextChapter(
                        chapter);
        if (next == null) {
            pendingNotifications.add(
                    "all adventure chapters are complete.");
        } else {
            pendingNotifications.add(
                    "unlocked chapter "
                            + next.getDisplayName()
                            + ".");
        }
    }

    public List<String> drainNotifications() {
        if (pendingNotifications.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result =
                new ArrayList<>(
                        pendingNotifications);
        pendingNotifications.clear();
        return Collections.unmodifiableList(result);
    }

    public AdventureProgress getProgress() {
        return progress;
    }
}
