package io.github.Plants_Vs_Zombies_2.network.gameplay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.collections.zombies.ZombieCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.user.GameProgerss;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** Credential-free gameplay data accepted by the Stage 8 synchronization API. */
public final class GameplayState {
    private final int coins;
    private final int diamonds;
    private final int sprouts;
    private final int plantFoodCount;
    private final int potCount;
    private final int greenhousePotsUnlocked;
    private final int lastCompletedChapter;
    private final int lastCompletedLevel;
    private final int completedMinigames;
    private final int completedDailyQuests;
    private final int completedNonDailyQuests;
    private final int highestScore;
    private final int gamesPlayed;
    private final Map<String, Integer> adventureUnlockedLevels;
    private final List<String> completedAdventureLevels;
    private final Map<String, Integer> minigameUnlockedLevels;
    private final List<String> completedMinigameLevels;
    private final List<PlantGameplayState> plants;
    private final List<ZombieGameplayState> zombies;
    private final Map<String, Integer> plantBoosts;
    private final String dailyOfferDate;
    private final String dailyOfferPlant;
    private final boolean dailyOfferPurchased;

    public GameplayState(int coins, int diamonds, int sprouts,
            int plantFoodCount, int potCount, int greenhousePotsUnlocked,
            int lastCompletedChapter, int lastCompletedLevel,
            int completedMinigames, int highestScore, int gamesPlayed,
            Map<String, Integer> adventureUnlockedLevels,
            List<String> completedAdventureLevels,
            Map<String, Integer> minigameUnlockedLevels,
            List<String> completedMinigameLevels,
            List<PlantGameplayState> plants,
            List<ZombieGameplayState> zombies,
            Map<String, Integer> plantBoosts,
            String dailyOfferDate, String dailyOfferPlant,
            boolean dailyOfferPurchased) {
        this(coins, diamonds, sprouts, plantFoodCount, potCount,
                greenhousePotsUnlocked, lastCompletedChapter,
                lastCompletedLevel, completedMinigames, highestScore,
                gamesPlayed, adventureUnlockedLevels,
                completedAdventureLevels, minigameUnlockedLevels,
                completedMinigameLevels, plants, zombies, plantBoosts,
                dailyOfferDate, dailyOfferPlant, dailyOfferPurchased, 0, 0);
    }

    public GameplayState(int coins, int diamonds, int sprouts,
            int plantFoodCount, int potCount, int greenhousePotsUnlocked,
            int lastCompletedChapter, int lastCompletedLevel,
            int completedMinigames, int highestScore, int gamesPlayed,
            Map<String, Integer> adventureUnlockedLevels,
            List<String> completedAdventureLevels,
            Map<String, Integer> minigameUnlockedLevels,
            List<String> completedMinigameLevels,
            List<PlantGameplayState> plants,
            List<ZombieGameplayState> zombies,
            Map<String, Integer> plantBoosts,
            String dailyOfferDate, String dailyOfferPlant,
            boolean dailyOfferPurchased, int completedDailyQuests,
            int completedNonDailyQuests) {
        this.coins = coins;
        this.diamonds = diamonds;
        this.sprouts = sprouts;
        this.plantFoodCount = plantFoodCount;
        this.potCount = potCount;
        this.greenhousePotsUnlocked = greenhousePotsUnlocked;
        this.lastCompletedChapter = lastCompletedChapter;
        this.lastCompletedLevel = lastCompletedLevel;
        this.completedMinigames = completedMinigames;
        this.completedDailyQuests = completedDailyQuests;
        this.completedNonDailyQuests = completedNonDailyQuests;
        this.highestScore = highestScore;
        this.gamesPlayed = gamesPlayed;
        this.adventureUnlockedLevels = copyMap(adventureUnlockedLevels);
        this.completedAdventureLevels = copyList(completedAdventureLevels);
        this.minigameUnlockedLevels = copyMap(minigameUnlockedLevels);
        this.completedMinigameLevels = copyList(completedMinigameLevels);
        this.plants = copyList(plants);
        this.zombies = copyList(zombies);
        this.plantBoosts = copyMap(plantBoosts);
        this.dailyOfferDate = dailyOfferDate == null ? "" : dailyOfferDate;
        this.dailyOfferPlant = dailyOfferPlant == null ? "" : dailyOfferPlant;
        this.dailyOfferPurchased = dailyOfferPurchased;
    }

