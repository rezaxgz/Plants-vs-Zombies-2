package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Fisherman zombie's ability to hook and pull plants.
 */
public class FishingHookAbility extends ZombieAbility {
    private double castTimePerGrid;
    private double delayBeforeReeling;
    private double delayBetweenCasting;
    private double reelTimePerGrid;

    public FishingHookAbility(double castTimePerGrid, double delayBeforeReeling,
                                double delayBetweenCasting, double reelTimePerGrid) {
        super(delayBetweenCasting);
        this.castTimePerGrid = castTimePerGrid;
        this.delayBeforeReeling = delayBeforeReeling;
        this.delayBetweenCasting = delayBetweenCasting;
        this.reelTimePerGrid = reelTimePerGrid;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (!canUse()) return false;

        // Cast fishing line to hook nearest plant in range
        // Plant target = board.findNearestPlantAhead(zombie);
        // if (target != null) {
        //     target.moveToward(zombie.getColumnPosition(), reelTimePerGrid);
        // }
        resetCooldown();
        return true;
    }

    public double getCastTimePerGrid() { return castTimePerGrid; }
    public double getDelayBeforeReeling() { return delayBeforeReeling; }
    public double getDelayBetweenCasting() { return delayBetweenCasting; }
    public double getReelTimePerGrid() { return reelTimePerGrid; }
}
