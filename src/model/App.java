package model;

import model.menu.Menu;
import model.menu.SignUpMenu;
import model.user.User;

public class App {
    private static final App instance = new App();

    private Menu currentMenu = new SignUpMenu();
    private User loggedInUser;
    private boolean running = true;

    private App() {
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
