package io.github.Plants_Vs_Zombies_2.model.menu;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.game.plantSelector.PlantSelection;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Level;

/**
 * Pre-game menu used to build the plant card loadout for one level.
 */
public final class PlantSelectionMenu extends Menu {
    private final PlantSelection selection;
    private final String chapterId;
    private final int levelNumber;
    private final Level level;

    public PlantSelectionMenu(PlantSelection selection,
            String chapterId, int levelNumber, Level level) {
        if (selection == null || chapterId == null
                || chapterId.isBlank() || levelNumber <= 0
                || level == null) {
            throw new IllegalArgumentException(
                    "plant selection metadata is incomplete");
        }
        this.selection = selection;
        this.chapterId = chapterId;
        this.levelNumber = levelNumber;
        this.level = level;
    }

    public PlantSelection getSelection() {
        return selection;
    }

    public String getChapterId() {
        return chapterId;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public Level getLevel() {
        return level;
    }

    @Override
    public void exit() {
        App.getInstance().changeMenu(new MainMenu());
    }

    @Override
    public String getName() {
        return "plantselection";
    }
}
