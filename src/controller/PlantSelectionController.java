package controller;

import java.util.List;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.collections.plants.PlantCollectionItem;
import model.game.plantSelector.PlantSelection;
import model.menu.Menu;
import model.menu.PlantSelectionMenu;
import model.roadmap.Chapter;
import model.roadmap.ChapterCatalog;
import model.user.User;

public final class PlantSelectionController {
    private static final int BOOST_DIAMOND_COST = 2;

    private PlantSelectionController() {
    }

    public static CommandResult handleShowAllPlants(Matcher matcher) {
        PlantSelectionMenu menu = getCurrentMenu();
        if (menu == null) {
            return selectionRequired();
        }
        PlantSelection selection = menu.getSelection();
        return formatPlants("all plants", selection,
                selection.getAllPlants(), true);
    }

    public static CommandResult handleShowAvailablePlants(Matcher matcher) {
        PlantSelectionMenu menu = getCurrentMenu();
        if (menu == null) {
            return selectionRequired();
        }
        PlantSelection selection = menu.getSelection();
        return formatPlants("available plants", selection,
                selection.getAvailablePlants(), false);
    }

    public static CommandResult handleAddPlant(Matcher matcher) {
        PlantSelectionMenu menu = getCurrentMenu();
        if (menu == null) {
            return selectionRequired();
        }
        PlantSelection selection = menu.getSelection();
        PlantCollectionItem plant = selection.findPlant(matcher.group("type"));
        CommandResult error = validatePlantForAdd(selection, plant);
        if (error != null) {
            return error;
        }
        selection.addPlant(plant);
        return CommandResult.success(plant.getName() + " added; selected "
                + selection.getSelectedPlants().size() + "/"
                + selection.getSlotCount() + " plants.");
    }

    private static CommandResult validatePlantForAdd(
            PlantSelection selection, PlantCollectionItem plant) {
        if (plant == null) {
            return CommandResult.error("plant does not exist!");
        }
        if (!plant.isUnlocked()) {
            return CommandResult.error("plant is locked!");
        }
        if (!selection.isAvailable(plant)) {
            return CommandResult.error(
                    "plant is not available in this level!");
        }
        if (selection.isSelected(plant)) {
            return CommandResult.error("plant is already selected!");
        }
        if (selection.getSelectedPlants().size()
                >= selection.getSlotCount()) {
            return CommandResult.error("all plant slots are full!");
        }
        return null;
    }

    public static CommandResult handleRemovePlant(Matcher matcher) {
        PlantSelectionMenu menu = getCurrentMenu();
        if (menu == null) {
            return selectionRequired();
        }
        PlantSelection selection = menu.getSelection();
        PlantCollectionItem plant = selection.findPlant(matcher.group("type"));
        if (plant == null) {
            return CommandResult.error("plant does not exist!");
        }
        if (!selection.isSelected(plant)) {
            return CommandResult.error("plant is not selected!");
        }
        selection.removePlant(plant);
        return CommandResult.success(plant.getName() + " removed; selected "
                + selection.getSelectedPlants().size() + "/"
                + selection.getSlotCount() + " plants.");
    }

    public static CommandResult handleBoostPlant(Matcher matcher) {
        PlantSelectionMenu menu = getCurrentMenu();
        User user = App.getInstance().getLoggedInUser();
        if (menu == null || user == null) {
            return selectionRequired();
        }
        PlantSelection selection = menu.getSelection();
        PlantCollectionItem plant = selection.findPlant(matcher.group("type"));
        if (plant == null) {
            return CommandResult.error("plant does not exist!");
        }
        if (!selection.isSelected(plant)) {
            return CommandResult.error("plant must be selected before it can be boosted!");
        }
        if (selection.isBoosted(plant)) {
            return CommandResult.error("plant is already boosted for this level!");
        }
        if (user.getDiamonds() < BOOST_DIAMOND_COST) {
            return CommandResult.error("not enough diamonds! required: 2, available: "
                    + user.getDiamonds());
        }
        user.deductDiamonds(BOOST_DIAMOND_COST);
        selection.boostPlant(plant);
        UserManager.saveAllUsers();
        return CommandResult.success(plant.getName()
                + " boosted for this level; 2 diamonds spent.");
    }

    public static CommandResult handleStartGame(Matcher matcher) {
        PlantSelectionMenu menu = getCurrentMenu();
        if (menu == null) {
            return selectionRequired();
        }
        if (menu.getSelection().getSelectedPlants().isEmpty()) {
            return CommandResult.error("select at least one plant before starting the game!");
        }
        return startGame(menu, false);
    }

    static CommandResult startGame(
            PlantSelectionMenu menu, boolean automatic) {
        Chapter chapter = ChapterCatalog.findById(menu.getChapterId());
        if (chapter == null) {
            return CommandResult.error("chapter does not exist!");
        }
        String prefix = automatic
                ? "fewer plants are available than the level's "
                        + menu.getSelection().getSlotCount()
                        + " slots; all available plants were selected automatically."
                : null;
        return MainController.launchAdventureGame(
                chapter, menu.getLevel(),
                menu.getSelection().getSelectedPlantLevels(),
                menu.getSelection().getBoostedPlantNames(), prefix);
    }

    private static CommandResult formatPlants(String title,
            PlantSelection selection, List<PlantCollectionItem> plants,
            boolean showUnavailable) {
        StringBuilder output = new StringBuilder(title)
                .append(" (").append(plants.size()).append(")")
                .append(System.lineSeparator())
                .append("selected: ")
                .append(selection.getSelectedPlants().size())
                .append('/').append(selection.getSlotCount());
        for (PlantCollectionItem plant : plants) {
            output.append(System.lineSeparator()).append("- ")
                    .append(plant.getName()).append(" | ")
                    .append(plant.isUnlocked() ? "unlocked" : "locked");
            if (showUnavailable) {
                output.append(" | ")
                        .append(selection.isAvailable(plant)
                                ? "available" : "unavailable");
            }
            if (selection.isSelected(plant)) {
                output.append(" | selected");
            }
            if (selection.isBoosted(plant)) {
                output.append(" | boosted");
            }
        }
        return CommandResult.success(output.toString());
    }

    private static PlantSelectionMenu getCurrentMenu() {
        Menu menu = App.getInstance().getCurrentMenu();
        if (!(menu instanceof PlantSelectionMenu)) {
            return null;
        }
        return (PlantSelectionMenu) menu;
    }

    private static CommandResult selectionRequired() {
        return CommandResult.error("plant selection is not active!");
    }
}
