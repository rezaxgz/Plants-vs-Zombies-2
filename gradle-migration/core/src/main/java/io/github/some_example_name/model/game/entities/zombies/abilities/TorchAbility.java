package io.github.some_example_name.model.game.entities.zombies.abilities;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.zombies.Zombie;

/**
 * Explorer's lit torch instantly burns plants less than one tile ahead.
 * Ice effects extinguish it and fire effects ignite it through Zombie's
 * existing status-effect handling.
 */
public class TorchAbility extends ZombieAbility {
    private static final double MAX_RANGE_TILES = 1.0;

    private final double configuredTorchReach;
    private boolean torchLit = true;

    public TorchAbility(double torchReach) {
        super(0.0);
        if (!Double.isFinite(torchReach) || torchReach <= 0.0) {
            throw new IllegalArgumentException(
                    "torchReach must be finite and positive");
        }
        this.configuredTorchReach = torchReach;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        return torchLit && zombie != null && board != null
                && !zombie.isDead() && !zombie.isHypnotized();
    }

    public boolean canBurn(Zombie zombie, BasePlant plant) {
        if (!torchLit || zombie == null || plant == null
                || plant.isRemoved() || plant.getEntityPosition() == null
                || plant.getEntityPosition().getRow() != zombie.getLane()) {
            return false;
        }
        double distanceAhead = zombie.getColumnPosition()
                - plant.getEntityPosition().getColumn();
        return distanceAhead >= 0.0
                && distanceAhead < getTorchRangeTiles();
    }

    public boolean isTorchLit() {
        return torchLit;
    }

    public void extinguish() {
        torchLit = false;
    }

    public void ignite() {
        torchLit = true;
    }

    public double getTorchReach() {
        return configuredTorchReach;
    }

    public double getTorchRangeTiles() {
        return Math.min(MAX_RANGE_TILES, configuredTorchReach);
    }
}
