package io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.movement;

import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.Projectile;

public final class BouncingProjectileMovement extends ProjectileMovement {
    private static final double DIAGONAL_COMPONENT = 1.0 / Math.sqrt(2.0);

    private final double speedTilesPerSecond;
    private final double minimumRow;
    private final double maximumRow;
    private int rowDirection;

    public BouncingProjectileMovement(double speedTilesPerSecond, int boardRows,
            boolean initiallyUpward) {
        if (!Double.isFinite(speedTilesPerSecond) || speedTilesPerSecond <= 0.0) {
            throw new IllegalArgumentException("speedTilesPerSecond must be positive and finite");
        }
        if (boardRows <= 0) {
            throw new IllegalArgumentException("boardRows must be positive");
        }
        this.speedTilesPerSecond = speedTilesPerSecond;
        this.minimumRow = 0.0;
        this.maximumRow = boardRows - 1.0;
        this.rowDirection = initiallyUpward ? -1 : 1;
    }

    @Override
    public void move(Projectile projectile, float deltaSeconds) {
        if (projectile == null) {
            throw new IllegalArgumentException("projectile cannot be null");
        }
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
        double distance = speedTilesPerSecond * deltaSeconds;
        double columnDelta = distance * DIAGONAL_COMPONENT;
        double rowDelta = distance * DIAGONAL_COMPONENT * rowDirection;
        double requestedRow = projectile.getRowPosition() + rowDelta;
        if (requestedRow < minimumRow || requestedRow > maximumRow) {
            rowDirection = -rowDirection;
            rowDelta = distance * DIAGONAL_COMPONENT * rowDirection;
        }
        projectile.translate(rowDelta, columnDelta);
    }
}
