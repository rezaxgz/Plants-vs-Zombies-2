package io.github.Plants_Vs_Zombies_2.model.game.gameTypes;

public abstract class GameType implements java.io.Serializable {
    public abstract boolean spawnsSuns(); // Night Ops, Plant what you get

    public abstract boolean checkForSpecialGameEnd(); // save seeds, dead line, love your plants, timed war
}
