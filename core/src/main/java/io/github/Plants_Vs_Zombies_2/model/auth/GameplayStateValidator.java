package io.github.Plants_Vs_Zombies_2.model.auth;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.greenHouse.GreenhouseBoard;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Chapter;
import io.github.Plants_Vs_Zombies_2.model.roadmap.ChapterCatalog;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.PlantGameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.ZombieGameplayState;

/** Complete pre-mutation validation for the server's gameplay sync endpoint. */
final class GameplayStateValidator {
    private static final int MAX_COUNTER = 1_000_000_000;
    private static final int MAX_PROGRESS = 100_000;
    private static final int MAX_COLLECTION_ENTRIES = 512;
    private static final int MAX_TEXT_LENGTH = 120;

    private GameplayStateValidator() { }

    static void validate(GameplayState incoming, GameplayState current)
            throws GameplayUpdateException {
        if (incoming == null) fail("gameplay state is required");
        requireCounter(incoming.getCoins(), "coins");
        requireCounter(incoming.getDiamonds(), "diamonds");
        requireCounter(incoming.getSprouts(), "sprouts");
        requireCounter(incoming.getPlantFoodCount(), "plantFoodCount");
        requireCounter(incoming.getPotCount(), "potCount");
        requireCounter(incoming.getGreenhousePotsUnlocked(),
                "greenhousePotsUnlocked");
        if (incoming.getGreenhousePotsUnlocked()
                > GreenhouseBoard.ROWS * GreenhouseBoard.COLUMNS) {
            fail("greenhousePotsUnlocked exceeds the greenhouse capacity");
        }
        requireProgress(incoming.getLastCompletedChapter(), "lastCompletedChapter");
        requireProgress(incoming.getLastCompletedLevel(), "lastCompletedLevel");
        requireCounter(incoming.getCompletedMinigames(), "completedMinigames");
        requireCounter(incoming.getCompletedDailyQuests(),
                "completedDailyQuests");
        requireCounter(incoming.getCompletedNonDailyQuests(),
                "completedNonDailyQuests");
        requireCounter(incoming.getHighestScore(), "highestScore");
        requireCounter(incoming.getGamesPlayed(), "gamesPlayed");
        if (incoming.getLastCompletedChapter() == 0
                && incoming.getLastCompletedLevel() != 0) {
            fail("last completed level requires a completed chapter");
        }
        validateLastCompletedPosition(incoming);

        validateIntegerMap(incoming.getAdventureUnlockedLevels(),
                "adventureUnlockedLevels", MAX_PROGRESS);
        validateStringList(incoming.getCompletedAdventureLevels(),
                "completedAdventureLevels");
        validateAdventureStructure(incoming, current);
        validateIntegerMap(incoming.getMinigameUnlockedLevels(),
                "minigameUnlockedLevels", MAX_PROGRESS);
        validateStringList(incoming.getCompletedMinigameLevels(),
                "completedMinigameLevels");
        validateMinigameStructure(incoming);
        if (incoming.getCompletedMinigames()
                < incoming.getCompletedMinigameLevels().size()) {
            fail("completedMinigames cannot be less than completed level entries");
        }
        validatePlants(incoming.getPlants(), current.getPlants());
        validateZombies(incoming.getZombies(), current.getZombies());
        validateBoosts(incoming.getPlantBoosts(), incoming.getPlants());
        validateDailyOffer(incoming);
        validateMonotonicProgress(incoming, current);
    }

    private static void validateLastCompletedPosition(GameplayState state)
            throws GameplayUpdateException {
        int chapterNumber = state.getLastCompletedChapter();
        if (chapterNumber == 0) return;
        List<Chapter> chapters = ChapterCatalog.getChapters();
        if (chapterNumber > chapters.size() || state.getLastCompletedLevel() < 1
                || state.getLastCompletedLevel()
                        > chapters.get(chapterNumber - 1).getLevelCount()) {
            fail("last completed chapter or level does not exist");
        }
    }

    private static void validateAdventureStructure(GameplayState state,
            GameplayState current) throws GameplayUpdateException {
        if (!state.getAdventureUnlockedLevels().keySet().equals(
                current.getAdventureUnlockedLevels().keySet())) {
            fail("adventure unlocks must contain the server chapter catalog");
        }
        for (Map.Entry<String, Integer> entry
                : state.getAdventureUnlockedLevels().entrySet()) {
            Chapter chapter = ChapterCatalog.findById(entry.getKey());
            if (chapter == null || entry.getValue() > chapter.getLevelCount()) {
                fail("adventure unlock progress references an invalid level");
            }
        }
        for (String key : state.getCompletedAdventureLevels()) {
            int separator = key.lastIndexOf(':');
            if (separator <= 0 || separator + 1 >= key.length()) {
                fail("completed adventure progress contains an invalid level key");
            }
            Chapter chapter = ChapterCatalog.findById(key.substring(0, separator));
            try {
                int level = Integer.parseInt(key.substring(separator + 1));
                if (chapter == null || chapter.getLevel(level) == null) {
                    fail("completed adventure progress references an invalid level");
                }
            } catch (NumberFormatException exception) {
                fail("completed adventure progress contains an invalid level number");
            }
        }
    }

