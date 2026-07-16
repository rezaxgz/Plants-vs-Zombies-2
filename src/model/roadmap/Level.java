package model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Constants;
import model.game.Board;
import model.game.Game;
import model.game.ZombieWave;

/**
 * Replayable level definition. Every created game receives fresh wave state.
 */
public final class Level {
    private final int number;
    private final String name;
    private final LevelKind kind;
    private final int numberOfRows;
    private final int numberOfColumns;
    private final int initialSunCount;
    private final List<ZombieWave> zombieWaves;

    public Level(String name, int numberOfRows,
            int numberOfColumns, int initialSunCount,
            List<ZombieWave> zombieWaves) {
        this(1, name, LevelKind.NORMAL,
                numberOfRows, numberOfColumns,
                initialSunCount, zombieWaves);
    }

    public Level(int number, String name, LevelKind kind,
            int numberOfRows, int numberOfColumns,
            int initialSunCount,
            List<ZombieWave> zombieWaves) {
        validate(number, name, kind, numberOfRows,
                numberOfColumns, initialSunCount,
                zombieWaves);
        this.number = number;
        this.name = name;
        this.kind = kind;
        this.numberOfRows = numberOfRows;
        this.numberOfColumns = numberOfColumns;
        this.initialSunCount = initialSunCount;
        this.zombieWaves = Collections.unmodifiableList(
                new ArrayList<>(zombieWaves));
    }

    private static void validate(int number, String name,
            LevelKind kind, int rows, int columns,
            int sun, List<ZombieWave> waves) {
        if (number <= 0 || name == null
                || name.isBlank() || kind == null) {
            throw new IllegalArgumentException(
                    "level identity values are invalid");
        }
        if (rows <= 0 || columns <= 0 || sun < 0) {
            throw new IllegalArgumentException(
                    "level dimensions and sun are invalid");
        }
        if (waves == null || waves.isEmpty()) {
            throw new IllegalArgumentException(
                    "zombieWaves cannot be empty");
        }
    }

    public static Level createExampleLevel() {
        List<ZombieWave> waves = List.of(
                ZombieWave.basicWave(400, false),
                ZombieWave.basicWave(500, false),
                ZombieWave.basicWave(1000, true));
        return new Level(
                "Example Lawn",
                Constants.DEFAULT_BOARD_ROWS,
                Constants.DEFAULT_BOARD_COLUMNS,
                150, waves);
    }

    public Game createGame() {
        List<ZombieWave> freshWaves =
                new ArrayList<>();
        for (ZombieWave wave : zombieWaves) {
            freshWaves.add(wave.copy());
        }
        return new Game(
                new Board(numberOfRows, numberOfColumns),
                null, initialSunCount, freshWaves);
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public LevelKind getKind() {
        return kind;
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }

    public int getNumberOfColumns() {
        return numberOfColumns;
    }

    public int getInitialSunCount() {
        return initialSunCount;
    }

    public List<ZombieWave> getZombieWaves() {
        return zombieWaves;
    }
}
