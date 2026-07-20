package model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Constants;
import model.game.Board;
import model.game.ChapterRuleset;
import model.game.Game;
import model.game.ZombieWave;
import model.game.entities.EntityPosition;
import model.game.entities.zombies.ZombieType;
import model.game.special.TimedWarObjective;

/**
 * Replayable level definition. Every game receives fresh wave state.
 */
public final class Level {
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
        this.zombieWaves =
                Collections.unmodifiableList(
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
        List<ZombieWave> freshWaves =
                new ArrayList<>();
        for (ZombieWave wave : zombieWaves) {
            freshWaves.add(wave.copy());
        }

        boolean startWavesImmediately =
                specialLevelType
                        != SpecialLevelType
                                .PLANT_WHAT_YOU_GET;
        Board board = new Board(numberOfRows, numberOfColumns);
        configureChapterBoard(board);
        Game game = new Game(
                board, null, initialSunCount, freshWaves,
                startWavesImmediately, chapterRuleset);
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
            case NONE:
            case BIG_WAVE_BEACH:
            case DARK_AGES:
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
        if (number >= 3) {
            board.addFrozenZombie(
                    ZombieType.ICEAGE,
                    new EntityPosition(0, 6));
            board.addFrozenZombie(
                    ZombieType.ICEAGE_CONEHEAD,
                    new EntityPosition(4, 7));
        }
    }

    private void configureSpecialRules(Game game) {
        switch (specialLevelType) {
            case CONVEYOR_BELT:
                game.enableConveyorBelt(
                        specialConfig.getPlantPool());
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
        TimedWarObjective objective =
                specialConfig.getTimedObjective();
        if (objective
                == TimedWarObjective.KILL_ZOMBIES) {
            game.enableTimedWarZombieKills(
                    specialConfig
                            .getDurationSeconds(),
                    specialConfig.getTarget());
        } else {
            game.enableTimedWarSunProduction(
                    specialConfig
                            .getDurationSeconds(),
                    specialConfig.getTarget());
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
