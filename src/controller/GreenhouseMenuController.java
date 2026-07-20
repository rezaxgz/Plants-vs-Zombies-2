package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.collections.plants.PlantCollectionItem;
import model.greenHouse.GreenhouseBoard;
import model.greenHouse.PlantedPlant;
import model.greenHouse.Pot;
import model.menu.GreenhouseMenu;
import model.menu.Menu;
import model.menu.ShopMenu;
import model.user.User;

public final class GreenhouseMenuController {
    private static final Random RANDOM = new Random();
    private static final long MARIGOLD_GROWTH_HOURS = 2;
    private static final long PLANT_GROWTH_HOURS = 8;
    private static final int MARIGOLD_REWARD_COINS = 500;

    private GreenhouseMenuController() {
    }

    public static CommandResult handleShowGreenhouse(Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error("You must be logged in.");
        }

        GreenhouseBoard board = user.getGreenHouse().getBoard();
        StringBuilder output = new StringBuilder("Greenhouse Status:\n");
        for (int y = 1; y <= GreenhouseBoard.ROWS; y++) {
            for (int x = 1; x <= GreenhouseBoard.COLUMNS; x++) {
                appendPotStatus(output, board.getPotAt(x, y), x, y);
            }
        }
        return CommandResult.success(output.toString().trim());
    }

    private static void appendPotStatus(StringBuilder output,
            Pot pot, int x, int y) {
        output.append(String.format("Pot (%d,%d): ", x, y));
        if (pot.isLocked()) {
            output.append("Locked");
        } else if (pot.isEmpty()) {
            output.append("Empty");
        } else {
            PlantedPlant plant = pot.getPlant();
            output.append(plant.getPlantName());
            if (plant.isGrown()) {
                output.append(" [ready]");
            } else {
                output.append(" [growing: ")
                        .append(plant.getRemainingHoursCeil())
                        .append(" hours left]");
            }
        }
        output.append(System.lineSeparator());
    }

    public static CommandResult handlePlantPot(Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error("You must be logged in.");
        }
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        Pot pot = user.getGreenHouse().getBoard().getPotAt(x, y);
        if (pot == null) {
            return CommandResult.error("invalid coordinates");
        }
        if (pot.isLocked()) {
            return CommandResult.error("this pot is locked");
        }
        if (!pot.isEmpty()) {
            return CommandResult.error("this pot is already occupied");
        }

        PlantedPlant plantedPlant = choosePlant(user);
        pot.setPlant(plantedPlant);
        UserManager.saveAllUsers();
        return CommandResult.success("planted "
                + plantedPlant.getPlantName() + " at ("
                + x + ", " + y + ")");
    }

    private static PlantedPlant choosePlant(User user) {
        List<PlantCollectionItem> eligiblePlants =
                getEligibleGreenhousePlants(user);
        boolean chooseMarigold = eligiblePlants.isEmpty()
                || RANDOM.nextDouble() < 0.5;
        if (chooseMarigold) {
            return new PlantedPlant("marigold", true,
                    MARIGOLD_GROWTH_HOURS);
        }
        PlantCollectionItem selected = eligiblePlants.get(
                RANDOM.nextInt(eligiblePlants.size()));
        return new PlantedPlant(selected.getName(), false,
                PLANT_GROWTH_HOURS);
    }

    static List<PlantCollectionItem> getEligibleGreenhousePlants(User user) {
        List<PlantCollectionItem> eligible = new ArrayList<>();
        if (user == null) {
            return eligible;
        }
        for (PlantCollectionItem plant
                : user.getPlantCollection().getUnlockedPlants()) {
            if (plant.hasPlantFoodAbility()) {
                eligible.add(plant);
            }
        }
        return eligible;
    }

    public static CommandResult handleCollect(Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error("You must be logged in.");
        }
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        Pot pot = user.getGreenHouse().getBoard().getPotAt(x, y);
        if (pot == null || pot.isLocked() || pot.isEmpty()) {
            return CommandResult.error("no plant to collect here");
        }

        PlantedPlant plant = pot.getPlant();
        if (!plant.isGrown()) {
            return CommandResult.error("plant is not ready yet");
        }

        pot.harvest();
        String message;
        if (plant.isMarigold()) {
            user.addCoins(MARIGOLD_REWARD_COINS);
            message = "harvested marigold! gained 500 coins";
        } else {
            boolean stored = user.addPlantBoost(plant.getPlantName());
            message = stored
                    ? "harvested " + plant.getPlantName()
                            + "! stored 1 boost for a later level"
                    : "harvested " + plant.getPlantName()
                            + "; its greenhouse boost was already stored";
        }
        UserManager.saveAllUsers();
        return CommandResult.success(message);
    }

    public static CommandResult handleGrow(Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error("You must be logged in.");
        }
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        Pot pot = user.getGreenHouse().getBoard().getPotAt(x, y);
        if (pot == null || pot.isLocked() || pot.isEmpty()) {
            return CommandResult.error("no growing plant here");
        }

        PlantedPlant plant = pot.getPlant();
        if (plant.isGrown()) {
            return CommandResult.error("plant is already ready for harvest");
        }

        int cost = plant.getRemainingHoursCeil();
        if (user.getDiamonds() < cost) {
            return CommandResult.error("not enough diamonds. need " + cost);
        }

        user.deductDiamonds(cost);
        plant.grow();
        UserManager.saveAllUsers();
        return CommandResult.success("spent " + cost
                + " diamonds. plant is now ready!");
    }

    public static CommandResult handleUnlock(Matcher matcher) {
        return CommandResult.error(
                "Pots can only be unlocked through the 2000-coin shop item.");
    }

    public static CommandResult handleEnterShop(Matcher matcher) {
        Menu currentMenu = App.getInstance().getCurrentMenu();
        GreenhouseMenu greenhouseMenu = currentMenu instanceof GreenhouseMenu
                ? (GreenhouseMenu) currentMenu : new GreenhouseMenu();
        App.getInstance().changeMenu(new ShopMenu(greenhouseMenu));
        return CommandResult.success(
                "Entered the shop. Type 'shop list' or 'shop daily' "
                        + "to see available goods.");
    }
}
