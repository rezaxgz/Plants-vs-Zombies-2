package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.armor.Armor;
import model.game.entities.zombies.armor.ArmorType;

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