    private static void validateMinigameStructure(GameplayState state)
            throws GameplayUpdateException {
        for (String id : state.getMinigameUnlockedLevels().keySet()) {
            if (!id.matches("[a-z0-9]+")) {
                fail("minigame unlock id must be normalized");
            }
        }
        for (String key : state.getCompletedMinigameLevels()) {
            if (!key.matches("[a-z0-9]+:[1-9][0-9]*")) {
                fail("completed minigame progress contains an invalid level key");
            }
        }
    }

    private static void validateMonotonicProgress(GameplayState incoming,
            GameplayState current) throws GameplayUpdateException {
        if (incoming.getHighestScore() < current.getHighestScore())
            fail("highestScore cannot decrease");
        if (incoming.getGamesPlayed() < current.getGamesPlayed())
            fail("gamesPlayed cannot decrease");
        if (incoming.getCompletedMinigames() < current.getCompletedMinigames())
            fail("completedMinigames cannot decrease");
        if (incoming.getCompletedDailyQuests()
                < current.getCompletedDailyQuests())
            fail("completedDailyQuests cannot decrease");
        if (incoming.getCompletedNonDailyQuests()
                < current.getCompletedNonDailyQuests())
            fail("completedNonDailyQuests cannot decrease");
        if (compareProgress(incoming, current) < 0)
            fail("completed chapter and level cannot move backward");
        requireSuperset(incoming.getCompletedAdventureLevels(),
                current.getCompletedAdventureLevels(), "completed adventure progress");
        requireSuperset(incoming.getCompletedMinigameLevels(),
                current.getCompletedMinigameLevels(), "completed minigame progress");
        requireMapNotDecreased(incoming.getAdventureUnlockedLevels(),
                current.getAdventureUnlockedLevels(), "adventure unlock progress");
        requireMapNotDecreased(incoming.getMinigameUnlockedLevels(),
                current.getMinigameUnlockedLevels(), "minigame unlock progress");
        if (incoming.getGreenhousePotsUnlocked()
                < current.getGreenhousePotsUnlocked()) {
            fail("greenhouse unlocked pots cannot decrease");
        }
    }

    private static int compareProgress(GameplayState first, GameplayState second) {
        int chapter = Integer.compare(first.getLastCompletedChapter(),
                second.getLastCompletedChapter());
        return chapter != 0 ? chapter : Integer.compare(first.getLastCompletedLevel(),
                second.getLastCompletedLevel());
    }

    private static void validatePlants(List<PlantGameplayState> incoming,
            List<PlantGameplayState> current) throws GameplayUpdateException {
        if (incoming == null || incoming.size() > MAX_COLLECTION_ENTRIES)
            fail("plant collection is structurally invalid");
        Map<String, PlantGameplayState> oldByName = plantMap(current, "stored plant");
        Map<String, PlantGameplayState> newByName = plantMap(incoming, "plant");
        if (!newByName.keySet().equals(oldByName.keySet()))
            fail("plant collection must contain the server catalog exactly once");
        for (Map.Entry<String, PlantGameplayState> entry : newByName.entrySet()) {
            PlantGameplayState value = entry.getValue();
            PlantGameplayState old = oldByName.get(entry.getKey());
            if (value.getLevel() < PlantCollectionItem.MIN_LEVEL
                    || value.getLevel() > PlantCollectionItem.MAX_LEVEL
                    || value.getCards() < 0 || value.getCards() > MAX_COUNTER)
                fail("plant level or cards are invalid");
            if (old.isUnlocked() && !value.isUnlocked())
                fail("unlocked plants cannot be relocked");
            if (value.getLevel() < old.getLevel())
                fail("plant levels cannot decrease");
            if (value.getLevel() == old.getLevel()
                    && value.getCards() < old.getCards())
                fail("plant cards cannot decrease without a level upgrade");
        }
    }

    private static Map<String, PlantGameplayState> plantMap(
            List<PlantGameplayState> values, String label)
            throws GameplayUpdateException {
        Map<String, PlantGameplayState> result = new HashMap<>();
        if (values == null) fail(label + " collection is required");
        for (PlantGameplayState value : values) {
            if (value == null || !validText(value.getName()))
                fail(label + " name is invalid");
            if (result.put(normalize(value.getName()), value) != null)
                fail(label + " collection contains duplicates");
        }
        return result;
    }

