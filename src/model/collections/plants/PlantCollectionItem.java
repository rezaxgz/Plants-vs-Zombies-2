package model.collections.plants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import model.game.entities.EntityPosition;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;
import model.game.entities.plants.PlantDefinition;
import model.game.entities.plants.PlantFactory;
import model.game.entities.plants.PlantTag;

public class PlantCollectionItem {
    public static final int PLANT_PRICE_IN_COINS = 2000;
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 4;

    private static final int[] CARDS_NEEDED = {10, 20, 40};
    private static final int[] COINS_NEEDED = {1000, 2000, 4000};

    private final PlantDefinition definition;
    private final PlantCategory category;
    private boolean unlocked;
    private int totalCardsCollected;
    private int currentLevel = MIN_LEVEL;

    PlantCollectionItem(PlantDefinition definition, PlantCategory category) {
        if (definition == null || category == null) {
            throw new IllegalArgumentException("plant definition and category cannot be null");
        }
        this.definition = definition;
        this.category = category;
    }

    public int getId() {
        return definition.getId();
    }

    public String getName() {
        return definition.getDisplayName();
    }

    public PlantCategory getCategory() {
        return category;
    }

    public List<PlantTag> getTags() {
        List<PlantTag> tags = new ArrayList<>(definition.getTags());
        tags.sort(Comparator.comparing(Enum::name));
        return List.copyOf(tags);
    }

    public int getCost() {
        return definition.getCost(currentLevel);
    }

    public int getBaseHP() {
        return definition.getBaseHP(currentLevel);
    }

    public int getDamage() {
        return definition.getDamage(currentLevel);
    }

    public String getBaseAbility() {
        return definition.getBaseAbility();
    }

    public String getPlantFoodEffect() {
        return definition.getPlantFoodEffect();
    }

    public String getLevelTwoUpgrade() {
        return definition.getLevelTwoUpgrade();
    }

    public String getLevelThreeUpgrade() {
        return definition.getLevelThreeUpgrade();
    }

    public String getLevelFourUpgrade() {
        return definition.getLevelFourUpgrade();
    }

    public float getActionIntervalSeconds() {
        return definition.getActionIntervalSeconds(currentLevel);
    }

    public float getRechargeSeconds() {
        return definition.getRechargeSeconds(currentLevel);
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public int getTotalCardsCollected() {
        return totalCardsCollected;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public boolean isAtMaximumLevel() {
        return currentLevel >= MAX_LEVEL;
    }

    public int getCardsNeededForNextLevel() {
        return isAtMaximumLevel() ? 0 : CARDS_NEEDED[currentLevel - MIN_LEVEL];
    }

    public int getCoinsNeededForNextLevel() {
        return isAtMaximumLevel() ? 0 : COINS_NEEDED[currentLevel - MIN_LEVEL];
    }

    public void addCards(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("card amount cannot be negative");
        }
        totalCardsCollected += amount;
    }

    public boolean upgrade() {
        if (!unlocked || isAtMaximumLevel()
                || totalCardsCollected < getCardsNeededForNextLevel()) {
            return false;
        }
        totalCardsCollected -= getCardsNeededForNextLevel();
        currentLevel++;
        return true;
    }

    public BasePlant createEntity(EntityPosition position) {
        return PlantFactory.createPlant(getName(), currentLevel, position);
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    void restoreState(boolean restoredUnlocked, int restoredLevel, int restoredCards) {
        unlocked = restoredUnlocked;
        currentLevel = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, restoredLevel));
        totalCardsCollected = Math.max(0, restoredCards);
    }
}
