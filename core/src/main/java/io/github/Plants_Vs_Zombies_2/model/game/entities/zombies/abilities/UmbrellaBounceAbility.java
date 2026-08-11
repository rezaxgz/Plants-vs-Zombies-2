package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * Parasol Zombie's open umbrella deflects every lobbed projectile.
 */
public class UmbrellaBounceAbility extends ZombieAbility {
    public UmbrellaBounceAbility() {
        super(0.0);
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        return zombie != null && board != null
                && !zombie.isDead()
                && !zombie.isHypnotized();
    }
}
