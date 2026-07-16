package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.plants.BasePlant;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.armor.Armor;
import model.game.entities.zombies.armor.ArmorType;

/**
 * Surfer moves at surfing speed while its board is intact. The first plant
 * reached is crushed by the board; afterwards the zombie walks normally.
 */
public class SurfAbility extends ZombieAbility {
    private static final double CRUSH_DISTANCE_TILES = 0.45;

    private final double surfingSpeed;
    private boolean boardActive = true;

    public SurfAbility(double surfingSpeed) {
        super(0.0);
        if (!Double.isFinite(surfingSpeed) || surfingSpeed <= 0.0) {
            throw new IllegalArgumentException(
                    "surfingSpeed must be finite and positive");
        }
        this.surfingSpeed = surfingSpeed;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (zombie == null || zombie.isDead()) {
            boardActive = false;
            return false;
        }
        boardActive = hasIntactSurfboard(zombie);
        return boardActive;
    }

    public boolean tryCrush(Zombie zombie, BasePlant plant,
            Board board) {
        if (!tryUse(zombie, board) || plant == null
                || plant.isRemoved()
                || plant.getEntityPosition() == null
                || plant.getEntityPosition().getRow() != zombie.getLane()) {
            return false;
        }

        double distance = zombie.getColumnPosition()
                - plant.getEntityPosition().getColumn();
        if (distance < 0.0 || distance > CRUSH_DISTANCE_TILES) {
            return false;
        }

        discardSurfboard(zombie);
        return true;
    }

    private void discardSurfboard(Zombie zombie) {
        Armor armor = zombie.getArmor();
        if (armor != null && armor.getType() == ArmorType.SURFBOARD
                && !armor.isDestroyed()) {
            armor.takeDamage(armor.getCurrentHealth());
        }
        boardActive = false;
    }

    private boolean hasIntactSurfboard(Zombie zombie) {
        Armor armor = zombie.getArmor();
        return armor != null
                && armor.getType() == ArmorType.SURFBOARD
                && !armor.isDestroyed();
    }

    public double getEffectiveSpeed(double baseSpeed) {
        return boardActive ? surfingSpeed : Zombie.DEFAULT_SPEED;
    }

    public double getSurfingSpeed() {
        return surfingSpeed;
    }

    public boolean isBoardActive() {
        return boardActive;
    }
}
