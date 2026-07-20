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

        User user = App.getInstance().getLoggedInUser();
        updateUserStatistics(user, chapter, levelNumber);
        addCompletionNotifications(chapter, levelNumber);
        addPersistentUnlockNews(user, chapter, levelNumber);
        UserManager.saveAllUsers();
        return true;
    }

    private static void updateUserStatistics(User user,
            Chapter chapter, int levelNumber) {
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

    private static void addPersistentUnlockNews(User user,
            Chapter chapter, int levelNumber) {
        if (user == null || chapter == null) {
            return;
        }
        if (levelNumber < chapter.getLevelCount()) {
            addLevelUnlockNews(user, chapter,
                    chapter.getLevel(levelNumber + 1));
            return;
        }

        Chapter nextChapter = ChapterCatalog.getNextChapter(chapter);
        if (nextChapter == null) {
            user.addNewsIfAbsent(
                    "Adventure Complete!",
                    "You completed every currently available adventure chapter.");
            return;
        }

        user.addNewsIfAbsent(
                "New Chapter Unlocked!",
                nextChapter.getDisplayName()
                        + " is now available in the game menu.");
        addLevelUnlockNews(user, nextChapter, nextChapter.getLevel(1));
    }

    private static void addLevelUnlockNews(User user,
            Chapter chapter, Level level) {
        if (level == null) {
            return;
        }
        String title = getLevelUnlockTitle(level);
        StringBuilder description = new StringBuilder()
                .append(chapter.getDisplayName())
                .append(" level ")
                .append(level.getNumber())
                .append(" (\"")
                .append(level.getName())
                .append("\") is now available");
        if (level.getSpecialLevelType().isSpecial()) {
            description.append(". Special rules: ")
                    .append(level.getSpecialLevelType().getDisplayName());
        }
        description.append('.');
        user.addNewsIfAbsent(title, description.toString());
    }

    private static String getLevelUnlockTitle(Level level) {
        if (level.getSpecialLevelType().isSpecial()) {
            return "Special Level Unlocked!";
        }
        if (level.getKind() == LevelKind.BOSS) {
            return "Boss Level Unlocked!";
        }
        return "New Level Unlocked!";
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
