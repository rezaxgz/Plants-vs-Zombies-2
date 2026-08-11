package model.game.entities.projectile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.game.entities.projectile.effect.ProjectileEffect;
import model.game.entities.projectile.movement.LobbedProjectileMovement;
import model.game.entities.zombies.Zombie;

public final class LobbedProjectile extends Projectile {
    private static final double MAX_ARC_TRAVEL_DISTANCE = 100.0;
    private static final double DEFAULT_FLIGHT_DURATION_SECONDS = 1.0;
    private static final double DEFAULT_PEAK_HEIGHT = 1.5;

    private final LobbedProjectileMovement arcMovement;
    private final List<ProjectileEffect> splashEffects;
    private final double splashRadiusTiles;

    public LobbedProjectile(String sourcePlantName, double rowPosition,
            double columnPosition, Zombie lockedTarget,
            List<ProjectileEffect> directEffects,
            List<ProjectileEffect> splashEffects, double splashRadiusTiles) {
        this(sourcePlantName, rowPosition, columnPosition, lockedTarget,
                directEffects, splashEffects, splashRadiusTiles,
                new LobbedProjectileMovement(rowPosition, columnPosition,
                        lockedTarget, DEFAULT_FLIGHT_DURATION_SECONDS,
                        DEFAULT_PEAK_HEIGHT));
    }

    private LobbedProjectile(String sourcePlantName, double rowPosition,
            double columnPosition, Zombie lockedTarget,
            List<ProjectileEffect> directEffects,
            List<ProjectileEffect> splashEffects, double splashRadiusTiles,
            LobbedProjectileMovement movement) {
        super(sourcePlantName, rowPosition, columnPosition, directEffects,
                movement, MAX_ARC_TRAVEL_DISTANCE,
                DEFAULT_FLIGHT_DURATION_SECONDS + 1.0);
        if (lockedTarget == null) {
            throw new IllegalArgumentException("lockedTarget cannot be null");
        }
        if (splashEffects == null) {
            throw new IllegalArgumentException("splashEffects cannot be null");
        }
        if (!Double.isFinite(splashRadiusTiles) || splashRadiusTiles < 0.0) {
            throw new IllegalArgumentException("splashRadiusTiles is invalid");
        }
        this.arcMovement = movement;
        this.splashEffects = Collections.unmodifiableList(new ArrayList<>(splashEffects));
        this.splashRadiusTiles = splashRadiusTiles;
    }

    @Override
    public boolean hasExpired() {
        return hasLanded();
    }

    public boolean hasLanded() {
        return arcMovement.hasLanded();
    }

    public double getAltitude() {
        return arcMovement.getAltitude();
    }

    public double getLandingRow() {
        return arcMovement.getLandingRow();
    }

    public double getLandingColumn() {
        return arcMovement.getLandingColumn();
    }

    public Zombie getLockedTarget() {
        return arcMovement.getLockedTarget();
    }

    public List<ProjectileEffect> getSplashEffects() {
        return splashEffects;
    }

    public double getSplashRadiusTiles() {
        return splashRadiusTiles;
    }
}
