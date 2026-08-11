package model.game.entities.projectile.effect;

import model.game.entities.zombies.Zombie;

public final class KnockbackEffect extends ProjectileEffect {
    private final double distanceTiles;

    public KnockbackEffect(double distanceTiles) {
        if (!Double.isFinite(distanceTiles) || distanceTiles < 0.0) {
            throw new IllegalArgumentException("distanceTiles must be finite and non-negative");
        }
        this.distanceTiles = distanceTiles;
    }

    @Override
    public void apply(Zombie zombie) {
        if (zombie == null) {
            throw new IllegalArgumentException("zombie cannot be null");
        }
        if (!zombie.isDead() && !zombie.getType().isLarge()) {
            zombie.moveTo(zombie.getColumnPosition() + distanceTiles);
        }
    }

    public double getDistanceTiles() {
        return distanceTiles;
    }
}
