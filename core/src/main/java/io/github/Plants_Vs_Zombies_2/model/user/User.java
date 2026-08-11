package io.github.Plants_Vs_Zombies_2.model.user;

import java.util.HashMap;
import java.util.Map;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.Settings;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollection;
import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.collections.zombies.ZombieCollection;
import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.greenHouse.GreenHouse;
import io.github.Plants_Vs_Zombies_2.model.news.News;
import io.github.Plants_Vs_Zombies_2.model.news.NewsPanel;
import io.github.Plants_Vs_Zombies_2.model.quest.AllQuestsProgress;
import io.github.Plants_Vs_Zombies_2.model.roadmap.AdventureProgress;
import io.github.Plants_Vs_Zombies_2.model.security.Question;
import io.github.Plants_Vs_Zombies_2.model.security.SecurityQuestion;
import io.github.Plants_Vs_Zombies_2.model.security.Sha256;
import io.github.Plants_Vs_Zombies_2.model.shop.item.ItemPrice;
import io.github.Plants_Vs_Zombies_2.model.shop.item.ShopItem;

public class User {
    // profile info
    private String username;
    private String passwordHash;
    private String nickName;
    private String email;
    private Gender gender;
    private SecurityQuestion securityQuestion;

    // progress info
    private int coins;
    private int diamonds;
    private PlantCollection plantCollection;
    private ZombieCollection zombieCollection;
    private AdventureProgress adventureProgress;

    // preferences
    private Settings settings;

    // greenhouse variables
    private int greenhousePotsUnlocked;
    private int potCount;
    private int plantFoodCount;
    private GreenHouse greenHouse;
    private Map<String, Integer> plantBoosts;

    // Daily offer state tracking variables
    private String dailyOfferDate = "";
    private String dailyOfferPlant = "";
    private boolean dailyOfferPurchased;

    private Inventory inventory;
    private GameProgerss gameProgerss;
    private AllQuestsProgress questProgress;
    private NewsPanel newsPanel;

    public User(String username, String password, String nickname,
            String email, Gender gender) {
        this(username, Sha256.hash(password), nickname, email,
                gender, null, 0, 0, 0, 0);
    }

