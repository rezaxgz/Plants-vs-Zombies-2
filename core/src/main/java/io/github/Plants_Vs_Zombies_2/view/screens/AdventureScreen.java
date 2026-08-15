package io.github.Plants_Vs_Zombies_2.view.screens;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.menu.TravelLogMenu;

/**
 * Phase-2 adventure menu shell.
 *
 * <p>Adventure/chapter commands currently live on the phase-one MainMenu
 * model, so this is deliberately a graphical-only screen until the chapter
 * widgets are implemented.</p>
 */
public final class AdventureScreen extends AbstractScreen {
    public AdventureScreen(ScreenNavigator navigator) {
        super(navigator, "Adventure");
        addMenuButton("Travel Log / Quests",
                () -> new TravelLogMenu(App.getInstance().getCurrentMenu()));
        addReturnToCurrentMenuButton("Back");
    }
}
