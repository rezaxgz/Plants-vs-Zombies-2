package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Constants;
import model.game.entities.EntityPosition;
import model.game.entities.other.Sun;
import model.game.gameTypes.GameType;

public class Game {
    private final Board board;
    private final GameType gameType;
    private int sunCount;
    private int zombieWaveNumber;
    private final List<ZombieWave> zombieWaves;
    private final List<String> pendingResults = new ArrayList<>();

    public Game() {
        this(new Board(), null, 0, Collections.emptyList());
    }

    public Game(Board board, int initialSunCount) {
        this(board, null, initialSunCount, Collections.emptyList());
    }

    public Game(Board board, GameType gameType, int initialSunCount, List<ZombieWave> zombieWaves) {
        if (board == null) {
            throw new IllegalArgumentException("board cannot be null");
        }
        if (initialSunCount < 0) {
            throw new IllegalArgumentException("initialSunCount cannot be negative");
        }

        this.board = board;
        this.gameType = gameType;
        this.sunCount = initialSunCount;
        this.zombieWaves = zombieWaves == null ? new ArrayList<>() : new ArrayList<>(zombieWaves);
    }

    /**
     * Terminal/game-loop entry point. A tick is converted to seconds here and every
     * model update below this point works only with seconds.
     */
    public final void tick() {
        update(Constants.ONE_TICK_IN_SECONDS);
    }

    public void advanceTicks(int tickCount) {
        if (tickCount < 0) {
            throw new IllegalArgumentException("tickCount cannot be negative");
        }
        for (int i = 0; i < tickCount; i++) {
            tick();
        }
    }

    public void update(float deltaSeconds) {
        board.update(deltaSeconds);
        pendingResults.addAll(board.drainResults());
    }

    public List<String> drainResults() {
        if (pendingResults.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>(pendingResults);
        pendingResults.clear();
        return Collections.unmodifiableList(results);
    }

    public boolean collectSun(Sun sun) {
        if (!board.containsEntity(sun)) {
            return false;
        }

        int collectedAmount = sun.collect();
        if (collectedAmount <= 0) {
            return false;
        }

        sunCount += collectedAmount;
        board.removeEntity(sun);
        return true;
    }

    public int collectSunAt(int row, int column) {
        return collectSunAt(new EntityPosition(row, column));
    }

    public int collectSunAt(EntityPosition position) {
        int collectedAmount = 0;
        List<Sun> sunsAtPosition = new ArrayList<>(board.getSunsAt(position));
        for (Sun sun : sunsAtPosition) {
            int amount = sun.getSunAmount();
            if (collectSun(sun)) {
                collectedAmount += amount;
            }
        }
        return collectedAmount;
    }

    public boolean spendSun(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        if (sunCount < amount) {
            return false;
        }
        sunCount -= amount;
        return true;
    }

    public boolean isGameOver() {
        return gameType != null && gameType.checkForSpecialGameEnd();
    }

    public Board getBoard() {
        return board;
    }

    public GameType getGameType() {
        return gameType;
    }

    public int getSunCount() {
        return sunCount;
    }

    public int getZombieWaveNumber() {
        return zombieWaveNumber;
    }

    public void setZombieWaveNumber(int zombieWaveNumber) {
        if (zombieWaveNumber < 0) {
            throw new IllegalArgumentException("zombieWaveNumber cannot be negative");
        }
        this.zombieWaveNumber = zombieWaveNumber;
    }

    public List<ZombieWave> getZombieWaves() {
        return Collections.unmodifiableList(zombieWaves);
    }
}
