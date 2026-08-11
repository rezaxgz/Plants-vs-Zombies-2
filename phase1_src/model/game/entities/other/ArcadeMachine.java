package model.game.entities.other;

import model.game.entities.zombies.ZombieType;
import model.game.entities.zombies.armor.ArmorType;

/**
 * Arcade cabinet pushed by an Arcade Zombie.
 */
public final class ArcadeMachine extends PushedObstacle {
    public static final int DEFAULT_HIT_POINTS =
            ZombieType.BASIC.getHitpoints()
                    + ArmorType.BUCKET.getBaseHealth();

    public ArcadeMachine(int lane, double columnPosition) {
        super("Arcade machine", DEFAULT_HIT_POINTS,
                lane, columnPosition);
    }
}
