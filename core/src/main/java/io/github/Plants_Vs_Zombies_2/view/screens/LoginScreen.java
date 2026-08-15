package io.github.Plants_Vs_Zombies_2.view.screens;

import io.github.Plants_Vs_Zombies_2.model.menu.SignUpMenu;

/** Graphical shell for the login menu. */
public final class LoginScreen extends AbstractScreen {
    public LoginScreen(ScreenNavigator navigator) {
        super(navigator, "Login");
        addMenuButton("Sign Up", SignUpMenu::new);
        addBackButton();
    }
}
