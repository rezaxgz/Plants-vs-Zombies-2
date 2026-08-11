package model.game.entities.projectile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.projectile.effect.ChillEffect;
import model.game.entities.projectile.effect.DamageEffect;
import model.game.entities.projectile.effect.FireEffect;
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
    private final boolean torchwoodEligible;
    private final int originalImpactDamage;
    private final double sourceRowPosition;
    private final double sourceColumnPosition;

    private double rowPosition;
    private double columnPosition;
    private double previousRowPosition;
    private double previousColumnPosition;
    private double traveledDistance;
    private int torchwoodDamageMultiplier = 1;

    public Projectile(String sourcePlantName, double rowPosition, double columnPosition,
            List<ProjectileEffect> effects, ProjectileMovement movement,
            double maxTravelDistance) {
        this(sourcePlantName, rowPosition, columnPosition, effects, movement,
                maxTravelDistance, DEFAULT_MAX_LIFETIME_SECONDS, false);
    }

    public Projectile(String sourcePlantName, double rowPosition, double columnPosition,
            List<ProjectileEffect> effects, ProjectileMovement movement,
            double maxTravelDistance, double maxLifetimeSeconds) {
        this(sourcePlantName, rowPosition, columnPosition, effects, movement,
                maxTravelDistance, maxLifetimeSeconds, false);
    }

    public Projectile(String sourcePlantName, double rowPosition, double columnPosition,
            List<ProjectileEffect> effects, ProjectileMovement movement,
            double maxTravelDistance, double maxLifetimeSeconds,
            boolean torchwoodEligible) {
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
                || !Double.isFinite(maxLifetimeSeconds)
                || maxLifetimeSeconds <= 0.0) {
            throw new IllegalArgumentException("projectile range and lifetime are invalid");
        }
        this.sourcePlantName = sourcePlantName;
        this.sourceRowPosition = rowPosition;
        this.sourceColumnPosition = columnPosition;
        this.rowPosition = rowPosition;
        this.columnPosition = columnPosition;
        this.previousRowPosition = rowPosition;
        this.previousColumnPosition = columnPosition;
        this.effects = new ArrayList<>(effects);
        this.movement = movement;
        this.maxTravelDistance = maxTravelDistance;
        this.maxLifetimeSeconds = maxLifetimeSeconds;
        this.torchwoodEligible = torchwoodEligible;
        this.originalImpactDamage = findImpactDamage(effects);
    }

    private static int findImpactDamage(List<ProjectileEffect> effects) {
        for (ProjectileEffect effect : effects) {
            if (effect instanceof DamageEffect) {
                return ((DamageEffect) effect).getDamage();
            }
            if (effect instanceof FireEffect) {
                return ((FireEffect) effect).getDamage();
            }
        }
        return 0;
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
        double requestedDistance = Math.sqrt(rowDelta * rowDelta
                + columnDelta * columnDelta);
        double remainingDistance = Math.max(0.0,
                maxTravelDistance - traveledDistance);
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
        applyEffects(zombie);
        markForRemoval();
    }

    protected final void applyEffects(Zombie zombie) {
        if (zombie == null) {
            throw new IllegalArgumentException("zombie cannot be null");
        }
        for (ProjectileEffect effect : effects) {
            if (zombie.isDead()) {
                break;
            }
            effect.apply(zombie);
        }
    }

    public boolean igniteByTorchwood(int damageMultiplier) {
        if (!torchwoodEligible || originalImpactDamage <= 0
                || damageMultiplier <= torchwoodDamageMultiplier) {
            return false;
        }
        torchwoodDamageMultiplier = damageMultiplier;
        effects.clear();
        effects.add(new FireEffect(originalImpactDamage * damageMultiplier));
        return true;
    }

    public boolean hasFireEffect() {
        for (ProjectileEffect effect : effects) {
            if (effect instanceof FireEffect) {
                return true;
            }
        }
        return false;
    }

    public boolean hasChillEffect() {
        for (ProjectileEffect effect : effects) {
            if (effect instanceof ChillEffect) {
                return true;
            }
        }
        return false;
    }

    public int getImpactDamage() {
        long scaledDamage = (long) originalImpactDamage
                * torchwoodDamageMultiplier;
        return (int) Math.min(Integer.MAX_VALUE, scaledDamage);
    }

    public double getIntersectionParameter(double targetRow, double targetColumn,
            double collisionRadius) {
        double rowDelta = rowPosition - previousRowPosition;
        double columnDelta = columnPosition - previousColumnPosition;
        double segmentLengthSquared = rowDelta * rowDelta
                + columnDelta * columnDelta;
        if (segmentLengthSquared <= 0.0) {
            return Double.NaN;
        }
        double targetRowDelta = targetRow - previousRowPosition;
        double targetColumnDelta = targetColumn - previousColumnPosition;
        double parameter = (targetRowDelta * rowDelta
                + targetColumnDelta * columnDelta) / segmentLengthSquared;
        if (parameter < 0.0 || parameter > 1.0) {
            return Double.NaN;
        }
        double nearestRow = previousRowPosition + parameter * rowDelta;
        double nearestColumn = previousColumnPosition + parameter * columnDelta;
        double distanceSquared = square(targetRow - nearestRow)
                + square(targetColumn - nearestColumn);
        return distanceSquared <= collisionRadius * collisionRadius
                ? parameter : Double.NaN;
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

    public double getSourceRowPosition() {
        return sourceRowPosition;
    }

    public double getSourceColumnPosition() {
        return sourceColumnPosition;
    }

    public List<ProjectileEffect> getEffects() {
        return Collections.unmodifiableList(new ArrayList<>(effects));
    }

    public boolean isTorchwoodEligible() {
        return torchwoodEligible;
    }

    public int getTorchwoodDamageMultiplier() {
        return torchwoodDamageMultiplier;
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
