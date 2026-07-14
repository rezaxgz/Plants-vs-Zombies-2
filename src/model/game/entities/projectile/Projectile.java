package model.game.entities.projectile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.projectile.effect.ProjectileEffect;
import model.game.entities.projectile.movement.ProjectileMovement;
import model.game.entities.zombies.Zombie;

public class Projectile extends Entity {
    private static final double DEFAULT_MAX_LIFETIME_SECONDS = 5.0;

    private final String sourcePlantName;
    private final List<ProjectileEffect> effects;
    private final ProjectileMovement movement;
    private final double maxTravelDistance;
    private final double maxLifetimeSeconds;

    private double rowPosition;
    private double columnPosition;
    private double previousRowPosition;
    private double previousColumnPosition;
    private double traveledDistance;

    public Projectile(String sourcePlantName, double rowPosition, double columnPosition,
            List<ProjectileEffect> effects, ProjectileMovement movement,
            double maxTravelDistance) {
        this(sourcePlantName, rowPosition, columnPosition, effects, movement,
                maxTravelDistance, DEFAULT_MAX_LIFETIME_SECONDS);
    }

    public Projectile(String sourcePlantName, double rowPosition, double columnPosition,
            List<ProjectileEffect> effects, ProjectileMovement movement,
            double maxTravelDistance, double maxLifetimeSeconds) {
        super(toEntityPosition(rowPosition, columnPosition));
        if (sourcePlantName == null || sourcePlantName.isBlank()) {
            throw new IllegalArgumentException("sourcePlantName cannot be blank");
        }
        if (!Double.isFinite(rowPosition) || !Double.isFinite(columnPosition)) {
            throw new IllegalArgumentException("projectile position must be finite");
        }
        if (effects == null || effects.isEmpty() || movement == null) {
            throw new IllegalArgumentException("projectile effects and movement are required");
        }
        if (Double.isNaN(maxTravelDistance) || maxTravelDistance <= 0.0
                || !Double.isFinite(maxLifetimeSeconds) || maxLifetimeSeconds <= 0.0) {
            throw new IllegalArgumentException("projectile range and lifetime are invalid");
        }
        this.sourcePlantName = sourcePlantName;
        this.rowPosition = rowPosition;
        this.columnPosition = columnPosition;
        this.previousRowPosition = rowPosition;
        this.previousColumnPosition = columnPosition;
        this.effects = Collections.unmodifiableList(new ArrayList<>(effects));
        this.movement = movement;
        this.maxTravelDistance = maxTravelDistance;
        this.maxLifetimeSeconds = maxLifetimeSeconds;
    }

    private static EntityPosition toEntityPosition(double row, double column) {
        int safeRow = Math.max(0, (int) Math.floor(row));
        int safeColumn = Math.max(0, (int) Math.floor(column));
        return new EntityPosition(safeRow, safeColumn);
    }

    @Override
    public void update(float deltaSeconds) {
        if (isRemoved()) {
            return;
        }
        super.update(deltaSeconds);
        previousRowPosition = rowPosition;
        previousColumnPosition = columnPosition;
        movement.move(this, deltaSeconds);
        setEntityPosition(toEntityPosition(rowPosition, columnPosition));
    }

    public void translate(double rowDelta, double columnDelta) {
        if (!Double.isFinite(rowDelta) || !Double.isFinite(columnDelta)) {
            throw new IllegalArgumentException("projectile movement delta must be finite");
        }
        double requestedDistance = Math.sqrt(rowDelta * rowDelta + columnDelta * columnDelta);
        double remainingDistance = Math.max(0.0, maxTravelDistance - traveledDistance);
        if (remainingDistance <= 0.0) {
            return;
        }
        if (requestedDistance > remainingDistance) {
            double scale = remainingDistance / requestedDistance;
            rowDelta *= scale;
            columnDelta *= scale;
            requestedDistance = remainingDistance;
        }
        rowPosition += rowDelta;
        columnPosition += columnDelta;
        traveledDistance += requestedDistance;
    }

    public void hit(Zombie zombie) {
        if (zombie == null || isRemoved()) {
            return;
        }
        for (ProjectileEffect effect : effects) {
            if (zombie.isDead()) {
                break;
            }
            effect.apply(zombie);
        }
        markForRemoval();
    }

    public double getIntersectionParameter(double targetRow, double targetColumn,
            double collisionRadius) {
        double rowDelta = rowPosition - previousRowPosition;
        double columnDelta = columnPosition - previousColumnPosition;
        double segmentLengthSquared = rowDelta * rowDelta + columnDelta * columnDelta;
        if (segmentLengthSquared <= 0.0) {
            return Double.NaN;
        }
        double targetRowDelta = targetRow - previousRowPosition;
        double targetColumnDelta = targetColumn - previousColumnPosition;
        double parameter = (targetRowDelta * rowDelta + targetColumnDelta * columnDelta)
                / segmentLengthSquared;
        if (parameter < 0.0 || parameter > 1.0) {
            return Double.NaN;
        }
        double nearestRow = previousRowPosition + parameter * rowDelta;
        double nearestColumn = previousColumnPosition + parameter * columnDelta;
        double distanceSquared = square(targetRow - nearestRow)
                + square(targetColumn - nearestColumn);
        return distanceSquared <= collisionRadius * collisionRadius ? parameter : Double.NaN;
    }

    private static double square(double value) {
        return value * value;
    }

    public boolean hasExpired() {
        return traveledDistance + 0.000001 >= maxTravelDistance
                || getElapsedSeconds() + 0.000001 >= maxLifetimeSeconds;
    }

    public String getSourcePlantName() {
        return sourcePlantName;
    }

    public List<ProjectileEffect> getEffects() {
        return effects;
    }

    public double getRowPosition() {
        return rowPosition;
    }

    public double getColumnPosition() {
        return columnPosition;
    }

    public double getPreviousRowPosition() {
        return previousRowPosition;
    }

    public double getPreviousColumnPosition() {
        return previousColumnPosition;
    }
}
