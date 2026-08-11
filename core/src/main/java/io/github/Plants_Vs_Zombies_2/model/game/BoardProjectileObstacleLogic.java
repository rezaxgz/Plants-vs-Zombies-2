package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.PushedObstacle;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.RollingBarrel;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.BouncingGrape;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.Projectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.ChillOnHitAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.JuggleAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.LaunchAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.ZombieAbility;

abstract class BoardProjectileObstacleLogic extends BoardProjectileImpactLogic {
    protected BoardProjectileObstacleLogic() {
        super();
    }

    protected BoardProjectileObstacleLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    BasePlant findFirstOctopusCoveredPlantHit(
            Projectile projectile) {
        BasePlant firstPlant = null;
        double firstParameter = Double.POSITIVE_INFINITY;
        for (BasePlant plant : getPlants()) {
            if (!plant.isCoveredByOctopus()
                    || plant.getEntityPosition() == null) {
                continue;
            }
            EntityPosition position = plant.getEntityPosition();
            double parameter = projectile.getIntersectionParameter(
                    position.getRow(), position.getColumn(),
                    PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(parameter)
                    && parameter > POSITION_EPSILON
                    && parameter < firstParameter) {
                firstParameter = parameter;
                firstPlant = plant;
            }
        }
        return firstPlant;
    }

    boolean isOctopusBeforeOtherTargets(
            Projectile projectile, BasePlant octopusPlant,
            PushedObstacle pushedObstacle,
            BasePlant frozenPlant, Zombie zombie) {
        EntityPosition octopusPosition = octopusPlant.getEntityPosition();
        double octopusParameter = projectile.getIntersectionParameter(
                octopusPosition.getRow(),
                octopusPosition.getColumn(),
                PROJECTILE_COLLISION_RADIUS);
        if (Double.isNaN(octopusParameter)) {
            return false;
        }

        if (pushedObstacle != null) {
            double obstacleParameter = projectile.getIntersectionParameter(
                    pushedObstacle.getLane(),
                    pushedObstacle.getColumnPosition(),
                    PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(obstacleParameter)
                    && obstacleParameter + POSITION_EPSILON < octopusParameter) {
                return false;
            }
        }

        if (frozenPlant != null) {
            EntityPosition frozenPosition = frozenPlant.getEntityPosition();
            double frozenParameter = projectile.getIntersectionParameter(
                    frozenPosition.getRow(),
                    frozenPosition.getColumn(),
                    PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(frozenParameter)
                    && frozenParameter + POSITION_EPSILON < octopusParameter) {
                return false;
            }
        }

        if (zombie != null) {
            double zombieParameter = projectile.getIntersectionParameter(
                    zombie.getLane(),
                    zombie.getColumnPosition(),
                    PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(zombieParameter)
                    && zombieParameter + POSITION_EPSILON < octopusParameter) {
                return false;
            }
        }
        return true;
    }

    PushedObstacle findFirstPushedObstacleHit(
            Projectile projectile) {
        PushedObstacle firstObstacle = null;
        double firstParameter = Double.POSITIVE_INFINITY;
        for (PushedObstacle obstacle : getPushedObstacles()) {
            double parameter = projectile.getIntersectionParameter(
                    obstacle.getLane(),
                    obstacle.getColumnPosition(),
                    PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(parameter)
                    && parameter > POSITION_EPSILON
                    && parameter < firstParameter) {
                firstParameter = parameter;
                firstObstacle = obstacle;
            }
        }
        return firstObstacle;
    }

    boolean isPushedObstacleBeforeOtherTargets(
            Projectile projectile, PushedObstacle obstacle,
            BasePlant frozenPlant, Zombie zombie) {
        double obstacleParameter = projectile.getIntersectionParameter(
                obstacle.getLane(),
                obstacle.getColumnPosition(),
                PROJECTILE_COLLISION_RADIUS);
        if (Double.isNaN(obstacleParameter)) {
            return false;
        }

        if (frozenPlant != null) {
            EntityPosition position = frozenPlant.getEntityPosition();
            double plantParameter = projectile.getIntersectionParameter(
                    position.getRow(),
                    position.getColumn(),
                    PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(plantParameter)
                    && plantParameter + POSITION_EPSILON < obstacleParameter) {
                return false;
            }
        }

        if (zombie != null) {
            double zombieParameter = projectile.getIntersectionParameter(
                    zombie.getLane(),
                    zombie.getColumnPosition(),
                    PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(zombieParameter)
                    && zombieParameter + POSITION_EPSILON < obstacleParameter) {
                return false;
            }
        }
        return true;
    }

