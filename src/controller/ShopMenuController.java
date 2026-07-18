package controller;

import model.App;
import model.CommandResult;
import model.collections.plants.PlantCollectionItem;
import model.enums.CurrencyType;
import model.greenHouse.GreenhouseBoard;
import model.greenHouse.Pot;
import model.shop.Shop;
import model.shop.item.ShopItem;
import model.user.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;

public class ShopMenuController {

    public static CommandResult handleShopList(Matcher matcher) {
        StringBuilder sb = new StringBuilder("--- Permanent Shop Items ---\n");
        for (ShopItem item : Shop.PERMANENT_ITEMS) {
            sb.append(String.format("ID: %s | %s | Price: %d %s | Yield: %d Units\n",
                    item.getId(), item.getName(), item.getPrice().getAmount(), item.getPrice().getType(),
                    item.getUnit()));
        }
        return CommandResult.success(sb.toString().trim());
    }

    public static CommandResult handleShopDaily(Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null)
            return CommandResult.error("You must be logged in.");

        refreshDailyOfferIfNeeded(user);

        if (user.getDailyOfferPlant() == null || user.getDailyOfferPlant().isEmpty()) {
            return CommandResult.error("No daily offer available. Unlock standard plants first!");
        }

        String status = user.isDailyOfferPurchased() ? "[Purchased]" : "[Available]";
        String message = String.format(
                "--- Daily Offer %s ---\nID: daily_offer\nTarget Seed: %s (10 Seed Packets)\nPrice: 1600 COIN (20%% Discount Applied)\n",
                status, user.getDailyOfferPlant());
        return CommandResult.success(message.trim());
    }

    public static CommandResult handleShopBuy(Matcher matcher) {
        String id = matcher.group("id");
        int count = Integer.parseInt(matcher.group("count"));
        String plantType = matcher.group("plantType");

        User user = App.getInstance().getLoggedInUser();
        if (user == null)
            return CommandResult.error("You must be logged in.");

        if (count <= 0)
            return CommandResult.error("Quantity must be greater than zero.");

        // 1. Process Daily Offer Choice
        if (id.equalsIgnoreCase("daily_offer")) {
            if (count > 1)
                return CommandResult.error("Daily offer package items are limited to 1 transaction per day.");

            refreshDailyOfferIfNeeded(user);
            if (user.isDailyOfferPurchased())
                return CommandResult.error("You have already purchased today's offer.");
            if (user.getDailyOfferPlant() == null || user.getDailyOfferPlant().isEmpty())
                return CommandResult.error("No daily offer generated.");

            if (user.getCoins() < 1600)
                return CommandResult.error("Insufficient coins. Cost is 1600 COIN.");

            user.deductCoins(1600);
            user.getPlantCollection().addSeeds(user.getDailyOfferPlant(), 10);
            user.setDailyOfferPurchased(true);

            saveDatabaseState();
            return CommandResult
                    .success("Successfully bought daily offer: 10 seed packets for " + user.getDailyOfferPlant() + ".");
        }

        // 2. Process Standard Shop Items
        ShopItem item = Shop.getItemById(id);
        if (item == null)
            return CommandResult.error("Unknown item identifier.");

        int totalCost = item.getPrice().getAmount() * count;
        if (!user.canAfford(item.getPrice())) {
            return CommandResult.error("Insufficient funds for this single transaction.");
        }
        if (item.getPrice().getType() == CurrencyType.COIN && user.getCoins() < totalCost) {
            return CommandResult.error("Insufficient coins. Total cost: " + totalCost + " COIN.");
        }
        if (item.getPrice().getType() == CurrencyType.DIAMOND && user.getDiamonds() < totalCost) {
            return CommandResult.error("Insufficient diamonds. Total cost: " + totalCost + " DIAMOND.");
        }

        // 3. Business Capacity Rules Routing Matrix
        switch (item.getType()) {
            case POT:
                int currentPots = countUnlockedPots(user.getGreenHouse().getBoard());
                if (currentPots + count > 20) {
                    return CommandResult.error(
                            "Transaction rejected. Total greenhouse structure size cannot exceed 20 pots limit.");
                }
                unlockNextSequentialPots(user.getGreenHouse().getBoard(), count);
                user.setGreenhousePotsUnlocked(currentPots + count);
                break;

            case PLANT_FOOD:
                if (user.getPlantFoodCount() + count > 3) {
                    return CommandResult
                            .error("Transaction rejected. Inventory plant food allocation maximum cap limit is 3.");
                }
                user.setPlantFoodCount(user.getPlantFoodCount() + count);
                break;

            case RANDOM_SEED_PACK:
                List<PlantCollectionItem> unlocked = user.getPlantCollection().getUnlockedPlants();
                if (unlocked.isEmpty())
                    return CommandResult.error("Your catalog collection profile contains zero unlocked items.");

                for (int i = 0; i < count; i++) {
                    PlantCollectionItem randomPlant = unlocked.get(new Random().nextInt(unlocked.size()));
                    user.getPlantCollection().addSeeds(randomPlant.getName(), item.getUnit());
                }
                break;

            case SELECTIVE_SEED_PACK:
                if (plantType == null || plantType.isEmpty()) {
                    return CommandResult.error(
                            "Target missing. Specified configuration flags requires specifying plant type label (-t [plant]).");
                }
                if (!user.getPlantCollection().isPlantUnlocked(plantType)) {
                    return CommandResult.error(
                            "Requested item requires catalog unlock profile clearance or label path validation error.");
                }
                user.getPlantCollection().addSeeds(plantType, count * item.getUnit());
                break;

            case CURRENCY_EXCHANGE:
                user.addCoins(count * item.getUnit());
                break;
            default:
                break;
        }

        // 4. Financial Settlements Processing Execution Stage
        if (item.getPrice().getType() == CurrencyType.COIN) {
            user.deductCoins(totalCost);
        } else {
            user.deductDiamonds(totalCost);
        }

        saveDatabaseState();
        return CommandResult.success(String.format("Successfully bought %d units of %s.", count, item.getName()));
    }

    // --- Helper Utilities Matrix Isolation Block ---

    private static void refreshDailyOfferIfNeeded(User user) {
        String baseTodayDate = LocalDate.now().toString();
        if (!baseTodayDate.equals(user.getDailyOfferDate())) {
            user.setDailyOfferDate(baseTodayDate);
            user.setDailyOfferPurchased(false);

            List<PlantCollectionItem> unlocked = user.getPlantCollection().getUnlockedPlants();
            if (!unlocked.isEmpty()) {
                PlantCollectionItem selectedChoice = unlocked.get(new Random().nextInt(unlocked.size()));
                user.setDailyOfferPlant(selectedChoice.getName());
            } else {
                user.setDailyOfferPlant("");
            }
        }
    }

    private static int countUnlockedPots(GreenhouseBoard board) {
        int count = 0;
        Pot[][] pots = board.getPots();
        for (Pot[] row : pots) {
            for (Pot pot : row) {
                if (pot != null && !pot.isLocked())
                    count++;
            }
        }
        return count;
    }

    private static void unlockNextSequentialPots(GreenhouseBoard board, int amountToUnlock) {
        int activated = 0;
        Pot[][] pots = board.getPots();
        for (Pot[] row : pots) {
            for (Pot pot : row) {
                if (activated >= amountToUnlock)
                    return;
                if (pot != null && pot.isLocked()) {
                    pot.unlock();
                    activated++;
                }
            }
        }
    }

    private static void saveDatabaseState() {
        // Adapt to your core save hook architecture.
        // If your database uses a direct wrapper like UserManager.saveAllUsers() call
        // it here:
        try {
            Class<?> userManagerClass = Class.forName("model.auth.UserManager");
            java.lang.reflect.Method saveMethod = userManagerClass.getMethod("saveAllUsers");
            saveMethod.invoke(null);
        } catch (Exception ignored) {
            // Falls back safely if executed in localized unit testing architecture layouts
        }
    }
}