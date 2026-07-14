package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Tomb Raiser's ability to summon graves on the lawn.
 */
public class TombSummonAbility extends ZombieAbility {
    private int ammo;
    private int tombsToSpawn;
    private double timeBetweenRaisings;

    public TombSummonAbility(int ammo, int tombsToSpawn, double timeBetweenRaisings) {
        super(timeBetweenRaisings);
        this.ammo = ammo;
        this.tombsToSpawn = tombsToSpawn;
        this.timeBetweenRaisings = timeBetweenRaisings;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (!canUse() || ammo <= 0) return false;

        // Summon grave at random valid position on the board
        // board.addStructure(new Grave(randomPosition));
        ammo--;
        resetCooldown();
        return true;
    }

    public int getAmmo() { return ammo; }
    public int getTombsToSpawn() { return tombsToSpawn; }
    public double getTimeBetweenRaisings() { return timeBetweenRaisings; }
}
