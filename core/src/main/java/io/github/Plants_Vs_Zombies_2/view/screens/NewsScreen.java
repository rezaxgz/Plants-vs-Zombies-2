package io.github.Plants_Vs_Zombies_2.view.screens;

/** Graphical shell for the news menu. */
public final class NewsScreen extends AbstractScreen {
    public NewsScreen(ScreenNavigator navigator) {
        super(navigator, "News");
        addBackButton();
    }
}
