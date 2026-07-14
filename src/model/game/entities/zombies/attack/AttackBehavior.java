package model.game.entities.zombies.attack;

import model.game.entities.plants.BasePlant;
import model.game.entities.zombies.Zombie;

public abstract class AttackBehavior {
    public abstract void attack(Zombie zombie, BasePlant plant, float deltaSeconds);
}
