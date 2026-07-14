package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Explorer zombie's torch - instantly destroys specific plants.
 */
public class TorchAbility extends ZombieAbility {
    private double torchReach;
    private boolean torchLit;

    public TorchAbility(double torchReach) {
        super(0);
        this.torchReach = torchReach;
        this.torchLit = true;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (!torchLit) return false;

        // Check for plants in torch range that should be instantly destroyed
        // e.g., Frost Bonnet, Blazing Knight, etc.
        return true;
    }

    public boolean isTorchLit() { return torchLit; }
    public void extinguish() { this.torchLit = false; }
    public double getTorchReach() { return torchReach; }
}
