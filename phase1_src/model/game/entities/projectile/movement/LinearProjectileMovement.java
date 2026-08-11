package model.game.entities.projectile.movement;

import model.game.entities.projectile.Projectile;

public final class LinearProjectileMovement extends ProjectileMovement {
    private final ProjectileDirection direction;
    private final double speedTilesPerSecond;

    public LinearProjectileMovement(ProjectileDirection direction, double speedTilesPerSecond) {
        if (direction == null) {
            throw new IllegalArgumentException("direction cannot be null");
        }
        if (!Double.isFinite(speedTilesPerSecond) || speedTilesPerSecond <= 0.0) {
            throw new IllegalArgumentException("speedTilesPerSecond must be finite and positive");
        }
        this.direction = direction;
        this.speedTilesPerSecond = speedTilesPerSecond;
    }

    @Override
    public void move(Projectile projectile, float deltaSeconds) {
        if (projectile == null) {
            throw new IllegalArgumentException("projectile cannot be null");
        }
        projectile.translate(
                direction.getRowComponent() * speedTilesPerSecond * deltaSeconds,
                direction.getColumnComponent() * speedTilesPerSecond * deltaSeconds);
    }

    public ProjectileDirection getDirection() {
        return direction;
    }
}
