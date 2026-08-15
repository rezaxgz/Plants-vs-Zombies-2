package io.github.Plants_Vs_Zombies_2.view.screens;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.menu.CollectionMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.GameMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.GreenhouseMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.LeaderboardMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.TravelLogMenu;

/** Graphical shell for the active game. */
public final class GameScreen extends AbstractScreen {
    public GameScreen(ScreenNavigator navigator) {
        super(navigator, "Game");

        addActionButton("Pause", navigator::showPauseScreen);
        addMenuButton("Collection", () -> new CollectionMenu(currentGameMenu()));
        addMenuButton("Greenhouse", () -> new GreenhouseMenu(currentGameMenu()));
        addMenuButton("Travel Log", () -> new TravelLogMenu(currentGameMenu()));
        addMenuButton("Leaderboard", () -> new LeaderboardMenu(currentGameMenu()));
        addBackButton();
    }

    private static GameMenu currentGameMenu() {
        return (GameMenu) App.getInstance().getCurrentMenu();
    }
}