    public static GameplayState fromUser(User user) {
        Objects.requireNonNull(user, "user");
        GameProgerss progress = user.getGameProgerss();
        List<PlantGameplayState> plants = new ArrayList<>();
        for (PlantCollectionItem plant : user.getPlantCollection().getAllPlants()) {
            plants.add(new PlantGameplayState(plant.getName(), plant.isUnlocked(),
                    plant.getCurrentLevel(), plant.getTotalCardsCollected()));
        }
        List<ZombieGameplayState> zombies = new ArrayList<>();
        for (ZombieCollectionItem zombie : user.getZombieCollection().getAllZombies()) {
            zombies.add(new ZombieGameplayState(zombie.getName(), zombie.isUnlocked()));
        }
        return new GameplayState(user.getCoins(), user.getDiamonds(), user.getSprouts(),
                user.getPlantFoodCount(), user.getPotCount(),
                user.getGreenhousePotsUnlocked(), progress.getLastCompletedChapter(),
                progress.getLastCompletedLevel(), progress.getCompletedMinigames(),
                progress.getHighestScore(), progress.getGamesPlayed(),
                user.getAdventureProgress().getHighestUnlockedLevelsForStorage(),
                user.getAdventureProgress().getCompletedLevelsForStorage(),
                progress.getHighestUnlockedMinigameLevelsForStorage(),
                progress.getCompletedMinigameLevelsForStorage(), plants, zombies,
                user.getPlantBoosts(), user.getDailyOfferDate(),
                user.getDailyOfferPlant(), user.isDailyOfferPurchased(),
                user.getQuestProgress().getCompletedDailyQuests(),
                user.getQuestProgress().getCompletedNonDailyQuests());
    }

    private static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static Map<String, Integer> copyMap(Map<String, Integer> values) {
        return values == null ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(values));
    }

    public int getCoins() { return coins; }
    public int getDiamonds() { return diamonds; }
    public int getSprouts() { return sprouts; }
    public int getPlantFoodCount() { return plantFoodCount; }
    public int getPotCount() { return potCount; }
    public int getGreenhousePotsUnlocked() { return greenhousePotsUnlocked; }
    public int getLastCompletedChapter() { return lastCompletedChapter; }
    public int getLastCompletedLevel() { return lastCompletedLevel; }
    public int getCompletedMinigames() { return completedMinigames; }
    public int getCompletedDailyQuests() { return completedDailyQuests; }
    public int getCompletedNonDailyQuests() { return completedNonDailyQuests; }
    public int getHighestScore() { return highestScore; }
    public int getGamesPlayed() { return gamesPlayed; }
    public Map<String, Integer> getAdventureUnlockedLevels() {
        return copyMap(adventureUnlockedLevels);
    }
    public List<String> getCompletedAdventureLevels() {
        return copyList(completedAdventureLevels);
    }
    public Map<String, Integer> getMinigameUnlockedLevels() {
        return copyMap(minigameUnlockedLevels);
    }
    public List<String> getCompletedMinigameLevels() {
        return copyList(completedMinigameLevels);
    }
    public List<PlantGameplayState> getPlants() { return copyList(plants); }
    public List<ZombieGameplayState> getZombies() { return copyList(zombies); }
    public Map<String, Integer> getPlantBoosts() { return copyMap(plantBoosts); }
    public String getDailyOfferDate() { return dailyOfferDate; }
    public String getDailyOfferPlant() { return dailyOfferPlant; }
    public boolean isDailyOfferPurchased() { return dailyOfferPurchased; }

    @Override public boolean equals(Object other) {
        if (!(other instanceof GameplayState value)) return false;
        return coins == value.coins && diamonds == value.diamonds
                && sprouts == value.sprouts && plantFoodCount == value.plantFoodCount
                && potCount == value.potCount
                && greenhousePotsUnlocked == value.greenhousePotsUnlocked
                && lastCompletedChapter == value.lastCompletedChapter
                && lastCompletedLevel == value.lastCompletedLevel
                && completedMinigames == value.completedMinigames
                && completedDailyQuests == value.completedDailyQuests
                && completedNonDailyQuests == value.completedNonDailyQuests
                && highestScore == value.highestScore && gamesPlayed == value.gamesPlayed
                && dailyOfferPurchased == value.dailyOfferPurchased
                && Objects.equals(adventureUnlockedLevels, value.adventureUnlockedLevels)
                && Objects.equals(completedAdventureLevels, value.completedAdventureLevels)
                && Objects.equals(minigameUnlockedLevels, value.minigameUnlockedLevels)
                && Objects.equals(completedMinigameLevels, value.completedMinigameLevels)
                && Objects.equals(plants, value.plants) && Objects.equals(zombies, value.zombies)
                && Objects.equals(plantBoosts, value.plantBoosts)
                && Objects.equals(dailyOfferDate, value.dailyOfferDate)
                && Objects.equals(dailyOfferPlant, value.dailyOfferPlant);
    }

    @Override public int hashCode() {
        return Objects.hash(coins, diamonds, sprouts, plantFoodCount, potCount,
                greenhousePotsUnlocked, lastCompletedChapter, lastCompletedLevel,
                completedMinigames, completedDailyQuests,
                completedNonDailyQuests, highestScore, gamesPlayed,
                adventureUnlockedLevels, completedAdventureLevels,
                minigameUnlockedLevels, completedMinigameLevels, plants, zombies,
                plantBoosts, dailyOfferDate, dailyOfferPlant, dailyOfferPurchased);
    }
}
