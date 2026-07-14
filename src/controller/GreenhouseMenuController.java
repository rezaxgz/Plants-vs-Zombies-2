package controller;

import model.App;
import model.CommandResult;
import model.collections.plants.PlantCollectionItem;
import model.greenHouse.*;
import model.user.User;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;

public class GreenhouseMenuController {
    private static final Random RANDOM = new Random();

    public static CommandResult handleShowGreenhouse(Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null)
            return CommandResult.error("You must be logged in.");

        GreenhouseBoard board = user.getGreenHouse().getBoard();
        StringBuilder sb = new StringBuilder("Greenhouse Status:\n");

        for (int y = 1; y <= GreenhouseBoard.ROWS; y++) {
            for (int x = 1; x <= GreenhouseBoard.COLUMNS; x++) {
                Pot pot = board.getPotAt(x, y);
                sb.append(String.format("Pot (%d,%d): ", x, y));

                if (pot.isLocked()) {
                    sb.append("Locked");
                } else if (pot.isEmpty()) {
                    sb.append("Empty");
                } else {
                    PlantedPlant p = pot.getPlant();
                    if (p.isGrown()) {
                        sb.append(p.getPlantName()).append(" [ready]");
                    } else {
                        sb.append(p.getPlantName()).append(" [growing: ")
                                .append(p.getRemainingHoursCeil()).append(" hours left]");
                    }
                }
                sb.append("\n");
            }
        }
        return CommandResult.success(sb.toString().trim());
    }

    public static CommandResult handlePlantPot(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));

        User user = App.getInstance().getLoggedInUser();
        Pot pot = user.getGreenHouse().getBoard().getPotAt(x, y);

        if (pot == null)
            return CommandResult.error("invalid coordinates");
        if (pot.isLocked())
            return CommandResult.error("this pot is locked");
        if (!pot.isEmpty())
            return CommandResult.error("this pot is already occupied");

        boolean isMarigold = RANDOM.nextDouble() < 0.5;
        String plantName = "marigold";
        long duration = 2;

        if (!isMarigold) {
            List<PlantCollectionItem> unlocked = user.getPlantCollection().getUnlockedPlants();
            if (unlocked != null && !unlocked.isEmpty()) {
                // Warning: Make sure PlantCollectionItem has a toString or getName method
                plantName = unlocked.get(RANDOM.nextInt(unlocked.size())).toString();
            } else {
                plantName = "random_plant_placeholder";
            }
            duration = 8;
        }

        pot.setPlant(new PlantedPlant(plantName, isMarigold, duration));
        return CommandResult.success("planted " + plantName + " at (" + x + ", " + y + ")");
    }

    public static CommandResult handleCollect(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));

        User user = App.getInstance().getLoggedInUser();
        Pot pot = user.getGreenHouse().getBoard().getPotAt(x, y);

        if (pot == null || pot.isLocked() || pot.isEmpty())
            return CommandResult.error("no plant to collect here");

        PlantedPlant plant = pot.getPlant();
        if (!plant.isGrown())
            return CommandResult.error("plant is not ready yet");

        pot.harvest();

        if (plant.isMarigold()) {
            user.addCoins(500);
            return CommandResult.success("harvested marigold! gained 500 coins");
        } else {
            user.addPlantBoost(plant.getPlantName(), 1);
            return CommandResult.success("harvested " + plant.getPlantName() + "! gained 1 boost");
        }
    }

    public static CommandResult handleGrow(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));

        User user = App.getInstance().getLoggedInUser();
        Pot pot = user.getGreenHouse().getBoard().getPotAt(x, y);

        if (pot == null || pot.isLocked() || pot.isEmpty())
            return CommandResult.error("no growing plant here");

        PlantedPlant plant = pot.getPlant();
        if (plant.isGrown())
            return CommandResult.error("plant is already ready for harvest");

        int cost = plant.getRemainingHoursCeil();

        if (user.getDiamonds() < cost) {
            return CommandResult.error("not enough diamonds. need " + cost);
        }

        user.deductDiamonds(cost);
        plant.grow();
        return CommandResult.success("spent " + cost + " diamonds. plant is now ready!");
    }

    public static CommandResult handleUnlock(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));

        User user = App.getInstance().getLoggedInUser();
        Pot pot = user.getGreenHouse().getBoard().getPotAt(x, y);

        if (pot == null)
            return CommandResult.error("invalid coordinates");
        if (!pot.isLocked())
            return CommandResult.error("pot is already unlocked");

        int cost = 100; // Arbitrary cost since it was unspecified
        if (user.getCoins() < cost)
            return CommandResult.error("not enough coins to unlock (need " + cost + ")");

        user.deductCoins(cost);
        pot.unlock();
        return CommandResult.success("unlocked pot at (" + x + ", " + y + ") for " + cost + " coins");
    }
}