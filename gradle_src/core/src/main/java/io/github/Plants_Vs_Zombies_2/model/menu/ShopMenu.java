package io.github.Plants_Vs_Zombies_2.model.menu;

import io.github.Plants_Vs_Zombies_2.model.App;

public class ShopMenu extends Menu {
    private final GreenhouseMenu parentGreenhouseMenu;

    public ShopMenu() {
        this(new GreenhouseMenu());
    }

    public ShopMenu(GreenhouseMenu parentGreenhouseMenu) {
        if (parentGreenhouseMenu == null) {
            throw new IllegalArgumentException(
                    "parent greenhouse menu cannot be null");
        }
        this.parentGreenhouseMenu = parentGreenhouseMenu;
    }

    @Override
    public String getName() {
        return "shop";
    }

    @Override
    public void exit() {
        App.getInstance().changeMenu(parentGreenhouseMenu);
    }
}
