package io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.movement;

import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.Projectile;

public abstract class ProjectileMovement implements java.io.Serializable {
    public abstract void move(Projectile projectile, float deltaSeconds);
}
