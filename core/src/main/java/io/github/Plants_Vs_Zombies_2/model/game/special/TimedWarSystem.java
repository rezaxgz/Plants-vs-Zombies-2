package io.github.Plants_Vs_Zombies_2.model.game.special;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.other.Sun;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * Counts zombie kills or plant-produced sun before a timer expires.
 */
public final class TimedWarSystem {
    private static final double TIME_EPSILON = 0.000001;

    private final TimedWarObjective objective;
    private final double durationSeconds;
    private final int target;
    private final Set<Zombie> countedZombies;
    private final Set<Sun> countedSuns;

    private double elapsedSeconds;
    private int progress;
    private TimedWarState state = TimedWarState.ACTIVE;

    private TimedWarSystem(
            TimedWarObjective objective,
            double durationSeconds,
            int target) {
        if (objective == null
                || !Double.isFinite(durationSeconds)
                || durationSeconds <= 0.0
                || target <= 0) {
            throw new IllegalArgumentException(
                    "Timed War settings are invalid");
        }
        this.objective = objective;
        this.durationSeconds = durationSeconds;
        this.target = target;
        countedZombies = Collections.newSetFromMap(
                new IdentityHashMap<>());
        countedSuns = Collections.newSetFromMap(
                new IdentityHashMap<>());
    }

    public static TimedWarSystem forZombieKills(
            double durationSeconds,
            int requiredKills) {
        return new TimedWarSystem(
                TimedWarObjective.KILL_ZOMBIES,
                durationSeconds, requiredKills);
    }

    public static TimedWarSystem forSunProduction(
            double durationSeconds,
            int requiredSun) {
        return new TimedWarSystem(
                TimedWarObjective.PRODUCE_SUN,
                durationSeconds, requiredSun);
    }

    public TimedWarState update(
            double deltaSeconds,
            List<Zombie> zombies,
            List<Sun> suns) {
        if (state != TimedWarState.ACTIVE) {
            return state;
        }
        if (!Double.isFinite(deltaSeconds)
                || deltaSeconds < 0.0
                || zombies == null || suns == null) {
            throw new IllegalArgumentException(
                    "Timed War update values are invalid");
        }

        countProgress(zombies, suns);
        elapsedSeconds += deltaSeconds;

        if (progress >= target) {
            state = TimedWarState.SUCCEEDED;
        } else if (elapsedSeconds + TIME_EPSILON >= durationSeconds) {
            state = TimedWarState.FAILED;
        }
        return state;
    }

    private void countProgress(
            List<Zombie> zombies,
            List<Sun> suns) {
        if (objective == TimedWarObjective.KILL_ZOMBIES) {
            countZombieKills(zombies);
        } else {
            countProducedSun(suns);
        }
    }

    private void countZombieKills(
            List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (zombie != null
                    && zombie.isDead()
                    && countedZombies.add(zombie)) {
                progress++;
            }
        }
    }

    private void countProducedSun(List<Sun> suns) {
        for (Sun sun : suns) {
            if (sun != null
                    && sun.isPersistent()
                    && countedSuns.add(sun)) {
                progress += sun.getSunAmount();
            }
        }
    }

    public String describeObjective() {
        if (objective == TimedWarObjective.KILL_ZOMBIES) {
            return "kill " + target
                    + " zombies in "
                    + durationSeconds + " seconds";
        }
        return "produce " + target
                + " sun in "
                + durationSeconds + " seconds";
    }

    public String describeProgress() {
        return progress + "/" + target
                + " " + objective;
    }

    public TimedWarObjective getObjective() {
        return objective;
    }

    public int getProgress() {
        return progress;
    }

    public int getTarget() {
        return target;
    }

    public double getRemainingSeconds() {
        return Math.max(
                0.0,
                durationSeconds - elapsedSeconds);
    }

    public TimedWarState getState() {
        return state;
    }
}
