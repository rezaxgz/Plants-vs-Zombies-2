package io.github.Plants_Vs_Zombies_2.model;

import io.github.Plants_Vs_Zombies_2.model.auth.SessionManager;
import io.github.Plants_Vs_Zombies_2.model.menu.MainMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.Menu;
import io.github.Plants_Vs_Zombies_2.model.menu.SignUpMenu;
import io.github.Plants_Vs_Zombies_2.model.user.User;

public class App {
    private static final App instance = new App();

    private Menu currentMenu = new SignUpMenu();
    private User loggedInUser;
    private boolean running = true;

    private App() {
        User persistentUser = SessionManager.restorePersistentUser();
        if (persistentUser != null) {
            loggedInUser = persistentUser;
            currentMenu = new MainMenu();
        }
    }

    public static App getInstance() {
        return instance;
    }

    public void changeMenu(Menu menu) {
        if (menu == null) {
            throw new IllegalArgumentException("menu cannot be null");
        }
        currentMenu = menu;
    }

    public Menu getCurrentMenu() {
        return currentMenu;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    public void logout() {
        SessionManager.clearPersistentSession();
        loggedInUser = null;
        changeMenu(new SignUpMenu());
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }
}
