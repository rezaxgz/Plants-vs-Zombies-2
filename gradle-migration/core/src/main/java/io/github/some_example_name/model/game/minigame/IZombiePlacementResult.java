package io.github.some_example_name.model.game.minigame;

/**
 * Result of spending sun to place a zombie in I, Zombie.
 */
public enum IZombiePlacementResult {
    SUCCESS,
    GAME_NOT_ACTIVE,
    UNKNOWN_ZOMBIE,
    BOSS_NOT_ALLOWED,
    NOT_ENOUGH_SUN,
    INVALID_POSITION,
    LEFT_OF_RED_LINE,
    POSITION_OCCUPIED
}
