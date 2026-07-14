package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Prospector zombie's ability to launch to the back of the lawn.
 */
public class LaunchAbility extends ZombieAbility {
    private double launchCountdown;
    private double timeToTravel;
    private double apex;
    private boolean launched;

    public LaunchAbility(double launchCountdown, double timeToTravel, double apex) {
        super(launchCountdown);
        this.launchCountdown = launchCountdown;
        this.timeToTravel = timeToTravel;
        this.apex = apex;
        this.launched = false;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (launched || !canUse()) return false;

        // Launch to back of lawn (column 0 or near house)
        // zombie.moveTo(0.0);
        // zombie.setStunned(2.5); // Stun after landing
        launched = true;
        return true;
    }

    public boolean hasLaunched() { return launched; }
    public double getLaunchCountdown() { return launchCountdown; }
    public double getTimeToTravel() { return timeToTravel; }
    public double getApex() { return apex; }
}
