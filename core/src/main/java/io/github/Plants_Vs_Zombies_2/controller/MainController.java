package io.github.Plants_Vs_Zombies_2.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.game.Game;
import io.github.Plants_Vs_Zombies_2.model.game.plantSelector.PlantSelection;
import io.github.Plants_Vs_Zombies_2.model.game.scored.DailyScoredGameFactory;
import io.github.Plants_Vs_Zombies_2.model.game.scored.ScoredGame;
import io.github.Plants_Vs_Zombies_2.model.game.special.TimedWarObjective;
import io.github.Plants_Vs_Zombies_2.model.menu.GameMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.LeaderboardMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.MainMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.Menu;
import io.github.Plants_Vs_Zombies_2.model.menu.PlantSelectionMenu;
import io.github.Plants_Vs_Zombies_2.model.roadmap.AdventureProgress;
import io.github.Plants_Vs_Zombies_2.model.roadmap.AdventureSession;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Chapter;
import io.github.Plants_Vs_Zombies_2.model.roadmap.ChapterCatalog;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Level;
import io.github.Plants_Vs_Zombies_2.model.roadmap.SpecialLevelType;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/**
 * Main-menu adventure navigation and level launching.
 */
public final class MainController {
    private MainController() {
    }

    public static CommandResult handleOpenLeaderboard(Matcher matcher) {
        Menu currentMenu = App.getInstance().getCurrentMenu();
        if (!(currentMenu instanceof MainMenu)) {
            return CommandResult.error("main menu is not active!");
        }
        App.getInstance().changeMenu(new LeaderboardMenu(currentMenu));
        return CommandResult.success("entered leaderboard menu");
    }

    public static CommandResult handleStartTimedWarChallenge(
            Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error(
                    "login is required to start Timed War!");
        }

        TimedWarObjective objective = parseTimedWarObjective(
                matcher.group("objective"));
        Chapter frostbite = ChapterCatalog.findById("frostbite-caves");
        Level template = frostbite == null
                ? null
                : frostbite.getLevel(3);
        if (template == null) {
            return CommandResult.error(
                    "Timed War template is unavailable!");
        }

        Level challenge = template.withTimedWarObjective(objective);
        Map<String, Integer> loadout = createTimedWarChallengeLoadout(
                user, objective);
        if (objective == TimedWarObjective.PRODUCE_SUN
                && !loadout.containsKey("Sunflower")) {
            return CommandResult.error(
                    "Sun Production Timed War requires "
                            + "an unlocked Sunflower!");
        }

        int difficultyLevel = user.getSettings().getDifficultyLevel();
        Game game = challenge.createGame(difficultyLevel);
        game.configurePlantLoadout(loadout, List.of());
        user.getGameProgerss().recordGameStarted();
        UserManager.saveAllUsers();
        App.getInstance().changeMenu(new GameMenu(game));

