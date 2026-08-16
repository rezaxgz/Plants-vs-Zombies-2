package io.github.Plants_Vs_Zombies_2.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.enums.CurrencyType;
import io.github.Plants_Vs_Zombies_2.model.greenHouse.GreenhouseBoard;
import io.github.Plants_Vs_Zombies_2.model.greenHouse.Pot;
import io.github.Plants_Vs_Zombies_2.model.shop.Shop;
import io.github.Plants_Vs_Zombies_2.model.shop.item.ShopItem;
import io.github.Plants_Vs_Zombies_2.model.user.User;

public final class ShopMenuController {
    private static final int MAXIMUM_PLANT_FOOD = 3;
    private static final int MAXIMUM_POTS = 20;
    private static final int DAILY_OFFER_PRICE = 1600;
    private static final int DAILY_OFFER_SEEDS = 10;
    private static final Random RANDOM = new Random();

    private ShopMenuController() {
    }

    /**
     * Keeps the daily offer ready for graphical shop screens. The refresh is
     * persisted only when the calendar day actually changed.
     */
    public static void prepareShop(User user) {
        if (user != null && refreshDailyOfferIfNeeded(user)) {
            UserManager.saveAllUsers();
        }
    }

    public static int getDailyOfferPrice() {
        return DAILY_OFFER_PRICE;
    }

    public static int getDailyOfferSeeds() {
        return DAILY_OFFER_SEEDS;
    }

    public static int getMaximumPlantFood() {
        return MAXIMUM_PLANT_FOOD;
    }

    public static int getMaximumPots() {
        return MAXIMUM_POTS;
    }

