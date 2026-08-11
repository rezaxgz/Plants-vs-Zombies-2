package io.github.Plants_Vs_Zombies_2.model.game.entities.other;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.armor.ArmorType;

/**
 * Rolling barrel that releases two Imps when destroyed.
 */
public final class RollingBarrel extends PushedObstacle {
    public static final int DEFAULT_HIT_POINTS = ArmorType.BUCKET.getBaseHealth();

    private final int waveNumber;
    private final int impCount;
    private boolean impsReleased;

    public RollingBarrel(int lane, double columnPosition,
            int waveNumber, int impCount) {
        super("Rolling barrel", DEFAULT_HIT_POINTS,
                lane, columnPosition);
        if (waveNumber < 0 || impCount <= 0) {
            throw new IllegalArgumentException(
                    "barrel wave and Imp count are invalid");
        }
        this.waveNumber = waveNumber;
        this.impCount = impCount;
    }

    public List<Zombie> releaseImps() {
        if (!isDestroyed() || impsReleased) {
            return Collections.emptyList();
        }
        impsReleased = true;

        List<Zombie> imps = new ArrayList<>();
        for (int index = 0; index < impCount; index++) {
            double column = Math.max(0.0,
                    getColumnPosition() + 0.05 * index);
            imps.add(new Zombie(ZombieType.IMP,
                    waveNumber, getLane(), column));
        }
        return Collections.unmodifiableList(imps);
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public int getImpCount() {
        return impCount;
    }

    public boolean areImpsReleased() {
        return impsReleased;
    }
}
