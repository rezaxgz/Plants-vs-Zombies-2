package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.game.entities.zombies.ZombieType;

public final class ZombieWave {
    private final int difficulty;
    private final List<ZombieType> zombieTypes;

    public ZombieWave(int difficulty, List<ZombieType> zombieTypes) {
        if (difficulty <= 0) {
            throw new IllegalArgumentException("difficulty must be positive");
        }
        if (zombieTypes == null || zombieTypes.isEmpty()) {
            throw new IllegalArgumentException("zombieTypes cannot be empty");
        }
        this.difficulty = difficulty;
        this.zombieTypes = Collections.unmodifiableList(new ArrayList<>(zombieTypes));
    }

    public static ZombieWave basicWave(int difficulty, boolean finalWave) {
        if (difficulty <= 0 || difficulty % ZombieType.BASIC.getWavePointCost() != 0) {
            throw new IllegalArgumentException("difficulty must be a positive multiple of the basic zombie cost");
        }

        int zombieCount = difficulty / ZombieType.BASIC.getWavePointCost();
        List<ZombieType> types = new ArrayList<>();
        if (finalWave) {
            types.add(ZombieType.FLAG);
        }
        while (types.size() < zombieCount) {
            types.add(ZombieType.BASIC);
        }
        return new ZombieWave(difficulty, types);
    }

    public int getDifficulty() {
        return difficulty;
    }

    public List<ZombieType> getZombieTypes() {
        return zombieTypes;
    }
}
