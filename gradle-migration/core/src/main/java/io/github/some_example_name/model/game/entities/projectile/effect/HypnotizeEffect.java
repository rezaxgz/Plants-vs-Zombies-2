package io.github.some_example_name.model.game.entities.projectile.effect;

import io.github.some_example_name.model.game.entities.zombies.Zombie;

public class HypnotizeEffect extends ProjectileEffect {
    @Override
    public void apply(Zombie zombie) {
        if (zombie == null) {
            throw new IllegalArgumentException("zombie cannot be null");
        }
        zombie.hypnotize();
    }
}
