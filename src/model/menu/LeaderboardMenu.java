package model.menu;

import model.App;

public final class LeaderboardMenu extends Menu {
    @Override
    public void exit() {
        App.getInstance().changeMenu(new MainMenu());
    }

    @Override
    public String getName() {
        return "leaderboard";
    }
}