        String message = "Timed War challenge started"
                + System.lineSeparator()
                + "objective: " + objective
                + System.lineSeparator()
                + "target: "
                + challenge.getSpecialConfig().getTarget()
                + System.lineSeparator()
                + "time: "
                + challenge.getSpecialConfig()
                        .getDurationSeconds()
                + " seconds"
                + System.lineSeparator()
                + "selected plants: "
                + String.join(", ", loadout.keySet())
                + System.lineSeparator()
                + "use 'show special level status'";
        return CommandResult.success(message)
                .addPostCommandResults(game.drainResults());
    }

    public static CommandResult handleStartScoredGame(
            Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error(
                    "login is required to start Scored Game!");
        }

        ScoredGame game = DailyScoredGameFactory.create();
        Map<String, Integer> loadout = createScoredGameLoadout(user);
        game.configurePlantLoadout(
                loadout, List.of());
        user.getGameProgerss().recordGameStarted();
        UserManager.saveAllUsers();
        App.getInstance().changeMenu(new GameMenu(game));

        String message = "daily Scored Game started"
                + System.lineSeparator()
                + "challenge date: " + game.getChallengeDate()
                + " UTC"
                + System.lineSeparator()
                + "selected plants: "
                + String.join(", ", loadout.keySet())
                + System.lineSeparator()
                + "use 'show score' and "
                + "'show score rules'";
        return CommandResult.success(message)
                .addPostCommandResults(game.drainResults());
    }

    public static CommandResult handleShowScoredGameRules(
            Matcher matcher) {
        return CommandResult.success(
                ScoredGame.getRulesDescription());
    }

    public static CommandResult handleShowChapters(
            Matcher matcher) {
        AdventureSession session = AdventureSession.getInstance();
        AdventureProgress progress = session.getProgress();
        StringBuilder output = new StringBuilder("chapters");

        for (Chapter chapter : ChapterCatalog.getChapters()) {
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
            if (chapter == session.getSelectedChapter()) {
                output.append(" | selected");
            }
        }

        return withNotifications(
                CommandResult.success(
                        output.toString()));
    }

    public static CommandResult handleShowLevels(
            Matcher matcher) {
        String requested = matcher.group("chapter");
        Chapter chapter = requested == null
                ? AdventureSession.getInstance()
                        .getSelectedChapter()
                : ChapterCatalog.findChapter(requested);

        if (chapter == null) {
            return withNotifications(
                    CommandResult.error(
                            "chapter does not exist!"));
        }

        AdventureProgress progress = AdventureSession.getInstance()
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
        Chapter chapter = ChapterCatalog.findChapter(
                matcher.group("chapter"));
        if (chapter == null) {
            return withNotifications(
                    CommandResult.error(
                            "chapter does not exist!"));
        }

        AdventureSession session = AdventureSession.getInstance();
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

        AdventureSession session = AdventureSession.getInstance();
        Chapter chapter = session.getSelectedChapter();
        Level level = chapter.getLevel(levelNumber);

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

        TimedWarObjective objective = parseTimedWarObjective(
                matcher.group("objective"));
        if (objective != null) {
            if (level.getSpecialLevelType() != SpecialLevelType.TIMED_WAR) {
                return withNotifications(
                        CommandResult.error(
                                "the -o objective option is only "
                                        + "available for Timed War levels!"));
            }
            level = level.withTimedWarObjective(objective);
        }

        return startAdventureLevel(
                chapter, level);
    }

    public static CommandResult handleShowCurrentLevel(
            Matcher matcher) {
        AdventureSession session = AdventureSession.getInstance();
        Chapter chapter = session.getSelectedChapter();
        int recommended = session.getProgress()
                .getRecommendedLevel(chapter);
        Level level = chapter.getLevel(recommended);

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
        AdventureSession session = AdventureSession.getInstance();
        Chapter chapter = session.getSelectedChapter();
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

        if (level.getSpecialLevelType() == SpecialLevelType.CONVEYOR_BELT) {
            return launchAdventureGame(chapter, level,
                    Map.of(), List.of(),
                    "Conveyor Belt levels skip plant selection.");
        }
        if (level.getSpecialLevelType() == SpecialLevelType.LOCKED_PLANTS) {
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

    private static TimedWarObjective parseTimedWarObjective(String rawObjective) {
        if (rawObjective == null
                || rawObjective.isBlank()) {
            return null;
        }
        String normalized = rawObjective.trim()
                .toLowerCase()
                .replace('-', '_');
        if ("kill".equals(normalized)) {
            return TimedWarObjective.KILL_ZOMBIES;
        }
        return TimedWarObjective.PRODUCE_SUN;
    }

    private static Map<String, Integer> createTimedWarChallengeLoadout(
            User user,
            TimedWarObjective objective) {
        Map<String, Integer> loadout = new LinkedHashMap<>();

        if (objective == TimedWarObjective.PRODUCE_SUN) {
            PlantCollectionItem sunflower = user.getPlantCollection()
                    .findPlant("Sunflower");
            if (sunflower != null
                    && sunflower.isUnlocked()) {
                loadout.put(
                        sunflower.getName(),
                        sunflower.getCurrentLevel());
            }
        }

        for (PlantCollectionItem plant : user.getPlantCollection()
                .getUnlockedPlants()) {
            loadout.putIfAbsent(
                    plant.getName(),
                    plant.getCurrentLevel());
            if (loadout.size() >= 8) {
                break;
            }
        }
        return loadout;
    }

    private static Map<String, Integer> createScoredGameLoadout(User user) {
        Map<String, Integer> loadout = new LinkedHashMap<>();
        for (PlantCollectionItem plant : user.getPlantCollection().getUnlockedPlants()) {
            loadout.put(
                    plant.getName(), plant.getCurrentLevel());
            if (loadout.size() >= 8) {
                break;
            }
        }
        if (loadout.isEmpty()) {
            throw new IllegalStateException(
                    "Scored Game requires an unlocked plant");
        }
        return loadout;
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

    /**
     * Starts an adventure level from the graphical Phase-2 flow while reusing
     * the exact Phase-1 game construction, special-level setup, boosts and
     * starting-item transfer logic.
     */
    public static CommandResult launchAdventureGameFromGui(
            Chapter chapter, Level level, PlantSelection selection) {
        if (chapter == null || level == null) {
            return CommandResult.error("chapter and level are required!");
        }
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error("login is required to start a level!");
        }

        if (level.getSpecialLevelType() == SpecialLevelType.CONVEYOR_BELT) {
            return launchAdventureGame(chapter, level,
                    Map.of(), List.of(),
                    "Conveyor Belt levels skip plant selection.", true);
        }
        if (level.getSpecialLevelType() == SpecialLevelType.LOCKED_PLANTS) {
            return launchAdventureGame(chapter, level,
                    createForcedLoadout(user, level), List.of(),
                    "Locked Plants uses its fixed plant loadout.", true);
        }
        if (selection == null || selection.getSelectedPlants().isEmpty()) {
            return CommandResult.error(
                    "select at least one plant before starting the game!");
        }
        return launchAdventureGame(chapter, level,
                selection.getSelectedPlantLevels(),
                selection.getBoostedPlantNames(), null, true);
    }

    static CommandResult launchAdventureGame(
            Chapter chapter, Level level,
            Map<String, Integer> selectedPlantLevels,
            List<String> boostedPlantNames,
            String preface) {
        return launchAdventureGame(chapter, level, selectedPlantLevels,
                boostedPlantNames, preface, true);
    }

    private static CommandResult launchAdventureGame(
            Chapter chapter, Level level,
            Map<String, Integer> selectedPlantLevels,
            List<String> boostedPlantNames,
            String preface, boolean startWavesImmediately) {
        User user = App.getInstance().getLoggedInUser();
        int difficultyLevel = user == null
                ? 3
                : user.getSettings().getDifficultyLevel();
        Game game = level.createGame(
                difficultyLevel, startWavesImmediately);
        configureUnlockedConveyorPool(user, level, game);
        Set<String> paidBoosts = new LinkedHashSet<>();
        if (boostedPlantNames != null) {
            paidBoosts.addAll(boostedPlantNames);
        }
        List<String> armedGreenhouseBoosts = new ArrayList<>();
        if (user != null && selectedPlantLevels != null) {
            addGreenhouseBoosts(user, selectedPlantLevels.keySet(),
                    paidBoosts, armedGreenhouseBoosts);
        }
        if (selectedPlantLevels != null
                && !selectedPlantLevels.isEmpty()) {
            game.configurePlantLoadout(selectedPlantLevels,
                    new ArrayList<>(paidBoosts),
                    armedGreenhouseBoosts);
        }

        int startingPlantFood = transferStartingPlantFood(user, game);
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
        if (!armedGreenhouseBoosts.isEmpty()) {
            result.addPreCommandResult(
                    "Greenhouse boost ready for this level: "
                            + String.join(", ", armedGreenhouseBoosts)
                            + ". It will be consumed when that plant is first used.");
        }
        if (startingPlantFood > 0) {
            result.addPreCommandResult("Transferred "
                    + startingPlantFood
                    + " shop-purchased plant food to this level.");
        }
        return withNotifications(result);
    }

    private static void configureUnlockedConveyorPool(
            User user, Level level, Game game) {
        if (user == null || game == null
                || level.getSpecialLevelType() != SpecialLevelType.CONVEYOR_BELT) {
            return;
        }

        List<String> unlockedPool = new ArrayList<>();
        for (String plantName : level.getSpecialConfig().getPlantPool()) {
            PlantCollectionItem item = user.getPlantCollection().findPlant(plantName);
            if (item != null && item.isUnlocked()) {
                unlockedPool.add(item.getName());
            }
        }
        if (unlockedPool.isEmpty()) {
            throw new IllegalStateException(
                    "Conveyor Belt requires at least one unlocked plant");
        }
        game.replaceConveyorPlantPool(unlockedPool);
    }

    private static void addGreenhouseBoosts(User user,
            Set<String> selectedPlantNames,
            Set<String> paidBoosts,
            List<String> greenhouseBoosts) {
        for (String plantName : selectedPlantNames) {
            if (containsPlantName(paidBoosts, plantName)
                    || !user.hasPlantBoost(plantName)) {
                continue;
            }
            greenhouseBoosts.add(plantName);
        }
    }

    private static int transferStartingPlantFood(
            User user, Game game) {
        if (user == null || user.getPlantFoodCount() <= 0) {
            return 0;
        }
        int transferred = Math.min(
                game.getMaximumPlantFoodCount(),
                user.getPlantFoodCount());
        game.loadStartingPlantFood(transferred);
        user.setPlantFoodCount(
                user.getPlantFoodCount() - transferred);
        return transferred;
    }

    private static boolean containsPlantName(
            Set<String> plantNames, String requestedName) {
        String normalizedRequested = normalizePlantName(requestedName);
        for (String plantName : plantNames) {
            if (normalizePlantName(plantName)
                    .equals(normalizedRequested)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizePlantName(String plantName) {
        if (plantName == null) {
            return "";
        }
        return plantName.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private static String formatLevels(
            Chapter chapter,
            AdventureProgress progress) {
        StringBuilder output = new StringBuilder(
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
        List<String> notifications = AdventureSession.getInstance()
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
