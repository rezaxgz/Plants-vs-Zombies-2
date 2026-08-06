package io.github.some_example_name.model.menu;

import io.github.some_example_name.model.App;

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
