package io.github.Plants_Vs_Zombies_2.model.game.entities.projectile;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

public final class BouncingGrape extends Entity {
    private static final double MAX_LIFETIME_SECONDS = 5.0;
    private static final double SPEED_TILES_PER_SECOND = 4.0;

    private final int damage;
    private final int maximumHits;
    private final String sourcePlantName;
    private final Set<Zombie> hitZombies = Collections.newSetFromMap(new IdentityHashMap<>());

    private double rowPosition;
    private double columnPosition;
    private double previousRowPosition;
    private double previousColumnPosition;
    private double rowDirection;
    private double columnDirection;
    private int hitCount;

    public BouncingGrape(double rowPosition, double columnPosition,
            double rowDirection, double columnDirection,
            int damage, int maximumHits, String sourcePlantName) {
        super(toEntityPosition(rowPosition, columnPosition));
        if (!Double.isFinite(rowPosition) || !Double.isFinite(columnPosition)
                || !Double.isFinite(rowDirection) || !Double.isFinite(columnDirection)) {
            throw new IllegalArgumentException("grape position and direction must be finite");
        }
        if (rowDirection == 0.0 && columnDirection == 0.0) {
            throw new IllegalArgumentException("grape direction cannot be zero");
        }
        if (damage < 0 || maximumHits <= 0) {
            throw new IllegalArgumentException("grape damage and hit count are invalid");
        }
        if (sourcePlantName == null || sourcePlantName.isBlank()) {
            throw new IllegalArgumentException(
                    "sourcePlantName cannot be blank");
        }
        double magnitude = Math.sqrt(rowDirection * rowDirection
                + columnDirection * columnDirection);
        this.rowPosition = rowPosition;
        this.columnPosition = columnPosition;
        this.previousRowPosition = rowPosition;
        this.previousColumnPosition = columnPosition;
        this.rowDirection = rowDirection / magnitude;
        this.columnDirection = columnDirection / magnitude;
        this.damage = damage;
        this.maximumHits = maximumHits;
        this.sourcePlantName = sourcePlantName;
    }

    @Override
    public void update(float deltaSeconds) {
        if (isRemoved()) {
            return;
        }
        super.update(deltaSeconds);
        previousRowPosition = rowPosition;
        previousColumnPosition = columnPosition;
        rowPosition += rowDirection * SPEED_TILES_PER_SECOND * deltaSeconds;
        columnPosition += columnDirection * SPEED_TILES_PER_SECOND * deltaSeconds;
        setEntityPosition(toEntityPosition(rowPosition, columnPosition));
        if (getElapsedSeconds() + 0.000001 >= MAX_LIFETIME_SECONDS) {
            markForRemoval();
        }
    }

    public void bounceInside(int rowCount, int columnCount) {
        double maximumRow = rowCount - 1.0;
        double maximumColumn = columnCount - 1.0;
        if (rowPosition < 0.0) {
            rowPosition = -rowPosition;
            rowDirection = Math.abs(rowDirection);
        } else if (rowPosition > maximumRow) {
            rowPosition = maximumRow - (rowPosition - maximumRow);
            rowDirection = -Math.abs(rowDirection);
        }
        if (columnPosition < 0.0) {
            columnPosition = -columnPosition;
            columnDirection = Math.abs(columnDirection);
        } else if (columnPosition > maximumColumn) {
            columnPosition = maximumColumn - (columnPosition - maximumColumn);
            columnDirection = -Math.abs(columnDirection);
        }
        rowPosition = Math.max(0.0, Math.min(maximumRow, rowPosition));
        columnPosition = Math.max(0.0, Math.min(maximumColumn, columnPosition));
        setEntityPosition(toEntityPosition(rowPosition, columnPosition));
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

    public boolean canHit(Zombie zombie) {
        return zombie != null && !zombie.isDead() && !hitZombies.contains(zombie);
    }

    public void hit(Zombie zombie) {
        if (!canHit(zombie) || isRemoved()) {
            return;
        }
        hitZombies.add(zombie);
        zombie.recordDamageSourcePlant(sourcePlantName);
        zombie.takeDamage(damage);
        hitCount++;
        if (hitCount >= maximumHits) {
            markForRemoval();
        }
    }

    private static double square(double value) {
        return value * value;
    }

    private static EntityPosition toEntityPosition(double row, double column) {
        int safeRow = Math.max(0, (int) Math.floor(row));
        int safeColumn = Math.max(0, (int) Math.floor(column));
        return new EntityPosition(safeRow, safeColumn);
    }

    public double getRowPosition() {
        return rowPosition;
    }

    public double getColumnPosition() {
        return columnPosition;
    }

    public int getDamage() {
        return damage;
    }

    public int getMaximumHits() {
        return maximumHits;
    }
}
