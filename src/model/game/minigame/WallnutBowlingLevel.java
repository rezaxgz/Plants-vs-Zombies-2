package model.game.minigame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.game.ZombieWave;
import model.game.entities.zombies.ZombieType;

/**
 * Three progressively harder Wall-nut Bowling levels.
 */
public enum WallnutBowlingLevel {
    LEVEL_ONE(1, "Opening Roll", 2,
            List.of(
                    wave(false, ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC),
                    wave(false, ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.CONEHEAD),
                    wave(true, ZombieType.FLAG, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.CONEHEAD, ZombieType.CONEHEAD)),
            conveyorPool(6, 3, 2)),
    LEVEL_TWO(2, "Split Decision", 2,
            List.of(
                    wave(false, ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.CONEHEAD),
                    wave(false, ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.CONEHEAD, ZombieType.CONEHEAD),
                    wave(false, ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.CONEHEAD, ZombieType.BUCKETHEAD),
                    wave(true, ZombieType.FLAG, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.CONEHEAD,
                            ZombieType.CONEHEAD, ZombieType.BUCKETHEAD)),
            conveyorPool(7, 2, 1)),
    LEVEL_THREE(3, "Championship Lane", 2,
            List.of(
                    wave(false, ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.CONEHEAD, ZombieType.CONEHEAD),
                    wave(false, ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.CONEHEAD,
                            ZombieType.CONEHEAD, ZombieType.CONEHEAD,
                            ZombieType.BUCKETHEAD),
                    wave(false, ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.CONEHEAD, ZombieType.CONEHEAD,
                            ZombieType.BUCKETHEAD, ZombieType.BUCKETHEAD),
                    wave(false, ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BUCKETHEAD, ZombieType.BUCKETHEAD,
                            ZombieType.BRICKHEAD),
                    wave(true, ZombieType.FLAG, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.BASIC,
                            ZombieType.BASIC, ZombieType.CONEHEAD,
                            ZombieType.CONEHEAD, ZombieType.CONEHEAD,
                            ZombieType.BUCKETHEAD, ZombieType.BUCKETHEAD,
                            ZombieType.BRICKHEAD)),
            conveyorPool(9, 2, 1));

    public static final int LEVEL_COUNT = 3;

    private final int number;
    private final String name;
    private final int redLineColumn;
    private final List<ZombieWave> waves;
    private final List<String> conveyorPlantTypes;

    WallnutBowlingLevel(int number, String name, int redLineColumn,
            List<ZombieWave> waves, List<String> conveyorPlantTypes) {
        this.number = number;
        this.name = name;
        this.redLineColumn = redLineColumn;
        this.waves = Collections.unmodifiableList(new ArrayList<>(waves));
        this.conveyorPlantTypes = Collections.unmodifiableList(
                new ArrayList<>(conveyorPlantTypes));
    }

    private static ZombieWave wave(boolean finalWave,
            ZombieType... zombieTypes) {
        List<ZombieType> types = List.of(zombieTypes);
        int difficulty = 0;
        for (ZombieType type : types) {
            difficulty += type.getWavePointCost();
        }
        return new ZombieWave(types, difficulty, finalWave);
    }

    private static List<String> conveyorPool(int normalCount,
            int explosiveCount, int largeCount) {
        List<String> types = new ArrayList<>();
        addRepeated(types, BowlingWallnutType.NORMAL, normalCount);
        addRepeated(types, BowlingWallnutType.EXPLOSIVE, explosiveCount);
        addRepeated(types, BowlingWallnutType.LARGE, largeCount);
        return types;
    }

    private static void addRepeated(List<String> result,
            BowlingWallnutType type, int count) {
        for (int index = 0; index < count; index++) {
            result.add(type.getDisplayName());
        }
    }

    public List<ZombieWave> createWaves() {
        List<ZombieWave> copies = new ArrayList<>();
        for (ZombieWave wave : waves) {
            copies.add(wave.copy());
        }
        return copies;
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public int getRedLineColumn() {
        return redLineColumn;
    }

    public List<String> getConveyorPlantTypes() {
        return conveyorPlantTypes;
    }

    public int getWaveCount() {
        return waves.size();
    }

    public int getZombieCount() {
        int count = 0;
        for (ZombieWave wave : waves) {
            count += wave.getZombieTypes().size();
        }
        return count;
    }

    public static WallnutBowlingLevel find(int levelNumber) {
        for (WallnutBowlingLevel level : values()) {
            if (level.number == levelNumber) {
                return level;
            }
        }
        return null;
    }
}
