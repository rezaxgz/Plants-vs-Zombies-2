package model.game.minigame;

import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.ZombieType;

/**
 * A stationary buckethead-durability zombie that creates zombie sun.
 */
final class IZombieSunProducer extends Zombie {
    IZombieSunProducer(int row, double column) {
        super(ZombieType.BUCKETHEAD, 0, row, column, false);
    }

    @Override
    public double getEffectiveSpeed() {
        return 0.0;
    }

    @Override
    public int getEffectiveEatDPS() {
        return 0;
    }
}
