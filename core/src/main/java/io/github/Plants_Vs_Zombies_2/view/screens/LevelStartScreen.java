package io.github.Plants_Vs_Zombies_2.view.screens;

/** Phase-2 level-objectives/start-menu shell. */
public final class LevelStartScreen extends AbstractScreen {
    public LevelStartScreen(ScreenNavigator navigator) {
        super(navigator, "Level Objectives");
        addReturnToCurrentMenuButton("Continue");
    }
}
