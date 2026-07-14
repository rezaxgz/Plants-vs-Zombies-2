package model.menu;

import model.App;

public class ShopMenu extends Menu {
    @Override
    public String getName() {
        return "shop";
    }

    public void exit() {
        App.getInstance().changeMenu(new GreenhouseMenu());
    }
}