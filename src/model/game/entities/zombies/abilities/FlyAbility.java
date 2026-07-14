package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Dodo zombie's ability to fly over plants and obstacles.
 */
public class FlyAbility extends ZombieAbility {
    private boolean flying;
    private int maxGridSquaresToFly;
    private double jumpChance;

    public FlyAbility(int maxGridSquares, double jumpChance) {
        super(0);
        this.maxGridSquaresToFly = maxGridSquares;
        this.jumpChance = jumpChance;
        this.flying = false;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        // Check if should start flying over obstacles
        // Random chance per grid walked
        // When flying, zombie ignores plants and obstacles
        return false;
    }

    public boolean isFlying() { return flying; }
    public void setFlying(boolean flying) { this.flying = flying; }
    public int getMaxGridSquaresToFly() { return maxGridSquaresToFly; }
    public double getJumpChance() { return jumpChance; }
}
