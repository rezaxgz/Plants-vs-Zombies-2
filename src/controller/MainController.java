package controller;

import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.menu.GameMenu;

public final class MainController {
    private MainController() {
    }

    public static CommandResult handleStartGame(Matcher matcher) {
        App.getInstance().changeMenu(new GameMenu());
        return CommandResult.success("game started\nentered game menu");
    }

    public static CommandResult handleLogout(Matcher matcher) {
        App.getInstance().logout();
        return CommandResult.success("logged out successfully\nentered signup menu");
    }
}
