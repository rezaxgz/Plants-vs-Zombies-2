package model.menu;

public class GreenhouseMenu extends Menu {
    @Override
    public String getName() {
        return "GreenhouseMenu";
    }

    @Override
    public void exit() {
        // Exit is handled by MenuController routing back to MainMenu
    }
}