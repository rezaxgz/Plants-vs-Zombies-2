package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.ArcadeMachine;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * Creates and keeps an Arcade machine immediately in front of its pusher.
 */
public class ArcadePushAbility extends ZombieAbility {
    private static final double MACHINE_OFFSET_TILES = 0.75;

    private ArcadeMachine machine;
    private boolean spawnedThisUse;

    public ArcadePushAbility() {
        super(0.0);
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        spawnedThisUse = false;
        if (zombie == null || board == null
                || zombie.isDead() || zombie.isHypnotized()) {
            return false;
        }

        if (machine == null) {
            machine = new ArcadeMachine(
                    zombie.getLane(),
                    Math.max(0.0,
                            zombie.getColumnPosition()
                                    - MACHINE_OFFSET_TILES));
            board.addEntity(machine);
            spawnedThisUse = true;
        }
        if (machine.isDestroyed()) {
            return false;
        }

        machine.moveTo(zombie.getLane(),
                zombie.getColumnPosition()
                        - MACHINE_OFFSET_TILES,
                board.getNumberOfColumns());
        return true;
    }

    public ArcadeMachine getMachine() {
        return machine;
    }

    public boolean didSpawnThisUse() {
        return spawnedThisUse;
    }
}
