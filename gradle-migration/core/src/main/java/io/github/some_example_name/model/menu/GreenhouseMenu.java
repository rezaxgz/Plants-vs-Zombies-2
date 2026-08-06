package io.github.some_example_name.model.menu;

import io.github.some_example_name.model.App;

public class GreenhouseMenu extends Menu {
    private final GameMenu parentGameMenu;

    public GreenhouseMenu() {
        this(null);
    }

    public GreenhouseMenu(GameMenu parentGameMenu) {
        this.parentGameMenu = parentGameMenu;
    }

    public GameMenu getParentGameMenu() {
        return parentGameMenu;
    }

    @Override
    public String getName() {
        return "greenhouse";
    }

    @Override
    public void exit() {
        if (parentGameMenu != null) {
            App.getInstance().changeMenu(parentGameMenu);
        } else {
            App.getInstance().changeMenu(new MainMenu());
        }
    }
}
