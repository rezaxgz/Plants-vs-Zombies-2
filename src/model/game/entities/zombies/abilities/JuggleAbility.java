package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.projectile.Projectile;
import model.game.entities.zombies.Zombie;

/**
 * Juggler catches incoming projectiles and reflects their impact damage back
 * toward the source plant. The short cooldown prevents unlimited simultaneous
 * catches while the configured capacity limits total reflections.
 */
public class JuggleAbility extends ZombieAbility {
    private final int maxProjectiles;
    private final double catchArcDegrees;
    private int reflectedProjectileCount;

    public JuggleAbility(int maxProjectiles,
            double catchArcDegrees) {
        super(0.5);
        if (maxProjectiles <= 0
                || !Double.isFinite(catchArcDegrees)
                || catchArcDegrees <= 0.0
                || catchArcDegrees > 360.0) {
            throw new IllegalArgumentException(
                    "invalid Juggler configuration");
        }
        this.maxProjectiles = maxProjectiles;
        this.catchArcDegrees = catchArcDegrees;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        return canReflect(zombie, board);
    }

    public boolean tryReflect(Zombie zombie,
            Projectile projectile, Board board) {
        if (projectile == null
                || !canReflect(zombie, board)) {
            return false;
        }
        reflectedProjectileCount++;
        resetCooldown();
        return true;
    }

    private boolean canReflect(Zombie zombie, Board board) {
        return canUse()
                && zombie != null
                && board != null
                && !zombie.isDead()
                && !zombie.isHypnotized()
                && !zombie.isFrozen()
                && !zombie.isStunned()
                && reflectedProjectileCount < maxProjectiles;
    }

    public boolean canCatchProjectile(String projectileType) {
        return projectileType != null
                && !projectileType.isBlank()
                && reflectedProjectileCount < maxProjectiles;
    }

    public int getMaxProjectiles() {
        return maxProjectiles;
    }

    public double getCatchArcDegrees() {
        return catchArcDegrees;
    }

    public int getReflectedProjectileCount() {
        return reflectedProjectileCount;
    }
}
