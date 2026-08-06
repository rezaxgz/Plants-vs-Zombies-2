package io.github.some_example_name.model.menu;

import io.github.some_example_name.model.App;

/**
 * Travel Log pages for quests and minigames.
 */
public class TravelLogMenu extends Menu {
    private final Menu parentMenu;

    public TravelLogMenu() {
        this(new MainMenu());
    }

    public TravelLogMenu(Menu parentMenu) {
        this.parentMenu = parentMenu == null ? new MainMenu() : parentMenu;
    }

    @Override
    public void exit() {
        App.getInstance().changeMenu(parentMenu);
    }

    @Override
    public String getName() {
        return "travellog";
    }
}
