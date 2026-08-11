package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * Gargantuar's smash ability - instantly destroys plants.
 */
public class SmashAbility extends ZombieAbility {
    private int smashDamage;

    public SmashAbility(int smashDamage) {
        super(2.0); // 2 second cooldown between smashes
        this.smashDamage = smashDamage;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (!canUse())
            return false;

        // Find plant in attack range and smash it
        // The actual smash logic is handled by Board/attack system
        resetCooldown();
        return true;
    }

    public int getSmashDamage() {
        return smashDamage;
    }
}
