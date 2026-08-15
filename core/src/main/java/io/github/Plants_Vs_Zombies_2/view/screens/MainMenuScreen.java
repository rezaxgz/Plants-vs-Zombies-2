package io.github.Plants_Vs_Zombies_2.view.screens;

import io.github.Plants_Vs_Zombies_2.model.menu.CollectionMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.GreenhouseMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.LeaderboardMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.NetworkMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.NewsMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.ProfileMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.SettingsMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.TravelLogMenu;

/** Main graphical hub. */
public final class MainMenuScreen extends AbstractScreen {
    public MainMenuScreen(ScreenNavigator navigator) {
        super(navigator, "Main Menu");

        addActionButton("Adventure", navigator::showAdventureScreen);
        addMenuButton("Collection", CollectionMenu::new);
        addMenuButton("Greenhouse", GreenhouseMenu::new);
        addMenuButton("Travel Log / Quests", TravelLogMenu::new);
        nextNavigationRow();
        addMenuButton("Leaderboard", LeaderboardMenu::new);
        addMenuButton("News", NewsMenu::new);
        addMenuButton("Profile", ProfileMenu::new);
        addMenuButton("Settings", SettingsMenu::new);
        nextNavigationRow();
        addMenuButton("Network", NetworkMenu::new);
        addActionButton("Logout", navigator::logout);
    }
}
