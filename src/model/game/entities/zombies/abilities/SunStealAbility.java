package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.EntityPosition;
import model.game.entities.other.Sun;
import model.game.entities.zombies.Zombie;

/**
 * Ra periodically pulls the nearest collectable sun from the lawn and stores
 * it. All stored sun is returned to the player's reserve when Ra dies.
 */
public class SunStealAbility extends ZombieAbility {
    private final int maxClaimedSun;
    private int sunStolen;
    private int lastStolenAmount;
    private boolean stolenSunReleased;

    public SunStealAbility(int maxClaimedSun) {
        super(3.0);
        if (maxClaimedSun <= 0) {
            throw new IllegalArgumentException("maxClaimedSun must be positive");
        }
        this.maxClaimedSun = maxClaimedSun;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        lastStolenAmount = 0;
        if (!canUse() || zombie == null || board == null
                || zombie.isDead() || zombie.isHypnotized()
                || sunStolen >= maxClaimedSun) {
            return false;
        }

        Sun target = findNearestStealableSun(zombie, board);
        if (target == null) {
            return false;
        }

        int stolen = target.collect();
        if (stolen <= 0) {
            return false;
        }
        board.removeEntity(target);
        sunStolen += stolen;
        lastStolenAmount = stolen;
        resetCooldown();
        return true;
    }

    private Sun findNearestStealableSun(Zombie zombie, Board board) {
        Sun nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        int remainingCapacity = maxClaimedSun - sunStolen;
        for (Sun sun : board.getSuns()) {
            if (!sun.isCollectable() || sun.getSunAmount() > remainingCapacity) {
                continue;
            }
            double distance = distanceSquared(zombie, sun.getEntityPosition());
            if (distance < nearestDistance) {
                nearest = sun;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static double distanceSquared(Zombie zombie, EntityPosition position) {
        double rowDistance = zombie.getLane() - position.getRow();
        double columnDistance = zombie.getColumnPosition() - position.getColumn();
        return rowDistance * rowDistance + columnDistance * columnDistance;
    }

    public int releaseStolenSun() {
        if (stolenSunReleased) {
            return 0;
        }
        stolenSunReleased = true;
        int released = sunStolen;
        sunStolen = 0;
        return released;
    }

    public int getSunStolen() {
        return sunStolen;
    }

    public int getLastStolenAmount() {
        return lastStolenAmount;
    }

    public int getMaxClaimedSun() {
        return maxClaimedSun;
    }
}
