package model.game.entities.projectile.effect;

import model.game.entities.zombies.Zombie;

public final class DamageEffect extends ProjectileEffect {
    private final int damage;

    public DamageEffect(int damage) {
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
        zombie.takeDamage(damage);
    }

    public int getDamage() {
        return damage;
    }
}
