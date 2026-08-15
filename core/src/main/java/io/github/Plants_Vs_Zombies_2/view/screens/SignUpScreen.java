package io.github.Plants_Vs_Zombies_2.view.screens;

import io.github.Plants_Vs_Zombies_2.model.menu.LoginMenu;

/** Graphical shell for the signup menu. */
public final class SignUpScreen extends AbstractScreen {
    public SignUpScreen(ScreenNavigator navigator) {
        super(navigator, "Sign Up");
        addMenuButton("Login", LoginMenu::new);
        addActionButton("Exit", navigator::exitApplication);
    }
}
