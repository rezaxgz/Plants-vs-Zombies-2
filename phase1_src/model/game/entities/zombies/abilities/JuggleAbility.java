package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.projectile.Projectile;
import model.game.entities.zombies.Zombie;

/**
 * Juggler reflects direct projectiles while spinning. Each reflected shot
 * refreshes the spin window; spinning slightly increases movement speed.
 */
public class JuggleAbility extends ZombieAbility {
    private static final double SPIN_GRACE_SECONDS = 2.0;
    private static final double SPIN_SPEED_MULTIPLIER = 1.1;

    private final int configuredMaxProjectiles;
    private final double catchArcDegrees;

    private int reflectedProjectileCount;
    private boolean spinning;
    private double spinTimeRemaining;

    public JuggleAbility(int maxProjectiles,
            double catchArcDegrees) {
        super(0.0);
        if (maxProjectiles <= 0
                || !Double.isFinite(catchArcDegrees)
                || catchArcDegrees <= 0.0
                || catchArcDegrees > 360.0) {
            throw new IllegalArgumentException(
                    "invalid Juggler configuration");
        }
        configuredMaxProjectiles = maxProjectiles;
        this.catchArcDegrees = catchArcDegrees;
    }

    @Override
    public void update(double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds)
                || deltaSeconds < 0.0) {
            throw new IllegalArgumentException(
                    "deltaSeconds must be finite and non-negative");
        }
        if (!spinning) {
            return;
        }
        spinTimeRemaining = Math.max(
                0.0, spinTimeRemaining - deltaSeconds);
        if (spinTimeRemaining == 0.0) {
            spinning = false;
        }
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
        spinning = true;
        spinTimeRemaining = SPIN_GRACE_SECONDS;
        return true;
    }

    private boolean canReflect(
            Zombie zombie, Board board) {
        return zombie != null && board != null
                && !zombie.isDead()
                && !zombie.isHypnotized()
                && !zombie.isFrozen()
                && !zombie.isStunned();
    }

    public boolean canCatchProjectile(
            String projectileType) {
        return projectileType != null
                && !projectileType.isBlank();
    }

    public double getSpeedMultiplier() {
        return spinning ? SPIN_SPEED_MULTIPLIER : 1.0;
    }

    public boolean isSpinning() {
        return spinning;
    }

    public int getMaxProjectiles() {
        return configuredMaxProjectiles;
    }

    public double getCatchArcDegrees() {
        return catchArcDegrees;
    }

    public int getReflectedProjectileCount() {
        return reflectedProjectileCount;
    }
}
