package model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.App;
import model.auth.UserManager;
import model.user.User;

/**
 * Current adventure selection and unlock notifications.
 * Progress itself belongs to the logged-in user.
 */
public final class AdventureSession {
    private static final AdventureSession INSTANCE = new AdventureSession();

    private final AdventureProgress guestProgress;
    private final List<String> pendingNotifications;
    private String selectedChapterId;

    private AdventureSession() {
        guestProgress = new AdventureProgress();
        pendingNotifications = new ArrayList<>();
        selectedChapterId = ChapterCatalog.getFirstChapter().getId();
    }

    public static AdventureSession getInstance() {
        return INSTANCE;
    }

    /**
     * Clears transient menu state without deleting any user's saved progress.
     */
    public void reset() {
        pendingNotifications.clear();
        selectedChapterId = ChapterCatalog.getFirstChapter().getId();
    }

    public boolean selectChapter(Chapter chapter) {
        if (!getProgress().isChapterUnlocked(chapter)) {
            return false;
        }
        selectedChapterId = chapter.getId();
        return true;
    }

    public Chapter getSelectedChapter() {
        Chapter selected = ChapterCatalog.findById(selectedChapterId);
        if (selected == null || !getProgress().isChapterUnlocked(selected)) {
            return ChapterCatalog.getFirstChapter();
        }
        return selected;
    }

    public boolean completeLevel(String chapterId, int levelNumber) {
        Chapter chapter = ChapterCatalog.findById(chapterId);
        if (!getProgress().completeLevel(chapter, levelNumber)) {
            return false;
        }

        updateUserStatistics(chapter, levelNumber);
        addCompletionNotifications(chapter, levelNumber);
        UserManager.saveAllUsers();
        return true;
    }

    private static void updateUserStatistics(
            Chapter chapter, int levelNumber) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null || chapter == null) {
            return;
        }
        int chapterNumber = ChapterCatalog.getChapters().indexOf(chapter) + 1;
        user.getGameProgerss().recordCompletedLevel(chapterNumber, levelNumber);
    }

    private void addCompletionNotifications(Chapter chapter, int levelNumber) {
        pendingNotifications.add("completed " + chapter.getDisplayName()
                + " level " + levelNumber + ".");

        if (levelNumber < chapter.getLevelCount()) {
            pendingNotifications.add("unlocked " + chapter.getDisplayName()
                    + " level " + (levelNumber + 1) + ".");
            return;
        }

        Chapter next = ChapterCatalog.getNextChapter(chapter);
        if (next == null) {
            pendingNotifications.add("all adventure chapters are complete.");
        } else {
            pendingNotifications.add(
                    "unlocked chapter " + next.getDisplayName() + ".");
        }
    }

    public List<String> drainNotifications() {
        if (pendingNotifications.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(pendingNotifications);
        pendingNotifications.clear();
        return Collections.unmodifiableList(result);
    }

    public AdventureProgress getProgress() {
        User user = App.getInstance().getLoggedInUser();
        return user == null ? guestProgress : user.getAdventureProgress();
    }
}
