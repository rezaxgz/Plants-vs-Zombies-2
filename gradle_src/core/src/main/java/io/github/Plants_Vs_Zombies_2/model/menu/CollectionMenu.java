package io.github.Plants_Vs_Zombies_2.model.menu;

import io.github.Plants_Vs_Zombies_2.model.App;

public class CollectionMenu extends Menu {
    private final GameMenu parentGameMenu;

    public CollectionMenu() {
        this(new GameMenu());
    }

    public CollectionMenu(GameMenu parentGameMenu) {
        if (parentGameMenu == null) {
            throw new IllegalArgumentException("parent game menu cannot be null");
        }
        this.parentGameMenu = parentGameMenu;
    }

    public GameMenu getParentGameMenu() {
        return parentGameMenu;
    }

    @Override
    public void exit() {
        App.getInstance().changeMenu(parentGameMenu);
    }

    @Override
    public String getName() {
        return "collection";
    }
}
