package io.github.some_example_name.model.game.entities.zombies;

import io.github.some_example_name.model.game.Board;

/**
 * Factory for creating zombies with proper initialization.
 */
public class ZombieFactory {

    /**
     * Create a zombie of the specified type.
     */
    public static Zombie createZombie(ZombieType type, int waveNumber, int lane,
            double columnPosition, Board board) {
        Zombie zombie = new Zombie(type, waveNumber, lane, columnPosition);

        // Apply any board-specific initialization
        // e.g., set initial state for snorkel zombies in water

        return zombie;
    }

    /**
     * Create a zombie by alias name.
     */
    public static Zombie createZombie(String alias, int waveNumber, int lane,
            double columnPosition, Board board) {
        ZombieType type = ZombieType.findByAlias(alias);
        if (type == null) {
            type = ZombieType.findByName(alias);
        }
        if (type == null) {
            throw new IllegalArgumentException("Unknown zombie type: " + alias);
        }
        return createZombie(type, waveNumber, lane, columnPosition, board);
    }

    /**
     * Create a basic zombie (fallback).
     */
    public static Zombie createBasicZombie(int waveNumber, int lane,
            double columnPosition, Board board) {
        return createZombie(ZombieType.BASIC, waveNumber, lane, columnPosition, board);
    }
}
