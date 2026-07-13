package model.user;

import model.Settings;
import model.collections.plants.PlantCollection;
import model.collections.zombies.ZombieCollection;
import model.enums.Gender;
import model.quest.AllQuestsProgress;
import model.security.Question;
import model.security.SecurityQuestion;
import model.security.Sha256;
import model.shop.item.ItemPrice;
import model.shop.item.ShopItem;

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

    private Inventory inventory;

    private GameProgerss gameProgerss;

    private AllQuestsProgress questProgress;

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
    }

    public static User fromStoredData(String username, String passwordHash, String nickname, String email,
            Gender gender, SecurityQuestion securityQuestion, int coins, int diamonds,
            int greenhousePotsUnlocked, int plantFoodCount) {
        return new User(username, passwordHash, nickname, email, gender, securityQuestion, coins, diamonds,
                greenhousePotsUnlocked, plantFoodCount);
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

    public int getDiamonds() {
        return diamonds;
    }

    public int getGreenhousePotsUnlocked() {
        return greenhousePotsUnlocked;
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public void setSecurityQuestion(int n, String answer) {
        Question question = Question.getByNumber(n);
        if (question == null) {
            throw new IllegalArgumentException("invalid security question number: " + n);
        }
        this.securityQuestion = new SecurityQuestion(question.getText(), answer);
    }

    public boolean canAfford(ItemPrice price) {
        return false;
    }

    public void payForItem(ShopItem item) {
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
}