    private static void validateZombies(List<ZombieGameplayState> incoming,
            List<ZombieGameplayState> current) throws GameplayUpdateException {
        if (incoming == null || incoming.size() > MAX_COLLECTION_ENTRIES)
            fail("zombie collection is structurally invalid");
        Map<String, ZombieGameplayState> oldByName = zombieMap(current);
        Map<String, ZombieGameplayState> newByName = zombieMap(incoming);
        if (!newByName.keySet().equals(oldByName.keySet()))
            fail("zombie collection must contain the server catalog exactly once");
        for (Map.Entry<String, ZombieGameplayState> entry : newByName.entrySet()) {
            if (oldByName.get(entry.getKey()).isUnlocked()
                    && !entry.getValue().isUnlocked())
                fail("unlocked zombies cannot be relocked");
        }
    }

    private static Map<String, ZombieGameplayState> zombieMap(
            List<ZombieGameplayState> values) throws GameplayUpdateException {
        Map<String, ZombieGameplayState> result = new HashMap<>();
        if (values == null) fail("zombie collection is required");
        for (ZombieGameplayState value : values) {
            if (value == null || !validText(value.getName()))
                fail("zombie name is invalid");
            if (result.put(normalize(value.getName()), value) != null)
                fail("zombie collection contains duplicates");
        }
        return result;
    }

    private static void validateBoosts(Map<String, Integer> boosts,
            List<PlantGameplayState> plants) throws GameplayUpdateException {
        if (boosts == null || boosts.size() > MAX_COLLECTION_ENTRIES)
            fail("plantBoosts is structurally invalid");
        Set<String> catalog = new HashSet<>();
        for (PlantGameplayState plant : plants) catalog.add(normalize(plant.getName()));
        for (Map.Entry<String, Integer> entry : boosts.entrySet()) {
            if (!validText(entry.getKey()) || !catalog.contains(normalize(entry.getKey()))
                    || entry.getValue() == null || entry.getValue() < 0
                    || entry.getValue() > 1)
                fail("plantBoosts contains an invalid entry");
        }
    }

    private static void validateDailyOffer(GameplayState state)
            throws GameplayUpdateException {
        if (state.getDailyOfferDate() == null || state.getDailyOfferPlant() == null
                || state.getDailyOfferDate().length() > MAX_TEXT_LENGTH
                || state.getDailyOfferPlant().length() > MAX_TEXT_LENGTH)
            fail("daily offer state is invalid");
        if (!state.getDailyOfferDate().isBlank()) {
            try {
                LocalDate.parse(state.getDailyOfferDate());
            } catch (DateTimeParseException exception) {
                fail("dailyOfferDate must be an ISO date");
            }
        }
    }

    private static void validateIntegerMap(Map<String, Integer> values,
            String label, int maximum) throws GameplayUpdateException {
        if (values == null || values.size() > MAX_COLLECTION_ENTRIES)
            fail(label + " is structurally invalid");
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (!validText(entry.getKey()) || entry.getValue() == null
                    || entry.getValue() < 0 || entry.getValue() > maximum)
                fail(label + " contains an invalid entry");
        }
    }

    private static void validateStringList(List<String> values, String label)
            throws GameplayUpdateException {
        if (values == null || values.size() > MAX_COLLECTION_ENTRIES)
            fail(label + " is structurally invalid");
        Set<String> distinct = new HashSet<>();
        for (String value : values) {
            if (!validText(value) || !distinct.add(value))
                fail(label + " contains an invalid or duplicate entry");
        }
    }

    private static void requireSuperset(List<String> incoming, List<String> old,
            String label) throws GameplayUpdateException {
        if (!new HashSet<>(incoming).containsAll(old)) fail(label + " cannot decrease");
    }

    private static void requireMapNotDecreased(Map<String, Integer> incoming,
            Map<String, Integer> old, String label) throws GameplayUpdateException {
        for (Map.Entry<String, Integer> entry : old.entrySet()) {
            if (incoming.getOrDefault(entry.getKey(), -1) < entry.getValue())
                fail(label + " cannot decrease");
        }
    }

    private static void requireCounter(int value, String label)
            throws GameplayUpdateException {
        if (value < 0 || value > MAX_COUNTER) fail(label + " is outside the allowed range");
    }

    private static void requireProgress(int value, String label)
            throws GameplayUpdateException {
        if (value < 0 || value > MAX_PROGRESS) fail(label + " is outside the allowed range");
    }

    private static boolean validText(String value) {
        return value != null && !value.isBlank() && value.length() <= MAX_TEXT_LENGTH;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static void fail(String message) throws GameplayUpdateException {
        throw new GameplayUpdateException(GameplayUpdateFailure.VALIDATION_FAILED, message);
    }
}
