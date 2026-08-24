package io.github.Plants_Vs_Zombies_2.model.game.minigame;

/**
 * Result of spending sun to place a zombie in I, Zombie.
 */
public enum IZombiePlacementResult {
    SUCCESS,
    GAME_NOT_ACTIVE,
    UNKNOWN_ZOMBIE,
    BOSS_NOT_ALLOWED,
    NOT_ENOUGH_SUN,
    RECHARGING,
    INVALID_POSITION,
    LEFT_OF_RED_LINE,
    POSITION_OCCUPIED
}
