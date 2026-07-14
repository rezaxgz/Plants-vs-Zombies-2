package controller;

import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.game.Game;
import model.menu.GameMenu;
import model.roadmap.Level;

public final class MainController {
    private MainController() {
    }

    public static CommandResult handleStartGame(Matcher matcher) {
        Level level = Level.createExampleLevel();
        Game game = level.createGame();
        App.getInstance().changeMenu(new GameMenu(game));
        return CommandResult.success("game started: " + level.getName() + "\nentered game menu")
                .addPostCommandResults(game.drainResults());
    }

    public static CommandResult handleLogout(Matcher matcher) {
        App.getInstance().logout();
        return CommandResult.success("logged out successfully\nentered signup menu");
    }
}
