package model.user;

import java.util.List;

import model.Settings;
import model.collections.plants.PlantCollection;
import model.collections.zombies.ZombieCollection;
import model.enums.Gender;
import model.quest.AllQuestsProgress;
import model.security.SecurityQuestion;
import model.shop.item.ItemPrice;
import model.shop.item.ShopItem;

public class User {
    // profile info
    private String username;
    private String passwordHash;
    private String nickName;
    private String email;
    private Gender gender;
    private List<SecurityQuestion> securityQuestion;

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

    public boolean canAfford(ItemPrice price) {
        return false;
    }

    public void payForItem(ShopItem item) {
    }
}