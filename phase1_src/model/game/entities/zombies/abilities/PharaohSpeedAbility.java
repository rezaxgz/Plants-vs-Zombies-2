package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.armor.Armor;
import model.game.entities.zombies.armor.ArmorType;

/**
 * Keeps Pharaoh slow while its sarcophagus exists and switches it to its
 * configured running speed as soon as that armor is destroyed.
 */
public class PharaohSpeedAbility extends ZombieAbility {
    private final double unarmoredSpeed;
    private boolean sarcophagusBroken;

    public PharaohSpeedAbility(double unarmoredSpeed) {
        super(0.0);
        if (!Double.isFinite(unarmoredSpeed) || unarmoredSpeed <= 0.0) {
            throw new IllegalArgumentException(
                    "unarmoredSpeed must be finite and positive");
        }
        this.unarmoredSpeed = unarmoredSpeed;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (sarcophagusBroken || zombie == null || zombie.isDead()) {
            return false;
        }

        Armor armor = zombie.getArmor();
        if (armor == null
                || armor.getType() != ArmorType.SARCOPHAGUS
                || !armor.isDestroyed()) {
            return false;
        }

        sarcophagusBroken = true;
        return true;
    }

    public double getEffectiveSpeed(double armoredSpeed) {
        return sarcophagusBroken ? unarmoredSpeed : armoredSpeed;
    }

    public double getUnarmoredSpeed() {
        return unarmoredSpeed;
    }

    public boolean isSarcophagusBroken() {
        return sarcophagusBroken;
    }
}
