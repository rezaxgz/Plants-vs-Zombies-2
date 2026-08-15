package io.github.Plants_Vs_Zombies_2.view.screens;

/** Graphical shell for the shop menu. */
public final class ShopScreen extends AbstractScreen {
    public ShopScreen(ScreenNavigator navigator) {
        super(navigator, "Shop");
        addBackButton();
    }
}
