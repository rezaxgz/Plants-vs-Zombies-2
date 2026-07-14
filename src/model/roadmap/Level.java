package model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Constants;
import model.game.Board;
import model.game.Game;
import model.game.ZombieWave;

public final class Level {
    private final String name;
    private final int numberOfRows;
    private final int numberOfColumns;
    private final int initialSunCount;
    private final List<ZombieWave> zombieWaves;

    public Level(String name, int numberOfRows, int numberOfColumns,
            int initialSunCount, List<ZombieWave> zombieWaves) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (numberOfRows <= 0 || numberOfColumns <= 0 || initialSunCount < 0) {
            throw new IllegalArgumentException("level dimensions and sun amount are invalid");
        }
        if (zombieWaves == null || zombieWaves.isEmpty()) {
            throw new IllegalArgumentException("zombieWaves cannot be empty");
        }
        this.name = name;
        this.numberOfRows = numberOfRows;
        this.numberOfColumns = numberOfColumns;
        this.initialSunCount = initialSunCount;
        this.zombieWaves = Collections.unmodifiableList(new ArrayList<>(zombieWaves));
    }

    public static Level createExampleLevel() {
        List<ZombieWave> waves = List.of(
                ZombieWave.basicWave(400, false),
                ZombieWave.basicWave(500, false),
                ZombieWave.basicWave(1000, true));
        return new Level("Example Lawn", Constants.DEFAULT_BOARD_ROWS,
                Constants.DEFAULT_BOARD_COLUMNS, 150, waves);
    }

    public Game createGame() {
        return new Game(new Board(numberOfRows, numberOfColumns), null, initialSunCount, zombieWaves);
    }

    public String getName() {
        return name;
    }

    public int getInitialSunCount() {
        return initialSunCount;
    }

    public List<ZombieWave> getZombieWaves() {
        return zombieWaves;
    }
}
