package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Hunter zombie's snowball throwing ability.
 * Throws snowballs that chill or freeze plants from a distance.
 */
public class SnowballThrowAbility extends ZombieAbility {
    private int snowballsPerBarrage;
    private double farRange;
    private double nearRange;

    public SnowballThrowAbility(int snowballsPerBarrage, double farRange, double nearRange) {
        super(4.0);
        this.snowballsPerBarrage = snowballsPerBarrage;
        this.farRange = farRange;
        this.nearRange = nearRange;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (!canUse()) return false;

        // Throw snowball at nearest plant in range
        // Plant target = board.findNearestPlantInRange(zombie.getLane(),
        //     zombie.getColumnPosition() - farRange, zombie.getColumnPosition() - nearRange);
        // if (target != null) target.setChilled(true);
        resetCooldown();
        return true;
    }

    public int getSnowballsPerBarrage() { return snowballsPerBarrage; }
    public double getFarRange() { return farRange; }
    public double getNearRange() { return nearRange; }
}
