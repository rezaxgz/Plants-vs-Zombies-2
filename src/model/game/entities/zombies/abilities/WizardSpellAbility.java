package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Wizard zombie's ability to transform plants into harmless sheep.
 */
public class WizardSpellAbility extends ZombieAbility {
    public WizardSpellAbility() {
        super(8.0);
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (!canUse()) return false;

        // Cast spell to transform random plant into sheep
        // Plant target = board.getRandomPlant();
        // if (target != null) target.transformToSheep();
        resetCooldown();
        return true;
    }
}
