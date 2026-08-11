package model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.game.special.ProtectedPlantSpec;
import model.game.special.TimedWarObjective;

/**
 * Immutable parameters used by one special level.
 */
public final class SpecialLevelConfig {
    private final List<String> plantPool;
    private final List<ProtectedPlantSpec>
            protectedPlants;
    private final TimedWarObjective timedObjective;
    private final double durationSeconds;
    private final int target;
    private final double deadLineColumn;
    private final int maximumLostPlants;

    private SpecialLevelConfig(
            List<String> plantPool,
            List<ProtectedPlantSpec> protectedPlants,
            TimedWarObjective timedObjective,
            double durationSeconds,
            int target,
            double deadLineColumn,
            int maximumLostPlants) {
        this.plantPool = immutableCopy(plantPool);
        this.protectedPlants =
                immutableCopy(protectedPlants);
        this.timedObjective = timedObjective;
        this.durationSeconds = durationSeconds;
        this.target = target;
        this.deadLineColumn = deadLineColumn;
        this.maximumLostPlants =
                maximumLostPlants;
    }

    private static <T> List<T> immutableCopy(
            List<T> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(
                new ArrayList<>(values));
    }

    public static SpecialLevelConfig none() {
        return new SpecialLevelConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                null, 0.0, 0, -1.0, 0);
    }

    public static SpecialLevelConfig plantPool(
            List<String> plantTypes) {
        if (plantTypes == null
                || plantTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "plant pool cannot be empty");
        }
        return new SpecialLevelConfig(
                plantTypes,
                Collections.emptyList(),
                null, 0.0, 0, -1.0, 0);
    }

    public static SpecialLevelConfig saveOurSeeds(
            List<ProtectedPlantSpec> plants) {
        if (plants == null || plants.isEmpty()) {
            throw new IllegalArgumentException(
                    "protected plants cannot be empty");
        }
        return new SpecialLevelConfig(
                Collections.emptyList(),
                plants,
                null, 0.0, 0, -1.0, 0);
    }

    public static SpecialLevelConfig timedWar(
            TimedWarObjective objective,
            double durationSeconds,
            int target) {
        if (objective == null
                || !Double.isFinite(durationSeconds)
                || durationSeconds <= 0.0
                || target <= 0) {
            throw new IllegalArgumentException(
                    "Timed War configuration is invalid");
        }
        return new SpecialLevelConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                objective, durationSeconds,
                target, -1.0, 0);
    }

    public static SpecialLevelConfig deadLine(
            double lineColumn) {
        if (!Double.isFinite(lineColumn)
                || lineColumn < 0.0) {
            throw new IllegalArgumentException(
                    "Dead Line column is invalid");
        }
        return new SpecialLevelConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                null, 0.0, 0, lineColumn, 0);
    }

    public static SpecialLevelConfig loveYourPlants(
            int maximumLostPlants) {
        if (maximumLostPlants <= 0) {
            throw new IllegalArgumentException(
                    "plant loss limit must be positive");
        }
        return new SpecialLevelConfig(
                Collections.emptyList(),
                Collections.emptyList(),
                null, 0.0, 0, -1.0,
                maximumLostPlants);
    }

    public List<String> getPlantPool() {
        return plantPool;
    }

    public List<ProtectedPlantSpec>
            getProtectedPlants() {
        return protectedPlants;
    }

    public TimedWarObjective getTimedObjective() {
        return timedObjective;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public int getTarget() {
        return target;
    }

    public double getDeadLineColumn() {
        return deadLineColumn;
    }

    public int getMaximumLostPlants() {
        return maximumLostPlants;
    }
}