    void damagePushedObstacle(
            PushedObstacle obstacle, int damage) {
        if (obstacle == null || obstacle.isDestroyed()) {
            return;
        }
        obstacle.takeDamage(damage);
        handleDestroyedPushedObstacle(obstacle);
    }

    void handleDestroyedPushedObstacle(
            PushedObstacle obstacle) {
        if (obstacle == null || !obstacle.isDestroyed()) {
            return;
        }
        if (obstacle instanceof RollingBarrel) {
            releaseBarrelImps((RollingBarrel) obstacle);
        }
    }

    void releaseBarrelImps(RollingBarrel barrel) {
        List<Zombie> imps = barrel.releaseImps();
        for (Zombie imp : imps) {
            addZombie(imp);
            pendingSpawnedZombies.add(imp);
        }
        if (!imps.isEmpty()) {
            pendingResults.add("Rolling barrel released "
                    + imps.size() + " Imp(s) in lane "
                    + barrel.getLane() + ".");
        }
    }

    BasePlant findFirstFrozenPlantHit(Projectile projectile) {
        BasePlant firstPlant = null;
        double firstParameter = Double.POSITIVE_INFINITY;
        for (BasePlant plant : getPlants()) {
            if (!plant.isFrozen() || plant.getEntityPosition() == null) {
                continue;
            }
            EntityPosition position = plant.getEntityPosition();
            double parameter = projectile.getIntersectionParameter(
                    position.getRow(), position.getColumn(),
                    PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(parameter) && parameter > POSITION_EPSILON
                    && parameter < firstParameter) {
                firstParameter = parameter;
                firstPlant = plant;
            }
        }
        return firstPlant;
    }

    BasePlant findFirstFrozenPlantHit(BouncingGrape grape) {
        BasePlant firstPlant = null;
        double firstParameter = Double.POSITIVE_INFINITY;
        for (BasePlant plant : getPlants()) {
            if (!plant.isFrozen() || plant.getEntityPosition() == null) {
                continue;
            }
            EntityPosition position = plant.getEntityPosition();
            double parameter = grape.getIntersectionParameter(
                    position.getRow(), position.getColumn(),
                    GRAPE_COLLISION_RADIUS);
            if (!Double.isNaN(parameter)
                    && parameter < firstParameter) {
                firstParameter = parameter;
                firstPlant = plant;
            }
        }
        return firstPlant;
    }

    boolean isFrozenPlantBeforeZombie(BouncingGrape grape,
            BasePlant frozenPlant, Zombie zombie) {
        if (zombie == null) {
            return true;
        }
        EntityPosition position = frozenPlant.getEntityPosition();
        double plantParameter = grape.getIntersectionParameter(
                position.getRow(), position.getColumn(),
                GRAPE_COLLISION_RADIUS);
        double zombieParameter = grape.getIntersectionParameter(
                zombie.getLane(), zombie.getColumnPosition(),
                GRAPE_COLLISION_RADIUS);
        return Double.isNaN(zombieParameter)
                || plantParameter <= zombieParameter + POSITION_EPSILON;
    }

    boolean isFrozenPlantBeforeZombie(Projectile projectile,
            BasePlant frozenPlant, Zombie zombie) {
        if (zombie == null) {
            return true;
        }
        EntityPosition position = frozenPlant.getEntityPosition();
        double plantParameter = projectile.getIntersectionParameter(
                position.getRow(), position.getColumn(),
                PROJECTILE_COLLISION_RADIUS);
        double zombieParameter = projectile.getIntersectionParameter(
                zombie.getLane(), zombie.getColumnPosition(),
                PROJECTILE_COLLISION_RADIUS);
        return Double.isNaN(zombieParameter)
                || plantParameter <= zombieParameter + POSITION_EPSILON;
    }

    void extinguishProspectorDynamite(
            Projectile projectile, Zombie target) {
        if (projectile == null || target == null
                || !projectile.hasChillEffect()) {
            return;
        }
        for (ZombieAbility ability : target.getAbilities()) {
            if (ability instanceof LaunchAbility
                    && ((LaunchAbility) ability).extinguish()) {
                pendingResults.add(target.getName()
                        + "'s dynamite was extinguished by ice.");
                return;
            }
        }
    }

