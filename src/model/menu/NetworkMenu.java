package model.menu;

import model.App;

public class NetworkMenu extends Menu {
    @Override
    public void exit() {
        App.getInstance().changeMenu(new MainMenu());
    }

    @Override
    public String getName() {
        return "network";
    }
}
