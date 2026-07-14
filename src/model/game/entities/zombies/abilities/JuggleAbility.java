package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Juggler zombie's ability to catch and reflect projectiles.
 */
public class JuggleAbility extends ZombieAbility {
    private int maxProjectiles;
    private double catchArcDegrees;

    public JuggleAbility(int maxProjectiles, double catchArcDegrees) {
        super(0.5);
        this.maxProjectiles = maxProjectiles;
        this.catchArcDegrees = catchArcDegrees;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        // Passive ability - reflects projectiles back
        // Called when projectile would hit this zombie
        return true;
    }

    public boolean canCatchProjectile(String projectileType) {
        // Check if this projectile type can be juggled
        // Full list from Zombies.md includes peas, cabbages, melons, etc.
        return true;
    }

    public int getMaxProjectiles() { return maxProjectiles; }
    public double getCatchArcDegrees() { return catchArcDegrees; }
}