    boolean tryReflectProjectile(Projectile projectile,
            Zombie target) {
        if (projectile == null || target == null
                || projectile.isRemoved()) {
            return false;
        }
        for (ZombieAbility ability : target.getAbilities()) {
            if (!(ability instanceof JuggleAbility)
                    || !((JuggleAbility) ability)
                            .tryReflect(target, projectile, asBoard())) {
                continue;
            }

            BasePlant source = findProjectileSourcePlant(projectile);
            int reflectedDamage = Math.max(1, projectile.getImpactDamage());
            if (source != null && !source.isDestroyed()) {
                boolean frozenByReflection = false;
                if (projectile.hasChillEffect()
                        && !source.isDisabled()) {
                    frozenByReflection = source.applyIceHit();
                }
                source.takeDamage(reflectedDamage);
                String freezeResult = projectile.hasChillEffect()
                        ? ", raising it to freeze level "
                                + source.getFreezeLevel() + "/"
                                + BasePlant.MAX_FREEZE_LEVEL + "."
                                + (frozenByReflection
                                        ? " The plant is frozen inside a "
                                                + BasePlant.ICE_SHELL_HIT_POINTS
                                                + " HP ice shell."
                                        : "")
                        : ".";
                pendingResults.add(target.getName()
                        + " reflected " + reflectedDamage
                        + " damage back to " + source.getName()
                        + freezeResult);
                reportDestroyedPlant(source);
            } else {
                pendingResults.add(target.getName()
                        + " caught and discarded a projectile.");
            }
            projectile.markForRemoval();
            return true;
        }
        return false;
    }

    void chillProjectileSourceIfBlockhead(Projectile projectile,
            Zombie blockhead) {
        for (ZombieAbility ability : blockhead.getAbilities()) {
            if (!(ability instanceof ChillOnHitAbility)
                    || !ability.tryUse(blockhead, asBoard())) {
                continue;
            }
            BasePlant source = findProjectileSourcePlant(projectile);
            if (source == null || source.isDisabled()) {
                return;
            }
            boolean frozen = source.applyIceHit();
            pendingResults.add(source.getName() + " received an ice hit from "
                    + blockhead.getName() + ", raising it to freeze level "
                    + source.getFreezeLevel() + "/"
                    + BasePlant.MAX_FREEZE_LEVEL + "."
                    + (frozen
                            ? " The plant is frozen inside a "
                                    + BasePlant.ICE_SHELL_HIT_POINTS
                                    + " HP ice shell."
                            : ""));
            return;
        }
    }

    BasePlant findProjectileSourcePlant(Projectile projectile) {
        BasePlant nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (BasePlant plant : getPlants()) {
            if (plant.getEntityPosition() == null
                    || !plant.getName().equalsIgnoreCase(
                            projectile.getSourcePlantName())) {
                continue;
            }
            double rowDistance = Math.abs(
                    plant.getEntityPosition().getRow()
                            - projectile.getSourceRowPosition());
            if (rowDistance > PROJECTILE_COLLISION_RADIUS) {
                continue;
            }
            double columnDistance = Math.abs(
                    plant.getEntityPosition().getColumn()
                            - projectile.getSourceColumnPosition());
            if (columnDistance < nearestDistance) {
                nearest = plant;
                nearestDistance = columnDistance;
            }
        }
        return nearest;
    }

    Zombie findFirstZombieHit(Projectile projectile) {
        Zombie firstTarget = null;
        double firstParameter = Double.POSITIVE_INFINITY;
        for (Zombie zombie : getZombies()) {
            if (zombie.isDead() || zombie.isHypnotized() || zombie.isSubmerged()
                    || !canProjectileHit(projectile, zombie)) {
                continue;
            }
            double parameter = projectile.getIntersectionParameter(
                    zombie.getLane(), zombie.getColumnPosition(), PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(parameter) && parameter < firstParameter) {
                firstParameter = parameter;
                firstTarget = zombie;
            }
        }
        return firstTarget;
    }

    boolean isProjectileOutsideBoard(Projectile projectile) {
        return projectile.getRowPosition() < -PROJECTILE_BOARD_MARGIN
                || projectile.getRowPosition() > numberOfRows - 1 + PROJECTILE_BOARD_MARGIN
                || projectile.getColumnPosition() < -PROJECTILE_BOARD_MARGIN
                || projectile.getColumnPosition() > numberOfColumns - 1 + PROJECTILE_BOARD_MARGIN;
    }
}
