package model.game.gameTypes;

public abstract class GameType {
    public abstract boolean spawnsSuns(); // Night Ops, Plant what you get

    public abstract boolean checkForSpecialGameEnd(); // save seeds, dead line, love your plants, timed war
}
