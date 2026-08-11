package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.game.minigame.IZombie;
import model.game.minigame.IZombieCard;
import model.game.minigame.IZombieLevel;
import model.game.minigame.VaseBreaker;
import model.game.minigame.VaseBreakerLevel;
import model.game.minigame.WallnutBowling;
import model.game.minigame.WallnutBowlingLevel;
import model.menu.GameMenu;
import model.quest.Quest;
import model.quest.QuestType;
import model.user.GameProgerss;
import model.user.User;

public final class TravelLogMenuController {
    private static final int QUESTS_PER_PAGE = 5;
    private static final String VASE_BREAKER_ID = "vasebreaker";
    private static final String VASE_BREAKER_NAME = "Vase Breaker";
    private static final String WALLNUT_BOWLING_ID = "wallnutbowling";
    private static final String WALLNUT_BOWLING_NAME = "Wall-nut Bowling";
    private static final String I_ZOMBIE_ID = "izombie";
    private static final String I_ZOMBIE_NAME = "I, Zombie";

    private TravelLogMenuController() {
    }

    public static CommandResult handlePage(Matcher matcher) {
        String pageName = normalizePageName(matcher.group("page"));
        if ("minigame".equals(pageName)
                || "minigames".equals(pageName)) {
            return handleMinigamesPage(matcher);
        }

        QuestType questType = parseQuestType(pageName);
        if (questType == null) {
            return CommandResult.error(
                    "Invalid Travel Log page. Available pages: "
                            + "main, epic, daily, minigame.");
        }

        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        List<Quest> quests = questsOfType(user, questType);
        CommandResult result = CommandResult.success(
                "--- Travel Log: " + formatPageName(questType) + " ---");
        appendQuestList(result, quests, questType);
        return result;
    }

