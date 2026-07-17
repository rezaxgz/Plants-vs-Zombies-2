package controller;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.collections.plants.PlantCollectionItem;
import model.collections.zombies.ZombieCollectionItem;
import model.user.User;

public final class CollectionMenuController {
    private CollectionMenuController() {
    }

    public static CommandResult handleShowPlants(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        List<PlantCollectionItem> plants = user.getPlantCollection().getUnlockedPlants();
        return formatPlantList("Unlocked plants", plants, false);
    }

    public static CommandResult handleShowAllPlants(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        return formatPlantList("All plants", user.getPlantCollection().getAllPlants(), true);
    }

    public static CommandResult handleShowZombies(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        List<ZombieCollectionItem> zombies = user.getZombieCollection().getUnlockedZombieItems();
        return formatZombieList("Encountered zombies", zombies, false);
    }

    public static CommandResult handleShowAllZombies(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        return formatZombieList("All zombies", user.getZombieCollection().getAllZombies(), true);
    }

    public static CommandResult handleShowPlant(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        PlantCollectionItem plant = user.getPlantCollection().findPlant(matcher.group("plant"));
        if (plant == null) {
            return CommandResult.error("plant does not exist!");
        }
        return CommandResult.success(formatPlantDetails(plant));
    }

    public static CommandResult handleShowZombie(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        ZombieCollectionItem zombie = user.getZombieCollection().findZombie(matcher.group("zombie"));
        if (zombie == null) {
            return CommandResult.error("zombie does not exist!");
        }
        if (!zombie.isUnlocked()) {
            return CommandResult.error("zombie has not been encountered yet!");
        }
        return CommandResult.success(formatZombieDetails(zombie));
    }

    public static CommandResult handleUpgradePlant(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        PlantCollectionItem plant = user.getPlantCollection().findPlant(matcher.group("plant"));
        if (plant == null) {
            return CommandResult.error("plant does not exist!");
        }
        if (!plant.isUnlocked()) {
            return CommandResult.error("plant is locked!");
        }
        if (plant.isAtMaximumLevel()) {
            return CommandResult.error("plant is already at maximum level!");
        }
        int cardsNeeded = plant.getCardsNeededForNextLevel();
        if (plant.getTotalCardsCollected() < cardsNeeded) {
            return CommandResult.error("not enough seed packets! required: " + cardsNeeded
                    + ", available: " + plant.getTotalCardsCollected());
        }
        int coinsNeeded = plant.getCoinsNeededForNextLevel();
        if (user.getCoins() < coinsNeeded) {
            return CommandResult.error("not enough coins! required: " + coinsNeeded
                    + ", available: " + user.getCoins());
        }

        user.deductCoins(coinsNeeded);
        plant.upgrade();
        UserManager.saveAllUsers();
        return CommandResult.success(plant.getName() + " upgraded to level "
                + plant.getCurrentLevel() + ".");
    }

    public static CommandResult handlePurchasePlant(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        PlantCollectionItem plant = user.getPlantCollection().findPlant(matcher.group("plant"));
        if (plant == null) {
            return CommandResult.error("plant does not exist!");
        }
        if (plant.isUnlocked()) {
            return CommandResult.error("plant is already unlocked!");
        }
        if (user.getCoins() < PlantCollectionItem.PLANT_PRICE_IN_COINS) {
            return CommandResult.error("not enough coins! required: "
                    + PlantCollectionItem.PLANT_PRICE_IN_COINS
                    + ", available: " + user.getCoins());
        }

        user.deductCoins(PlantCollectionItem.PLANT_PRICE_IN_COINS);
        user.getPlantCollection().unlockPlant(plant);
        UserManager.saveAllUsers();
        return CommandResult.success(plant.getName() + " purchased and unlocked.");
    }

