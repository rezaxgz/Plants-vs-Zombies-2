package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Troglobite's ability to push ice blocks.
 * Blocks crush plants and absorb damage.
 */
public class IceBlockPushAbility extends ZombieAbility {
    private int numberOfIceBlocks;

    public IceBlockPushAbility(int numberOfIceBlocks) {
        super(0);
        this.numberOfIceBlocks = numberOfIceBlocks;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        // Push ice blocks that crush plants and absorb damage
        return true;
    }

    public int getNumberOfIceBlocks() { return numberOfIceBlocks; }
}
