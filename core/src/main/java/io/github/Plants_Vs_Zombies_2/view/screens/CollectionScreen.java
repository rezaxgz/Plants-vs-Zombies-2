package io.github.Plants_Vs_Zombies_2.view.screens;

/** Graphical shell for the plant/zombie collection menu. */
public final class CollectionScreen extends AbstractScreen {
    public CollectionScreen(ScreenNavigator navigator) {
        super(navigator, "Collection");
        addBackButton();
    }
}
