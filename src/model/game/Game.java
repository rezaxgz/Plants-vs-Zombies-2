package model.game;

import java.util.List;

import model.game.gameTypes.GameType;
import model.game.structure.BaseStructure;

public abstract class Game {
    private Board board;
    private GameType gameType;
    private int sunCount;
    private int zombieWaveNumber;
    private List<ZombieWave> zombieWaves;

    public void tick() {
        board.tickAllEntities();
        // other stuff (sun drops, ...)
    }

    public boolean isGameOver() {
        return false;
    }
}
