package io.github.some_example_name.model.menu;

import io.github.some_example_name.model.App;

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
