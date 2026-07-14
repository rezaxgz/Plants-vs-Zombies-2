package model.game.entities.zombies.movement;

import model.game.entities.zombies.Zombie;

public abstract class MovementBehavior {
    public abstract void move(Zombie zombie, float deltaSeconds, double minimumColumn);
}
