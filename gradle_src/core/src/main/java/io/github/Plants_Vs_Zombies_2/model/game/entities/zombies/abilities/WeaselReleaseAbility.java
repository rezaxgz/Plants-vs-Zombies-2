package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;

/**
 * Weasel Hoarder releases its stored weasels in three damage stages:
 * below 75%, 50%, and 25% health. Death releases every remaining weasel.
 */
public class WeaselReleaseAbility extends ZombieAbility {
    private static final int RELEASE_STAGES = 3;

    private final int totalWeasels;
    private int releasedWeasels;
    private List<Zombie> lastSpawnedWeasels = Collections.emptyList();

    public WeaselReleaseAbility(int totalWeasels) {
        super(0.0);
        if (totalWeasels <= 0) {
            throw new IllegalArgumentException("totalWeasels must be positive");
        }
        this.totalWeasels = totalWeasels;
    }

    @Override
    public boolean tryUse(Zombie hoarder, Board board) {
        lastSpawnedWeasels = Collections.emptyList();
        if (hoarder == null || board == null || releasedWeasels >= totalWeasels) {
            return false;
        }

        int desiredReleased = desiredReleasedCount(hoarder);
        int amountToSpawn = desiredReleased - releasedWeasels;
        if (amountToSpawn <= 0) {
            return false;
        }

        List<Zombie> spawned = new ArrayList<>();
        for (int i = 0; i < amountToSpawn; i++) {
            double column = Math.max(0.0, Math.min(
                    board.getNumberOfColumns() - 0.001,
                    hoarder.getColumnPosition() - 0.08 * (i + 1)));
            Zombie weasel = new Zombie(ZombieType.WEASEL,
                    hoarder.getWaveNumber(), hoarder.getLane(), column);
            if (hoarder.isHypnotized()) {
                weasel.hypnotize();
            }
            board.addZombie(weasel);
            spawned.add(weasel);
        }

        releasedWeasels += spawned.size();
        lastSpawnedWeasels = Collections.unmodifiableList(spawned);
        return !spawned.isEmpty();
    }

    private int desiredReleasedCount(Zombie hoarder) {
        if (hoarder.isDead()) {
            return totalWeasels;
        }
        double healthFraction = hoarder.getMaximumHitPoints() == 0
                ? 0.0
                : (double) hoarder.getHitPoints()
                        / hoarder.getMaximumHitPoints();
        int completedStages = 0;
        if (healthFraction <= 0.75) {
            completedStages = 1;
        }
        if (healthFraction <= 0.50) {
            completedStages = 2;
        }
        if (healthFraction <= 0.25) {
            completedStages = 3;
        }
        return Math.min(totalWeasels,
                (totalWeasels * completedStages + RELEASE_STAGES - 1)
                        / RELEASE_STAGES);
    }

    public List<Zombie> getLastSpawnedWeasels() {
        return lastSpawnedWeasels;
    }

    public int getTotalWeasels() {
        return totalWeasels;
    }

    public int getReleasedWeasels() {
        return releasedWeasels;
    }
}