    private static CommandResult formatPlantList(String title,
            List<PlantCollectionItem> plants, boolean showLockState) {
        CommandResult result = CommandResult.success(title + " (" + plants.size() + "):");
        if (plants.isEmpty()) {
            return result.addPostCommandResult("none");
        }
        for (PlantCollectionItem plant : plants) {
            String state = showLockState ? " | " + (plant.isUnlocked() ? "unlocked" : "locked") : "";
            result.addPostCommandResult(plant.getId() + ". " + plant.getName()
                    + " | " + plant.getCategory() + state
                    + " | level " + plant.getCurrentLevel());
        }
        return result;
    }

    private static CommandResult formatZombieList(String title,
            List<ZombieCollectionItem> zombies, boolean showLockState) {
        CommandResult result = CommandResult.success(title + " (" + zombies.size() + "):");
        if (zombies.isEmpty()) {
            return result.addPostCommandResult("none");
        }
        for (ZombieCollectionItem zombie : zombies) {
            String state = showLockState ? " | " + (zombie.isUnlocked() ? "unlocked" : "locked") : "";
            result.addPostCommandResult(zombie.getName() + state);
        }
        return result;
    }

    private static String formatPlantDetails(PlantCollectionItem plant) {
        String tags = plant.getTags().isEmpty() ? "none" : plant.getTags().toString();
        String actionInterval = Float.isNaN(plant.getActionIntervalSeconds())
                ? "-" : formatDecimal(plant.getActionIntervalSeconds()) + "s";
        String nextUpgrade = plant.isAtMaximumLevel()
                ? "maximum level"
                : plant.getCardsNeededForNextLevel() + " seed packets + "
                        + plant.getCoinsNeededForNextLevel() + " coins";
        return plant.getName() + System.lineSeparator()
                + "status: " + (plant.isUnlocked() ? "unlocked" : "locked") + System.lineSeparator()
                + "category: " + plant.getCategory() + System.lineSeparator()
                + "tags: " + tags + System.lineSeparator()
                + "level: " + plant.getCurrentLevel() + System.lineSeparator()
                + "seed packets: " + plant.getTotalCardsCollected() + System.lineSeparator()
                + "next upgrade: " + nextUpgrade + System.lineSeparator()
                + "sun cost: " + plant.getCost() + System.lineSeparator()
                + "base HP: " + plant.getBaseHP() + System.lineSeparator()
                + "damage: " + plant.getDamage() + System.lineSeparator()
                + "action interval: " + actionInterval + System.lineSeparator()
                + "recharge: " + formatDecimal(plant.getRechargeSeconds()) + "s" + System.lineSeparator()
                + "ability: " + plant.getBaseAbility() + System.lineSeparator()
                + "plant food: " + plant.getPlantFoodEffect() + System.lineSeparator()
                + "level 2: " + plant.getLevelTwoUpgrade() + System.lineSeparator()
                + "level 3: " + plant.getLevelThreeUpgrade() + System.lineSeparator()
                + "level 4: " + plant.getLevelFourUpgrade();
    }

    private static String formatZombieDetails(ZombieCollectionItem zombie) {
        String abilities = zombie.getAbilities().isEmpty() ? "none" : zombie.getAbilities().toString();
        return zombie.getName() + System.lineSeparator()
                + "type: " + zombie.getTypeName() + System.lineSeparator()
                + "hitpoints: " + zombie.getHitpoints() + System.lineSeparator()
                + "speed: " + formatDecimal(zombie.getSpeed()) + System.lineSeparator()
                + "eat DPS: " + zombie.getEatDPS() + System.lineSeparator()
                + "wave point cost: " + zombie.getWavePointCost() + System.lineSeparator()
                + "weight: " + zombie.getWeight() + System.lineSeparator()
                + "armor: " + zombie.getDefaultArmor() + System.lineSeparator()
                + "abilities: " + abilities + System.lineSeparator()
                + "large: " + zombie.isLarge() + System.lineSeparator()
                + "boss: " + zombie.isBoss();
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static User getLoggedInUser() {
        return App.getInstance().getLoggedInUser();
    }

    private static CommandResult loginRequired() {
        return CommandResult.error("you must be logged in to use the collection menu!");
    }
}
