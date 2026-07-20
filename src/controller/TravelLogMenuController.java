package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.game.minigame.VaseBreaker;
import model.game.minigame.VaseBreakerLevel;
import model.menu.GameMenu;
import model.quest.Quest;
import model.user.GameProgerss;
import model.user.User;

public final class TravelLogMenuController {
    private static final int QUESTS_PER_PAGE = 5;
    private static final String VASE_BREAKER_ID = "vasebreaker";
    private static final String VASE_BREAKER_NAME = "Vase Breaker";

    private TravelLogMenuController() {
    }

    public static CommandResult handlePage(Matcher matcher) {
        int pageNumber = Integer.parseInt(matcher.group("page"));
        User currentUser = App.getInstance().getLoggedInUser();
        if (currentUser == null) {
            return CommandResult.error(
                    "You must be logged in to view the Travel Log.");
        }

        List<Quest> activeQuests = new ArrayList<>(
                currentUser.getQuestProgress().getActiveQuests());
        Collections.sort(activeQuests);
        int totalPages = (int) Math.ceil(
                (double) activeQuests.size() / QUESTS_PER_PAGE);
        if (pageNumber < 1
                || pageNumber > totalPages && totalPages != 0) {
            return CommandResult.error("Invalid page number.");
        }

        CommandResult result = CommandResult.success(
                "--- Travel Log (Page " + pageNumber + " of "
                        + Math.max(1, totalPages) + ") ---");
        appendQuestPage(result, activeQuests, pageNumber);
        return result;
    }

    private static void appendQuestPage(CommandResult result,
            List<Quest> activeQuests, int pageNumber) {
        int startIndex = (pageNumber - 1) * QUESTS_PER_PAGE;
        int endIndex = Math.min(startIndex + QUESTS_PER_PAGE,
                activeQuests.size());
        for (int index = startIndex; index < endIndex; index++) {
            Quest quest = activeQuests.get(index);
            result.addPostCommandResult(String.format(
                    "[%s] %s: %s (Type: %s)",
                    quest.getPriority(), quest.getName(),
                    quest.getInstructions(), quest.getType()));
        }
        if (activeQuests.isEmpty()) {
            result.addPostCommandResult("You have no active quests.");
        }
    }

    public static CommandResult handleMinigamesPage(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        user.addMinigameUnlockNews(VASE_BREAKER_NAME + " level 1");
        GameProgerss progress = user.getGameProgerss();
        int unlocked = progress.getHighestUnlockedMinigameLevel(
                VASE_BREAKER_ID, VaseBreakerLevel.LEVEL_COUNT);
        int completed = completedVaseBreakerLevels(progress);

        String output = "--- Travel Log: Minigames ---"
                + System.lineSeparator()
                + VASE_BREAKER_NAME + " | 3 progressively harder levels"
                + " | highest unlocked: " + unlocked
                + " | completed: " + completed + "/3"
                + System.lineSeparator()
                + "use 'show vasebreaker levels' for details"
                + System.lineSeparator()
                + "use 'start vasebreaker -l <1-3>' to play";
        return CommandResult.success(output);
    }

    public static CommandResult handleShowVaseBreakerLevels(
            Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        GameProgerss progress = user.getGameProgerss();
        StringBuilder output = new StringBuilder(
                VASE_BREAKER_NAME + " levels");
        for (VaseBreakerLevel level : VaseBreakerLevel.values()) {
            output.append(System.lineSeparator())
                    .append(level.getNumber()).append(". ")
                    .append(level.getName()).append(" | ")
                    .append(levelStatus(progress, level.getNumber()))
                    .append(" | vases: ")
                    .append(level.getTotalVaseCount())
                    .append(" (plant: ")
                    .append(level.getPlantVases())
                    .append(", giant: ")
                    .append(level.getGiantVases())
                    .append(") | seed lifetime: ")
                    .append(String.format(java.util.Locale.ROOT,
                            "%.1fs", level.getSeedPacketLifeSpanSeconds()));
        }
        return CommandResult.success(output.toString());
    }

    public static CommandResult handleStartVaseBreaker(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        user.addMinigameUnlockNews(VASE_BREAKER_NAME + " level 1");
        int levelNumber;
        try {
            levelNumber = Integer.parseInt(matcher.group("level"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("Vase Breaker level is invalid!");
        }
        VaseBreakerLevel level = VaseBreakerLevel.find(levelNumber);
        if (level == null) {
            return CommandResult.error(
                    "Vase Breaker has exactly three levels: 1, 2, and 3.");
        }
        if (!user.getGameProgerss().isMinigameLevelUnlocked(
                VASE_BREAKER_ID, levelNumber,
                VaseBreakerLevel.LEVEL_COUNT)) {
            return CommandResult.error("Vase Breaker level "
                    + levelNumber + " is locked; complete level "
                    + (levelNumber - 1) + " first.");
        }

        VaseBreaker game = new VaseBreaker(level);
        user.getGameProgerss().recordGameStarted();
        App.getInstance().changeMenu(GameMenu.forMinigame(
                game, VASE_BREAKER_ID, VASE_BREAKER_NAME,
                levelNumber, VaseBreakerLevel.LEVEL_COUNT));
        UserManager.saveAllUsers();

        String instructions = "started " + VASE_BREAKER_NAME
                + " level " + levelNumber + " - " + level.getName()
                + System.lineSeparator()
                + "commands: show vases | break vase -l (<row>, <column>)"
                + System.lineSeparator()
                + "show vase seeds | plant vase-seed -s (<seed row>, "
                + "<seed column>) -l (<target row>, <target column>)"
                + System.lineSeparator()
                + "advance time -t <count> ticks | show map";
        return CommandResult.success(instructions)
                .addPostCommandResults(game.drainResults());
    }

    private static String levelStatus(GameProgerss progress,
            int levelNumber) {
        if (progress.isMinigameLevelCompleted(
                VASE_BREAKER_ID, levelNumber)) {
            return "completed";
        }
        if (progress.isMinigameLevelUnlocked(VASE_BREAKER_ID,
                levelNumber, VaseBreakerLevel.LEVEL_COUNT)) {
            return "unlocked";
        }
        return "locked";
    }

    private static int completedVaseBreakerLevels(GameProgerss progress) {
        int completed = 0;
        for (int level = 1;
                level <= VaseBreakerLevel.LEVEL_COUNT; level++) {
            if (progress.isMinigameLevelCompleted(
                    VASE_BREAKER_ID, level)) {
                completed++;
            }
        }
        return completed;
    }

    private static User getLoggedInUser() {
        return App.getInstance().getLoggedInUser();
    }

    private static CommandResult loginRequired() {
        return CommandResult.error(
                "You must be logged in to view the Travel Log.");
    }
}
