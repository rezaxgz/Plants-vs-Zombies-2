package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies;

import java.util.concurrent.ThreadLocalRandom;

import io.github.Plants_Vs_Zombies_2.model.Constants;

/**
 * Enhanced Zombie class with support for armor, abilities, and special
 * behaviors.
 */
public class Zombie extends ZombieCombatLogic {
    public Zombie(ZombieType type, int waveNumber, int lane, double columnPosition) {
        this(type, waveNumber, lane, columnPosition,
                ThreadLocalRandom.current().nextDouble() < Constants.GLOWING_ZOMBIE_CHANCE);
    }

    public Zombie(ZombieType type, int waveNumber, int lane,
            double columnPosition, boolean glowing) {
        super(type, waveNumber, lane, columnPosition, glowing);
    }
}
