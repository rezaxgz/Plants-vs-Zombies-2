package io.github.some_example_name.model.game.minigame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.Game;
import io.github.some_example_name.model.game.GameStatus;
import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.zombies.Zombie;
import io.github.some_example_name.model.game.entities.zombies.ZombieType;
import io.github.some_example_name.model.game.special.ConveyorPlacementResult;
import io.github.some_example_name.model.game.special.ConveyorPlantPacket;
import io.github.some_example_name.view.game.WallnutBowlingView;

/**
 * Fully playable Wall-nut Bowling minigame.
 */
public final class WallnutBowling extends Game {
    public static final int NORMAL_WALLNUT_DAMAGE = ZombieType.BASIC.getHitpoints();
    public static final int EXPLOSION_DAMAGE = 1800;

    private static final double COLLISION_RADIUS = 0.45;
    private static final double BOARD_EXIT_MARGIN = 0.50;

    private final WallnutBowlingLevel level;
    private final Random random;
    private final List<BowlingWallnut> rollingWallnuts = new ArrayList<>();
    private long nextWallnutId = 1;

    public WallnutBowling(WallnutBowlingLevel level) {
        this(level, new Random());
    }

    WallnutBowling(WallnutBowlingLevel level, Random random) {
        super(new Board(), null, 0, requireLevel(level).createWaves(), true);
        if (random == null) {
            throw new IllegalArgumentException("random source is required");
        }
        this.level = level;
        this.random = random;
        disableSkySuns("Wall-nut Bowling has no falling sun");
        enableConveyorBelt(level.getConveyorPlantTypes());
        addPendingResult("Wall-nut Bowling level " + level.getNumber()
                + " started: " + level.getName() + ".");
        addPendingResult("Launch Wall-nuts only from columns 0 through "
                + level.getRedLineColumn()
                + ", on the house side of the red bowling line.");
        addPendingResult("Normal Wall-nuts deal " + NORMAL_WALLNUT_DAMAGE
                + " damage and bounce; Explode-o-nuts deal "
                + EXPLOSION_DAMAGE + " damage in a 3x3 area; "
                + "Giant Wall-nuts crush every zombie they touch.");
    }

    private static WallnutBowlingLevel requireLevel(
            WallnutBowlingLevel level) {
        if (level == null) {
            throw new IllegalArgumentException(
                    "Wall-nut Bowling level is required");
        }
        return level;
    }

    @Override
    public void update(float deltaSeconds) {
        if (getStatus() != GameStatus.ACTIVE) {
            return;
        }
        updateRollingWallnuts(deltaSeconds);
        super.update(deltaSeconds);
        rollingWallnuts.removeIf(BowlingWallnut::isRemoved);
    }

    @Override
    public ConveyorPlacementResult plantFromConveyor(
            int index, EntityPosition position) {
        if (!hasConveyorBelt()) {
            return ConveyorPlacementResult.NOT_CONVEYOR_LEVEL;
        }
        ConveyorPlantPacket packet = getConveyorPacket(index);
        if (packet == null) {
            return ConveyorPlacementResult.INVALID_PACKET;
        }
        if (!getBoard().isPositionInsideBoard(position)) {
            return ConveyorPlacementResult.INVALID_POSITION;
        }
        if (position.getColumn() > level.getRedLineColumn()) {
            return ConveyorPlacementResult.OUTSIDE_BOWLING_ZONE;
        }
        BowlingWallnutType type = BowlingWallnutType.find(
                packet.getPlantType());
        if (type == null) {
            return ConveyorPlacementResult.UNKNOWN_PLANT;
        }
        if (isLaunchTileOccupied(position)) {
            return ConveyorPlacementResult.POSITION_OCCUPIED;
        }

        BowlingWallnut wallnut = new BowlingWallnut(
                nextWallnutId++, type, position);
        rollingWallnuts.add(wallnut);
        consumeConveyorPacket(index);
        addPendingResult(type.getDisplayName() + " #" + wallnut.getId()
                + " crossed the red line from " + position
                + " and started rolling right.");
        return ConveyorPlacementResult.SUCCESS;
    }