    /**
     * GUI-friendly validation that performs no purchase mutation. This lets
     * the shop reject impossible purchases before opening its confirmation
     * dialog, matching the phase-two specification.
     */
    public static CommandResult validatePurchase(
            String id, int count, String plantType) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error("You must be logged in.");
        }
        if (count <= 0) {
            return CommandResult.error(
                    "Quantity must be greater than zero.");
        }

        if (id != null && id.equalsIgnoreCase("daily_offer")) {
            prepareShop(user);
            if (count != 1) {
                return CommandResult.error(
                        "Daily offer is limited to one package per day.");
            }
            if (user.isDailyOfferPurchased()) {
                return CommandResult.error(
                        "You have already purchased today's offer.");
            }
            if (user.getDailyOfferPlant() == null
                    || user.getDailyOfferPlant().isEmpty()) {
                return CommandResult.error(
                        "No daily offer is available right now.");
            }
            if (user.getCoins() < DAILY_OFFER_PRICE) {
                return CommandResult.error(
                        "Insufficient coins. Cost is "
                                + DAILY_OFFER_PRICE + " COIN.");
            }
            return CommandResult.success("Purchase is available.");
        }

        ShopItem item = Shop.getItemById(id);
        if (item == null) {
            return CommandResult.error("Unknown item identifier.");
        }

        int totalCost;
        try {
            totalCost = Math.multiplyExact(
                    item.getPrice().getAmount(), count);
        } catch (ArithmeticException exception) {
            return CommandResult.error("Transaction cost is too large.");
        }
        CommandResult fundsError = validateFunds(user, item, totalCost);
        if (fundsError != null) {
            return fundsError;
        }

        switch (item.getType()) {
            case POT:
                if (user.getGreenHouse().getBoard().getUnlockedPotCount()
                        + count > MAXIMUM_POTS) {
                    return CommandResult.error(
                            "A greenhouse cannot exceed 20 pots.");
                }
                break;
            case PLANT_FOOD:
                if (user.getPlantFoodCount() + count > MAXIMUM_PLANT_FOOD) {
                    return CommandResult.error(
                            "At most 3 starting plant foods can be stored.");
                }
                break;
            case RANDOM_SEED_PACK:
                if (user.getPlantCollection().getUnlockedPlants().isEmpty()) {
                    return CommandResult.error(
                            "Unlock a plant before buying random seed packets.");
                }
                break;
            case SELECTIVE_SEED_PACK:
                if (plantType == null || plantType.isBlank()) {
                    return CommandResult.error(
                            "Choose an unlocked plant first.");
                }
                PlantCollectionItem plant = user.getPlantCollection()
                        .findPlant(plantType);
                if (plant == null) {
                    return CommandResult.error("Plant does not exist.");
                }
                if (!plant.isUnlocked()) {
                    return CommandResult.error(
                            "Only unlocked plants can receive selective seeds.");
                }
                break;
            case CURRENCY_EXCHANGE:
                try {
                    Math.multiplyExact(count, item.getUnit());
                } catch (ArithmeticException exception) {
                    return CommandResult.error(
                            "Exchange amount is too large.");
                }
                break;
            default:
                return CommandResult.error(
                        "This item cannot be purchased here.");
        }
        return CommandResult.success("Purchase is available.");
    }

    /** GUI entry point; command parsing is deliberately kept out of views. */
    public static CommandResult purchase(
            String id, int count, String plantType) {
        CommandResult validation = validatePurchase(id, count, plantType);
        if (!validation.isSuccsesful()) {
            return validation;
        }

        User user = App.getInstance().getLoggedInUser();
        if (id.equalsIgnoreCase("daily_offer")) {
            return buyDailyOffer(user, count);
        }

        ShopItem item = Shop.getItemById(id);
        int totalCost = Math.multiplyExact(
                item.getPrice().getAmount(), count);
        CommandResult itemError = applyItem(
                user, item, count, plantType);
        if (itemError != null) {
            return itemError;
        }
        deductCurrency(user, item, totalCost);
        UserManager.saveAllUsers();
        return CommandResult.success(String.format(
                "Successfully bought %d unit%s of %s.",
                count, count == 1 ? "" : "s", item.getName()));
    }

    public static CommandResult handleShopList(Matcher matcher) {
        StringBuilder output = new StringBuilder(
                "--- Permanent Shop Items ---\n");
        for (ShopItem item : Shop.PERMANENT_ITEMS) {
            output.append(String.format(
                    "ID: %s | %s | Price: %d %s | Yield: %d Units%n",
                    item.getId(), item.getName(),
                    item.getPrice().getAmount(),
                    item.getPrice().getType(), item.getUnit()));
        }
        return CommandResult.success(output.toString().trim());
    }

    public static CommandResult handleShopDaily(Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error("You must be logged in.");
        }
        if (refreshDailyOfferIfNeeded(user)) {
            UserManager.saveAllUsers();
        }
        if (user.getDailyOfferPlant() == null
                || user.getDailyOfferPlant().isEmpty()) {
            return CommandResult.error(
                    "No daily offer available. Unlock standard plants first!");
        }

        String status = user.isDailyOfferPurchased()
                ? "[Purchased]"
                : "[Available]";
        String message = String.format(
                "--- Daily Offer %s ---%n"
                        + "ID: daily_offer%n"
                        + "Target Seed: %s (10 Seed Packets)%n"
                        + "Price: 1600 COIN (20%% Discount Applied)",
                status, user.getDailyOfferPlant());
        return CommandResult.success(message);
    }

    public static CommandResult handleShopBuy(Matcher matcher) {
        String id = matcher.group("id").trim();
        int count = Integer.parseInt(matcher.group("count"));
        String plantType = matcher.group("plantType");
        if (plantType != null) {
            plantType = plantType.trim();
        }
        return purchase(id, count, plantType);
    }

    private static CommandResult buyDailyOffer(User user, int count) {
        if (count != 1) {
            return CommandResult.error(
                    "Daily offer is limited to one package per day.");
        }
        if (refreshDailyOfferIfNeeded(user)) {
            UserManager.saveAllUsers();
        }
        if (user.isDailyOfferPurchased()) {
            return CommandResult.error(
                    "You have already purchased today's offer.");
        }
        if (user.getDailyOfferPlant() == null
                || user.getDailyOfferPlant().isEmpty()) {
            return CommandResult.error("No daily offer generated.");
        }
        if (user.getCoins() < DAILY_OFFER_PRICE) {
            return CommandResult.error(
                    "Insufficient coins. Cost is 1600 COIN.");
        }

        user.deductCoins(DAILY_OFFER_PRICE);
        user.getPlantCollection().addSeeds(
                user.getDailyOfferPlant(), DAILY_OFFER_SEEDS);
        user.setDailyOfferPurchased(true);
        UserManager.saveAllUsers();
        return CommandResult.success(
                "Successfully bought daily offer: 10 seed packets for "
                        + user.getDailyOfferPlant() + ".");
    }

    private static CommandResult validateFunds(User user,
            ShopItem item, int totalCost) {
        CurrencyType currency = item.getPrice().getType();
        if (currency == CurrencyType.COIN
                && user.getCoins() < totalCost) {
            return CommandResult.error(
                    "Insufficient coins. Total cost: "
                            + totalCost + " COIN.");
        }
        if (currency == CurrencyType.DIAMOND
                && user.getDiamonds() < totalCost) {
            return CommandResult.error(
                    "Insufficient diamonds. Total cost: "
                            + totalCost + " DIAMOND.");
        }
        return null;
    }

    private static CommandResult applyItem(User user,
            ShopItem item, int count, String plantType) {
        switch (item.getType()) {
            case POT:
                return unlockPots(user, count);
            case PLANT_FOOD:
                return addStartingPlantFood(user, count);
            case RANDOM_SEED_PACK:
                return addRandomSeeds(user, item, count);
            case SELECTIVE_SEED_PACK:
                return addSelectiveSeeds(
                        user, item, count, plantType);
            case CURRENCY_EXCHANGE:
                user.addCoins(count * item.getUnit());
                return null;
            default:
                return CommandResult.error(
                        "This item cannot be purchased here.");
        }
    }

    private static CommandResult unlockPots(User user, int count) {
        GreenhouseBoard board = user.getGreenHouse().getBoard();
        int currentPots = board.getUnlockedPotCount();
        if (currentPots + count > MAXIMUM_POTS) {
            return CommandResult.error(
                    "Transaction rejected. A greenhouse cannot exceed 20 pots.");
        }
        unlockNextSequentialPots(board, count);
        user.setGreenhousePotsUnlocked(board.getUnlockedPotCount());
        return null;
    }

    private static CommandResult addStartingPlantFood(
            User user, int count) {
        if (user.getPlantFoodCount() + count > MAXIMUM_PLANT_FOOD) {
            return CommandResult.error(
                    "Transaction rejected. At most 3 starting plant foods "
                            + "can be stored.");
        }
        user.setPlantFoodCount(
                user.getPlantFoodCount() + count);
        return null;
    }

    private static CommandResult addRandomSeeds(User user,
            ShopItem item, int count) {
        List<PlantCollectionItem> unlocked = user.getPlantCollection().getUnlockedPlants();
        if (unlocked.isEmpty()) {
            return CommandResult.error(
                    "No unlocked plant is available for a random seed pack.");
        }
        for (int i = 0; i < count; i++) {
            PlantCollectionItem randomPlant = unlocked.get(
                    RANDOM.nextInt(unlocked.size()));
            user.getPlantCollection().addSeeds(
                    randomPlant.getName(), item.getUnit());
        }
        return null;
    }

    private static CommandResult addSelectiveSeeds(User user,
            ShopItem item, int count, String plantType) {
        if (plantType == null || plantType.isBlank()) {
            return CommandResult.error(
                    "Selective seed packs require -t <plant_type>.");
        }
        PlantCollectionItem plant = user.getPlantCollection()
                .findPlant(plantType);
        if (plant == null) {
            return CommandResult.error("Plant does not exist.");
        }
        if (!plant.isUnlocked()) {
            return CommandResult.error(
                    "Only unlocked plants can receive selective seeds.");
        }
        user.getPlantCollection().addSeeds(
                plant.getName(), count * item.getUnit());
        return null;
    }

    private static void deductCurrency(User user,
            ShopItem item, int totalCost) {
        if (item.getPrice().getType() == CurrencyType.COIN) {
            user.deductCoins(totalCost);
        } else {
            user.deductDiamonds(totalCost);
        }
    }

    private static boolean refreshDailyOfferIfNeeded(User user) {
        String today = LocalDate.now().toString();
        if (today.equals(user.getDailyOfferDate())) {
            return false;
        }
        user.setDailyOfferDate(today);
        user.setDailyOfferPurchased(false);
        List<PlantCollectionItem> unlocked = user.getPlantCollection().getUnlockedPlants();
        if (unlocked.isEmpty()) {
            user.setDailyOfferPlant("");
        } else {
            PlantCollectionItem selected = unlocked.get(
                    RANDOM.nextInt(unlocked.size()));
            user.setDailyOfferPlant(selected.getName());
        }
        return true;
    }

    private static void unlockNextSequentialPots(
            GreenhouseBoard board, int amountToUnlock) {
        int unlocked = 0;
        for (Pot[] row : board.getPots()) {
            for (Pot pot : row) {
                if (unlocked >= amountToUnlock) {
                    return;
                }
                if (pot != null && pot.isLocked()) {
                    pot.unlock();
                    unlocked++;
                }
            }
        }
    }
}
