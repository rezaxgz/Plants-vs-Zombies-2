package model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Constants;
import model.game.Board;
import model.game.Game;
import model.game.ZombieWave;
import model.game.special.ProtectedPlantSpec;

/**
 * Replayable level definition. Every created game receives fresh wave state.
 */
public final class Level {
    private final int number;
    private final String name;
    private final LevelKind kind;
    private final SpecialLevelType specialLevelType;
    private final List<String> specialPlantPool;
    private final List<ProtectedPlantSpec>
            protectedPlantSpecs;
    private final int numberOfRows;
    private final int numberOfColumns;
    private final int initialSunCount;
    private final List<ZombieWave> zombieWaves;

    public Level(String name, int numberOfRows,
            int numberOfColumns, int initialSunCount,
            List<ZombieWave> zombieWaves) {
        this(1, name, LevelKind.NORMAL,
                SpecialLevelType.NONE,
                Collections.emptyList(),
                Collections.emptyList(),
                numberOfRows, numberOfColumns,
                initialSunCount, zombieWaves);
    }

    public Level(int number, String name, LevelKind kind,
            int numberOfRows, int numberOfColumns,
            int initialSunCount,
            List<ZombieWave> zombieWaves) {
        this(number, name, kind,
                SpecialLevelType.NONE,
                Collections.emptyList(),
                Collections.emptyList(),
                numberOfRows, numberOfColumns,
                initialSunCount, zombieWaves);
    }

    public Level(int number, String name, LevelKind kind,
            SpecialLevelType specialLevelType,
            List<String> specialPlantPool,
            List<ProtectedPlantSpec> protectedPlantSpecs,
            int numberOfRows, int numberOfColumns,
            int initialSunCount,
            List<ZombieWave> zombieWaves) {
        validate(number, name, kind, specialLevelType,
                specialPlantPool, protectedPlantSpecs,
                numberOfRows, numberOfColumns,
                initialSunCount, zombieWaves);
        this.number = number;
        this.name = name;
        this.kind = kind;
        this.specialLevelType = specialLevelType;
        this.specialPlantPool =
                immutableCopy(specialPlantPool);
        this.protectedPlantSpecs =
                immutableCopy(protectedPlantSpecs);
        this.numberOfRows = numberOfRows;
        this.numberOfColumns = numberOfColumns;
        this.initialSunCount = initialSunCount;
        this.zombieWaves =
                immutableCopy(zombieWaves);
    }

    private static <T> List<T> immutableCopy(
            List<T> values) {
        return Collections.unmodifiableList(
                new ArrayList<>(values));
    }

    private static void validate(
            int number, String name, LevelKind kind,
            SpecialLevelType specialLevelType,
            List<String> specialPlantPool,
            List<ProtectedPlantSpec> protectedPlantSpecs,
            int rows, int columns, int sun,
            List<ZombieWave> waves) {
        if (number <= 0 || name == null
                || name.isBlank() || kind == null
                || specialLevelType == null) {
            throw new IllegalArgumentException(
                    "level identity values are invalid");
        }
        if (rows <= 0 || columns <= 0 || sun < 0) {
            throw new IllegalArgumentException(
                    "level dimensions and sun are invalid");
        }
        if (waves == null || waves.isEmpty()
                || specialPlantPool == null
                || protectedPlantSpecs == null) {
            throw new IllegalArgumentException(
                    "level collections are invalid");
        }
        if (requiresPlantPool(specialLevelType)
                && specialPlantPool.isEmpty()) {
            throw new IllegalArgumentException(
                    specialLevelType
                            + " requires a plant pool");
        }
        if (specialLevelType
                == SpecialLevelType.SAVE_OUR_SEEDS
                && protectedPlantSpecs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Save Our Seeds requires protected plants");
        }
    }

    private static boolean requiresPlantPool(
            SpecialLevelType specialLevelType) {
        return specialLevelType
                == SpecialLevelType.CONVEYOR_BELT
                || specialLevelType
                        == SpecialLevelType.LOCKED_PLANTS;
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

        Game game = new Game(
                new Board(numberOfRows, numberOfColumns),
                null, initialSunCount, freshWaves);
        configureSpecialRules(game);
        return game;
    }

    private void configureSpecialRules(Game game) {
        if (specialLevelType
                == SpecialLevelType.CONVEYOR_BELT) {
            game.enableConveyorBelt(
                    specialPlantPool);
        } else if (specialLevelType
                == SpecialLevelType.LOCKED_PLANTS) {
            game.enableLockedPlantsForcedLoadout(
                    specialPlantPool);
        } else if (specialLevelType
                == SpecialLevelType.SAVE_OUR_SEEDS) {
            game.enableSaveOurSeeds(
                    protectedPlantSpecs);
        }
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

    public SpecialLevelType getSpecialLevelType() {
        return specialLevelType;
    }

    public List<String> getSpecialPlantPool() {
        return specialPlantPool;
    }

    public List<ProtectedPlantSpec>
            getProtectedPlantSpecs() {
        return protectedPlantSpecs;
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
