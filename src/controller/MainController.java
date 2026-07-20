package controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.collections.plants.PlantCollectionItem;
import model.game.Game;
import model.game.plantSelector.PlantSelection;
import model.menu.GameMenu;
import model.menu.PlantSelectionMenu;
import model.roadmap.AdventureProgress;
import model.roadmap.AdventureSession;
import model.roadmap.Chapter;
import model.roadmap.ChapterCatalog;
import model.roadmap.Level;
import model.roadmap.SpecialLevelType;
import model.user.User;

/**
 * Main-menu adventure navigation and level launching.
 */
public final class MainController {
    private MainController() {
    }

    public static CommandResult handleShowChapters(
            Matcher matcher) {
        AdventureSession session =
                AdventureSession.getInstance();
        AdventureProgress progress =
                session.getProgress();
        StringBuilder output =
                new StringBuilder("chapters");

        for (Chapter chapter :
                ChapterCatalog.getChapters()) {
            output.append(System.lineSeparator())
                    .append("- ")
                    .append(chapter.getDisplayName())
                    .append(" | ")
                    .append(progress
                            .isChapterUnlocked(chapter)
                                    ? "unlocked"
                                    : "locked")
                    .append(" | completed: ")
                    .append(progress
                            .getCompletedLevelCount(
                                    chapter))
                    .append('/')
                    .append(chapter.getLevelCount());
            if (chapter
                    == session.getSelectedChapter()) {
                output.append(" | selected");
            }
        }

        return withNotifications(
                CommandResult.success(
                        output.toString()));
    }

    public static CommandResult handleShowLevels(
            Matcher matcher) {
        String requested =
                matcher.group("chapter");
        Chapter chapter = requested == null
                ? AdventureSession.getInstance()
                        .getSelectedChapter()
                : ChapterCatalog.findChapter(requested);

        if (chapter == null) {
            return withNotifications(
                    CommandResult.error(
                            "chapter does not exist!"));
        }

        AdventureProgress progress =
                AdventureSession.getInstance()
                        .getProgress();
        if (!progress.isChapterUnlocked(chapter)) {
            return withNotifications(
                    CommandResult.error(
                            "chapter is locked!"));
        }

        return withNotifications(
                CommandResult.success(
                        formatLevels(
                                chapter, progress)));
    }

    public static CommandResult handleEnterChapter(
            Matcher matcher) {
        Chapter chapter =
                ChapterCatalog.findChapter(
                        matcher.group("chapter"));
        if (chapter == null) {
            return withNotifications(
                    CommandResult.error(
                            "chapter does not exist!"));
        }

        AdventureSession session =
                AdventureSession.getInstance();
        if (!session.selectChapter(chapter)) {
            return withNotifications(
                    CommandResult.error(
                            "chapter is locked!"));
        }

        return withNotifications(
                CommandResult.success(
                        "selected chapter: "
                                + chapter
                                        .getDisplayName()
                                + System.lineSeparator()
                                + formatLevels(
                                        chapter,
                                        session
                                                .getProgress())));
    }

    public static CommandResult handleStartLevel(
            Matcher matcher) {
        int levelNumber;
        try {
            levelNumber = Integer.parseInt(
                    matcher.group("level"));
        } catch (NumberFormatException exception) {
            return withNotifications(
                    CommandResult.error(
                            "level number is too large!"));
        }

        AdventureSession session =
                AdventureSession.getInstance();
        Chapter chapter =
                session.getSelectedChapter();
        Level level =
                chapter.getLevel(levelNumber);

        if (level == null) {
            return withNotifications(
                    CommandResult.error(
                            "level does not exist in "
                                    + chapter
                                            .getDisplayName()
                                    + "!"));
        }
        if (!session.getProgress()
                .isLevelUnlocked(
                        chapter, levelNumber)) {
            return withNotifications(
                    CommandResult.error(
                            "level is locked!"));
        }

        return startAdventureLevel(
                chapter, level);
    }

    public static CommandResult handleShowCurrentLevel(
            Matcher matcher) {
        AdventureSession session =
                AdventureSession.getInstance();
        Chapter chapter =
                session.getSelectedChapter();
        int recommended = session.getProgress()
                .getRecommendedLevel(chapter);
        Level level =
                chapter.getLevel(recommended);

        String message = "selected chapter: "
                + chapter.getDisplayName()
                + System.lineSeparator()
                + "recommended level: "
                + level.getNumber() + " - "
                + level.getName()
                + " [" + level.getKind() + "]";
        return withNotifications(
                CommandResult.success(message));
    }

