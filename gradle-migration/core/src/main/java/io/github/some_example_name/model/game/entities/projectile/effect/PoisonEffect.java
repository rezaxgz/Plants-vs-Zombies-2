package io.github.some_example_name.model.game.entities.projectile.effect;

import io.github.some_example_name.model.game.entities.zombies.Zombie;

public final class PoisonEffect extends ProjectileEffect {
    private final int impactDamage;
    private final int damagePerTick;
    private final double tickIntervalSeconds;
    private final double durationSeconds;

    public PoisonEffect(int impactDamage, int damagePerTick,
            double tickIntervalSeconds, double durationSeconds) {
        if (impactDamage < 0 || damagePerTick < 0) {
            throw new IllegalArgumentException("poison damage values cannot be negative");
        }
        if (!Double.isFinite(tickIntervalSeconds) || tickIntervalSeconds <= 0.0
                || !Double.isFinite(durationSeconds) || durationSeconds < 0.0) {
            throw new IllegalArgumentException("poison timing values are invalid");
        }
        this.impactDamage = impactDamage;
        this.damagePerTick = damagePerTick;
        this.tickIntervalSeconds = tickIntervalSeconds;
        this.durationSeconds = durationSeconds;
    }

    @Override
    public void apply(Zombie zombie) {
        if (zombie == null) {
            throw new IllegalArgumentException("zombie cannot be null");
        }
        zombie.takeDirectDamage(impactDamage);
        if (!zombie.isDead()) {
            zombie.applyPoison(damagePerTick, tickIntervalSeconds, durationSeconds);
        }
    }
}
