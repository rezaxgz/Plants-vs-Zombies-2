package io.github.some_example_name.model.game.entities.zombies.abilities;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.entities.zombies.Zombie;

/**
 * Newspaper Zombie permanently becomes faster and deals more eating damage
 * once its newspaper armor has been destroyed.
 */
public class EnrageAbility extends ZombieAbility {
    private final double enragedDamageScale;
    private final double enragedSpeedScale;
    private boolean enraged;

    public EnrageAbility(double damageScale,
            double speedScale) {
        super(0.0);
        if (!Double.isFinite(damageScale)
                || damageScale <= 0.0
                || !Double.isFinite(speedScale)
                || speedScale <= 0.0) {
            throw new IllegalArgumentException(
                    "enrage multipliers must be positive");
        }
        this.enragedDamageScale = damageScale;
        this.enragedSpeedScale = speedScale;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (enraged || zombie == null || zombie.isDead()) {
            return false;
        }
        enraged = true;
        return true;
    }

    public boolean isEnraged() {
        return enraged;
    }

    public double getEnragedDamageScale() {
        return enragedDamageScale;
    }

    public double getEnragedSpeedScale() {
        return enragedSpeedScale;
    }
}
