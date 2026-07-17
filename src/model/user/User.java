package model.user;

import java.util.HashMap;
import java.util.Map;

import model.Settings;
import model.collections.plants.PlantCollection;
import model.collections.zombies.ZombieCollection;
import model.enums.Gender;
import model.greenHouse.GreenHouse;
import model.quest.AllQuestsProgress;
import model.security.Question;
import model.security.SecurityQuestion;
import model.security.Sha256;
import model.shop.item.ItemPrice;
import model.shop.item.ShopItem;
import model.news.NewsPanel;
import model.news.News;

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

    // preferences
    private Settings settings;

    // greenhouse variables
    private int greenhousePotsUnlocked;
    private int plantFoodCount;
    private GreenHouse greenHouse;
    private Map<String, Integer> plantBoosts;

    // Daily offer state tracking variables
    private String dailyOfferDate = "";
    private String dailyOfferPlant = "";
    private boolean dailyOfferPurchased = false;

    private Inventory inventory;
    private GameProgerss gameProgerss;
    private AllQuestsProgress questProgress;

    private NewsPanel newsPanel;

    public User(String username, String password, String nickname, String email, Gender gender) {
        this(username, Sha256.hash(password), nickname, email, gender, null, 0, 0, 0, 0);
    }

    private User(String username, String passwordHash, String nickname, String email, Gender gender,
            SecurityQuestion securityQuestion, int coins, int diamonds, int greenhousePotsUnlocked,
            int plantFoodCount) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickName = nickname;
        this.email = email;
        this.gender = gender;
        this.securityQuestion = securityQuestion;
        this.coins = coins;
        this.diamonds = diamonds;
        this.greenhousePotsUnlocked = greenhousePotsUnlocked;
        this.plantFoodCount = plantFoodCount;
        this.greenHouse = new GreenHouse();
        this.plantBoosts = new HashMap<>();
        this.plantCollection = new PlantCollection();
        this.zombieCollection = new ZombieCollection();
        this.gameProgerss = new GameProgerss();
        this.questProgress = new AllQuestsProgress();
        this.newsPanel = new NewsPanel();
    }

    public static User fromStoredData(String username, String passwordHash, String nickname, String email,
            Gender gender, SecurityQuestion securityQuestion, int coins, int diamonds,
            int greenhousePotsUnlocked, int plantFoodCount, GreenHouse greenHouse,
            Map<String, Integer> plantBoosts) {
        return fromStoredData(username, passwordHash, nickname, email, gender, securityQuestion,
                coins, diamonds, greenhousePotsUnlocked, plantFoodCount, greenHouse,
                plantBoosts, null, null);
    }

    public static User fromStoredData(String username, String passwordHash, String nickname, String email,
            Gender gender, SecurityQuestion securityQuestion, int coins, int diamonds,
            int greenhousePotsUnlocked, int plantFoodCount, GreenHouse greenHouse,
            Map<String, Integer> plantBoosts, PlantCollection plantCollection,
            ZombieCollection zombieCollection) {
        User user = new User(username, passwordHash, nickname, email, gender, securityQuestion, coins, diamonds,
                greenhousePotsUnlocked, plantFoodCount);
        if (greenHouse != null) {
            user.greenHouse = greenHouse;
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
        return user;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHashForStorage() {
        return passwordHash;
    }

    public String getNickName() {
        return nickName;
    }

    public String getEmail() {
        return email;
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
        this.coins += amount;
    }

    public void deductCoins(int amount) {
        this.coins -= amount;
    }

    public int getDiamonds() {
        return diamonds;
    }

    public void addDiamonds(int amount) {
        this.diamonds += amount;
    }

    public void deductDiamonds(int amount) {
        this.diamonds -= amount;
    }

    public int getGreenhousePotsUnlocked() {
        return greenhousePotsUnlocked;
    }

    public void setGreenhousePotsUnlocked(int greenhousePotsUnlocked) {
        this.greenhousePotsUnlocked = greenhousePotsUnlocked;
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

    public void addPlantBoost(String plantName, int amount) {
        plantBoosts.put(plantName, Math.min(1, plantBoosts.getOrDefault(plantName, 0) + amount));
    }

    public void setSecurityQuestion(int n, String answer) {
        Question question = Question.getByNumber(n);
        if (question == null) {
            throw new IllegalArgumentException("invalid security question number: " + n);
        }
        this.securityQuestion = new SecurityQuestion(question.getText(), answer);
    }

    public boolean canAfford(ItemPrice price) {
        if (price == null)
            return false;
        if (price.getType() == model.enums.CurrencyType.COIN) {
            return this.coins >= price.getAmount();
        } else {
            return this.diamonds >= price.getAmount();
        }
    }

    public void payForItem(ShopItem item) {
        if (item == null || item.getPrice() == null)
            return;
        ItemPrice price = item.getPrice();
        if (price.getType() == model.enums.CurrencyType.COIN) {
            deductCoins(price.getAmount());
        } else {
            deductDiamonds(price.getAmount());
        }
    }

    public boolean doesMatchPassword(String password) {
        String hashPassword = Sha256.hash(password);
        return hashPassword.equals(this.passwordHash);
    }

    public boolean doesMatchEmail(String email) {
        return email.equals(this.email);
    }

    public String getSecurityQuestion() {
        return securityQuestion.getQuestion();
    }

    public boolean isCorrectSecurityAnswer(String answer) {
        return securityQuestion.isAnswerCorrect(answer);
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
        newsPanel.addNews(new News(System.currentTimeMillis(), title, description, false));
    }
}