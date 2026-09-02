package io.github.Plants_Vs_Zombies_2.model.game.minigame;

/**
 * Read-only state needed by the shared I, Zombie graphical presentation.
 *
 * <p>Both the local minigame and the server-authoritative multiplayer mirror
 * implement this contract, so {@code GameScreen} owns one rendering path for
 * cards, brains, the red line, the HUD, entities and projectiles.</p>
 */
public interface IZombiePresentationModel {
    IZombieLevel getLevel();

    int getRedLineColumn();

    boolean isBrainAvailable(int row);

    double getCardCooldownRemainingSeconds(IZombieCard card);
}
