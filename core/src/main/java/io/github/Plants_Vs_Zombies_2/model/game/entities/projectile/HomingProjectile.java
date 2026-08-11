package io.github.Plants_Vs_Zombies_2.model.game.entities.projectile;

import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.ProjectileEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.movement.HomingProjectileMovement;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

public class HomingProjectile extends Projectile {
    private static final double TARGET_REACHED_RADIUS = 0.08;

    private final Zombie lockedTarget;
    private final boolean plantFoodProjectile;

    public HomingProjectile(String sourcePlantName, double rowPosition,
            double columnPosition, Zombie lockedTarget,
            List<ProjectileEffect> effects, double speedTilesPerSecond,
            double maxLifetimeSeconds, boolean plantFoodProjectile) {
        super(sourcePlantName, rowPosition, columnPosition, effects,
                new HomingProjectileMovement(requireTarget(lockedTarget),
                        speedTilesPerSecond),
                Double.POSITIVE_INFINITY, maxLifetimeSeconds);
        this.lockedTarget = lockedTarget;
        this.plantFoodProjectile = plantFoodProjectile;
    }

    private static Zombie requireTarget(Zombie target) {
        if (target == null) {
            throw new IllegalArgumentException("lockedTarget cannot be null");
        }
        return target;
    }

    public boolean hasReachedTarget() {
        if (!isTargetAvailable()) {
            return false;
        }
        double rowDelta = lockedTarget.getLane() - getRowPosition();
        double columnDelta = lockedTarget.getColumnPosition() - getColumnPosition();
        return rowDelta * rowDelta + columnDelta * columnDelta <= TARGET_REACHED_RADIUS * TARGET_REACHED_RADIUS;
    }

    public boolean isTargetAvailable() {
        return !lockedTarget.isDead() && !lockedTarget.isRemoved()
                && !lockedTarget.isHypnotized();
    }

    public Zombie getLockedTarget() {
        return lockedTarget;
    }

    public boolean isPlantFoodProjectile() {
        return plantFoodProjectile;
    }
}
