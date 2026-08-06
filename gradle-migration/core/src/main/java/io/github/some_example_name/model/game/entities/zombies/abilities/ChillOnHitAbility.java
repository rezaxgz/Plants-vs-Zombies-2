package io.github.some_example_name.model.game.entities.zombies.abilities;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.entities.zombies.Zombie;
import io.github.some_example_name.model.game.entities.zombies.armor.Armor;
import io.github.some_example_name.model.game.entities.zombies.armor.ArmorType;

/**
 * The Blockhead's intact ice-block armor chills the plant whose projectile
 * strikes it.
 */
public class ChillOnHitAbility extends ZombieAbility {
    public ChillOnHitAbility() {
        super(0.0);
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (zombie == null || board == null || zombie.isDead()) {
            return false;
        }
        Armor armor = zombie.getArmor();
        return armor != null
                && armor.getType() == ArmorType.ICE_BLOCK
                && !armor.isDestroyed();
    }
}
