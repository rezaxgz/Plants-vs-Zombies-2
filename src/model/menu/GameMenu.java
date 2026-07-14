package model.menu;

import model.App;
import model.game.Game;
import model.roadmap.Level;

public class GameMenu extends Menu {
    private final Game game;

    public GameMenu() {
        this(Level.createExampleLevel().createGame());
    }

    public GameMenu(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    @Override
    public void exit() {
        App.getInstance().changeMenu(new MainMenu());
    }

    @Override
    public String getName() {
        return "game";
    }
}
