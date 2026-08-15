package io.github.Plants_Vs_Zombies_2.view.screens;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.menu.GreenhouseMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.ShopMenu;

/** Graphical shell for the greenhouse menu. */
public final class GreenhouseScreen extends AbstractScreen {
    public GreenhouseScreen(ScreenNavigator navigator) {
        super(navigator, "Greenhouse");
        addMenuButton("Shop", () -> new ShopMenu(currentGreenhouseMenu()));
        addBackButton();
    }

    private static GreenhouseMenu currentGreenhouseMenu() {
        return (GreenhouseMenu) App.getInstance().getCurrentMenu();
    }
}
