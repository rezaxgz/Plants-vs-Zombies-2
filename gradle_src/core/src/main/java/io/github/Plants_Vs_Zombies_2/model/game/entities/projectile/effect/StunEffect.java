package io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect;

import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

public final class StunEffect extends ProjectileEffect {
    private final double durationSeconds;

    public StunEffect(double durationSeconds) {
        if (!Double.isFinite(durationSeconds) || durationSeconds < 0.0) {
            throw new IllegalArgumentException("durationSeconds must be finite and non-negative");
        }
        this.durationSeconds = durationSeconds;
    }

    @Override
    public void apply(Zombie zombie) {
        if (zombie == null) {
            throw new IllegalArgumentException("zombie cannot be null");
        }
        zombie.applyStun(durationSeconds);
    }
}
