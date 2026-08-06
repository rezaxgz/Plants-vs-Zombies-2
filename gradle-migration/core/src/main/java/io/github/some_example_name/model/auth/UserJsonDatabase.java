package io.github.some_example_name.model.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import io.github.some_example_name.model.Settings;
import io.github.some_example_name.model.collections.plants.PlantCollection;
import io.github.some_example_name.model.collections.plants.PlantCollectionItem;
import io.github.some_example_name.model.collections.zombies.ZombieCollection;
import io.github.some_example_name.model.collections.zombies.ZombieCollectionItem;
import io.github.some_example_name.model.enums.Gender;
import io.github.some_example_name.model.greenHouse.GreenHouse;
import io.github.some_example_name.model.greenHouse.PlantedPlant;
import io.github.some_example_name.model.greenHouse.Pot;
import io.github.some_example_name.model.roadmap.AdventureProgress;
import io.github.some_example_name.model.security.SecurityQuestion;
import io.github.some_example_name.model.user.GameProgerss;
import io.github.some_example_name.model.user.User;

final class UserJsonDatabase {
    private static final int CURRENT_VERSION = 1;

    private UserJsonDatabase() {
    }

    static List<User> load(Path databasePath) {
        Path absolutePath = databasePath.toAbsolutePath().normalize();
        if (!Files.exists(absolutePath))
            return new ArrayList<>();

        try {
            String json = Files.readString(absolutePath, StandardCharsets.UTF_8);
            if (json.isBlank())
                return new ArrayList<>();

            Object parsed = new JsonParser(json).parse();
            Map<String, Object> root = requireObject(parsed, "database root");
            int version = getInt(root, "version", CURRENT_VERSION);
            if (version != CURRENT_VERSION) {
                throw new IllegalArgumentException("unsupported user database version: " + version);
            }

            Object usersValue = root.get("users");
            if (usersValue == null)
                return new ArrayList<>();

            List<Object> storedUsers = requireArray(usersValue, "users");
            List<User> users = new ArrayList<>();
            for (int i = 0; i < storedUsers.size(); i++) {
                Map<String, Object> storedUser = requireObject(storedUsers.get(i), "users[" + i + "]");
                users.add(readUser(storedUser, i));
            }
            return users;
        } catch (IOException e) {
            throw new IllegalStateException("could not read user database: " + absolutePath, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("invalid user database JSON at " + absolutePath + ": " + e.getMessage(), e);
        }
    }

    static void save(Path databasePath, List<User> users) {
        Path absolutePath = databasePath.toAbsolutePath().normalize();
        Path parent = absolutePath.getParent();
        Path temporaryFile = null;

        try {
            if (parent != null) {
                Files.createDirectories(parent);
                temporaryFile = Files.createTempFile(parent, "users-", ".json.tmp");
            } else {
                temporaryFile = Files.createTempFile("users-", ".json.tmp");
            }

            Files.writeString(temporaryFile, writeUsers(users), StandardCharsets.UTF_8);
            moveIntoPlace(temporaryFile, absolutePath);
            temporaryFile = null;
        } catch (IOException e) {
            throw new IllegalStateException("could not save user database: " + absolutePath, e);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void moveIntoPlace(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @SuppressWarnings("unchecked")
    private static User readUser(Map<String, Object> storedUser, int index) {
        String prefix = "users[" + index + "]";
        String username = requireString(storedUser, "username", prefix);
        String passwordHash = requireString(storedUser, "passwordHash", prefix);
        String nickname = requireString(storedUser, "nickname", prefix);
        String email = requireString(storedUser, "email", prefix);
        String genderName = requireString(storedUser, "gender", prefix);

        Gender gender;
        try {
            gender = Gender.valueOf(genderName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(prefix + ".gender has an invalid value: " + genderName);
        }

        SecurityQuestion securityQuestion = readSecurityQuestion(storedUser.get("securityQuestion"), prefix);
        int coins = getInt(storedUser, "coins", 0);
        int diamonds = getInt(storedUser, "diamonds", 0);
        int greenhousePotsUnlocked = getInt(storedUser, "greenhousePotsUnlocked", 0);
        int potCount = getInt(storedUser, "potCount", 0);
        int plantFoodCount = getInt(storedUser, "plantFoodCount", 0);

        Object ghObj = storedUser.get("greenHouse");
        GreenHouse greenHouse = ghObj != null ? readGreenHouse((Map<String, Object>) ghObj, prefix + ".greenHouse")
                : new GreenHouse();

        Map<String, Integer> plantBoosts = new HashMap<>();
        Object boostsObj = storedUser.get("plantBoosts");
        if (boostsObj != null) {
            Map<String, Object> bMap = (Map<String, Object>) boostsObj;
            for (Map.Entry<String, Object> entry : bMap.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    plantBoosts.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                }
            }
        }

        PlantCollection plantCollection = readPlantCollection(storedUser.get("plantCollection"), prefix);
        ZombieCollection zombieCollection = readZombieCollection(storedUser.get("zombieCollection"), prefix);
        Settings settings = readSettings(storedUser.get("settings"), prefix);
        AdventureProgress adventureProgress = readAdventureProgress(
                storedUser.get("adventureProgress"), prefix);
        GameProgerss gameProgress = readGameProgress(
                storedUser.get("gameProgress"), prefix);

        User user = User.fromStoredData(username, passwordHash, nickname, email, gender, securityQuestion, coins,
                diamonds, greenhousePotsUnlocked, plantFoodCount, greenHouse, plantBoosts,
                plantCollection, zombieCollection, settings, adventureProgress, gameProgress);

        Object newsObj = storedUser.get("news");
        if (newsObj != null) {
            List<Object> newsArray = requireArray(newsObj, prefix + ".news");
            for (int i = 0; i < newsArray.size(); i++) {
                Map<String, Object> nMap = requireObject(newsArray.get(i), prefix + ".news[" + i + "]");
                long ts = getLong(nMap, "timestampMillis", System.currentTimeMillis());
                String t = requireString(nMap, "title", prefix + ".news[" + i + "]");
                String d = requireString(nMap, "description", prefix + ".news[" + i + "]");
                boolean r = getBoolean(nMap, "hasRead", false);
                user.getNewsPanel().addNews(new io.github.some_example_name.model.news.News(ts, t, d, r));
            }
        }

        // Safely extract and assign daily offer variables
        String dailyOfferDate = storedUser.containsKey("dailyOfferDate") ? (String) storedUser.get("dailyOfferDate")
                : "";
        String dailyOfferPlant = storedUser.containsKey("dailyOfferPlant") ? (String) storedUser.get("dailyOfferPlant")
                : "";
        boolean dailyOfferPurchased = storedUser.containsKey("dailyOfferPurchased")
                && getBoolean(storedUser, "dailyOfferPurchased", false);

        user.setDailyOfferDate(dailyOfferDate);
        user.setDailyOfferPlant(dailyOfferPlant);
        user.setDailyOfferPurchased(dailyOfferPurchased);
        user.setPotCount(potCount);

        return user;
    }

    private static PlantCollection readPlantCollection(Object value, String prefix) {
        PlantCollection collection = new PlantCollection();
        if (value == null) {
            return collection;
        }
        List<Object> storedPlants = requireArray(value, prefix + ".plantCollection");
        for (int i = 0; i < storedPlants.size(); i++) {
            Map<String, Object> plant = requireObject(
                    storedPlants.get(i), prefix + ".plantCollection[" + i + "]");
            String context = prefix + ".plantCollection[" + i + "]";
            String name = requireString(plant, "name", context);
            boolean unlocked = getBoolean(plant, "unlocked", false);
            int level = getInt(plant, "level", PlantCollectionItem.MIN_LEVEL);
            int cards = getInt(plant, "cards", 0);
            collection.restorePlantState(name, unlocked, level, cards);
        }
        return collection;
    }

    private static ZombieCollection readZombieCollection(Object value, String prefix) {
        ZombieCollection collection = new ZombieCollection();
        if (value == null) {
            return collection;
        }
        List<Object> storedZombies = requireArray(value, prefix + ".zombieCollection");
        for (int i = 0; i < storedZombies.size(); i++) {
            Object storedZombie = storedZombies.get(i);
            if (storedZombie instanceof String zombieName) {
                collection.restoreZombieState(zombieName, true);
                continue;
            }
            String context = prefix + ".zombieCollection[" + i + "]";
            Map<String, Object> zombie = requireObject(storedZombie, context);
            String name = requireString(zombie, "name", context);
            boolean unlocked = getBoolean(zombie, "unlocked", false);
            collection.restoreZombieState(name, unlocked);
        }
        return collection;
    }

    private static Settings readSettings(Object value, String prefix) {
        if (value == null) {
            return new Settings();
        }
        Map<String, Object> storedSettings = requireObject(
                value, prefix + ".settings");
        int difficulty = getInt(storedSettings, "difficultyLevel",
                Settings.DEFAULT_DIFFICULTY);
        try {
            return new Settings(difficulty);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(prefix
                    + ".settings.difficultyLevel must be between 1 and 5");
        }
    }

    private static AdventureProgress readAdventureProgress(
            Object value, String prefix) {
        if (value == null) {
            return new AdventureProgress();
        }
        Map<String, Object> storedProgress = requireObject(
                value, prefix + ".adventureProgress");
        Map<String, Integer> unlockedLevels = new HashMap<>();
        Object unlockedValue = storedProgress.get("highestUnlockedLevels");
        if (unlockedValue != null) {
            Map<String, Object> unlockedMap = requireObject(unlockedValue,
                    prefix + ".adventureProgress.highestUnlockedLevels");
            for (Map.Entry<String, Object> entry : unlockedMap.entrySet()) {
                if (!(entry.getValue() instanceof Number number)) {
                    throw new IllegalArgumentException(prefix
                            + ".adventureProgress.highestUnlockedLevels."
                            + entry.getKey() + " must be a number");
                }
                unlockedLevels.put(entry.getKey(), number.intValue());
            }
        }

        Set<String> completedLevels = new HashSet<>();
        Object completedValue = storedProgress.get("completedLevels");
        if (completedValue != null) {
            List<Object> completedArray = requireArray(completedValue,
                    prefix + ".adventureProgress.completedLevels");
            for (int i = 0; i < completedArray.size(); i++) {
                Object storedLevel = completedArray.get(i);
                if (!(storedLevel instanceof String levelKey)) {
                    throw new IllegalArgumentException(prefix
                            + ".adventureProgress.completedLevels[" + i
                            + "] must be a string");
                }
                completedLevels.add(levelKey);
            }
        }
        return AdventureProgress.fromStoredData(unlockedLevels, completedLevels);
    }

    private static GameProgerss readGameProgress(Object value, String prefix) {
        if (value == null) {
            return new GameProgerss();
        }
        Map<String, Object> storedProgress = requireObject(
                value, prefix + ".gameProgress");
        Map<String, Integer> unlockedMinigames = readMinigameUnlocks(storedProgress, prefix);
        List<String> completedMinigameLevels = readCompletedMinigameLevels(storedProgress, prefix);
        return GameProgerss.fromStoredData(
                getInt(storedProgress, "lastCompletedChapter", 0),
                getInt(storedProgress, "lastCompletedLevel", 0),
                getInt(storedProgress, "completedMinigames", 0),
                getInt(storedProgress, "highestScore", 0),
                getInt(storedProgress, "gamesPlayed", 0),
                unlockedMinigames, completedMinigameLevels);
    }

    private static Map<String, Integer> readMinigameUnlocks(
            Map<String, Object> storedProgress, String prefix) {
        Map<String, Integer> result = new LinkedHashMap<>();
        Object value = storedProgress.get("highestUnlockedMinigameLevels");
        if (value == null) {
            return result;
        }
        Map<String, Object> stored = requireObject(value,
                prefix + ".gameProgress.highestUnlockedMinigameLevels");
        for (Map.Entry<String, Object> entry : stored.entrySet()) {
            if (!(entry.getValue() instanceof Number number)) {
                throw new IllegalArgumentException(prefix
                        + ".gameProgress.highestUnlockedMinigameLevels."
                        + entry.getKey() + " must be a number");
            }
            result.put(entry.getKey(), number.intValue());
        }
        return result;
    }

    private static List<String> readCompletedMinigameLevels(
            Map<String, Object> storedProgress, String prefix) {
        List<String> result = new ArrayList<>();
        Object value = storedProgress.get("completedMinigameLevels");
        if (value == null) {
            return result;
        }
        List<Object> stored = requireArray(value,
                prefix + ".gameProgress.completedMinigameLevels");
        for (int index = 0; index < stored.size(); index++) {
            if (!(stored.get(index) instanceof String key)) {
                throw new IllegalArgumentException(prefix
                        + ".gameProgress.completedMinigameLevels["
                        + index + "] must be a string");
            }
            result.add(key);
        }
        return result;
    }

    private static GreenHouse readGreenHouse(Map<String, Object> map, String prefix) {
        GreenHouse gh = new GreenHouse();
        if (!map.containsKey("pots"))
            return gh;

        List<Object> potsList = requireArray(map.get("pots"), prefix + ".pots");
        int index = 0;
        Pot[][] pots = gh.getBoard().getPots();

        for (int y = 0; y < pots.length; y++) {
            for (int x = 0; x < pots[y].length; x++) {
                if (index < potsList.size()) {
                    Map<String, Object> potMap = requireObject(potsList.get(index), prefix + ".pots[" + index + "]");
                    pots[y][x].setLocked(getBoolean(potMap, "isLocked", y > 0));

                    Object plantObj = potMap.get("plant");
                    if (plantObj != null) {
                        Map<String, Object> pMap = requireObject(plantObj, prefix + ".plant");
                        String name = requireString(pMap, "name", prefix + ".plant");
                        boolean isMarigold = getBoolean(pMap, "isMarigold", false);
                        long plantedTime = getLong(pMap, "plantedTimeMillis", System.currentTimeMillis());
                        long duration = getLong(pMap, "durationMillis", 0);
                        pots[y][x].setPlant(new PlantedPlant(name, isMarigold, plantedTime, duration));
                    }
                    index++;
                }
            }
        }
        return gh;
    }

    private static SecurityQuestion readSecurityQuestion(Object value, String prefix) {
        if (value == null)
            return null;
        Map<String, Object> storedQuestion = requireObject(value, prefix + ".securityQuestion");
        String question = requireString(storedQuestion, "question", prefix + ".securityQuestion");
        Object answerHashValue = storedQuestion.get("answerHash");
        if (answerHashValue instanceof String answerHash) {
            return SecurityQuestion.fromStoredHash(question, answerHash);
        }
        Object plainAnswerValue = storedQuestion.get("answer");
        if (plainAnswerValue instanceof String plainAnswer) {
            return new SecurityQuestion(question, plainAnswer);
        }
        throw new IllegalArgumentException(prefix + ".securityQuestion.answerHash must be a string");
    }

    private static String writeUsers(List<User> users) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"version\": ").append(CURRENT_VERSION).append(",\n  \"users\": [");
        if (!users.isEmpty())
            json.append('\n');
        for (int i = 0; i < users.size(); i++) {
            appendUser(json, users.get(i), "    ");
            if (i + 1 < users.size())
                json.append(',');
            json.append('\n');
        }
        json.append("  ]\n}\n");
        return json.toString();
    }

    private static void appendUser(StringBuilder json, User user, String indent) {
        json.append(indent).append("{\n");
        appendStringProperty(json, indent, "username", user.getUsername(), true);
        appendStringProperty(json, indent, "passwordHash", user.getPasswordHashForStorage(), true);
        appendStringProperty(json, indent, "nickname", user.getNickName(), true);
        appendStringProperty(json, indent, "email", user.getEmail(), true);
        appendStringProperty(json, indent, "gender", user.getGender().name(), true);

        json.append(indent).append("  \"securityQuestion\": ");
        SecurityQuestion securityQuestion = user.getSecurityQuestionData();
        if (securityQuestion == null) {
            json.append("null,\n");
        } else {
            json.append("{\n");
            appendStringProperty(json, indent + "  ", "question", securityQuestion.getQuestion(), true);
            appendStringProperty(json, indent + "  ", "answerHash", securityQuestion.getAnswerHashForStorage(), false);
            json.append(indent).append("  },\n");
        }

        appendNumberProperty(json, indent, "coins", user.getCoins(), true);
        appendNumberProperty(json, indent, "diamonds", user.getDiamonds(), true);
        appendNumberProperty(json, indent, "greenhousePotsUnlocked", user.getGreenhousePotsUnlocked(), true);
        appendNumberProperty(json, indent, "potCount", user.getPotCount(), true);
        appendNumberProperty(json, indent, "plantFoodCount", user.getPlantFoodCount(), true);
        appendSettings(json, user.getSettings(), indent);
        appendAdventureProgress(json, user.getAdventureProgress(), indent);
        appendGameProgress(json, user.getGameProgerss(), indent);

        // Serialize daily offer properties
        appendStringProperty(json, indent, "dailyOfferDate", user.getDailyOfferDate(), true);
        appendStringProperty(json, indent, "dailyOfferPlant", user.getDailyOfferPlant(), true);
        appendBooleanProperty(json, indent, "dailyOfferPurchased", user.isDailyOfferPurchased(), true);

        json.append(indent).append("  \"plantBoosts\": {\n");
        int bCount = 0;
        Map<String, Integer> boosts = user.getPlantBoosts();
        for (Map.Entry<String, Integer> entry : boosts.entrySet()) {
            appendNumberProperty(json, indent + "  ", entry.getKey(), entry.getValue(), ++bCount < boosts.size());
        }
        json.append(indent).append("  },\n");

        appendPlantCollection(json, user.getPlantCollection(), indent);
        appendZombieCollection(json, user.getZombieCollection(), indent);

        json.append(indent).append("  \"news\": [\n");
        List<io.github.some_example_name.model.news.News> newsList = user.getNewsPanel().getAllNews();
        for (int i = 0; i < newsList.size(); i++) {
            io.github.some_example_name.model.news.News n = newsList.get(i);
            json.append(indent).append("    {\n");
            appendLongProperty(json, indent + "    ", "timestampMillis", n.getTimestampMillis(), true);
            appendStringProperty(json, indent + "    ", "title", n.getTitle(), true);
            appendStringProperty(json, indent + "    ", "description", n.getDescription(), true);
            appendBooleanProperty(json, indent + "    ", "hasRead", n.isHasRead(), false);
            json.append(indent).append("    }");
            if (i + 1 < newsList.size())
                json.append(",");
            json.append("\n");
        }
        json.append(indent).append("  ],\n");

        appendGreenHouse(json, user.getGreenHouse(), indent);
        json.append(indent).append('}');
    }

    private static void appendSettings(StringBuilder json,
            Settings settings, String indent) {
        json.append(indent).append("  \"settings\": {\n");
        appendNumberProperty(json, indent + "  ", "difficultyLevel",
                settings.getDifficultyLevel(), false);
        json.append(indent).append("  },\n");
    }

    private static void appendAdventureProgress(StringBuilder json,
            AdventureProgress progress, String indent) {
        json.append(indent).append("  \"adventureProgress\": {\n");
        json.append(indent).append("    \"highestUnlockedLevels\": {\n");
        Map<String, Integer> unlocked = progress
                .getHighestUnlockedLevelsForStorage();
        int index = 0;
        for (Map.Entry<String, Integer> entry : unlocked.entrySet()) {
            appendNumberProperty(json, indent + "    ", entry.getKey(),
                    entry.getValue(), ++index < unlocked.size());
        }
        json.append(indent).append("    },\n");
        json.append(indent).append("    \"completedLevels\": [");
        List<String> completed = progress.getCompletedLevelsForStorage();
        for (int i = 0; i < completed.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }
            appendQuoted(json, completed.get(i));
        }
        json.append("]\n");
        json.append(indent).append("  },\n");
    }

    private static void appendGameProgress(StringBuilder json,
            GameProgerss progress, String indent) {
        json.append(indent).append("  \"gameProgress\": {\n");
        appendNumberProperty(json, indent + "  ", "lastCompletedChapter",
                progress.getLastCompletedChapter(), true);
        appendNumberProperty(json, indent + "  ", "lastCompletedLevel",
                progress.getLastCompletedLevel(), true);
        appendNumberProperty(json, indent + "  ", "completedMinigames",
                progress.getCompletedMinigames(), true);
        appendNumberProperty(json, indent + "  ", "highestScore",
                progress.getHighestScore(), true);
        appendNumberProperty(json, indent + "  ", "gamesPlayed",
                progress.getGamesPlayed(), true);
        appendMinigameProgress(json, progress, indent);
        json.append(indent).append("  },\n");
    }

    private static void appendMinigameProgress(StringBuilder json,
            GameProgerss progress, String indent) {
        json.append(indent)
                .append("    \"highestUnlockedMinigameLevels\": {\n");
        Map<String, Integer> unlocked = progress
                .getHighestUnlockedMinigameLevelsForStorage();
        int index = 0;
        for (Map.Entry<String, Integer> entry : unlocked.entrySet()) {
            appendNumberProperty(json, indent + "    ", entry.getKey(),
                    entry.getValue(), ++index < unlocked.size());
        }
        json.append(indent).append("    },\n");
        json.append(indent).append("    \"completedMinigameLevels\": [");
        List<String> completed = progress
                .getCompletedMinigameLevelsForStorage();
        for (int completedIndex = 0; completedIndex < completed.size(); completedIndex++) {
            if (completedIndex > 0) {
                json.append(", ");
            }
            appendQuoted(json, completed.get(completedIndex));
        }
        json.append("]\n");
    }

    private static void appendPlantCollection(StringBuilder json,
            PlantCollection collection, String indent) {
        json.append(indent).append("  \"plantCollection\": [\n");
        List<PlantCollectionItem> plants = collection.getAllPlants();
        for (int i = 0; i < plants.size(); i++) {
            PlantCollectionItem plant = plants.get(i);
            json.append(indent).append("    {\n");
            appendStringProperty(json, indent + "    ", "name", plant.getName(), true);
            appendBooleanProperty(json, indent + "    ", "unlocked", plant.isUnlocked(), true);
            appendNumberProperty(json, indent + "    ", "level", plant.getCurrentLevel(), true);
            appendNumberProperty(json, indent + "    ", "cards", plant.getTotalCardsCollected(), false);
            json.append(indent).append("    }");
            if (i + 1 < plants.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append(indent).append("  ],\n");
    }

    private static void appendZombieCollection(StringBuilder json,
            ZombieCollection collection, String indent) {
        json.append(indent).append("  \"zombieCollection\": [\n");
        List<ZombieCollectionItem> zombies = collection.getAllZombies();
        for (int i = 0; i < zombies.size(); i++) {
            ZombieCollectionItem zombie = zombies.get(i);
            json.append(indent).append("    {\n");
            appendStringProperty(json, indent + "    ", "name", zombie.getName(), true);
            appendBooleanProperty(json, indent + "    ", "unlocked", zombie.isUnlocked(), false);
            json.append(indent).append("    }");
            if (i + 1 < zombies.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append(indent).append("  ],\n");
    }

    private static void appendGreenHouse(StringBuilder json, GreenHouse gh, String indent) {
        json.append(indent).append("  \"greenHouse\": {\n");
        json.append(indent).append("    \"pots\": [\n");
        Pot[][] pots = gh.getBoard().getPots();
        for (int y = 0; y < pots.length; y++) {
            for (int x = 0; x < pots[y].length; x++) {
                Pot pot = pots[y][x];
                json.append(indent).append("      {\n");
                appendBooleanProperty(json, indent + "      ", "isLocked", pot.isLocked(), !pot.isEmpty());
                if (!pot.isEmpty()) {
                    json.append(indent).append("        \"plant\": {\n");
                    appendStringProperty(json, indent + "        ", "name", pot.getPlant().getPlantName(), true);
                    appendBooleanProperty(json, indent + "        ", "isMarigold", pot.getPlant().isMarigold(), true);
                    appendLongProperty(json, indent + "        ", "plantedTimeMillis",
                            pot.getPlant().getPlantedTimeMillis(), true);
                    appendLongProperty(json, indent + "        ", "durationMillis", pot.getPlant().getDurationMillis(),
                            false);
                    json.append(indent).append("        }\n");
                }
                boolean isLast = (y == pots.length - 1 && x == pots[y].length - 1);
                json.append(indent).append("      }").append(isLast ? "" : ",").append("\n");
            }
        }
        json.append(indent).append("    ]\n");
        json.append(indent).append("  }\n");
    }

    private static void appendStringProperty(StringBuilder json, String indent, String name, String value,
            boolean comma) {
        json.append(indent).append("  ");
        appendQuoted(json, name);
        json.append(": ");
        appendQuoted(json, value);
        if (comma)
            json.append(',');
        json.append('\n');
    }

    private static void appendNumberProperty(StringBuilder json, String indent, String name, int value, boolean comma) {
        json.append(indent).append("  ");
        appendQuoted(json, name);
        json.append(": ").append(value);
        if (comma)
            json.append(',');
        json.append('\n');
    }

    private static void appendLongProperty(StringBuilder json, String indent, String name, long value, boolean comma) {
        json.append(indent).append("  ");
        appendQuoted(json, name);
        json.append(": ").append(value);
        if (comma)
            json.append(',');
        json.append('\n');
    }

    private static void appendBooleanProperty(StringBuilder json, String indent, String name, boolean value,
            boolean comma) {
        json.append(indent).append("  ");
        appendQuoted(json, name);
        json.append(": ").append(value ? "true" : "false");
        if (comma)
            json.append(',');
        json.append('\n');
    }

    private static void appendQuoted(StringBuilder json, String value) {
        if (value == null) {
            json.append("null");
            return;
        }
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        json.append('"');
    }

    private static String requireString(Map<String, Object> object, String name, String context) {
        Object value = object.get(name);
        if (!(value instanceof String stringValue))
            throw new IllegalArgumentException(context + "." + name + " must be a string");
        return stringValue;
    }

    private static int getInt(Map<String, Object> object, String name, int defaultValue) {
        Object value = object.get(name);
        if (value == null)
            return defaultValue;
        if (!(value instanceof Number number))
            throw new IllegalArgumentException(name + " must be a number");
        return number.intValue();
    }

    private static long getLong(Map<String, Object> object, String name, long defaultValue) {
        Object value = object.get(name);
        if (value == null)
            return defaultValue;
        if (!(value instanceof Number number))
            throw new IllegalArgumentException(name + " must be a number");
        return number.longValue();
    }

    private static boolean getBoolean(Map<String, Object> object, String name, boolean defaultValue) {
        Object value = object.get(name);
        if (value == null)
            return defaultValue;
        if (value instanceof Boolean boolValue)
            return boolValue;
        throw new IllegalArgumentException(name + " must be a boolean");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requireObject(Object value, String context) {
        if (!(value instanceof Map<?, ?>))
            throw new IllegalArgumentException(context + " must be an object");
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> requireArray(Object value, String context) {
        if (!(value instanceof List<?>))
            throw new IllegalArgumentException(context + " must be an array");
        return (List<Object>) value;
    }

    private static final class JsonParser {
        private final String input;
        private int index;

        private JsonParser(String input) {
            this.input = input;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != input.length()) {
                throw error("unexpected trailing content");
            }
            return value;
        }

        private Object parseValue() {
            if (index >= input.length())
                throw error("unexpected end of input");

            char c = input.charAt(index);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) {
                        yield parseNumber();
                    }
                    throw error("unexpected character '" + c + "'");
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();
            Map<String, Object> object = new LinkedHashMap<>();
            if (consume('}')) {
                return object;
            }

            while (true) {
                skipWhitespace();
                if (index >= input.length() || input.charAt(index) != '"') {
                    throw error("object key must be a string");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                object.put(key, parseValue());
                skipWhitespace();
                if (consume('}')) {
                    return object;
                }
                expect(',');
                skipWhitespace();
            }
        }

        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();
            List<Object> array = new ArrayList<>();
            if (consume(']')) {
                return array;
            }

            while (true) {
                skipWhitespace();
                array.add(parseValue());
                skipWhitespace();
                if (consume(']')) {
                    return array;
                }
                expect(',');
                skipWhitespace();
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < input.length()) {
                char c = input.charAt(index++);
                if (c == '"') {
                    return value.toString();
                }
                if (c != '\\') {
                    if (c < 0x20) {
                        throw error("unescaped control character in string");
                    }
                    value.append(c);
                    continue;
                }

                if (index >= input.length()) {
                    throw error("unfinished string escape");
                }
                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(parseUnicodeEscape());
                    default -> throw error("invalid string escape: \\" + escaped);
                }
            }
            throw error("unterminated string");
        }

        private char parseUnicodeEscape() {
            if (index + 4 > input.length()) {
                throw error("unfinished unicode escape");
            }
            String digits = input.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(digits, 16);
            } catch (NumberFormatException e) {
                throw error("invalid unicode escape: " + digits);
            }
        }

        private Number parseNumber() {
            int start = index;
            consume('-');
            consumeDigits();
            boolean decimal = false;

            if (consume('.')) {
                decimal = true;
                consumeDigits();
            }
            if (consume('e') || consume('E')) {
                decimal = true;
                consume('+');
                consume('-');
                consumeDigits();
            }

            String number = input.substring(start, index);
            try {
                return decimal ? Double.parseDouble(number) : Long.parseLong(number);
            } catch (NumberFormatException e) {
                throw error("invalid number: " + number);
            }
        }

        private void consumeDigits() {
            int start = index;
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("expected a digit");
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!input.startsWith(literal, index)) {
                throw error("expected " + literal);
            }
            index += literal.length();
            return value;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw error("expected '" + expected + "'");
            }
        }

        private boolean consume(char expected) {
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + index);
        }
    }
}