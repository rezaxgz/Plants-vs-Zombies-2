package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies;

import java.util.concurrent.ThreadLocalRandom;

import io.github.Plants_Vs_Zombies_2.model.Constants;

/**
 * Enhanced Zombie class with support for armor, abilities, and special
 * behaviors.
 */
public class Zombie extends ZombieCombatLogic {
    private int tornadoAdvanceColumns;

    public Zombie(ZombieType type, int waveNumber, int lane, double columnPosition) {
        this(type, waveNumber, lane, columnPosition,
                ThreadLocalRandom.current().nextDouble() < Constants.GLOWING_ZOMBIE_CHANCE);
    }

    public Zombie(ZombieType type, int waveNumber, int lane,
            double columnPosition, boolean glowing) {
        super(type, waveNumber, lane, columnPosition, glowing);
    }

    /**
     * Number of columns this zombie skipped when an Ancient Egypt tornado
     * spawned it farther into the lawn. Zero means a normal edge spawn.
     */
    public int getTornadoAdvanceColumns() {
        return tornadoAdvanceColumns;
    }

    public void setTornadoAdvanceColumns(int tornadoAdvanceColumns) {
        if (tornadoAdvanceColumns < 0) {
            throw new IllegalArgumentException(
                    "tornadoAdvanceColumns cannot be negative");
        }
        this.tornadoAdvanceColumns = tornadoAdvanceColumns;
    }
}