    public static CommandResult handleStartGame(
            Matcher matcher) {
        AdventureSession session =
                AdventureSession.getInstance();
        Chapter chapter =
                session.getSelectedChapter();
        int levelNumber = session.getProgress()
                .getRecommendedLevel(chapter);
        return startAdventureLevel(
                chapter,
                chapter.getLevel(levelNumber));
    }

    private static CommandResult startAdventureLevel(
            Chapter chapter, Level level) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return withNotifications(
                    CommandResult.error("login is required to start a level!"));
        }

        if (level.getSpecialLevelType()
                == SpecialLevelType.CONVEYOR_BELT) {
            return launchAdventureGame(chapter, level,
                    Map.of(), List.of(),
                    "Conveyor Belt levels skip plant selection.");
        }
        if (level.getSpecialLevelType()
                == SpecialLevelType.LOCKED_PLANTS) {
            return launchAdventureGame(chapter, level,
                    createForcedLoadout(user, level), List.of(),
                    "Locked Plants uses its fixed plant loadout.");
        }

        PlantSelection selection = new PlantSelection(
                user.getPlantCollection(), level);
        PlantSelectionMenu menu = new PlantSelectionMenu(
                selection, chapter.getId(), level.getNumber(), level);
        if (selection.shouldStartAutomatically()) {
            selection.selectAllAvailable();
            return PlantSelectionController.startGame(menu, true);
        }

        App.getInstance().changeMenu(menu);
        String message = "plant selection started for "
                + chapter.getDisplayName() + " level "
                + level.getNumber() + " - " + level.getName()
                + System.lineSeparator()
                + "selected: 0/" + selection.getSlotCount()
                + System.lineSeparator()
                + "use show available plants, add plant -t <type>, "
                + "boost plant -t <type>, and start game";
        return withNotifications(CommandResult.success(message));
    }

    private static Map<String, Integer> createForcedLoadout(
            User user, Level level) {
        Map<String, Integer> loadout = new LinkedHashMap<>();
        for (String plantName : level.getSpecialConfig().getPlantPool()) {
            PlantCollectionItem plant = user.getPlantCollection()
                    .findPlant(plantName);
            loadout.put(plantName, plant == null
                    ? PlantCollectionItem.MIN_LEVEL
                    : plant.getCurrentLevel());
        }
        return loadout;
    }

    static CommandResult launchAdventureGame(
            Chapter chapter, Level level,
            Map<String, Integer> selectedPlantLevels,
            List<String> boostedPlantNames,
            String preface) {
        Game game = level.createGame();
        if (selectedPlantLevels != null
                && !selectedPlantLevels.isEmpty()) {
            game.configurePlantLoadout(
                    selectedPlantLevels, boostedPlantNames);
        }
        User user = App.getInstance().getLoggedInUser();
        if (user != null) {
            user.getGameProgerss().recordGameStarted();
            UserManager.saveAllUsers();
        }
        App.getInstance().changeMenu(
                new GameMenu(game, chapter.getId(),
                        level.getNumber(), level));

        String message = "game started: "
                + chapter.getDisplayName()
                + " level " + level.getNumber()
                + " - " + level.getName()
                + " [" + level.getKind() + "]"
                + System.lineSeparator()
                + "entered game menu";
        CommandResult result = CommandResult.success(message)
                .addPostCommandResults(game.drainResults());
        if (preface != null && !preface.isBlank()) {
            result.addPreCommandResult(preface);
        }
        return withNotifications(result);
    }

    private static String formatLevels(
            Chapter chapter,
            AdventureProgress progress) {
        StringBuilder output =
                new StringBuilder(
                        chapter.getDisplayName()
                                + " levels");
        for (Level level : chapter.getLevels()) {
            output.append(System.lineSeparator())
                    .append(level.getNumber())
                    .append(". ")
                    .append(level.getName())
                    .append(" [")
                    .append(level.getKind())
                    .append("] | ");

            if (progress.isLevelCompleted(
                    chapter, level.getNumber())) {
                output.append("completed");
            } else if (progress.isLevelUnlocked(
                    chapter, level.getNumber())) {
                output.append("unlocked");
            } else {
                output.append("locked");
            }
        }
        return output.toString();
    }

    private static CommandResult withNotifications(
            CommandResult result) {
        List<String> notifications =
                AdventureSession.getInstance()
                        .drainNotifications();
        return result.addPreCommandResults(
                notifications);
    }

    public static CommandResult handleLogout(
            Matcher matcher) {
        AdventureSession.getInstance().reset();
        UserManager.saveAllUsers();
        App.getInstance().logout();
        return CommandResult.success(
                "logged out successfully"
                        + System.lineSeparator()
                        + "entered signup menu");
    }
}
