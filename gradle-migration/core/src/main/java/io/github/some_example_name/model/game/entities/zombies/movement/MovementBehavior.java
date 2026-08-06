package io.github.some_example_name.model.game.entities.zombies.movement;

import io.github.some_example_name.model.game.entities.zombies.Zombie;

public abstract class MovementBehavior {
    public abstract void move(Zombie zombie, float deltaSeconds, double minimumColumn);
}
