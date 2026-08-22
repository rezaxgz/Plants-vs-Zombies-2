package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFamily;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.homing.Homing;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.homing.HomingBehavior;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.lobber.Lobber;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.lobber.LobberPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.shooter.Shooter;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.strikeThrough.StrikeThrough;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.HomingProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.LobbedProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.PiercingProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

abstract class BoardPlantAttackLogic extends BoardUpdateLogic {
    protected BoardPlantAttackLogic() {
        super();
    }

    protected BoardPlantAttackLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    void activateReadyShooters(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Shooter) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Shooter shooter = (Shooter) entity;
            if (shooter.isReadyToShoot() && hasTarget(shooter)) {
                entitiesToAdd.addAll(shooter.shoot(numberOfRows));
            }
        }
    }

    boolean hasTarget(Shooter shooter) {
        for (Zombie zombie : getZombies()) {
            if (!zombie.isHypnotized() && shooter.canTarget(zombie, numberOfRows)) {
                return true;
            }
        }
        return false;
    }

    void activateReadyLobbers(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Lobber) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Lobber lobber = (Lobber) entity;
            if (!lobber.isReadyToAttack()) {
                continue;
            }
            Zombie target = findFirstLobberTarget(lobber);
            if (target == null) {
                continue;
            }
            LobbedProjectile projectile = lobber.shoot(target);
            if (projectile != null) {
                entitiesToAdd.add(projectile);
            }
        }
    }

    Zombie findFirstLobberTarget(Lobber lobber) {
        EntityPosition position = lobber.getEntityPosition();
        if (position == null) {
            return null;
        }
        Zombie nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Zombie zombie : getZombies()) {
            if (zombie.isDead() || zombie.isHypnotized()
                    || zombie.getLane() != position.getRow()) {
                continue;
            }
            double distance = zombie.getColumnPosition() - position.getColumn();
            if (distance > POSITION_EPSILON && distance < nearestDistance) {
                nearest = zombie;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    void activateReadyStrikeThroughs(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof StrikeThrough) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            StrikeThrough plant = (StrikeThrough) entity;
            if (!plant.isReadyToAttack() || !hasStrikeThroughTarget(plant)) {
                continue;
            }
            PiercingProjectile projectile = plant.shoot();
            if (projectile != null) {
                entitiesToAdd.add(projectile);
            }
        }
    }

    boolean hasStrikeThroughTarget(StrikeThrough plant) {
        for (Zombie zombie : getZombies()) {
            if (!zombie.isDead() && !zombie.isHypnotized()
                    && !zombie.isSubmerged()
                    && plant.canTarget(zombie.getColumnPosition(), zombie.getLane())) {
                return true;
            }
        }
        return false;
    }

    void activateReadyHomings(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Homing) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Homing plant = (Homing) entity;
            if (!plant.isReadyToAttack()) {
                continue;
            }
            Zombie target = findHomingTarget(plant);
            if (target != null) {
                performHomingAttack(plant, target, entitiesToAdd);
            }
        }
    }

    Zombie findHomingTarget(Homing plant) {
        switch (plant.getType().getBehavior()) {
            case HYPNOTIZE:
                return randomHomingTarget(plant, false);
            case LIGHTNING:
                return plant.hasTargetPriorityUp()
                        ? strongestHomingTarget(plant)
                        : randomHomingTarget(plant, true);
            case MAGNET:
                return nearestMagnetTarget(plant);
            case GUIDED_PROJECTILE:
                return nearestHomingTarget(plant);
            case FAMILY_BOOST:
                return null;
            default:
                throw new IllegalStateException("Unknown homing behavior: "
                        + plant.getType().getBehavior());
        }
    }

    void performHomingAttack(Homing plant, Zombie target,
            List<Entity> entitiesToAdd) {
        HomingBehavior behavior = plant.getType().getBehavior();
        if (behavior == HomingBehavior.HYPNOTIZE
                || behavior == HomingBehavior.GUIDED_PROJECTILE) {
            HomingProjectile projectile = plant.shoot(target);
            if (projectile != null) {
                entitiesToAdd.add(projectile);
            }
            return;
        }
        if (!plant.consumeDirectAttackReadiness(target)) {
            return;
        }
        if (behavior == HomingBehavior.LIGHTNING) {
            target.recordDamageSourcePlant(plant.getName());
            target.takeDamage(plant.getDamage());
            if (target.isDead()) {
                reportZombieDeath(target);
            }
        } else if (behavior == HomingBehavior.MAGNET
                && target.removeMagnetizableArmor()) {
            pendingResults.add(plant.getName() + " removed " + target.getName()
                    + "'s metal armor.");
        }
    }

    Zombie randomHomingTarget(Homing plant, boolean allowBoss) {
        List<Zombie> candidates = homingCandidates(plant, allowBoss, false);
        if (candidates.isEmpty()) {
            return null;
        }
        Collections.shuffle(candidates);
        return candidates.get(0);
    }

    Zombie nearestHomingTarget(Homing plant) {
        List<Zombie> candidates = homingCandidates(plant, true, false);
        return candidates.stream()
                .min(Comparator.comparingDouble(zombie -> distanceSquared(plant, zombie)))
                .orElse(null);
    }

    Zombie strongestHomingTarget(Homing plant) {
        List<Zombie> candidates = homingCandidates(plant, true, false);
        return candidates.stream()
                .max(Comparator.comparingInt(Zombie::getCurrentDurability)
                        .thenComparingDouble(zombie -> -distanceSquared(plant, zombie)))
                .orElse(null);
    }

    Zombie nearestMagnetTarget(Homing plant) {
        List<Zombie> candidates = homingCandidates(plant, true, true);
        return candidates.stream()
                .min(Comparator.comparingDouble(zombie -> distanceSquared(plant, zombie)))
                .orElse(null);
    }

    List<Zombie> homingCandidates(Homing plant, boolean allowBoss,
            boolean requireMagnetizableArmor) {
        List<Zombie> candidates = new ArrayList<>();
        for (Zombie zombie : getZombies()) {
            if (!isHomingTargetable(zombie) || (!allowBoss && isZomboss(zombie))) {
                continue;
            }
            if (requireMagnetizableArmor && !zombie.hasMagnetizableArmor()) {
                continue;
            }
            if (isInsideHomingRange(plant, zombie)) {
                candidates.add(zombie);
            }
        }
        return candidates;
    }

    void applyPendingStrikeThroughBoardEffects(
            List<Entity> updateSnapshot, List<Entity> entitiesToAdd) {
        applyStrikeThroughFamilyBoosts(updateSnapshot, entitiesToAdd);
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof StrikeThrough) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            StrikeThrough plant = (StrikeThrough) entity;
            PiercingProjectile projectile = plant.drainPlantFoodProjectile();
            if (projectile != null) {
                entitiesToAdd.add(projectile);
                pendingResults.add(plant.getName() + " used its plant food effect.");
            }
        }
    }

    void applyStrikeThroughFamilyBoosts(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof StrikeThrough) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            StrikeThrough mint = (StrikeThrough) entity;
            if (mint.drainFamilyBoostPending()) {
                activateFamilyBoost(PlantFamily.STRIKE_THROUGH,
                        mint.getFamilyBoostDurationSeconds(),
                        mint.resetsFamilyCooldowns(), mint, entitiesToAdd,
                        "Pierce-mint applied plant food to every Strike-through plant.");
            }
        }
    }

    void applyPendingLobberBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        applyLobberFamilyBoosts(updateSnapshot, entitiesToAdd);
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Lobber) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Lobber lobber = (Lobber) entity;
            warmTilesAroundPepperPult(lobber);
            if (lobber.drainPlantFoodPending()) {
                addLobberPlantFoodProjectiles(lobber, entitiesToAdd);
            }
        }
    }

    void applyLobberFamilyBoosts(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Lobber) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Lobber mint = (Lobber) entity;
            if (!mint.drainFamilyBoostPending()) {
                continue;
            }
            activateFamilyBoost(PlantFamily.LOBBER,
                    mint.getFamilyBoostDurationSeconds(),
                    mint.resetsFamilyCooldowns(), mint, entitiesToAdd,
                    "Arma-mint applied plant food to every Lobber plant.");
        }
    }

    void warmTilesAroundPepperPult(Lobber lobber) {
        if (lobber.getType() == LobberPlantType.PEPPER_PULT
                && lobber.getEntityPosition() != null) {
            meltFrozenTiles(lobber.getEntityPosition(), lobber.getWarmthRadius());
        }
    }

    void addLobberPlantFoodProjectiles(Lobber lobber,
            List<Entity> entitiesToAdd) {
        List<Zombie> targets = getLobberPlantFoodTargets(lobber);
        for (Zombie target : targets) {
            LobbedProjectile projectile = lobber.createPlantFoodProjectile(target);
            if (projectile != null) {
                entitiesToAdd.add(projectile);
            }
        }
        if (!targets.isEmpty()) {
            pendingResults.add(lobber.getName() + " used its plant food effect.");
        }
    }

    List<Zombie> getLobberPlantFoodTargets(Lobber lobber) {
        List<Zombie> candidates = new ArrayList<>();
        for (Zombie zombie : getZombies()) {
            if (!zombie.isDead() && !zombie.isHypnotized()) {
                candidates.add(zombie);
            }
        }
        Collections.shuffle(candidates);
        int targetCount = Math.min(lobber.getPlantFoodTargetCount(), candidates.size());
        return new ArrayList<>(candidates.subList(0, targetCount));
    }

    void applyPendingShooterBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Shooter) || entity.isRemoved()) {
                continue;
            }
            Shooter shooter = (Shooter) entity;
            entitiesToAdd.addAll(shooter.drainProjectiles());
            if (shooter.isDisabled()) {
                continue;
            }
            if (shooter.drainFamilyBoostPending()) {
                applyShooterFamilyBoost(shooter, entitiesToAdd);
            }
        }
    }

    void applyShooterFamilyBoost(Shooter mint, List<Entity> entitiesToAdd) {
        activateFamilyBoost(PlantFamily.SHOOTER,
                mint.getFamilyBoostDurationSeconds(), mint.resetsFamilyCooldowns(),
                mint, entitiesToAdd,
                "Appease-mint applied plant food to every Shooter plant.");
    }
}
