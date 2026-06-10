package model;

import model.menu.Menu;
import model.user.User;

public class App {
    private Menu currentMenu;
    private User loggedInUser;

    public void changeMenu(Menu menu) {
        currentMenu = menu;
    }
}
