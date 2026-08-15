package io.github.Plants_Vs_Zombies_2.view.screens;

/** Graphical shell for the existing phase-one network menu. */
public final class NetworkScreen extends AbstractScreen {
    public NetworkScreen(ScreenNavigator navigator) {
        super(navigator, "Network");
        addBackButton();
    }
}
