package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * Abstract base class for special zombie abilities.
 * Each ability has a cooldown and can be activated under certain conditions.
 */
public abstract class ZombieAbility implements java.io.Serializable {
    protected double cooldown;
    protected double elapsedSinceLastUse;
    protected boolean active;

    public ZombieAbility(double cooldown) {
        this.cooldown = cooldown;
        this.elapsedSinceLastUse = cooldown; // Start ready to use
        this.active = true;
    }

    public void update(double deltaSeconds) {
        if (elapsedSinceLastUse < cooldown) {
            elapsedSinceLastUse += deltaSeconds;
        }
    }

    public boolean canUse() {
        return active && elapsedSinceLastUse >= cooldown;
    }

    public void resetCooldown() {
        elapsedSinceLastUse = 0;
    }

    /**
     * Attempts to use the ability. Returns true if ability was used.
     */
    public abstract boolean tryUse(Zombie zombie, Board board);

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getCooldown() {
        return cooldown;
    }

    public double getElapsedSinceLastUse() {
        return elapsedSinceLastUse;
    }
}
