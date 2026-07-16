package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.EntityPosition;
import model.game.entities.plants.BasePlant;
import model.game.entities.zombies.Zombie;

/**
 * Fisherman stays at the right edge and periodically pulls the nearest plant
 * in its lane one tile toward itself. An adjacent hooked plant is destroyed.
 */
public class FishingHookAbility extends ZombieAbility {
    private final double castTimePerGrid;
    private final double delayBeforeReeling;
    private final double delayBetweenCasting;
    private final double reelTimePerGrid;

    private BasePlant lastTarget;
    private EntityPosition lastFromPosition;
    private EntityPosition lastToPosition;
    private boolean lastTargetDestroyed;

    public FishingHookAbility(double castTimePerGrid,
            double delayBeforeReeling,
            double delayBetweenCasting,
            double reelTimePerGrid) {
        super(delayBetweenCasting);
        if (!Double.isFinite(castTimePerGrid)
                || !Double.isFinite(delayBeforeReeling)
                || !Double.isFinite(delayBetweenCasting)
                || !Double.isFinite(reelTimePerGrid)
                || castTimePerGrid < 0.0
                || delayBeforeReeling < 0.0
                || delayBetweenCasting < 0.0
                || reelTimePerGrid < 0.0) {
            throw new IllegalArgumentException(
                    "fishing hook timing values are invalid");
        }
        this.castTimePerGrid = castTimePerGrid;
        this.delayBeforeReeling = delayBeforeReeling;
        this.delayBetweenCasting = delayBetweenCasting;
        this.reelTimePerGrid = reelTimePerGrid;
    }

    @Override
    public boolean tryUse(Zombie fisherman, Board board) {
        clearLastResult();
        if (!canUse() || fisherman == null || board == null
                || fisherman.isDead() || fisherman.isHypnotized()
                || fisherman.isFrozen() || fisherman.isStunned()) {
            return false;
        }

        BasePlant target = findNearestPlantInLane(
                fisherman, board);
        if (target == null) {
            return false;
        }

        EntityPosition from = target.getEntityPosition();
        int rightEdge = board.getNumberOfColumns() - 1;
        if (from.getColumn() >= rightEdge - 1) {
            target.takeDamage(Integer.MAX_VALUE);
            board.removeEntity(target);
            lastTarget = target;
            lastFromPosition = from;
            lastToPosition = from;
            lastTargetDestroyed = true;
            resetCooldown();
            return true;
        }

        EntityPosition destination = new EntityPosition(
                from.getRow(), from.getColumn() + 1);
        if (!board.movePlant(target, destination)) {
            return false;
        }

        lastTarget = target;
        lastFromPosition = from;
        lastToPosition = destination;
        resetCooldown();
        return true;
    }

    private BasePlant findNearestPlantInLane(
            Zombie fisherman, Board board) {
        BasePlant nearest = null;
        int greatestColumn = Integer.MIN_VALUE;
        for (BasePlant plant : board.getPlants()) {
            if (plant.isRemoved()
                    || plant.getEntityPosition() == null
                    || plant.getEntityPosition().getRow()
                            != fisherman.getLane()) {
                continue;
            }
            int column = plant.getEntityPosition().getColumn();
            if (column > greatestColumn) {
                greatestColumn = column;
                nearest = plant;
            }
        }
        return nearest;
    }

    private void clearLastResult() {
        lastTarget = null;
        lastFromPosition = null;
        lastToPosition = null;
        lastTargetDestroyed = false;
    }

    public BasePlant getLastTarget() {
        return lastTarget;
    }

    public EntityPosition getLastFromPosition() {
        return lastFromPosition;
    }

    public EntityPosition getLastToPosition() {
        return lastToPosition;
    }

    public boolean wasLastTargetDestroyed() {
        return lastTargetDestroyed;
    }

    public double getCastTimePerGrid() {
        return castTimePerGrid;
    }

    public double getDelayBeforeReeling() {
        return delayBeforeReeling;
    }

    public double getDelayBetweenCasting() {
        return delayBetweenCasting;
    }

    public double getReelTimePerGrid() {
        return reelTimePerGrid;
    }
}
