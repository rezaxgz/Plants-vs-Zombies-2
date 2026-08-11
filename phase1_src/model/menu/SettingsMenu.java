package model.menu;

import model.App;

public class SettingsMenu extends Menu {
    @Override
    public void exit() {
        App.getInstance().changeMenu(new MainMenu());
    }

    @Override
    public String getName() {
        return "settings";
    }
}
