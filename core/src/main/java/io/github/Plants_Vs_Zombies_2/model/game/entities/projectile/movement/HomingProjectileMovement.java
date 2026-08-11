package io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.movement;

import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.Projectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

public class HomingProjectileMovement extends ProjectileMovement {
    private final Zombie target;
    private final double speedTilesPerSecond;

    public HomingProjectileMovement(Zombie target, double speedTilesPerSecond) {
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null");
        }
        if (!Double.isFinite(speedTilesPerSecond) || speedTilesPerSecond <= 0.0) {
            throw new IllegalArgumentException("speed must be finite and positive");
        }
        this.target = target;
        this.speedTilesPerSecond = speedTilesPerSecond;
    }

    @Override
    public void move(Projectile projectile, float deltaSeconds) {
        if (projectile == null) {
            throw new IllegalArgumentException("projectile cannot be null");
        }
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
        if (target.isDead() || target.isRemoved()) {
            return;
        }
        double rowDelta = target.getLane() - projectile.getRowPosition();
        double columnDelta = target.getColumnPosition() - projectile.getColumnPosition();
        double distance = Math.sqrt(rowDelta * rowDelta + columnDelta * columnDelta);
        if (distance <= 0.000001) {
            return;
        }
        double travelDistance = Math.min(distance, speedTilesPerSecond * deltaSeconds);
        double scale = travelDistance / distance;
        projectile.translate(rowDelta * scale, columnDelta * scale);
    }
}