    private User(String username, String passwordHash, String nickname,
            String email, Gender gender,
            SecurityQuestion securityQuestion, int coins, int diamonds,
            int greenhousePotsUnlocked, int plantFoodCount) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickName = nickname;
        this.email = email;
        this.gender = gender;
        this.securityQuestion = securityQuestion;
        this.coins = coins;
        this.diamonds = diamonds;
        this.plantFoodCount = plantFoodCount;
        this.greenHouse = new GreenHouse();
        this.greenhousePotsUnlocked = Math.max(
                greenhousePotsUnlocked,
                this.greenHouse.getBoard().getUnlockedPotCount());
        this.plantBoosts = new HashMap<>();
        this.plantCollection = new PlantCollection();
        this.zombieCollection = new ZombieCollection();
        this.adventureProgress = new AdventureProgress();
        this.settings = new Settings();
        this.gameProgerss = new GameProgerss();
        this.questProgress = new AllQuestsProgress();
        this.newsPanel = new NewsPanel();
    }

    public static User fromStoredData(String username, String passwordHash,
            String nickname, String email, Gender gender,
            SecurityQuestion securityQuestion, int coins, int diamonds,
            int greenhousePotsUnlocked, int plantFoodCount,
            GreenHouse greenHouse, Map<String, Integer> plantBoosts) {
        return fromStoredData(username, passwordHash, nickname, email,
                gender, securityQuestion, coins, diamonds,
                greenhousePotsUnlocked, plantFoodCount, greenHouse,
                plantBoosts, null, null, null, null, null);
    }

    public static User fromStoredData(String username, String passwordHash,
            String nickname, String email, Gender gender,
            SecurityQuestion securityQuestion, int coins, int diamonds,
            int greenhousePotsUnlocked, int plantFoodCount,
            GreenHouse greenHouse, Map<String, Integer> plantBoosts,
            PlantCollection plantCollection,
            ZombieCollection zombieCollection) {
        return fromStoredData(username, passwordHash, nickname, email,
                gender, securityQuestion, coins, diamonds,
                greenhousePotsUnlocked, plantFoodCount, greenHouse,
                plantBoosts, plantCollection, zombieCollection,
                null, null, null);
    }

    public static User fromStoredData(String username, String passwordHash,
            String nickname, String email, Gender gender,
            SecurityQuestion securityQuestion, int coins, int diamonds,
            int greenhousePotsUnlocked, int plantFoodCount,
            GreenHouse greenHouse, Map<String, Integer> plantBoosts,
            PlantCollection plantCollection,
            ZombieCollection zombieCollection, Settings settings,
            AdventureProgress adventureProgress,
            GameProgerss gameProgerss) {
        User user = new User(username, passwordHash, nickname, email,
                gender, securityQuestion, coins, diamonds,
                greenhousePotsUnlocked, plantFoodCount);
        if (greenHouse != null) {
            user.greenHouse = greenHouse;
            user.greenhousePotsUnlocked = greenHouse.getBoard().getUnlockedPotCount();
        }
        if (plantBoosts != null) {
            user.plantBoosts = plantBoosts;
        }
        if (plantCollection != null) {
            user.plantCollection = plantCollection;
        }
        if (zombieCollection != null) {
            user.zombieCollection = zombieCollection;
        }
        if (settings != null) {
            user.settings = settings;
        }
        if (adventureProgress != null) {
            user.adventureProgress = adventureProgress;
        }
        if (gameProgerss != null) {
            user.gameProgerss = gameProgerss;
        }
        return user;
    }

    public String getUsername() {
        return username;
    }

    public void changeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username cannot be blank");
        }
        this.username = username;
    }

    public String getPasswordHashForStorage() {
        return passwordHash;
    }

    public String getNickName() {
        return nickName;
    }

    public void changeNickname(String nickname) {
        if (nickname == null) {
            throw new IllegalArgumentException("nickname cannot be null");
        }
        this.nickName = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void changeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email cannot be blank");
        }
        this.email = email;
    }

    public Gender getGender() {
        return gender;
    }

    public SecurityQuestion getSecurityQuestionData() {
        return securityQuestion;
    }

    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {
        coins += amount;
    }

    public void deductCoins(int amount) {
        coins -= amount;
    }

    public int getDiamonds() {
        return diamonds;
    }

    public void addDiamonds(int amount) {
        diamonds += amount;
    }

    public void deductDiamonds(int amount) {
        diamonds -= amount;
    }

    public int getGreenhousePotsUnlocked() {
        return greenhousePotsUnlocked;
    }

    public void setGreenhousePotsUnlocked(int greenhousePotsUnlocked) {
        this.greenhousePotsUnlocked = greenhousePotsUnlocked;
    }

    public int getPotCount() {
        return potCount;
    }

    public void setPotCount(int potCount) {
        if (potCount < 0) {
            throw new IllegalArgumentException("potCount cannot be negative");
        }
        this.potCount = potCount;
    }

    public void addPots(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        potCount += amount;
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public void setPlantFoodCount(int plantFoodCount) {
        this.plantFoodCount = plantFoodCount;
    }

    public GreenHouse getGreenHouse() {
        return greenHouse;
    }

    public Map<String, Integer> getPlantBoosts() {
        return plantBoosts;
    }

    public PlantCollection getPlantCollection() {
        return plantCollection;
    }

    public ZombieCollection getZombieCollection() {
        return zombieCollection;
    }

    public AdventureProgress getAdventureProgress() {
        return adventureProgress;
    }

    public Settings getSettings() {
        return settings;
    }

    public boolean unlockPlant(String plantName) {
        return plantCollection.unlockPlant(plantName);
    }

    public boolean addPlantCards(String plantName, int amount) {
        return plantCollection.addCards(plantName, amount);
    }

    public boolean unlockZombie(String zombieName) {
        return zombieCollection.unlockZombie(zombieName);
    }

    public String getDailyOfferDate() {
        return dailyOfferDate;
    }

    public void setDailyOfferDate(String dailyOfferDate) {
        this.dailyOfferDate = dailyOfferDate;
    }

    public String getDailyOfferPlant() {
        return dailyOfferPlant;
    }

    public void setDailyOfferPlant(String dailyOfferPlant) {
        this.dailyOfferPlant = dailyOfferPlant;
    }

    public boolean isDailyOfferPurchased() {
        return dailyOfferPurchased;
    }

    public void setDailyOfferPurchased(boolean dailyOfferPurchased) {
        this.dailyOfferPurchased = dailyOfferPurchased;
    }

    public boolean addPlantBoost(String plantName) {
        String key = findPlantBoostKey(plantName);
        if (key == null) {
            key = canonicalPlantName(plantName);
        }
        if (key.isBlank() || plantBoosts.getOrDefault(key, 0) >= 1) {
            return false;
        }
        plantBoosts.put(key, 1);
        return true;
    }

    public void addPlantBoost(String plantName, int amount) {
        if (amount <= 0) {
            return;
        }
        addPlantBoost(plantName);
    }

    public boolean hasPlantBoost(String plantName) {
        String key = findPlantBoostKey(plantName);
        return key != null && plantBoosts.getOrDefault(key, 0) > 0;
    }

    public boolean consumePlantBoost(String plantName) {
        String key = findPlantBoostKey(plantName);
        if (key == null || plantBoosts.getOrDefault(key, 0) <= 0) {
            return false;
        }
        plantBoosts.remove(key);
        return true;
    }

    private String findPlantBoostKey(String plantName) {
        String normalized = normalizePlantName(plantName);
        if (normalized.isBlank()) {
            return null;
        }
        for (String key : plantBoosts.keySet()) {
            if (normalizePlantName(key).equals(normalized)) {
                return key;
            }
        }
        return null;
    }

    private String canonicalPlantName(String plantName) {
        PlantCollectionItem plant = plantCollection.findPlant(plantName);
        return plant == null ? (plantName == null ? "" : plantName.trim())
                : plant.getName();
    }

    private static String normalizePlantName(String plantName) {
        if (plantName == null) {
            return "";
        }
        return plantName.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    public void setSecurityQuestion(int number, String answer) {
        Question question = Question.getByNumber(number);
        if (question == null) {
            throw new IllegalArgumentException(
                    "invalid security question number: " + number);
        }
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException(
                    "security question answer cannot be blank");
        }
        securityQuestion = new SecurityQuestion(question.getText(), answer);
    }

    public boolean canAfford(ItemPrice price) {
        if (price == null) {
            return false;
        }
        if (price.getType() == io.github.Plants_Vs_Zombies_2.model.enums.CurrencyType.COIN) {
            return coins >= price.getAmount();
        }
        return diamonds >= price.getAmount();
    }

    public void payForItem(ShopItem item) {
        if (item == null || item.getPrice() == null) {
            return;
        }
        ItemPrice price = item.getPrice();
        if (price.getType() == io.github.Plants_Vs_Zombies_2.model.enums.CurrencyType.COIN) {
            deductCoins(price.getAmount());
        } else {
            deductDiamonds(price.getAmount());
        }
    }

    public boolean doesMatchPassword(String password) {
        return password != null && Sha256.hash(password).equals(passwordHash);
    }

    public boolean doesMatchEmail(String candidateEmail) {
        return candidateEmail != null && candidateEmail.equals(email);
    }

    public String getSecurityQuestion() {
        return securityQuestion == null ? null : securityQuestion.getQuestion();
    }

    public boolean isCorrectSecurityAnswer(String answer) {
        return securityQuestion != null
                && securityQuestion.isAnswerCorrect(answer);
    }

    public void changePassword(String password) {
        passwordHash = Sha256.hash(password);
    }

    public GameProgerss getGameProgerss() {
        return gameProgerss;
    }

    public AllQuestsProgress getQuestProgress() {
        return questProgress;
    }

    public NewsPanel getNewsPanel() {
        return newsPanel;
    }

    public void addNews(String title, String description) {
        newsPanel.addNews(new News(System.currentTimeMillis(),
                title, description, false));
    }

    public boolean addNewsIfAbsent(String title, String description) {
        return newsPanel.addNewsIfAbsent(new News(System.currentTimeMillis(),
                title, description, false));
    }

    public boolean addMinigameUnlockNews(String minigameName) {
        if (minigameName == null || minigameName.isBlank()) {
            return false;
        }
        String normalizedName = minigameName.trim();
        boolean added = addNewsIfAbsent(
                "New Minigame Unlocked!",
                normalizedName + " is now available to play.");
        if (added && App.getInstance().getLoggedInUser() == this) {
            UserManager.saveAllUsers();
        }
        return added;
    }
}
