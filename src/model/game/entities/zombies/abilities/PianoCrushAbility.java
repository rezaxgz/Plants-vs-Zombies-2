package model.game.entities.zombies.abilities;

import java.util.concurrent.ThreadLocalRandom;

import model.game.Board;
import model.game.entities.plants.BasePlant;
import model.game.entities.zombies.Zombie;

/**
 * Piano Zombie slowly pushes its piano, crushes plants on contact, and makes
 * other hostile zombies randomly change to an adjacent lane every few seconds.
 */
public class PianoCrushAbility extends ZombieAbility {
    private static final double MUSIC_INTERVAL_SECONDS = 4.0;

    private final double collisionRangeTiles;
    private boolean playing = true;
    private int lastMovedZombieCount;

    public PianoCrushAbility(double collisionRangeTiles) {
        super(MUSIC_INTERVAL_SECONDS);
        if (!Double.isFinite(collisionRangeTiles)
                || collisionRangeTiles <= 0.0) {
            throw new IllegalArgumentException(
                    "piano collision range must be positive");
        }
        this.collisionRangeTiles = collisionRangeTiles;
        this.elapsedSinceLastUse = 0.0;
    }

    @Override
    public boolean tryUse(Zombie pianoZombie, Board board) {
        lastMovedZombieCount = 0;
        if (!playing || !canUse()
                || pianoZombie == null || board == null
                || pianoZombie.isDead()
                || pianoZombie.isHypnotized()
                || pianoZombie.isFrozen()
                || pianoZombie.isStunned()) {
            return false;
        }

        for (Zombie zombie : board.getZombies()) {
            if (zombie == pianoZombie || zombie.isDead()
                    || zombie.isHypnotized()) {
                continue;
            }
            int targetLane = chooseAdjacentLane(
                    zombie.getLane(),
                    board.getNumberOfRows());
            if (targetLane != zombie.getLane()) {
                zombie.moveToLane(targetLane);
                lastMovedZombieCount++;
            }
        }
        resetCooldown();
        return true;
    }

    private int chooseAdjacentLane(
            int currentLane, int laneCount) {
        if (laneCount <= 1) {
            return currentLane;
        }
        if (currentLane <= 0) {
            return 1;
        }
        if (currentLane >= laneCount - 1) {
            return laneCount - 2;
        }
        return ThreadLocalRandom.current().nextBoolean()
                ? currentLane - 1 : currentLane + 1;
    }

    public boolean canCrush(
            Zombie zombie, BasePlant plant) {
        if (!playing || zombie == null || plant == null
                || zombie.isDead() || zombie.isFrozen()
                || zombie.isStunned() || plant.isRemoved()
                || plant.getEntityPosition() == null
                || plant.getEntityPosition().getRow()
                        != zombie.getLane()) {
            return false;
        }
        double distance = zombie.getColumnPosition()
                - plant.getEntityPosition().getColumn();
        return distance >= 0.0
                && distance <= collisionRangeTiles;
    }

    public double getFastMoveSpeed() {
        return collisionRangeTiles;
    }

    public double getCollisionRangeTiles() {
        return collisionRangeTiles;
    }

    public int getLastMovedZombieCount() {
        return lastMovedZombieCount;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }
}