    public static CommandResult handleNumberedPage(Matcher matcher) {
        int pageNumber;
        try {
            pageNumber = Integer.parseInt(matcher.group("pageNumber"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("Invalid page number.");
        }
        User currentUser = getLoggedInUser();
        if (currentUser == null) {
            return loginRequired();
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

    private static List<Quest> questsOfType(User user, QuestType type) {
        List<Quest> quests = new ArrayList<>();
        for (Quest quest : user.getQuestProgress().getActiveQuests()) {
            if (quest.getType() == type) {
                quests.add(quest);
            }
        }
        Collections.sort(quests);
        return quests;
    }

    private static void appendQuestList(CommandResult result,
            List<Quest> quests, QuestType type) {
        for (Quest quest : quests) {
            result.addPostCommandResult(formatQuest(quest));
        }
        if (quests.isEmpty()) {
            result.addPostCommandResult("You have no active "
                    + formatPageName(type).toLowerCase(Locale.ROOT)
                    + " quests.");
        }
    }

    private static String formatQuest(Quest quest) {
        return String.format("[%s] %s: %s (Type: %s)",
                quest.getPriority(), quest.getName(),
                quest.getInstructions(), quest.getType());
    }

    private static QuestType parseQuestType(String pageName) {
        if ("main".equals(pageName)) {
            return QuestType.MAIN;
        }
        if ("epic".equals(pageName)) {
            return QuestType.EPIC;
        }
        if ("daily".equals(pageName)) {
            return QuestType.DAILY;
        }
        return null;
    }

    private static String normalizePageName(String pageName) {
        return pageName.trim().toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");
    }

    private static String formatPageName(QuestType type) {
        String lowerCase = type.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lowerCase.charAt(0))
                + lowerCase.substring(1);
    }

    private static void appendQuestPage(CommandResult result,
            List<Quest> activeQuests, int pageNumber) {
        int startIndex = (pageNumber - 1) * QUESTS_PER_PAGE;
        int endIndex = Math.min(startIndex + QUESTS_PER_PAGE,
                activeQuests.size());
        for (int index = startIndex; index < endIndex; index++) {
            Quest quest = activeQuests.get(index);
            result.addPostCommandResult(formatQuest(quest));
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
        addInitialMinigameNews(user);
        GameProgerss progress = user.getGameProgerss();
        StringBuilder output = new StringBuilder(
                "--- Travel Log: Minigames ---");
        appendMinigameSummary(output, progress,
                VASE_BREAKER_ID, VASE_BREAKER_NAME,
                VaseBreakerLevel.LEVEL_COUNT,
                "show vasebreaker levels",
                "start vasebreaker -l <1-3>");
        appendMinigameSummary(output, progress,
                WALLNUT_BOWLING_ID, WALLNUT_BOWLING_NAME,
                WallnutBowlingLevel.LEVEL_COUNT,
                "show wallnut bowling levels",
                "start wallnut bowling -l <1-3>");
        appendMinigameSummary(output, progress,
                I_ZOMBIE_ID, I_ZOMBIE_NAME,
                IZombieLevel.LEVEL_COUNT,
                "show i-zombie levels",
                "start i-zombie -l <1-3>");
        UserManager.saveAllUsers();
        return CommandResult.success(output.toString());
    }

    private static void appendMinigameSummary(StringBuilder output,
            GameProgerss progress, String id, String name,
            int maximumLevel, String detailsCommand,
            String startCommand) {
        int unlocked = progress.getHighestUnlockedMinigameLevel(
                id, maximumLevel);
        int completed = completedLevels(progress, id, maximumLevel);
        output.append(System.lineSeparator())
                .append(name)
                .append(" | ")
                .append(maximumLevel)
                .append(" progressively harder levels")
                .append(" | highest unlocked: ")
                .append(unlocked)
                .append(" | completed: ")
                .append(completed).append('/').append(maximumLevel)
                .append(System.lineSeparator())
                .append("  details: '").append(detailsCommand).append("'")
                .append(System.lineSeparator())
                .append("  play: '").append(startCommand).append("'");
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
                    .append(levelStatus(progress, VASE_BREAKER_ID,
                            level.getNumber(),
                            VaseBreakerLevel.LEVEL_COUNT))
                    .append(" | vases: ")
                    .append(level.getTotalVaseCount())
                    .append(" (plant: ")
                    .append(level.getPlantVases())
                    .append(", giant: ")
                    .append(level.getGiantVases())
                    .append(") | seed lifetime: ")
                    .append(String.format(Locale.ROOT,
                            "%.1fs",
                            level.getSeedPacketLifeSpanSeconds()));
        }
        return CommandResult.success(output.toString());
    }

    public static CommandResult handleShowWallnutBowlingLevels(
            Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        GameProgerss progress = user.getGameProgerss();
        StringBuilder output = new StringBuilder(
                WALLNUT_BOWLING_NAME + " levels");
        for (WallnutBowlingLevel level : WallnutBowlingLevel.values()) {
            output.append(System.lineSeparator())
                    .append(level.getNumber()).append(". ")
                    .append(level.getName()).append(" | ")
                    .append(levelStatus(progress, WALLNUT_BOWLING_ID,
                            level.getNumber(),
                            WallnutBowlingLevel.LEVEL_COUNT))
                    .append(" | waves: ")
                    .append(level.getWaveCount())
                    .append(" | zombies: ")
                    .append(level.getZombieCount())
                    .append(" | red line after column ")
                    .append(level.getRedLineColumn());
        }
        return CommandResult.success(output.toString());
    }

    public static CommandResult handleShowIZombieLevels(
            Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        GameProgerss progress = user.getGameProgerss();
        StringBuilder output = new StringBuilder(
                I_ZOMBIE_NAME + " levels");
        for (IZombieLevel level : IZombieLevel.values()) {
            output.append(System.lineSeparator())
                    .append(level.getNumber()).append(". ")
                    .append(level.getName()).append(" | ")
                    .append(levelStatus(progress, I_ZOMBIE_ID,
                            level.getNumber(), IZombieLevel.LEVEL_COUNT))
                    .append(" | plants: ")
                    .append(level.getPlantCount())
                    .append(" | red line after column ")
                    .append(level.getRedLineColumn())
                    .append(" | zombie cards: ");
            appendIZombieCards(output, level);
        }
        return CommandResult.success(output.toString());
    }

    private static void appendIZombieCards(
            StringBuilder output, IZombieLevel level) {
        for (int index = 0;
                index < level.getZombieCards().size(); index++) {
            IZombieCard card = level.getZombieCards().get(index);
            output.append(card.getType().name())
                    .append('(').append(card.getCost()).append(')');
            if (index < level.getZombieCards().size() - 1) {
                output.append(", ");
            }
        }
    }

    public static CommandResult handleStartVaseBreaker(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        user.addMinigameUnlockNews(VASE_BREAKER_NAME + " level 1");
        int levelNumber = parseLevelNumber(matcher);
        VaseBreakerLevel level = VaseBreakerLevel.find(levelNumber);
        if (level == null) {
            return CommandResult.error(
                    "Vase Breaker has exactly three levels: 1, 2, and 3.");
        }
        if (!isLevelUnlocked(user, VASE_BREAKER_ID, levelNumber,
                VaseBreakerLevel.LEVEL_COUNT)) {
            return lockedLevel(VASE_BREAKER_NAME, levelNumber);
        }

        VaseBreaker game = new VaseBreaker(level);
        startMinigame(user, game, VASE_BREAKER_ID, VASE_BREAKER_NAME,
                levelNumber, VaseBreakerLevel.LEVEL_COUNT);
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

    public static CommandResult handleStartWallnutBowling(
            Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        user.addMinigameUnlockNews(WALLNUT_BOWLING_NAME + " level 1");
        int levelNumber = parseLevelNumber(matcher);
        WallnutBowlingLevel level = WallnutBowlingLevel.find(levelNumber);
        if (level == null) {
            return CommandResult.error(WALLNUT_BOWLING_NAME
                    + " has exactly three levels: 1, 2, and 3.");
        }
        if (!isLevelUnlocked(user, WALLNUT_BOWLING_ID, levelNumber,
                WallnutBowlingLevel.LEVEL_COUNT)) {
            return lockedLevel(WALLNUT_BOWLING_NAME, levelNumber);
        }

        WallnutBowling game = new WallnutBowling(level);
        startMinigame(user, game, WALLNUT_BOWLING_ID,
                WALLNUT_BOWLING_NAME, levelNumber,
                WallnutBowlingLevel.LEVEL_COUNT);
        String instructions = "started " + WALLNUT_BOWLING_NAME
                + " level " + levelNumber + " - " + level.getName()
                + System.lineSeparator()
                + "the red line allows launches only in columns 0 through "
                + level.getRedLineColumn()
                + System.lineSeparator()
                + "commands: show conveyor belt | plant from-conveyor "
                + "-i <index> -l (<row>, <column>)"
                + System.lineSeparator()
                + "show bowling wallnuts | advance time -t <count> ticks"
                + " | show map";
        return CommandResult.success(instructions)
                .addPostCommandResults(game.drainResults());
    }

    public static CommandResult handleStartIZombie(
            Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        user.addMinigameUnlockNews(I_ZOMBIE_NAME + " level 1");
        int levelNumber = parseLevelNumber(matcher);
        IZombieLevel level = IZombieLevel.find(levelNumber);
        if (level == null) {
            return CommandResult.error(
                    "I, Zombie has exactly three levels: 1, 2, and 3.");
        }
        if (!isLevelUnlocked(user, I_ZOMBIE_ID, levelNumber,
                IZombieLevel.LEVEL_COUNT)) {
            return lockedLevel(I_ZOMBIE_NAME, levelNumber);
        }

        IZombie game = new IZombie(level);
        startMinigame(user, game, I_ZOMBIE_ID, I_ZOMBIE_NAME,
                levelNumber, IZombieLevel.LEVEL_COUNT);
        String instructions = "started I, Zombie level " + levelNumber
                + " - " + level.getName()
                + System.lineSeparator()
                + "initial sun: 150 | plants are left of the red line"
                + System.lineSeparator()
                + "commands: show i-zombie status | show zombie cards"
                + System.lineSeparator()
                + "place zombie -t <type> -l (<row>, <column>)"
                + System.lineSeparator()
                + "advance time -t <count> ticks | show map";
        return CommandResult.success(instructions)
                .addPostCommandResults(game.drainResults());
    }

    private static int parseLevelNumber(Matcher matcher) {
        try {
            return Integer.parseInt(matcher.group("level"));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static boolean isLevelUnlocked(User user, String minigameId,
            int levelNumber, int maximumLevel) {
        return user.getGameProgerss().isMinigameLevelUnlocked(
                minigameId, levelNumber, maximumLevel);
    }

    private static CommandResult lockedLevel(String name, int levelNumber) {
        return CommandResult.error(name + " level " + levelNumber
                + " is locked; complete level "
                + (levelNumber - 1) + " first.");
    }

    private static void startMinigame(User user, model.game.Game game,
            String id, String name, int levelNumber, int maximumLevel) {
        user.getGameProgerss().recordGameStarted();
        App.getInstance().changeMenu(GameMenu.forMinigame(
                game, id, name, levelNumber, maximumLevel));
        UserManager.saveAllUsers();
    }

    private static String levelStatus(GameProgerss progress,
            String minigameId, int levelNumber, int maximumLevel) {
        if (progress.isMinigameLevelCompleted(minigameId, levelNumber)) {
            return "completed";
        }
        if (progress.isMinigameLevelUnlocked(minigameId,
                levelNumber, maximumLevel)) {
            return "unlocked";
        }
        return "locked";
    }

    private static int completedLevels(GameProgerss progress,
            String minigameId, int maximumLevel) {
        int completed = 0;
        for (int level = 1; level <= maximumLevel; level++) {
            if (progress.isMinigameLevelCompleted(minigameId, level)) {
                completed++;
            }
        }
        return completed;
    }

    private static void addInitialMinigameNews(User user) {
        user.addMinigameUnlockNews(VASE_BREAKER_NAME + " level 1");
        user.addMinigameUnlockNews(WALLNUT_BOWLING_NAME + " level 1");
        user.addMinigameUnlockNews(I_ZOMBIE_NAME + " level 1");
    }

    private static User getLoggedInUser() {
        return App.getInstance().getLoggedInUser();
    }

    private static CommandResult loginRequired() {
        return CommandResult.error(
                "You must be logged in to view the Travel Log.");
    }
}
