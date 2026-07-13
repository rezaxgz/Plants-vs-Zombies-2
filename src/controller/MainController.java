package controller;

import java.util.regex.Matcher;

import model.App;
import model.CommandResult;

public final class MainController {
    private MainController() {
    }

    public static CommandResult handleLogout(Matcher matcher) {
        App.getInstance().logout();
        return CommandResult.success("logged out successfully\nentered signup menu");
    }
}