    private boolean isLaunchTileOccupied(EntityPosition position) {
        for (BowlingWallnut wallnut : rollingWallnuts) {
            if (!wallnut.isRemoved()
                    && Math.abs(wallnut.getRowPosition()
                            - position.getRow()) <= COLLISION_RADIUS
                    && Math.abs(wallnut.getColumnPosition()
                            - position.getColumn()) <= COLLISION_RADIUS) {
                return true;
            }
        }
        for (Zombie zombie : getBoard().getZombies()) {
            if (!zombie.isDead() && zombie.getLane() == position.getRow()
                    && Math.abs(zombie.getColumnPosition()
                            - position.getColumn()) <= COLLISION_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private void updateRollingWallnuts(float deltaSeconds) {
        for (BowlingWallnut wallnut : new ArrayList<>(rollingWallnuts)) {
            if (wallnut.isRemoved()) {
                continue;
            }
            int boundaryTurn = wallnut.advance(deltaSeconds,
                    getBoard().getNumberOfRows());
            if (boundaryTurn > 0) {
                addPendingResult(WallnutBowlingView.formatWallnut(wallnut)
                        + " hit the top or bottom edge and turned "
                        + boundaryTurn + " degrees "
                        + wallnut.getDirectionDescription() + ".");
            }
            resolveWallnutCollision(wallnut);
            removeWallnutOutsideBoard(wallnut);
        }
    }

    private void resolveWallnutCollision(BowlingWallnut wallnut) {
        Zombie target = findCollisionTarget(wallnut);
        if (target == null) {
            return;
        }
        switch (wallnut.getType()) {
            case NORMAL:
                hitWithNormalWallnut(wallnut, target);
                break;
            case EXPLOSIVE:
                explodeWallnut(wallnut, target);
                break;
            case LARGE:
                crushWithLargeWallnut(wallnut, target);
                break;
            default:
                throw new IllegalStateException("unknown bowling Wall-nut");
        }
    }

    private Zombie findCollisionTarget(BowlingWallnut wallnut) {
        List<Zombie> candidates = new ArrayList<>();
        for (Zombie zombie : getBoard().getZombies()) {
            if (zombie.isDead() || zombie.isHypnotized()
                    || wallnut.hasHit(zombie)) {
                continue;
            }
            double rowDistance = Math.abs(wallnut.getRowPosition()
                    - zombie.getLane());
            double columnDistance = Math.abs(wallnut.getColumnPosition()
                    - zombie.getColumnPosition());
            if (rowDistance <= COLLISION_RADIUS
                    && columnDistance <= COLLISION_RADIUS) {
                candidates.add(zombie);
            }
        }
        return candidates.stream()
                .min(Comparator.comparingDouble(zombie -> Math.abs(wallnut.getColumnPosition()
                        - zombie.getColumnPosition())))
                .orElse(null);
    }

    private void hitWithNormalWallnut(BowlingWallnut wallnut,
            Zombie target) {
        wallnut.recordHit(target);
        target.takeDamage(NORMAL_WALLNUT_DAMAGE);
        int turnDegrees = wallnut.turnAfterZombieImpact(
                getBoard().getNumberOfRows(), random);
        addPendingResult(WallnutBowlingView.formatWallnut(wallnut) + " hit "
                + target.getName() + " for " + NORMAL_WALLNUT_DAMAGE
                + " damage and turned " + turnDegrees + " degrees "
                + wallnut.getDirectionDescription() + ".");
    }

    private void explodeWallnut(BowlingWallnut wallnut,
            Zombie firstTarget) {
        int affected = 0;
        int centerLane = firstTarget.getLane();
        double centerColumn = firstTarget.getColumnPosition();
        for (Zombie zombie : new ArrayList<>(getBoard().getZombies())) {
            if (zombie.isDead() || zombie.isHypnotized()) {
                continue;
            }
            if (Math.abs(zombie.getLane() - centerLane) <= 1
                    && Math.abs(zombie.getColumnPosition()
                            - centerColumn) <= 1.0) {
                zombie.takeDamage(EXPLOSION_DAMAGE);
                affected++;
            }
        }
        wallnut.markForRemoval();
        addPendingResult(WallnutBowlingView.formatWallnut(wallnut) + " hit "
                + firstTarget.getName() + " and exploded for "
                + EXPLOSION_DAMAGE + " damage across a 3x3 area, hitting "
                + affected + " zombie(s).");
    }

    private void crushWithLargeWallnut(BowlingWallnut wallnut,
            Zombie target) {
        wallnut.recordHit(target);
        target.kill();
        addPendingResult(WallnutBowlingView.formatWallnut(wallnut) + " crushed "
                + target.getName() + " and kept rolling straight.");
    }

    private void removeWallnutOutsideBoard(BowlingWallnut wallnut) {
        if (wallnut.isRemoved()) {
            return;
        }
        double exitColumn = getBoard().getNumberOfColumns()
                + BOARD_EXIT_MARGIN;
        if (wallnut.getColumnPosition() <= exitColumn) {
            return;
        }
        wallnut.markForRemoval();
        addPendingResult(WallnutBowlingView.formatWallnut(wallnut)
                + " rolled out of the right side of the lawn.");
    }

    @Override
    public boolean allowsDirectPlanting() {
        return false;
    }

    @Override
    public String getDirectPlantingDisabledMessage() {
        return "ordinary planting is disabled in Wall-nut Bowling; "
                + "use a Conveyor Belt Wall-nut instead!";
    }

    @Override
    protected boolean shouldProcessZombieDeathDrops() {
        return false;
    }

    public WallnutBowlingLevel getLevel() {
        return level;
    }

    public int getRedLineColumn() {
        return level.getRedLineColumn();
    }

    public List<BowlingWallnut> getRollingWallnuts() {
        List<BowlingWallnut> active = new ArrayList<>();
        for (BowlingWallnut wallnut : rollingWallnuts) {
            if (!wallnut.isRemoved()) {
                active.add(wallnut);
            }
        }
        return Collections.unmodifiableList(active);
    }

    public List<BowlingWallnut> getRollingWallnutsAt(
            int row, int column) {
        List<BowlingWallnut> result = new ArrayList<>();
        for (BowlingWallnut wallnut : getRollingWallnuts()) {
            if ((int) Math.floor(wallnut.getRowPosition() + 0.5) == row
                    && (int) Math.floor(wallnut.getColumnPosition()) == column) {
                result.add(wallnut);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public String describeRollingWallnut(BowlingWallnut wallnut) {
        return WallnutBowlingView.describeRollingWallnut(wallnut);
    }
}
