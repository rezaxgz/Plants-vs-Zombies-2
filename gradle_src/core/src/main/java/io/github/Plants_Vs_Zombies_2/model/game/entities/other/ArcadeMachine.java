package io.github.Plants_Vs_Zombies_2.model.game.entities.other;

import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.armor.ArmorType;

/**
 * Arcade cabinet pushed by an Arcade Zombie.
 */
public final class ArcadeMachine extends PushedObstacle {
    public static final int DEFAULT_HIT_POINTS = ZombieType.BASIC.getHitpoints()
            + ArmorType.BUCKET.getBaseHealth();

    public ArcadeMachine(int lane, double columnPosition) {
        super("Arcade machine", DEFAULT_HIT_POINTS,
                lane, columnPosition);
    }
}
