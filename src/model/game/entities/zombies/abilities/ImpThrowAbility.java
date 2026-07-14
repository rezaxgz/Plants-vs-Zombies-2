package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Ability to throw an Imp when damaged or near the house.
 */
public class ImpThrowAbility extends ZombieAbility {
    private double healthThreshold;
    private boolean thrown;
    private String impType;

    public ImpThrowAbility(double healthThreshold, String impType) {
        super(0); // No cooldown, one-time use
        this.healthThreshold = healthThreshold;
        this.thrown = false;
        this.impType = impType;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (thrown) return false;

        double healthPercent = (double) zombie.getHitPoints() / zombie.getMaximumHitPoints();
        if (healthPercent <= healthThreshold || zombie.getColumnPosition() <= 2.0) {
            thrown = true;
            // Spawn imp at current position
            // board.spawnZombieByAlias(impType, zombie.getLane(), zombie.getColumnPosition());
            return true;
        }
        return false;
    }

    public boolean hasThrown() { return thrown; }
    public String getImpType() { return impType; }
    public double getHealthThreshold() { return healthThreshold; }
}
