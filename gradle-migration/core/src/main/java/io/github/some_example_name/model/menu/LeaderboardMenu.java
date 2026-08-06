package io.github.some_example_name.model.menu;

import io.github.some_example_name.model.App;

public final class LeaderboardMenu extends Menu {
    private final Menu parentMenu;

    public LeaderboardMenu() {
        this(new MainMenu());
    }

    public LeaderboardMenu(Menu parentMenu) {
        this.parentMenu = parentMenu == null ? new MainMenu() : parentMenu;
    }

    @Override
    public void exit() {
        App.getInstance().changeMenu(parentMenu);
    }

    @Override
    public String getName() {
        return "leaderboard";
    }
}
