package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantTag;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.wallnut.Wallnut;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.wallnut.WallnutPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * Dodo Rider flies over defensive, lane-changing, and explosive obstacles.
 * Tall-nut remains too high to cross.
 */
public class FlyAbility extends ZombieAbility {
    private final int maxGridSquaresToFly;
    private final double jumpChance;
    private boolean flying;

    public FlyAbility(int maxGridSquaresToFly,
            double jumpChance) {
        super(0.0);
        if (maxGridSquaresToFly <= 0) {
            throw new IllegalArgumentException(
                    "maxGridSquaresToFly must be positive");
        }
        if (!Double.isFinite(jumpChance)
                || jumpChance < 0.0
                || jumpChance > 1.0) {
            throw new IllegalArgumentException(
                    "jumpChance must be finite and in [0, 1]");
        }
        this.maxGridSquaresToFly = maxGridSquaresToFly;
        this.jumpChance = jumpChance;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        return zombie != null && board != null
                && !zombie.isDead()
                && !zombie.isHypnotized()
                && !zombie.isFrozen()
                && !zombie.isStunned();
    }

    public boolean tryFlyOver(Zombie zombie,
            BasePlant plant, Board board) {
        if (flying || !tryUse(zombie, board)
                || !canFlyOver(plant)) {
            return false;
        }
        double distanceAhead = zombie.getColumnPosition()
                - plant.getEntityPosition().getColumn();
        if (distanceAhead < 0.0
                || distanceAhead > maxGridSquaresToFly) {
            return false;
        }

        double landingColumn = Math.max(0.0,
                plant.getEntityPosition().getColumn()
                        - Math.max(0.5,
                                maxGridSquaresToFly - 1.0));
        flying = true;
        zombie.setFlying(true);
        zombie.moveTo(landingColumn);
        return true;
    }

    public boolean canFlyOver(BasePlant plant) {
        if (plant == null || plant.isRemoved()
                || plant.getEntityPosition() == null) {
            return false;
        }
        if (plant instanceof Wallnut) {
            WallnutPlantType type = ((Wallnut) plant).getType();
            return type != WallnutPlantType.TALL_NUT
                    && type != WallnutPlantType.REINFORCE_MINT;
        }
        return plant.getTags().contains(
                PlantTag.MOVE_ZOMBIES)
                || plant.getTags().contains(
                        PlantTag.EXPLOSIVE);
    }

    public void finishFlight(Zombie zombie) {
        flying = false;
        if (zombie != null) {
            zombie.setFlying(false);
        }
    }

    public boolean isFlying() {
        return flying;
    }

    public int getMaxGridSquaresToFly() {
        return maxGridSquaresToFly;
    }

    public double getJumpChance() {
        return jumpChance;
    }
}
