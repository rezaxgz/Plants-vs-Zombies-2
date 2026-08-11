package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Prospector's dynamite launches it beside the house after ten seconds. It
 * then walks in the opposite direction. An icy projectile can extinguish the
 * dynamite before launch.
 */
public class LaunchAbility extends ZombieAbility {
    private static final double LANDING_COLUMN = 0.25;

    private final double launchCountdown;
    private final double timeToTravel;
    private final double apex;

    private boolean launched;
    private boolean extinguished;
    private boolean launchedThisUse;

    public LaunchAbility(double launchCountdown,
            double timeToTravel, double apex) {
        super(launchCountdown);
        if (!Double.isFinite(launchCountdown)
                || launchCountdown < 0.0
                || !Double.isFinite(timeToTravel)
                || timeToTravel < 0.0
                || !Double.isFinite(apex)
                || apex < 0.0) {
            throw new IllegalArgumentException(
                    "invalid Prospector launch configuration");
        }
        this.launchCountdown = launchCountdown;
        this.timeToTravel = timeToTravel;
        this.apex = apex;
        this.elapsedSinceLastUse = 0.0;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        launchedThisUse = false;
        if (launched || extinguished || !canUse()
                || zombie == null || board == null
                || zombie.isDead() || zombie.isHypnotized()) {
            return false;
        }

        launched = true;
        launchedThisUse = true;
        zombie.moveTo(LANDING_COLUMN);
        zombie.applyStun(timeToTravel);
        return true;
    }

    public boolean extinguish() {
        if (launched || extinguished) {
            return false;
        }
        extinguished = true;
        setActive(false);
        return true;
    }

    public boolean hasLaunched() {
        return launched;
    }

    public boolean isExtinguished() {
        return extinguished;
    }

    public boolean didLaunchThisUse() {
        return launchedThisUse;
    }

    public double getLaunchCountdown() {
        return launchCountdown;
    }

    public double getTimeToTravel() {
        return timeToTravel;
    }

    public double getApex() {
        return apex;
    }
}
