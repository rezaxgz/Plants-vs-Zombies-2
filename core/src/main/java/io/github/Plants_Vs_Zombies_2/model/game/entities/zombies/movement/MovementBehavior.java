package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.movement;

import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

public abstract class MovementBehavior implements java.io.Serializable {
    public abstract void move(Zombie zombie, float deltaSeconds, double minimumColumn);
}
