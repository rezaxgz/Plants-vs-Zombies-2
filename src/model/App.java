package model;

import model.menu.Menu;
import model.menu.SignUpMenu;
import model.user.User;

public class App {
    private static App instance = new App();

    private Menu currentMenu = new SignUpMenu();
    private User loggedInUser = null;

    public static App getInstance() {
        if (instance == null) {
            instance = new App();
        }
        return instance;
    }

    public void changeMenu(Menu menu) {
        currentMenu = menu;
    }

    public Menu getCurrentMenu() {
        return currentMenu;
    }
}
