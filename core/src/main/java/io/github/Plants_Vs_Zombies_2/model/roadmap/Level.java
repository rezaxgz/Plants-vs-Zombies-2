package io.github.Plants_Vs_Zombies_2.model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.Constants;
import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.ChapterRuleset;
import io.github.Plants_Vs_Zombies_2.model.game.Game;
import io.github.Plants_Vs_Zombies_2.model.game.ZombieWave;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.special.TimedWarObjective;
import io.github.Plants_Vs_Zombies_2.model.game.tile.TileType;

/**
 * Replayable level definition. Every game receives fresh wave state.
 */
public final class Level {
    private static final double TIMED_WAR_KILL_SECONDS = 30.0;
    private static final int TIMED_WAR_KILL_TARGET = 5;
    private static final double TIMED_WAR_SUN_SECONDS = 60.0;
    private static final int TIMED_WAR_SUN_TARGET = 200;

    private final int number;
    private final String name;
    private final LevelKind kind;
    private final SpecialLevelType specialLevelType;
    private final SpecialLevelConfig specialConfig;
    private final ChapterRuleset chapterRuleset;
    private final int numberOfRows;
    private final int numberOfColumns;
    private final int initialSunCount;
    private final int plantSlotCount;
    private final List<ZombieWave> zombieWaves;

    public Level(String name, int numberOfRows,
            int numberOfColumns, int initialSunCount,
            List<ZombieWave> zombieWaves) {
        this(1, name, LevelKind.NORMAL,
                SpecialLevelType.NONE,
                SpecialLevelConfig.none(),
                ChapterRuleset.NONE,
                numberOfRows, numberOfColumns,
                initialSunCount, Constants.DEFAULT_PLANT_SLOTS,
                zombieWaves);
    }

    public Level(int number, String name,
            LevelKind kind, int numberOfRows,
            int numberOfColumns,
            int initialSunCount,
            List<ZombieWave> zombieWaves) {
        this(number, name, kind,
                SpecialLevelType.NONE,
                SpecialLevelConfig.none(),
                ChapterRuleset.NONE,
                numberOfRows, numberOfColumns,
                initialSunCount, Constants.DEFAULT_PLANT_SLOTS,
                zombieWaves);
    }

    public Level(int number, String name,
            LevelKind kind,
            SpecialLevelType specialLevelType,
            SpecialLevelConfig specialConfig,
            int numberOfRows, int numberOfColumns,
            int initialSunCount,
            List<ZombieWave> zombieWaves) {
        this(number, name, kind, specialLevelType,
                specialConfig, ChapterRuleset.NONE,
                numberOfRows, numberOfColumns,
                initialSunCount, Constants.DEFAULT_PLANT_SLOTS,
                zombieWaves);
    }

    public Level(int number, String name,
            LevelKind kind,
            SpecialLevelType specialLevelType,
            SpecialLevelConfig specialConfig,
            ChapterRuleset chapterRuleset,
            int numberOfRows, int numberOfColumns,
            int initialSunCount,
            List<ZombieWave> zombieWaves) {
        this(number, name, kind, specialLevelType,
                specialConfig, chapterRuleset,
                numberOfRows, numberOfColumns, initialSunCount,
                Constants.DEFAULT_PLANT_SLOTS, zombieWaves);
    }

    public Level(int number, String name,
            LevelKind kind,
            SpecialLevelType specialLevelType,
            SpecialLevelConfig specialConfig,
            int numberOfRows, int numberOfColumns,
            int initialSunCount, int plantSlotCount,
            List<ZombieWave> zombieWaves) {
        this(number, name, kind, specialLevelType,
                specialConfig, ChapterRuleset.NONE,
                numberOfRows, numberOfColumns, initialSunCount,
                plantSlotCount, zombieWaves);
    }

    public Level(int number, String name,
            LevelKind kind,
            SpecialLevelType specialLevelType,
            SpecialLevelConfig specialConfig,
            ChapterRuleset chapterRuleset,
            int numberOfRows, int numberOfColumns,
            int initialSunCount, int plantSlotCount,
            List<ZombieWave> zombieWaves) {
        validate(number, name, kind,
                specialLevelType, specialConfig, chapterRuleset,
                numberOfRows, numberOfColumns,
                initialSunCount, plantSlotCount, zombieWaves);
        this.number = number;
        this.name = name;
        this.kind = kind;
        this.specialLevelType = specialLevelType;
        this.specialConfig = specialConfig;
        this.chapterRuleset = chapterRuleset;
        this.numberOfRows = numberOfRows;
        this.numberOfColumns = numberOfColumns;
        this.initialSunCount = initialSunCount;
        this.plantSlotCount = plantSlotCount;
        this.zombieWaves = Collections.unmodifiableList(
                new ArrayList<>(zombieWaves));
    }

