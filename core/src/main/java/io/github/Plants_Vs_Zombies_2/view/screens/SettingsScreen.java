package io.github.Plants_Vs_Zombies_2.view.screens;

/** Graphical shell for the settings menu. */
public final class SettingsScreen extends AbstractScreen {
    public SettingsScreen(ScreenNavigator navigator) {
        super(navigator, "Settings");
        addBackButton();
    }
}
