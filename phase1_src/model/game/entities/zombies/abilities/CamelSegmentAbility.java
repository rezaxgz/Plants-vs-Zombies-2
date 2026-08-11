package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Models Camel Zombie as three linked durability segments. The front segment
 * owns the single attack behavior while the remaining segments follow it.
 */
public class CamelSegmentAbility extends ZombieAbility {
    private final int totalSegments;
    private int currentSegments;
    private int segmentHitPoints;

    public CamelSegmentAbility(int totalSegments) {
        super(0.0);
        if (totalSegments <= 0) {
            throw new IllegalArgumentException("totalSegments must be positive");
        }
        this.totalSegments = totalSegments;
        this.currentSegments = totalSegments;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (zombie == null) {
            return false;
        }
        if (segmentHitPoints == 0) {
            segmentHitPoints = Math.max(1,
                    (int) Math.ceil((double) zombie.getMaximumHitPoints()
                            / totalSegments));
        }

        int updatedSegments = zombie.isDead() ? 0
                : Math.max(1, (int) Math.ceil(
                        (double) zombie.getHitPoints() / segmentHitPoints));
        updatedSegments = Math.min(totalSegments, updatedSegments);
        if (updatedSegments == currentSegments) {
            return false;
        }
        currentSegments = updatedSegments;
        return true;
    }

    public int getTotalSegments() {
        return totalSegments;
    }

    public int getCurrentSegments() {
        return currentSegments;
    }

    public int getSegmentHitPoints() {
        return segmentHitPoints;
    }
}
