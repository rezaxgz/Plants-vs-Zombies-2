package io.github.Plants_Vs_Zombies_2.model.game.special;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.other.Sun;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * Tracks the special objectives used by Timed War levels.
 *
 * Zombie-elimination Timed War uses a rolling window: the player may satisfy
 * the kill requirement at any point in the level by getting enough kills in
 * the most recent window. Sun-production challenges keep their original
 * fixed-duration behavior.
 */
public final class TimedWarSystem implements java.io.Serializable {
    private static final double TIME_EPSILON = 0.000001;

    private final TimedWarObjective objective;
    private final double durationSeconds;
    private final int target;
    private final int collectedSunTarget;
    private final Set<Zombie> countedZombies;
    private final Set<Sun> countedSuns;
    private final Deque<Double> recentZombieKillTimes;

    private double elapsedSeconds;
    private int progress;
    private int collectedSunProgress;
    private boolean zombieKillRequirementMet;
    private TimedWarState state = TimedWarState.ACTIVE;

    private TimedWarSystem(
            TimedWarObjective objective,
            double durationSeconds,
            int target,
            int collectedSunTarget) {
        if (objective == null
                || !Double.isFinite(durationSeconds)
                || durationSeconds <= 0.0
                || target <= 0
                || collectedSunTarget < 0) {
            throw new IllegalArgumentException(
                    "Timed War settings are invalid");
        }
        this.objective = objective;
        this.durationSeconds = durationSeconds;
        this.target = target;
        this.collectedSunTarget = collectedSunTarget;
        countedZombies = Collections.newSetFromMap(
                new IdentityHashMap<>());
        countedSuns = Collections.newSetFromMap(
                new IdentityHashMap<>());
        recentZombieKillTimes = new ArrayDeque<>();
    }

    public static TimedWarSystem forZombieKills(
            double durationSeconds,
            int requiredKills) {
        return new TimedWarSystem(
                TimedWarObjective.KILL_ZOMBIES,
                durationSeconds, requiredKills, 0);
    }

    public static TimedWarSystem forZombieKillsAndCollectedSun(
            double durationSeconds,
            int requiredKills,
            int requiredCollectedSun) {
        if (requiredCollectedSun <= 0) {
            throw new IllegalArgumentException(
                    "collected sun target must be positive");
        }
        return new TimedWarSystem(
                TimedWarObjective.KILL_ZOMBIES,
                durationSeconds, requiredKills,
                requiredCollectedSun);
    }

    public static TimedWarSystem forSunProduction(
            double durationSeconds,
            int requiredSun) {
        return new TimedWarSystem(
                TimedWarObjective.PRODUCE_SUN,
                durationSeconds, requiredSun, 0);
    }

    public TimedWarState update(
            double deltaSeconds,
            List<Zombie> zombies,
            List<Sun> suns) {
        if (!Double.isFinite(deltaSeconds)
                || deltaSeconds < 0.0
                || zombies == null || suns == null) {
            throw new IllegalArgumentException(
                    "Timed War update values are invalid");
        }
        if (state == TimedWarState.FAILED) {
            return state;
        }

        if (objective == TimedWarObjective.KILL_ZOMBIES) {
            // Keep the rolling count live even after the requirement has been
            // latched as met; the HUD displays kills in the actual last
            // window for the rest of the level.
            countZombieKills(zombies);
            elapsedSeconds += deltaSeconds;
            pruneExpiredZombieKills();
            if (state == TimedWarState.ACTIVE) {
                updateZombieKillState();
            }
        } else if (state == TimedWarState.ACTIVE) {
            countProducedSun(suns);
            elapsedSeconds += deltaSeconds;
            if (progress >= target) {
                state = TimedWarState.SUCCEEDED;
            } else if (elapsedSeconds + TIME_EPSILON >= durationSeconds) {
                state = TimedWarState.FAILED;
            }
        }
        return state;
    }

    /**
     * Records newly processed zombie deaths immediately. The regular update
     * method also scans the tracked wave zombies, so terminal/self-test callers
     * that do not use the game death callback still behave correctly.
     */
    public void recordZombieDeaths(List<Zombie> zombies) {
        if (state == TimedWarState.FAILED
                || objective != TimedWarObjective.KILL_ZOMBIES
                || zombies == null) {
            return;
        }
        countZombieKills(zombies);
        pruneExpiredZombieKills();
        if (state == TimedWarState.ACTIVE) {
            updateZombieKillState();
        }
    }

    /** Records sun that the player actually collected, not starting/cheat sun. */
    public void recordCollectedSun(int amount) {
        if (state != TimedWarState.ACTIVE
                || collectedSunTarget <= 0
                || amount <= 0) {
            return;
        }
        if (collectedSunProgress > Integer.MAX_VALUE - amount) {
            collectedSunProgress = Integer.MAX_VALUE;
        } else {
            collectedSunProgress += amount;
        }
        updateZombieKillState();
    }

