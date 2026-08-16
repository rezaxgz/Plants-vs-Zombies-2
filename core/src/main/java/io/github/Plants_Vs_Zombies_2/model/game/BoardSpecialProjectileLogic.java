package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFamily;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.homing.Homing;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.BouncingGrape;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.HomingProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.LobbedProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.ProjectileEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.UmbrellaBounceAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.ZombieAbility;

abstract class BoardSpecialProjectileLogic extends BoardProjectileObstacleLogic {
    protected BoardSpecialProjectileLogic() {
        super();
    }

    protected BoardSpecialProjectileLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    void resolveHomingProjectileImpacts(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof HomingProjectile) || entity.isRemoved()) {
                continue;
            }
            HomingProjectile projectile = (HomingProjectile) entity;
            BasePlant frozenPlant = findFirstFrozenPlantHit(projectile);
            if (frozenPlant != null) {
                damageFrozenPlantIce(frozenPlant,
                        Math.max(1, projectile.getImpactDamage()),
                        projectile.hasFireEffect());
                projectile.markForRemoval();
                continue;
            }
            if (!projectile.isTargetAvailable()) {
                projectile.markForRemoval();
                continue;
            }
            if (projectile.hasReachedTarget()) {
                Zombie target = projectile.getLockedTarget();
                if (resolveFrozenZombieImpact(projectile, target)) {
                    continue;
                }
                extinguishProspectorDynamite(projectile, target);
                chillProjectileSourceIfBlockhead(projectile, target);
                projectile.hit(target);
                if (target.isDead()) {
                    reportZombieDeath(target);
                } else if (target.isHypnotized()) {
                    pendingResults.add("Zombie " + target.getName() + " was hypnotized.");
                }
                continue;
            }
            if (projectile.hasExpired() || isProjectileOutsideBoard(projectile)) {
                projectile.markForRemoval();
            }
        }
    }

    void resolveLobbedProjectileImpacts(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof LobbedProjectile) || entity.isRemoved()) {
                continue;
            }
            LobbedProjectile projectile = (LobbedProjectile) entity;
            if (!projectile.hasLanded()) {
                continue;
            }
            Zombie target = findLobbedLandingTarget(projectile);
            if (target == null || isProtectedFromLobbers(target)) {
                projectile.markForRemoval();
                continue;
            }
            if (resolveFrozenZombieImpact(projectile, target)) {
                continue;
            }
            extinguishProspectorDynamite(projectile, target);
            chillProjectileSourceIfBlockhead(projectile, target);
            projectile.hit(target);
            applyLobbedSplash(projectile, target);
            reportDeadLobberTargets();
        }
    }

    Zombie findLobbedLandingTarget(LobbedProjectile projectile) {
        Zombie lockedTarget = projectile.getLockedTarget();
        if (isAtLobbedLandingPoint(lockedTarget, projectile)) {
            return lockedTarget;
        }
        Zombie nearest = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        for (Zombie zombie : getZombies()) {
            if (zombie.isDead() || zombie.isHypnotized()) {
                continue;
            }
            double rowDelta = zombie.getLane() - projectile.getLandingRow();
            double columnDelta = zombie.getColumnPosition() - projectile.getLandingColumn();
            double distanceSquared = rowDelta * rowDelta + columnDelta * columnDelta;
            if (distanceSquared <= PROJECTILE_COLLISION_RADIUS * PROJECTILE_COLLISION_RADIUS
                    && distanceSquared < nearestDistanceSquared) {
                nearest = zombie;
                nearestDistanceSquared = distanceSquared;
            }
        }
        return nearest;
    }

    void applyLobbedSplash(LobbedProjectile projectile, Zombie directTarget) {
        if (projectile.getSplashEffects().isEmpty()
                || projectile.getSplashRadiusTiles() <= 0.0) {
            return;
        }
        for (Zombie zombie : getZombies()) {
            if (zombie == directTarget || zombie.isDead() || zombie.isHypnotized()
                    || isProtectedFromLobbers(zombie)
                    || !isInsideLobbedSplash(zombie, projectile)) {
                continue;
            }
            for (ProjectileEffect effect : projectile.getSplashEffects()) {
                if (zombie.isDead()) {
                    break;
                }
                zombie.recordDamageSourcePlant(
                        projectile.getSourcePlantName());
                effect.apply(zombie);
            }
        }
    }

    boolean isProtectedFromLobbers(Zombie zombie) {
        if (zombie == null) {
            return false;
        }
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof UmbrellaBounceAbility
                    && ability.tryUse(zombie, asBoard())) {
                pendingResults.add(zombie.getName()
                        + " deflected a lobbed projectile.");
                return true;
            }
        }
        return false;
    }

    void reportDeadLobberTargets() {
        for (Zombie zombie : getZombies()) {
            if (zombie.isDead()) {
                reportZombieDeath(zombie);
            }
        }
    }

    void resolveGrapeImpacts(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof BouncingGrape) || entity.isRemoved()) {
                continue;
            }
            BouncingGrape grape = (BouncingGrape) entity;
            grape.bounceInside(numberOfRows, numberOfColumns);
            BasePlant frozenPlant = findFirstFrozenPlantHit(grape);
            Zombie target = findFirstZombieHit(grape);
            if (frozenPlant != null
                    && isFrozenPlantBeforeZombie(grape, frozenPlant, target)) {
                damageFrozenPlantIce(frozenPlant,
                        Math.max(1, grape.getDamage()), false);
                grape.markForRemoval();
                continue;
            }
            if (target != null) {
                if (damageFrozenZombieShell(
                        target, grape.getDamage(), false)) {
                    grape.markForRemoval();
                } else {
                    grape.hit(target);
                }
                if (target.isDead()) {
                    reportZombieDeath(target);
                }
            }
        }
    }

    Zombie findFirstZombieHit(BouncingGrape grape) {
        Zombie firstTarget = null;
        double firstParameter = Double.POSITIVE_INFINITY;
        for (Zombie zombie : getZombies()) {
            if (!grape.canHit(zombie) || zombie.isHypnotized()
                    || zombie.isSubmerged()) {
                continue;
            }
            double parameter = grape.getIntersectionParameter(
                    zombie.getLane(), zombie.getColumnPosition(), GRAPE_COLLISION_RADIUS);
            if (!Double.isNaN(parameter) && parameter < firstParameter) {
                firstParameter = parameter;
                firstTarget = zombie;
            }
        }
        return firstTarget;
    }

    void reportDeadZombies(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (entity instanceof Zombie && ((Zombie) entity).isDead()) {
                reportZombieDeath((Zombie) entity);
            }
        }
    }

    void applyPendingHomingBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        applyHomingFamilyBoosts(updateSnapshot, entitiesToAdd);
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Homing) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Homing plant = (Homing) entity;
            if (plant.drainPlantFoodPending()) {
                applyHomingPlantFood(plant, entitiesToAdd);
            }
        }
    }

    void applyHomingFamilyBoosts(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Homing) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Homing mint = (Homing) entity;
            if (mint.drainFamilyBoostPending()) {
                activateFamilyBoost(PlantFamily.HOMING,
                        mint.getFamilyBoostDurationSeconds(),
                        mint.resetsFamilyCooldowns(), mint, entitiesToAdd,
                        "catTail-mint applied plant food to every Homing plant.");
            }
        }
    }

    void applyHomingPlantFood(Homing plant,
            List<Entity> entitiesToAdd) {
        switch (plant.getType().getBehavior()) {
            case HYPNOTIZE:
                addHomingPlantFoodProjectiles(plant, entitiesToAdd, false);
                break;
            case LIGHTNING:
                strikeRandomHomingTargets(plant);
                break;
            case MAGNET:
                removeAllMetalArmorInRange(plant);
                break;
            case GUIDED_PROJECTILE:
                addCatTailBarrage(plant, entitiesToAdd);
                break;
            case FAMILY_BOOST:
                break;
            default:
                throw new IllegalStateException("Unknown homing behavior: "
                        + plant.getType().getBehavior());
        }
    }

    void addHomingPlantFoodProjectiles(Homing plant,
            List<Entity> entitiesToAdd, boolean allowBoss) {
        List<Zombie> targets = randomHomingTargets(plant,
                plant.getPlantFoodTargetCount(), allowBoss);
        for (Zombie target : targets) {
            HomingProjectile projectile = plant.createPlantFoodProjectile(target);
            if (projectile != null) {
                entitiesToAdd.add(projectile);
            }
        }
        reportHomingPlantFood(plant, targets.size());
    }

    void strikeRandomHomingTargets(Homing plant) {
        List<Zombie> targets = randomHomingTargets(plant,
                plant.getPlantFoodTargetCount(), true);
        for (Zombie target : targets) {
            target.recordDamageSourcePlant(plant.getName());
            target.takeDamage(plant.getDamage());
            if (target.isDead()) {
                reportZombieDeath(target);
            }
        }
        reportHomingPlantFood(plant, targets.size());
    }

    void removeAllMetalArmorInRange(Homing plant) {
        int removedArmorCount = 0;
        for (Zombie zombie : homingCandidates(plant, true, true)) {
            if (zombie.removeMagnetizableArmor()) {
                removedArmorCount++;
            }
        }
        reportHomingPlantFood(plant, removedArmorCount);
    }

    void addCatTailBarrage(Homing plant,
            List<Entity> entitiesToAdd) {
        List<Zombie> targets = homingCandidates(plant, true, false);
        targets.sort(Comparator.comparingDouble(zombie -> distanceSquared(plant, zombie)));
        if (targets.isEmpty()) {
            return;
        }
        int projectileCount = plant.getPlantFoodTargetCount();
        for (int i = 0; i < projectileCount; i++) {
            Zombie target = targets.get(i % targets.size());
            HomingProjectile projectile = plant.createPlantFoodProjectile(target);
            if (projectile != null) {
                entitiesToAdd.add(projectile);
            }
        }
        reportHomingPlantFood(plant, projectileCount);
    }

    List<Zombie> randomHomingTargets(Homing plant, int maximumTargets,
            boolean allowBoss) {
        List<Zombie> targets = homingCandidates(plant, allowBoss, false);
        Collections.shuffle(targets);
        int count = Math.min(maximumTargets, targets.size());
        return new ArrayList<>(targets.subList(0, count));
    }

    void reportHomingPlantFood(Homing plant, int affectedCount) {
        if (affectedCount > 0) {
            pendingResults.add(plant.getName() + " used its plant food effect on "
                    + affectedCount + " target(s).");
        }
    }
}
