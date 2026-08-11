package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.movement;

import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

public class BasicZombieMovement extends MovementBehavior {
    public BasicZombieMovement(double speed) {
        if (!Double.isFinite(speed) || speed < 0.0) {
            throw new IllegalArgumentException("speed must be finite and non-negative");
        }
    }

    @Override
    public void move(Zombie zombie, float deltaSeconds, double minimumColumn) {
        if (zombie == null) {
            throw new IllegalArgumentException("zombie cannot be null");
        }
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
        double distance = zombie.getEffectiveSpeed() * deltaSeconds;
        zombie.moveTo(Math.max(minimumColumn, zombie.getColumnPosition() - distance));
    }
}
