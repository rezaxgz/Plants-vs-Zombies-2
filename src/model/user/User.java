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
        this.username = username;
        this.passwordHash = Sha256.hash(password);
        this.nickName = nickname;
        this.email = email;
        this.gender = gender;
    }

    public void setSecurityQuestion(int n, String answer) {
        this.securityQuestion = new SecurityQuestion(Question.getByNumber(n).getText(), answer);
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