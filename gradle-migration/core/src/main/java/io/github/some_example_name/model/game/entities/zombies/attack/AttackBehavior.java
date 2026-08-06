package io.github.some_example_name.model.game.entities.zombies.attack;

import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.zombies.Zombie;

public abstract class AttackBehavior {
    public abstract void attack(Zombie zombie, BasePlant plant, float deltaSeconds);
}
