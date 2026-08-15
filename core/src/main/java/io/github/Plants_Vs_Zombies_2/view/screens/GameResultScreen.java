package io.github.Plants_Vs_Zombies_2.view.screens;

/** Phase-2 win/loss-menu shell. */
public final class GameResultScreen extends AbstractScreen {
    public GameResultScreen(ScreenNavigator navigator, boolean won) {
        super(navigator, won ? "You Win!" : "You Lose");
        addActionButton("Exit", navigator::exitCurrentMenu);
        if (!won) {
            // The retry button will recreate the level once restart logic is
            // connected. Returning to the current game is safer than mutating
            // model state here in the navigation-only phase.
            addReturnToCurrentMenuButton("Back to Game");
        }
    }
}
