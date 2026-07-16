package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.ZombieType;
import model.game.entities.zombies.armor.ArmorType;

/**
 * Dark King periodically grants crown armor to the nearest eligible Dark Ages
 * peasant inside its configured rectangular buff area.
 */
public class KingBuffAbility extends ZombieAbility {
    private final double buffAreaX;
    private final double buffAreaY;
    private final double delayBetweenKnightings;

    private Zombie lastKnightedZombie;

    public KingBuffAbility(double buffAreaX,
            double buffAreaY,
            double delayBetweenKnightings) {
        super(delayBetweenKnightings);
        if (!Double.isFinite(buffAreaX)
                || !Double.isFinite(buffAreaY)
                || !Double.isFinite(delayBetweenKnightings)
                || buffAreaX < 0.0
                || buffAreaY < 0.0
                || delayBetweenKnightings < 0.0) {
            throw new IllegalArgumentException(
                    "invalid Dark King configuration");
        }
        this.buffAreaX = buffAreaX;
        this.buffAreaY = buffAreaY;
        this.delayBetweenKnightings =
                delayBetweenKnightings;
    }

    @Override
    public boolean tryUse(Zombie king, Board board) {
        lastKnightedZombie = null;
        if (!canUse() || king == null || board == null
                || king.isDead() || king.isHypnotized()
                || king.isFrozen() || king.isStunned()) {
            return false;
        }

        Zombie target = findNearestKnightTarget(king, board);
        if (target == null
                || !target.equipArmor(ArmorType.CROWN)) {
            return false;
        }

        lastKnightedZombie = target;
        resetCooldown();
        return true;
    }

    private Zombie findNearestKnightTarget(
            Zombie king, Board board) {
        Zombie nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Zombie candidate : board.getZombies()) {
            if (!isValidKnightTarget(king, candidate)) {
                continue;
            }

            double rowDistance =
                    candidate.getLane() - king.getLane();
            double columnDistance =
                    candidate.getColumnPosition()
                            - king.getColumnPosition();
            if (Math.abs(columnDistance) > buffAreaX
                    || Math.abs(rowDistance) > buffAreaY) {
                continue;
            }

            double distance = rowDistance * rowDistance
                    + columnDistance * columnDistance;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private boolean isValidKnightTarget(
            Zombie king, Zombie candidate) {
        if (candidate == null || candidate == king
                || candidate.isDead()
                || candidate.isHypnotized()
                || !candidate.canReceiveArmor()) {
            return false;
        }

        ZombieType type = candidate.getType();
        return type == ZombieType.DARK
                || type == ZombieType.DARK_CONEHEAD
                || type == ZombieType.DARK_BUCKETHEAD
                || type == ZombieType.DARK_SHOULDER_ARMOR
                || type == ZombieType.DARK_BRICKHEAD;
    }

    public Zombie getLastKnightedZombie() {
        return lastKnightedZombie;
    }

    public double getBuffAreaX() {
        return buffAreaX;
    }

    public double getBuffAreaY() {
        return buffAreaY;
    }

    public double getDelayBetweenKnightings() {
        return delayBetweenKnightings;
    }
}
