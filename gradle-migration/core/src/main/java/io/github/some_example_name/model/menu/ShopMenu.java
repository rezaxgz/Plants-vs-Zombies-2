package io.github.some_example_name.model.menu;

import io.github.some_example_name.model.App;

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
