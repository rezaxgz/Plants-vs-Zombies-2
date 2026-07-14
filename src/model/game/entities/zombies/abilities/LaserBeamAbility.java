package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Crystal Skull zombie's laser beam ability.
 */
public class LaserBeamAbility extends ZombieAbility {
    private int laserDamage;
    private double laserLength;
    private double chargingTime;

    public LaserBeamAbility(int laserDamage, double laserLength, double chargingTime) {
        super(chargingTime);
        this.laserDamage = laserDamage;
        this.laserLength = laserLength;
        this.chargingTime = chargingTime;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (!canUse()) return false;

        // Fire laser beam that destroys plants in line
        // for (Plant p : board.getPlantsInLine(zombie.getLane(),
        //     zombie.getColumnPosition() - laserLength, zombie.getColumnPosition())) {
        //     p.takeDamage(laserDamage);
        // }
        resetCooldown();
        return true;
    }

    public int getLaserDamage() { return laserDamage; }
    public double getLaserLength() { return laserLength; }
    public double getChargingTime() { return chargingTime; }
}
