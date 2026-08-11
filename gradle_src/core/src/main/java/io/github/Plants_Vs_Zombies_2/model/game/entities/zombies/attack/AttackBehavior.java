package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.attack;

import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

public abstract class AttackBehavior {
    public abstract void attack(Zombie zombie, BasePlant plant, float deltaSeconds);
}
