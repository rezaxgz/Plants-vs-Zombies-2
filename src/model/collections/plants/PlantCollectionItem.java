package model.collections.plants;

import java.util.List;

import model.game.entities.plants.PlantCategory;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantTag;

public class PlantCollectionItem {
    // collection info
    private boolean isUnlocked;
    private int[] cardsNeededForLevelUp;
    private int totalCardsCollected;
    private int cuurentLevel;

    // plant info
    private String name;
    private PlantCategory category;
    private List<PlantTag> tags;
    private int cost; // in suns
    private int baseHP;
    private int damage;

    private final int PLANT_PRICE_IN_COINS = 2000;

    BasePlant createEntity() {
        return null;
    }
}
