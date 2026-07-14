package model.game.entities.projectile.effect;

import model.game.entities.zombies.Zombie;

public final class FireEffect extends ProjectileEffect {
    private final int damage;

    public FireEffect(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        this.damage = damage;
    }

    @Override
    public void apply(Zombie zombie) {
        if (zombie == null) {
            throw new IllegalArgumentException("zombie cannot be null");
        }
        zombie.applyFireDamage(damage);
    }
}
