package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.PlantFoodDrop;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.PushedObstacle;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.Sun;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.HomingProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.LobbedProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.PiercingProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.Projectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.structure.BaseStructure;
import io.github.Plants_Vs_Zombies_2.model.game.structure.Grave;
import io.github.Plants_Vs_Zombies_2.model.game.tile.Tile;
import io.github.Plants_Vs_Zombies_2.model.game.tile.TileType;

abstract class BoardProjectileImpactLogic extends BoardPlantAttackLogic {
    protected BoardProjectileImpactLogic() {
        super();
    }

    protected BoardProjectileImpactLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    void resolveProjectileImpacts(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Projectile) || entity instanceof LobbedProjectile
                    || entity instanceof HomingProjectile || entity.isRemoved()) {
                continue;
            }
            Projectile projectile = (Projectile) entity;
            resolveProjectileHits(projectile);
            if (!projectile.isRemoved()
                    && (projectile.hasExpired() || isProjectileOutsideBoard(projectile))) {
                projectile.markForRemoval();
            }
        }
    }

    void resolveProjectileHits(Projectile projectile) {
        while (!projectile.isRemoved()) {
            Grave grave = findFirstGraveHit(projectile);
            PushedObstacle pushedObstacle = findFirstPushedObstacleHit(projectile);
            BasePlant octopusPlant = findFirstOctopusCoveredPlantHit(projectile);
            BasePlant frozenPlant = findFirstFrozenPlantHit(projectile);
            Zombie target = findFirstZombieHit(projectile);

            if (grave != null && isGraveBeforeOtherTargets(
                    projectile, grave, octopusPlant, pushedObstacle,
                    frozenPlant, target)) {
                damageGrave(grave, Math.max(1,
                        projectile.getImpactDamage()));
                projectile.markForRemoval();
                return;
            }

            if (octopusPlant != null
                    && isOctopusBeforeOtherTargets(
                            projectile, octopusPlant,
                            pushedObstacle, frozenPlant, target)) {
                boolean octopusDestroyed = octopusPlant.damageOctopus(1);
                projectile.markForRemoval();
                pendingResults.add(octopusPlant.getName()
                        + "'s octopus cover was hit."
                        + (octopusDestroyed
                                ? " The plant is active again."
                                : ""));
                return;
            }

            if (pushedObstacle != null
                    && isPushedObstacleBeforeOtherTargets(
                            projectile, pushedObstacle,
                            frozenPlant, target)) {
                int damage = Math.max(
                        1, projectile.getImpactDamage());
                damagePushedObstacle(pushedObstacle, damage);
                projectile.markForRemoval();
                pendingResults.add(
                        pushedObstacle.getDisplayName()
                                + " absorbed " + damage
                                + " projectile damage."
                                + (pushedObstacle.isDestroyed()
                                        ? " It was destroyed."
                                        : ""));
                return;
            }

            if (frozenPlant != null
                    && isFrozenPlantBeforeZombie(projectile, frozenPlant, target)) {
                damageFrozenPlantIce(frozenPlant,
                        Math.max(1, projectile.getImpactDamage()),
                        projectile.hasFireEffect());
                projectile.markForRemoval();
                return;
            }
            if (target == null) {
                return;
            }
            if (resolveFrozenZombieImpact(projectile, target)) {
                return;
            }

            extinguishProspectorDynamite(projectile, target);
            if (tryReflectProjectile(projectile, target)) {
                return;
            }
            chillProjectileSourceIfBlockhead(projectile, target);
            projectile.hit(target);
            if (target.isDead()) {
                reportZombieDeath(target);
            }
            if (!(projectile instanceof PiercingProjectile)) {
                return;
            }
        }
    }

    Grave findFirstGraveHit(Projectile projectile) {
        Grave firstGrave = null;
        double firstParameter = Double.POSITIVE_INFINITY;
        for (BaseStructure structure : getStructures()) {
            if (!(structure instanceof Grave)) {
                continue;
            }
            EntityPosition position = structure.getPosition();
            double parameter = projectile.getIntersectionParameter(
                    position.getRow(), position.getColumn(),
                    PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(parameter)
                    && parameter > POSITION_EPSILON
                    && parameter < firstParameter) {
                firstParameter = parameter;
                firstGrave = (Grave) structure;
            }
        }
        return firstGrave;
    }

    boolean isGraveBeforeOtherTargets(
            Projectile projectile, Grave grave,
            BasePlant octopusPlant, PushedObstacle pushedObstacle,
            BasePlant frozenPlant, Zombie zombie) {
        EntityPosition gravePosition = grave.getPosition();
        double graveParameter = projectile.getIntersectionParameter(
                gravePosition.getRow(), gravePosition.getColumn(),
                PROJECTILE_COLLISION_RADIUS);
        if (Double.isNaN(graveParameter)) {
            return false;
        }
        return graveParameter <= minimumTargetParameter(
                projectile, octopusPlant, pushedObstacle,
                frozenPlant, zombie) + POSITION_EPSILON;
    }

    double minimumTargetParameter(Projectile projectile,
            BasePlant octopusPlant, PushedObstacle pushedObstacle,
            BasePlant frozenPlant, Zombie zombie) {
        double minimum = Double.POSITIVE_INFINITY;
        if (octopusPlant != null) {
            minimum = Math.min(minimum, collisionParameter(
                    projectile, octopusPlant.getEntityPosition()));
        }
        if (pushedObstacle != null) {
            minimum = Math.min(minimum,
                    projectile.getIntersectionParameter(
                            pushedObstacle.getLane(),
                            pushedObstacle.getColumnPosition(),
                            PROJECTILE_COLLISION_RADIUS));
        }
        if (frozenPlant != null) {
            minimum = Math.min(minimum, collisionParameter(
                    projectile, frozenPlant.getEntityPosition()));
        }
        if (zombie != null) {
            minimum = Math.min(minimum,
                    projectile.getIntersectionParameter(
                            zombie.getLane(),
                            zombie.getColumnPosition(),
                            PROJECTILE_COLLISION_RADIUS));
        }
        return minimum;
    }

    void damageGrave(Grave grave, int damage) {
        grave.takeDamage(damage);
        if (grave.isRemoved()) {
            destroyGrave(grave);
            return;
        }
        pendingResults.add("Grave at " + grave.getPosition()
                + " absorbed " + damage + " projectile damage; "
                + grave.getHitPoints() + " HP remains.");
    }

    void destroyGrave(Grave grave) {
        if (grave == null || !structures.remove(grave)) {
            return;
        }
        setTileType(grave.getPosition(),
                grave.getUnderlyingTileType());
        pendingResults.add("Grave at " + grave.getPosition()
                + " was destroyed.");
        releaseGraveReward(grave);
    }

    void releaseGraveReward(Grave grave) {
        switch (grave.getReward()) {
            case SUN:
                addEntity(new Sun(50, grave.getPosition()));
                pendingResults.add("The grave at "
                        + grave.getPosition()
                        + " released 50 sun; collect it before it disappears.");
                break;
            case PLANT_FOOD:
                addEntity(new PlantFoodDrop(grave.getPosition()));
                pendingResults.add("The grave at "
                        + grave.getPosition()
                        + " released one plant food; collect it before it despawns.");
                break;
            case NONE:
                break;
            default:
                throw new IllegalStateException(
                        "unknown grave reward");
        }
    }

    boolean resolveFrozenZombieImpact(
            Projectile projectile, Zombie zombie) {
        if (!damageFrozenZombieShell(zombie,
                Math.max(1, projectile.getImpactDamage()),
                projectile.hasFireEffect())) {
            return false;
        }
        projectile.markForRemoval();
        return true;
    }

    boolean damageFrozenZombieShell(
            Zombie zombie, int damage, boolean fireDamage) {
        if (zombie == null || !zombie.isEncasedInIce()) {
            return false;
        }
        boolean released = zombie.damageFrozenShell(
                Math.max(1, damage), fireDamage);
        reportFrozenZombieShellHit(zombie, damage, released,
                fireDamage);
        return true;
    }

    void damageZombieOrFrozenShell(
            Zombie zombie, int damage, boolean fireDamage) {
        if (zombie == null || zombie.isDead()
                || damageFrozenZombieShell(
                        zombie, damage, fireDamage)) {
            return;
        }
        if (fireDamage) {
            zombie.applyFireDamage(damage);
        } else {
            zombie.takeDamage(damage);
        }
    }

    boolean damageFrozenPlantIce(BasePlant plant,
            int damage, boolean fireDamage) {
        if (plant == null || !plant.isFrozen()) {
            return false;
        }
        boolean released = plant.damageIce(
                Math.max(1, damage), fireDamage);
        if (released) {
            pendingResults.add("The ice around " + plant.getName()
                    + " at " + plant.getEntityPosition()
                    + " was destroyed; the plant is active again.");
        } else {
            String impact = fireDamage
                    ? "a fire attack"
                    : damage + " damage";
            pendingResults.add("The ice around " + plant.getName()
                    + " absorbed " + impact + "; "
                    + plant.getIceShellHitPoints()
                    + " HP remains.");
        }
        return true;
    }

    void reportFrozenZombieShellHit(
            Zombie zombie, int damage, boolean released,
            boolean fireDamage) {
        if (released) {
            releaseFrozenZombie(zombie);
            return;
        }
        String impact = fireDamage
                ? "a fire attack"
                : damage + " damage";
        pendingResults.add("The ice around " + zombie.getName()
                + " absorbed " + impact + "; "
                + zombie.getFrozenShellHitPoints()
                + " HP remains.");
    }

    Zombie findEncasedZombieAt(EntityPosition position) {
        if (position == null) {
            return null;
        }
        for (Zombie zombie : getZombies()) {
            if (zombie.isEncasedInIce()
                    && zombie.getLane() == position.getRow()
                    && (int) Math.floor(zombie.getColumnPosition()) == position.getColumn()) {
                return zombie;
            }
        }
        return null;
    }

    void releaseFrozenZombie(Zombie zombie) {
        EntityPosition position = zombie.getEntityPosition();
        Tile tile = getTileAt(position);
        if (tile != null && tile.getTileType() == TileType.FROZEN) {
            tile.setTileType(TileType.NORMAL);
        }
        pendingResults.add("The ice around " + zombie.getName()
                + " at " + position + " was destroyed; the zombie is active.");
    }
}
