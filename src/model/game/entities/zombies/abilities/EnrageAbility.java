package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Newspaper zombie's enrage ability when newspaper is destroyed.
 */
public class EnrageAbility extends ZombieAbility {
    private double enragedDamageScale;
    private double enragedSpeedScale;
    private boolean enraged;

    public EnrageAbility(double damageScale, double speedScale) {
        super(0);
        this.enragedDamageScale = damageScale;
        this.enragedSpeedScale = speedScale;
        this.enraged = false;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        // Triggered when newspaper armor is destroyed
        if (!enraged) {
            enraged = true;
            // Apply speed and damage multipliers to zombie
            return true;
        }
        return false;
    }

    public boolean isEnraged() { return enraged; }
    public double getEnragedDamageScale() { return enragedDamageScale; }
    public double getEnragedSpeedScale() { return enragedSpeedScale; }
}
