package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Dark King's ability to buff nearby zombies with armor.
 */
public class KingBuffAbility extends ZombieAbility {
    private double buffAreaX;
    private double buffAreaY;
    private double delayBetweenKnightings;

    public KingBuffAbility(double buffAreaX, double buffAreaY, double delayBetweenKnightings) {
        super(delayBetweenKnightings);
        this.buffAreaX = buffAreaX;
        this.buffAreaY = buffAreaY;
        this.delayBetweenKnightings = delayBetweenKnightings;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (!canUse()) return false;

        // Buff nearby valid knight targets with armor
        // List<Zombie> nearby = board.getZombiesInArea(
        //     zombie.getLane(), zombie.getColumnPosition(), buffAreaX, buffAreaY);
        // for (Zombie z : nearby) {
        //     if (isValidKnightTarget(z)) z.addArmor(ArmorType.CROWN);
        // }
        resetCooldown();
        return true;
    }

    public double getBuffAreaX() { return buffAreaX; }
    public double getBuffAreaY() { return buffAreaY; }
}
