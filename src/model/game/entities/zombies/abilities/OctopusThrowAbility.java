package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Octopus zombie's ability to throw octopi that disable plants.
 */
public class OctopusThrowAbility extends ZombieAbility {
    public OctopusThrowAbility() {
        super(5.0);
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (!canUse()) return false;

        // Throw octopus at plant to disable it
        // Plant target = board.findNearestPlantAhead(zombie);
        // if (target != null) target.setDisabled(true);
        resetCooldown();
        return true;
    }
}
