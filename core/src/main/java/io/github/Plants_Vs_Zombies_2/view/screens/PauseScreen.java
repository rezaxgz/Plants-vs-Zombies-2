package io.github.Plants_Vs_Zombies_2.view.screens;

/**
 * Phase-2 pause-menu shell. Actual game pausing/restarting is wired when the
 * real-time game loop is introduced; navigation is already functional.
 */
public final class PauseScreen extends AbstractScreen {
    public PauseScreen(ScreenNavigator navigator) {
        super(navigator, "Game Paused");
        addReturnToCurrentMenuButton("Resume");
        addActionButton("Save & Exit", navigator::exitCurrentMenu);
    }
}