    private static void validate(
            int number, String name, LevelKind kind,
            SpecialLevelType specialLevelType,
            SpecialLevelConfig specialConfig,
            ChapterRuleset chapterRuleset,
            int rows, int columns, int sun,
            int plantSlots, List<ZombieWave> waves) {
        if (number <= 0 || name == null
                || name.isBlank() || kind == null
                || specialLevelType == null
                || specialConfig == null
                || chapterRuleset == null) {
            throw new IllegalArgumentException(
                    "level identity values are invalid");
        }
        if (rows <= 0 || columns <= 0 || sun < 0
                || plantSlots <= 0
                || waves == null || waves.isEmpty()) {
            throw new IllegalArgumentException(
                    "level board or waves are invalid");
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
        return createGame(3);
    }

    public Game createGame(int difficultyLevel) {
        boolean startWavesImmediately = specialLevelType
                != SpecialLevelType.PLANT_WHAT_YOU_GET;
        return createGame(difficultyLevel, startWavesImmediately);
    }

    /**
     * Creates a level while allowing the graphical client to hold zombie waves
     * before they are visually implemented. Phase-1 callers keep using the
     * normal overload above, so their original wave-start behavior is intact.
     */
    public Game createGame(int difficultyLevel,
            boolean startWavesImmediately) {
        List<ZombieWave> freshWaves = new ArrayList<>();
        for (ZombieWave wave : zombieWaves) {
            freshWaves.add(
                    wave.forDifficulty(difficultyLevel));
        }

        Board board = new Board(numberOfRows, numberOfColumns);
        configureChapterBoard(board);
        Game game = new Game(
                board, null, initialSunCount, freshWaves,
                startWavesImmediately, chapterRuleset,
                difficultyLevel);
        configureSpecialRules(game);
        return game;
    }

    private void configureChapterBoard(Board board) {
        switch (chapterRuleset) {
            case ANCIENT_EGYPT:
                configureAncientEgyptBoard(board);
                break;
            case FROSTBITE_CAVES:
                configureFrostbiteBoard(board);
                break;
            case BIG_WAVE_BEACH:
                configureBigWaveBeachBoard(board);
                break;
            case DARK_AGES:
                configureDarkAgesBoard(board);
                break;
            case NONE:
                break;
            default:
                throw new IllegalStateException(
                        "unknown chapter ruleset");
        }
    }

    private void configureAncientEgyptBoard(Board board) {
        board.addGrave(new EntityPosition(0, 4));
        board.addGrave(new EntityPosition(2, 5));
        board.addGrave(new EntityPosition(4, 6));
    }

    private void configureFrostbiteBoard(Board board) {
        board.enableFrostbiteCavesRules();
        if (number >= 2) {
            board.setSliderTile(
                    new EntityPosition(1, 5), -1);
            board.setSliderTile(
                    new EntityPosition(3, 5), 1);
        }
    }

    private void configureDarkAgesBoard(Board board) {
        addNecromancyTile(board, 0, 5);
        addNecromancyTile(board, 2, 6);
        addNecromancyTile(board, 4, 5);
    }

    private void addNecromancyTile(Board board,
            int preferredRow, int preferredColumn) {
        int row = Math.min(preferredRow, numberOfRows - 1);
        int column = Math.min(preferredColumn, numberOfColumns - 1);
        board.setTileType(new EntityPosition(row, column),
                TileType.NECROMANCY);
    }

    private void configureBigWaveBeachBoard(Board board) {
        int initialWaterColumns = Math.min(3, numberOfColumns);
        int maximumWaterColumns = Math.min(5, numberOfColumns);
        List<EntityPosition> lowBeachPositions = createLowBeachPositions(initialWaterColumns,
                maximumWaterColumns);
        board.configureBigWaveBeach(initialWaterColumns,
                maximumWaterColumns, lowBeachPositions);
    }

    private List<EntityPosition> createLowBeachPositions(
            int initialWaterColumns, int maximumWaterColumns) {
        List<EntityPosition> positions = new ArrayList<>();
        int firstFloodedColumn = numberOfColumns - initialWaterColumns - 1;
        if (maximumWaterColumns > initialWaterColumns
                && firstFloodedColumn >= 0) {
            addLowBeachPosition(positions, 1, firstFloodedColumn);
            addLowBeachPosition(positions, 3, firstFloodedColumn);
        }
        int secondFloodedColumn = firstFloodedColumn - 1;
        if (maximumWaterColumns > initialWaterColumns + 1
                && secondFloodedColumn >= 0) {
            addLowBeachPosition(positions, 2, secondFloodedColumn);
        }
        return positions;
    }

    private void addLowBeachPosition(List<EntityPosition> positions,
            int preferredRow, int column) {
        int row = Math.min(preferredRow, numberOfRows - 1);
        EntityPosition position = new EntityPosition(row, column);
        if (!positions.contains(position)) {
            positions.add(position);
        }
    }

    private void configureSpecialRules(Game game) {
        switch (specialLevelType) {
            case CONVEYOR_BELT:
                if (kind == LevelKind.BOSS) {
                    game.enableBossConveyorBelt(
                            specialConfig.getPlantPool());
                } else {
                    game.enableConveyorBelt(
                            specialConfig.getPlantPool());
                }
                break;
            case LOCKED_PLANTS:
                game.enableLockedPlantsForcedLoadout(
                        specialConfig.getPlantPool());
                break;
            case SAVE_OUR_SEEDS:
                game.enableSaveOurSeeds(
                        specialConfig
                                .getProtectedPlants());
                break;
            case TIMED_WAR:
                configureTimedWar(game);
                break;
            case NIGHT_OPS:
                game.enableNightOps();
                break;
            case DEAD_LINE:
                game.enableDeadLine(
                        specialConfig
                                .getDeadLineColumn());
                break;
            case LOVE_YOUR_PLANTS:
                game.enableLoveYourPlants(
                        specialConfig
                                .getMaximumLostPlants());
                break;
            case PLANT_WHAT_YOU_GET:
                game.enablePlantWhatYouGet();
                break;
            case NONE:
                break;
            default:
                throw new IllegalStateException(
                        "unknown special level type");
        }
    }

    private void configureTimedWar(Game game) {
        TimedWarObjective objective = specialConfig.getTimedObjective();
        if (objective == TimedWarObjective.KILL_ZOMBIES) {
            if (specialConfig.getMinimumCollectedSun() > 0) {
                game.enableTimedWarZombieKillsAndSunCollection(
                        specialConfig.getDurationSeconds(),
                        specialConfig.getTarget(),
                        specialConfig.getMinimumCollectedSun());
            } else {
                game.enableTimedWarZombieKills(
                        specialConfig.getDurationSeconds(),
                        specialConfig.getTarget());
            }
        } else {
            game.enableTimedWarSunProduction(
                    specialConfig
                            .getDurationSeconds(),
                    specialConfig.getTarget());
        }
    }

    public Level withTimedWarObjective(
            TimedWarObjective objective) {
        if (objective == null) {
            throw new IllegalArgumentException(
                    "Timed War objective cannot be null");
        }
        if (specialLevelType != SpecialLevelType.TIMED_WAR) {
            throw new IllegalStateException(
                    "this level is not a Timed War level");
        }

        boolean sunProduction = objective == TimedWarObjective.PRODUCE_SUN;
        double duration = sunProduction
                ? TIMED_WAR_SUN_SECONDS
                : TIMED_WAR_KILL_SECONDS;
        int objectiveTarget = sunProduction
                ? TIMED_WAR_SUN_TARGET
                : TIMED_WAR_KILL_TARGET;
        String objectiveName = sunProduction
                ? "Sun Production"
                : "Zombie Elimination";

        return new Level(
                number,
                name + " [" + objectiveName + "]",
                kind,
                specialLevelType,
                SpecialLevelConfig.timedWar(
                        objective,
                        duration,
                        objectiveTarget),
                chapterRuleset,
                numberOfRows,
                numberOfColumns,
                initialSunCount,
                plantSlotCount,
                zombieWaves);
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

    public SpecialLevelConfig getSpecialConfig() {
        return specialConfig;
    }

    public ChapterRuleset getChapterRuleset() {
        return chapterRuleset;
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

    public int getPlantSlotCount() {
        return plantSlotCount;
    }

    public List<ZombieWave> getZombieWaves() {
        return zombieWaves;
    }
}
