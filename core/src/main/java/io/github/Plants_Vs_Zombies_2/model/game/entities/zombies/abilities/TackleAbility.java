package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * One-use All-Star tackle. The zombie initially runs quickly, destroys the
 * first plant or hypnotized zombie it reaches, and then walks very slowly.
 */
public class TackleAbility extends ZombieAbility {
    private final int smashDamage;
    private final double walkingSpeedScale;
    private boolean running = true;

    public TackleAbility(int smashDamage, double walkingSpeedScale) {
        super(0.0);
        if (smashDamage <= 0) {
            throw new IllegalArgumentException("smashDamage must be positive");
        }
        if (!Double.isFinite(walkingSpeedScale)
                || walkingSpeedScale <= 0.0
                || walkingSpeedScale > 1.0) {
            throw new IllegalArgumentException(
                    "walkingSpeedScale must be finite and in the range (0, 1]");
        }
        this.smashDamage = smashDamage;
        this.walkingSpeedScale = walkingSpeedScale;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (!running || zombie == null || board == null || zombie.isDead()) {
            return false;
        }
        running = false;
        resetCooldown();
        return true;
    }

    public int getSmashDamage() {
        return smashDamage;
    }

    public double getWalkingSpeedScale() {
        return walkingSpeedScale;
    }

    public double getSpeedMultiplier() {
        return running ? 1.0 / walkingSpeedScale : walkingSpeedScale;
    }

    public boolean isRunning() {
        return running;
    }
}