    private void countZombieKills(List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (zombie != null
                    && zombie.isDead()
                    && countedZombies.add(zombie)) {
                recentZombieKillTimes.addLast(elapsedSeconds);
            }
        }
    }

    private void pruneExpiredZombieKills() {
        double oldestAllowed = elapsedSeconds - durationSeconds;
        while (!recentZombieKillTimes.isEmpty()
                && recentZombieKillTimes.peekFirst()
                        < oldestAllowed - TIME_EPSILON) {
            recentZombieKillTimes.removeFirst();
        }
        progress = recentZombieKillTimes.size();
    }

    private void updateZombieKillState() {
        if (objective != TimedWarObjective.KILL_ZOMBIES) {
            return;
        }
        if (!zombieKillRequirementMet
                && recentZombieKillTimes.size() >= target) {
            zombieKillRequirementMet = true;
        }
        progress = recentZombieKillTimes.size();
        if (zombieKillRequirementMet
                && (collectedSunTarget <= 0
                        || collectedSunProgress >= collectedSunTarget)) {
            state = TimedWarState.SUCCEEDED;
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
            String killObjective = "kill " + target
                    + " zombies within any "
                    + formatDuration(durationSeconds)
                    + "-second window";
            if (collectedSunTarget > 0) {
                return killObjective + " and collect at least "
                        + collectedSunTarget + " sun";
            }
            return killObjective;
        }
        return "produce " + target
                + " sun in "
                + formatDuration(durationSeconds) + " seconds";
    }

    public String describeProgress() {
        if (objective == TimedWarObjective.KILL_ZOMBIES) {
            String killProgress = getRecentZombieKills()
                    + "/" + target + " kills in the last "
                    + formatDuration(durationSeconds) + " seconds";
            if (zombieKillRequirementMet) {
                killProgress += " (met)";
            }
            if (collectedSunTarget > 0) {
                return killProgress + ", collected sun "
                        + collectedSunProgress + "/"
                        + collectedSunTarget;
            }
            return killProgress;
        }
        return progress + "/" + target
                + " " + objective;
    }

    public String describeUnmetRequirements() {
        if (objective == TimedWarObjective.KILL_ZOMBIES) {
            StringBuilder unmet = new StringBuilder();
            if (!zombieKillRequirementMet) {
                unmet.append("you did not defeat ")
                        .append(target)
                        .append(" zombies within any ")
                        .append(formatDuration(durationSeconds))
                        .append("-second window");
            }
            if (collectedSunTarget > 0
                    && collectedSunProgress < collectedSunTarget) {
                if (unmet.length() > 0) {
                    unmet.append(" and ");
                }
                unmet.append("you collected only ")
                        .append(collectedSunProgress)
                        .append(" of the required ")
                        .append(collectedSunTarget)
                        .append(" sun");
            }
            return unmet.toString();
        }
        if (progress < target) {
            return "you reached only " + progress + "/" + target
                    + " " + objective;
        }
        return "";
    }

    private static String formatDuration(double seconds) {
        if (Math.abs(seconds - Math.rint(seconds)) < TIME_EPSILON) {
            return Integer.toString((int) Math.rint(seconds));
        }
        return Double.toString(seconds);
    }

    public TimedWarObjective getObjective() {
        return objective;
    }

    public int getProgress() {
        if (objective == TimedWarObjective.KILL_ZOMBIES
                && zombieKillRequirementMet) {
            return target;
        }
        return progress;
    }

    public int getTarget() {
        return target;
    }

    public int getRecentZombieKills() {
        return objective == TimedWarObjective.KILL_ZOMBIES
                ? recentZombieKillTimes.size()
                : 0;
    }

    public boolean isZombieKillRequirementMet() {
        return zombieKillRequirementMet;
    }

    public int getCollectedSunProgress() {
        return collectedSunProgress;
    }

    public int getCollectedSunTarget() {
        return collectedSunTarget;
    }

    public boolean isCollectedSunRequirementMet() {
        return collectedSunTarget > 0
                && collectedSunProgress >= collectedSunTarget;
    }

    public double getWindowSeconds() {
        return objective == TimedWarObjective.KILL_ZOMBIES
                ? durationSeconds
                : 0.0;
    }

    public double getRemainingSeconds() {
        if (objective == TimedWarObjective.KILL_ZOMBIES) {
            if (zombieKillRequirementMet) {
                return 0.0;
            }
            if (recentZombieKillTimes.isEmpty()) {
                return durationSeconds;
            }
            return Math.max(0.0,
                    durationSeconds - (elapsedSeconds
                            - recentZombieKillTimes.peekFirst()));
        }
        return Math.max(
                0.0,
                durationSeconds - elapsedSeconds);
    }

    public TimedWarState getState() {
        return state;
    }
}
