package io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect;

import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

public abstract class ProjectileEffect implements java.io.Serializable {
    public abstract void apply(Zombie zombie);
}
