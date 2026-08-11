package model.game;

import java.util.ArrayList;
import java.util.List;
import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.other.ArcadeMachine;
import model.game.entities.other.IceBlock;
import model.game.entities.other.PushedObstacle;
import model.game.entities.other.RollingBarrel;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.wallnut.Wallnut;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.ZombieType;
import model.game.entities.zombies.abilities.ArcadePushAbility;
import model.game.entities.zombies.abilities.BarrelPushAbility;
import model.game.entities.zombies.abilities.IceBlockPushAbility;
import model.game.entities.zombies.abilities.LaunchAbility;
import model.game.entities.zombies.abilities.PianoCrushAbility;
import model.game.entities.zombies.abilities.ZombieAbility;
import model.game.tile.Tile;
import model.game.tile.TileType;

abstract class BoardZombieMovementLogic extends BoardModifierLogic {
    protected BoardZombieMovementLogic() {
        super();
    }

    protected BoardZombieMovementLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    void updateZombies(List<Entity> updateSnapshot, float deltaSeconds) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Zombie) || entity.isRemoved()) {
                continue;
            }
            Zombie zombie = (Zombie) entity;
            if (zombie.isDead()) {
                reportZombieDeath(zombie);
                continue;
            }
            if (zombie.getType().isBoss()) {
                continue;
            }
            if (zombie.isEncasedInIce()) {
                warmFrozenZombie(zombie, deltaSeconds);
                continue;
            }
            if (zombie.isHypnotized()) {
                updateHypnotizedZombie(zombie, deltaSeconds);
            } else {
                updateTroglobiteIceBlocks(zombie);
                updatePushedObjects(zombie);
                updatePianoMusic(zombie);
                attractZombieToSweetPotato(zombie);
                if (!updateProspector(zombie, deltaSeconds)) {
                    updateZombie(zombie, deltaSeconds);
                }
                updateTroglobiteIceBlocks(zombie);
                updatePushedObjects(zombie);
            }
            applySliderTile(zombie);
        }
    }

    void warmFrozenZombie(Zombie zombie,
            float deltaSeconds) {
        if (deltaSeconds <= 0.0f
                || !hasAdjacentFirePlant(zombie)) {
            return;
        }
        boolean released = zombie.meltFrozenShell(
                FROZEN_SHELL_WARMING_DAMAGE_PER_SECOND
                        * deltaSeconds);
        if (released) {
            releaseFrozenZombie(zombie);
        }
    }

    boolean hasAdjacentFirePlant(Zombie zombie) {
        return zombie != null
                && hasAdjacentActiveFirePlant(
                        zombie.getEntityPosition(), null);
    }

    void applySliderTile(Zombie zombie) {
        if (zombie.getType() == ZombieType.DODO
                || zombie.isFlying()) {
            zombie.clearSliderTrigger();
            return;
        }
        int column = (int) Math.floor(
                zombie.getColumnPosition());
        EntityPosition position = new EntityPosition(
                zombie.getLane(), column);
        Tile tile = getTileAt(position);
        if (tile == null
                || (tile.getTileType() != TileType.SLIDER_UP
                        && tile.getTileType()
                                != TileType.SLIDER_DOWN)) {
            zombie.clearSliderTrigger();
            return;
        }
        if (!zombie.markSliderTriggered(column)) {
            return;
        }
        int laneDelta = tile.getTileType()
                == TileType.SLIDER_UP ? -1 : 1;
        int targetLane = zombie.getLane() + laneDelta;
        if (targetLane < 0 || targetLane >= numberOfRows) {
            return;
        }
        int sourceLane = zombie.getLane();
        zombie.moveToLane(targetLane);
        pendingResults.add(zombie.getName()
                + " slid from lane " + sourceLane
                + " to lane " + targetLane + " at column "
                + column + ".");
    }

    void updatePushedObjects(Zombie zombie) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof ArcadePushAbility) {
                updateArcadeMachine(zombie,
                        (ArcadePushAbility) ability);
            } else if (ability instanceof BarrelPushAbility) {
                updateRollingBarrel(zombie,
                        (BarrelPushAbility) ability);
            }
        }
    }

    void updateArcadeMachine(
            Zombie zombie, ArcadePushAbility ability) {
        if (!ability.tryUse(zombie, asBoard())) {
            return;
        }
        ArcadeMachine machine = ability.getMachine();
        if (ability.didSpawnThisUse()) {
            pendingResults.add(zombie.getName()
                    + " started pushing an arcade machine.");
        }
        crushWithPushedObstacle(machine);
    }

    void updateRollingBarrel(
            Zombie zombie, BarrelPushAbility ability) {
        if (!ability.tryUse(zombie, asBoard())) {
            return;
        }
        RollingBarrel barrel = ability.getBarrel();
        if (ability.didSpawnThisUse()) {
            pendingResults.add(zombie.getName()
                    + " started pushing a rolling barrel.");
        }
        crushWithPushedObstacle(barrel);
    }

    void crushWithPushedObstacle(
            PushedObstacle obstacle) {
        if (obstacle == null || obstacle.isDestroyed()) {
            return;
        }
        crushPlantsWithPushedObstacle(obstacle);
        crushHypnotizedZombiesWithPushedObstacle(obstacle);
    }

    void crushPlantsWithPushedObstacle(
            PushedObstacle obstacle) {
        for (BasePlant plant :
                new ArrayList<>(getPlants())) {
            if (plant.getEntityPosition() == null
                    || plant.getEntityPosition().getRow()
                            != obstacle.getLane()
                    || Math.abs(
                            plant.getEntityPosition().getColumn()
                            - obstacle.getColumnPosition())
                            > PushedObstacle.COLLISION_RADIUS_TILES) {
                continue;
            }
            plant.takeDamage(Integer.MAX_VALUE);
            pendingResults.add(obstacle.getDisplayName()
                    + " crushed " + plant.getName()
                    + " at " + plant.getEntityPosition() + ".");
            reportDestroyedPlant(plant);
        }
    }

    void crushHypnotizedZombiesWithPushedObstacle(
            PushedObstacle obstacle) {
        for (Zombie target :
                new ArrayList<>(getZombies())) {
            if (!target.isHypnotized() || target.isDead()
                    || target.getLane() != obstacle.getLane()
                    || Math.abs(target.getColumnPosition()
                            - obstacle.getColumnPosition())
                            > PushedObstacle.COLLISION_RADIUS_TILES) {
                continue;
            }
            target.kill();
            pendingResults.add(obstacle.getDisplayName()
                    + " crushed hypnotized "
                    + target.getName() + ".");
            reportZombieDeath(target);
        }
    }

    void updatePianoMusic(Zombie pianoZombie) {
        for (ZombieAbility ability : pianoZombie.getAbilities()) {
            if (!(ability instanceof PianoCrushAbility)
                    || !ability.tryUse(pianoZombie, asBoard())) {
                continue;
            }
            PianoCrushAbility piano =
                    (PianoCrushAbility) ability;
            pendingResults.add(pianoZombie.getName()
                    + " changed the lane of "
                    + piano.getLastMovedZombieCount()
                    + " zombie(s) with piano music.");
        }
    }

    boolean updateProspector(
            Zombie prospector, float deltaSeconds) {
        for (ZombieAbility ability : prospector.getAbilities()) {
            if (!(ability instanceof LaunchAbility)) {
                continue;
            }

            LaunchAbility launch = (LaunchAbility) ability;
            launch.tryUse(prospector, asBoard());
            if (launch.didLaunchThisUse()) {
                pendingResults.add(prospector.getName()
                        + " launched to the back of the lawn.");
            }
            if (launch.hasLaunched()) {
                updateReverseMovingProspector(
                        prospector, deltaSeconds);
                return true;
            }
            return false;
        }
        return false;
    }

    void updateReverseMovingProspector(
            Zombie prospector, float deltaSeconds) {
        BasePlant target =
                findNearestPlantToRight(prospector);
        if (target == null) {
            prospector.moveRight(deltaSeconds,
                    numberOfColumns + PROJECTILE_BOARD_MARGIN);
            if (prospector.getColumnPosition()
                    >= numberOfColumns - POSITION_EPSILON) {
                prospector.kill();
                reportZombieDeath(prospector);
            }
            return;
        }

        double attackColumn =
                target.getEntityPosition().getColumn()
                        - Zombie.ATTACK_REACH;
        if (prospector.getColumnPosition() + POSITION_EPSILON
                >= attackColumn) {
            attackPlant(prospector, target, deltaSeconds);
            handleWallnutAfterAttack(
                    prospector, target, deltaSeconds);
            reportDestroyedPlant(target);
        } else {
            prospector.moveRight(deltaSeconds, attackColumn);
        }
    }

    BasePlant findNearestPlantToRight(Zombie zombie) {
        BasePlant nearest = null;
        int nearestColumn = Integer.MAX_VALUE;
        for (BasePlant plant : getPlants()) {
            if (plant.isRemoved()
                    || plant.getEntityPosition() == null
                    || plant.getEntityPosition().getRow()
                            != zombie.getLane()) {
                continue;
            }
            int column =
                    plant.getEntityPosition().getColumn();
            if (column + POSITION_EPSILON
                    >= zombie.getColumnPosition()
                    && column < nearestColumn) {
                nearest = plant;
                nearestColumn = column;
            }
        }
        return nearest;
    }

    void updateTroglobiteIceBlocks(Zombie zombie) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (!(ability instanceof IceBlockPushAbility)
                    || !ability.tryUse(zombie, asBoard())) {
                continue;
            }
            IceBlockPushAbility push = (IceBlockPushAbility) ability;
            if (push.didSpawnThisUse()) {
                pendingResults.add(zombie.getName() + " started pushing "
                        + push.getActiveIceBlocks().size() + " ice block(s).");
            }
            for (IceBlock block : push.getActiveIceBlocks()) {
                crushPlantsWithIceBlock(block);
                crushHypnotizedZombiesWithIceBlock(block);
            }
        }
    }

    void crushPlantsWithIceBlock(IceBlock block) {
        for (BasePlant plant : new ArrayList<>(getPlants())) {
            if (plant.getEntityPosition() == null
                    || plant.getEntityPosition().getRow() != block.getLane()
                    || Math.abs(plant.getEntityPosition().getColumn()
                            - block.getColumnPosition())
                            > IceBlock.COLLISION_RADIUS_TILES) {
                continue;
            }
            plant.takeDamage(Integer.MAX_VALUE);
            pendingResults.add("Troglobite ice block crushed "
                    + plant.getName() + " at " + plant.getEntityPosition() + ".");
            reportDestroyedPlant(plant);
        }
    }

    void crushHypnotizedZombiesWithIceBlock(IceBlock block) {
        for (Zombie target : new ArrayList<>(getZombies())) {
            if (!target.isHypnotized() || target.isDead()
                    || target.getLane() != block.getLane()
                    || Math.abs(target.getColumnPosition()
                            - block.getColumnPosition())
                            > IceBlock.COLLISION_RADIUS_TILES) {
                continue;
            }
            target.kill();
            pendingResults.add("Troglobite ice block crushed hypnotized "
                    + target.getName() + ".");
            reportZombieDeath(target);
        }
    }

    void attractZombieToSweetPotato(Zombie zombie) {
        for (BasePlant plant : getPlants()) {
            if (!(plant instanceof Wallnut)) {
                continue;
            }
            Wallnut wallnut = (Wallnut) plant;
            if (!wallnut.attractsAdjacentLanes() || wallnut.getEntityPosition() == null) {
                continue;
            }
            int targetLane = wallnut.getEntityPosition().getRow();
            double distance = Math.abs(zombie.getColumnPosition()
                    - wallnut.getEntityPosition().getColumn());
            if (Math.abs(zombie.getLane() - targetLane) == 1
                    && distance <= SWEET_POTATO_ATTRACTION_RANGE) {
                zombie.moveToLane(targetLane);
                return;
            }
        }
    }

    void updateHypnotizedZombie(
            Zombie zombie, float deltaSeconds) {
        Zombie target =
                findNearestHostileZombieToRight(zombie);
        PushedObstacle obstacle =
                findNearestPushedObstacleToRight(zombie);

        if (obstacle != null
                && (target == null
                || obstacle.getColumnPosition()
                        <= target.getColumnPosition())) {
            updateHypnotizedZombieAgainstObstacle(
                    zombie, obstacle, deltaSeconds);
            return;
        }

        if (target == null) {
            zombie.moveRight(deltaSeconds,
                    numberOfColumns + PROJECTILE_BOARD_MARGIN);
            if (zombie.getColumnPosition()
                    >= numberOfColumns - POSITION_EPSILON) {
                zombie.markForRemoval();
            }
            return;
        }

        double attackColumn =
                target.getColumnPosition() - Zombie.ATTACK_REACH;
        if (zombie.getColumnPosition() + POSITION_EPSILON
                >= attackColumn) {
            zombie.attackZombie(target, deltaSeconds);
            if (target.isDead()) {
                reportZombieDeath(target);
            }
        } else {
            zombie.moveRight(deltaSeconds, attackColumn);
        }
    }

    PushedObstacle findNearestPushedObstacleToRight(
            Zombie zombie) {
        PushedObstacle nearest = null;
        double nearestColumn = Double.POSITIVE_INFINITY;
        for (PushedObstacle obstacle : getPushedObstacles()) {
            double column = obstacle.getColumnPosition();
            if (obstacle.getLane() != zombie.getLane()
                    || column + Zombie.ATTACK_REACH
                            < zombie.getColumnPosition()
                    || column >= nearestColumn) {
                continue;
            }
            nearest = obstacle;
            nearestColumn = column;
        }
        return nearest;
    }

    void updateHypnotizedZombieAgainstObstacle(
            Zombie zombie, PushedObstacle obstacle,
            float deltaSeconds) {
        double attackColumn = obstacle.getColumnPosition()
                - Zombie.ATTACK_REACH;
        if (zombie.getColumnPosition() + POSITION_EPSILON
                >= attackColumn) {
            zombie.attackObstacle(obstacle, deltaSeconds);
            handleDestroyedPushedObstacle(obstacle);
            return;
        }
        zombie.moveRight(deltaSeconds, attackColumn);
    }

    Zombie findNearestHostileZombieToRight(Zombie hypnotizedZombie) {
        Zombie nearest = null;
        double nearestColumn = Double.POSITIVE_INFINITY;
        for (Zombie zombie : getZombies()) {
            if (zombie == hypnotizedZombie || zombie.isDead()
                    || zombie.isHypnotized()
                    || zombie.getLane() != hypnotizedZombie.getLane()) {
                continue;
            }
            double column = zombie.getColumnPosition();
            if (column + Zombie.ATTACK_REACH
                    >= hypnotizedZombie.getColumnPosition() - POSITION_EPSILON
                    && column < nearestColumn) {
                nearest = zombie;
                nearestColumn = column;
            }
        }
        return nearest;
    }

    Zombie findNearestHypnotizedZombieAhead(Zombie zombie) {
        Zombie nearest = null;
        double nearestColumn = Double.NEGATIVE_INFINITY;
        for (Zombie candidate : getZombies()) {
            if (candidate == zombie || candidate.isDead()
                    || !candidate.isHypnotized()
                    || candidate.getLane() != zombie.getLane()) {
                continue;
            }
            double column = candidate.getColumnPosition();
            if (column <= zombie.getColumnPosition() + POSITION_EPSILON
                    && column > nearestColumn) {
                nearest = candidate;
                nearestColumn = column;
            }
        }
        return nearest;
    }

    void updateZombieAgainstHypnotizedTarget(Zombie zombie,
            Zombie target, float deltaSeconds) {
        double attackColumn = target.getColumnPosition() + Zombie.ATTACK_REACH;
        if (zombie.getColumnPosition() <= attackColumn + POSITION_EPSILON) {
            if (tryTackleZombie(zombie, target)) {
                reportZombieDeath(target);
                return;
            }
            zombie.attackZombie(target, deltaSeconds);
            if (target.isDead()) {
                reportZombieDeath(target);
            }
        } else {
            zombie.move(deltaSeconds, attackColumn);
        }
    }
}
