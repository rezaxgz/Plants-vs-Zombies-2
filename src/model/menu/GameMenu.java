package model.menu;

import model.game.Game;

public class GameMenu extends Menu {
    private final Game game;

    public GameMenu() {
        this(new Game());
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'exit'");
    }

    @Override
    public String getName() {
        return "game";
    }
}
