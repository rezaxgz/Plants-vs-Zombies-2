package model.menu;

public class MainMenu extends Menu {

    @Override
    public void exit() {
        // The main menu can only be left with the "menu logout" command.
    }

    @Override
    public String getName() {
        return "main";
    }
}
