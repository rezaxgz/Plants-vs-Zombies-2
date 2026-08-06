package io.github.some_example_name.model.game.entities.projectile.movement;

import io.github.some_example_name.model.game.entities.projectile.Projectile;

public abstract class ProjectileMovement {
    public abstract void move(Projectile projectile, float deltaSeconds);
}
