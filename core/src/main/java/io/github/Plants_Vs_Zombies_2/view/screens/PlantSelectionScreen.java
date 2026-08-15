package io.github.Plants_Vs_Zombies_2.view.screens;

/** Graphical shell for pre-level plant selection. */
public final class PlantSelectionScreen extends AbstractScreen {
    public PlantSelectionScreen(ScreenNavigator navigator) {
        super(navigator, "Choose Your Plants");
        addBackButton();
    }
}
