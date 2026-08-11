package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.ArrayList;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFamily;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.explosive.Explosive;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.explosive.ExplosiveBehavior;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.sunProducer.SunProducer;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.BouncingGrape;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.FlyAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.ZombieAbility;
import io.github.Plants_Vs_Zombies_2.model.game.tile.Tile;
import io.github.Plants_Vs_Zombies_2.model.game.tile.TileType;

abstract class BoardExplosiveLogic extends BoardSpecialProjectileLogic {
    protected BoardExplosiveLogic() {
        super();
    }

    protected BoardExplosiveLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    void applyPendingSunProducerBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof SunProducer) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            SunProducer mint = (SunProducer) entity;
            if (!mint.drainFamilyBoostPending()) {
                continue;
            }
            applySunProducerFamilyBoost(mint, entitiesToAdd);
        }
    }

    void applySunProducerFamilyBoost(SunProducer mint,
            List<Entity> entitiesToAdd) {
        activateFamilyBoost(PlantFamily.SUN_PRODUCER,
                mint.getFamilyBoostDurationSeconds(), mint.resetsFamilyCooldowns(),
                mint, entitiesToAdd,
                "Enlighten-mint applied plant food to every Sun Producer plant.");
    }

    void applyPendingExplosiveBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        applyExplosiveFamilyBoosts(updateSnapshot, entitiesToAdd);
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Explosive) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Explosive explosive = (Explosive) entity;
            triggerContactExplosive(explosive);
            applyExplosivePlantFoodEffects(explosive, entitiesToAdd);
            if (explosive.drainActivationPending()) {
                executeExplosiveActivation(explosive, entitiesToAdd);
            }
        }
    }

    void applyExplosiveFamilyBoosts(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Explosive) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Explosive mint = (Explosive) entity;
            if (!mint.drainFamilyBoostPending()) {
                continue;
            }
            activateFamilyBoost(PlantFamily.EXPLOSIVE,
                    mint.getFamilyBoostDurationSeconds(),
                    mint.resetsFamilyCooldowns(), mint, entitiesToAdd,
                    "Bombard-mint applied plant food to every Explosive plant.");
        }
    }

    void triggerContactExplosive(Explosive explosive) {
        if (!explosive.canTriggerOnContact()) {
            return;
        }
        if (findExplosiveTriggerTarget(explosive) != null) {
            explosive.trigger();
        }
    }

    Zombie findExplosiveTriggerTarget(Explosive explosive) {
        if (explosive.getEntityPosition() == null) {
            return null;
        }
        Zombie closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        int lane = explosive.getEntityPosition().getRow();
        double column = explosive.getEntityPosition().getColumn();
        for (Zombie zombie : getZombies()) {
            if (zombie.isDead() || zombie.isHypnotized()
                    || zombie.isSubmerged()
                    || zombie.getLane() != lane) {
                continue;
            }
            if (canDodoFlyOverExplosive(zombie, explosive)) {
                continue;
            }
            if (explosive.getType().getBehavior() == ExplosiveBehavior.WATER_TRAP
                    && !isWaterZombie(zombie)) {
                continue;
            }
            double distance = Math.abs(zombie.getColumnPosition() - column);
            if (distance <= explosive.getType().getTriggerRangeTiles()
                    && distance < closestDistance) {
                closest = zombie;
                closestDistance = distance;
            }
        }
        return closest;
    }

    boolean canDodoFlyOverExplosive(
            Zombie zombie, Explosive explosive) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof FlyAbility
                    && ability.tryUse(zombie, asBoard())
                    && ((FlyAbility) ability)
                            .canFlyOver(explosive)) {
                return true;
            }
        }
        return false;
    }

    boolean isWaterZombie(Zombie zombie) {
        if (zombie.isSubmerged()) {
            return true;
        }
        int column = Math.max(0, Math.min(numberOfColumns - 1,
                (int) Math.floor(zombie.getColumnPosition())));
        Tile tile = getTileAt(new EntityPosition(zombie.getLane(), column));
        return tile != null && tile.getTileType() == TileType.WATER;
    }

    void applyExplosivePlantFoodEffects(Explosive explosive,
            List<Entity> entitiesToAdd) {
        spawnArmedMineClones(explosive, explosive.drainCloneMineCount(), entitiesToAdd);
        if (explosive.drainGlobalFreezePending()) {
            freezeAllZombies(explosive.getFreezeDurationSeconds());
            pendingResults.add(explosive.getName() + " froze every zombie with plant food.");
        }
        crushMultipleZombies(explosive, explosive.drainPlantFoodSquashTargetCount());
        drownMultipleZombies(explosive, explosive.drainPlantFoodKelpTargetCount());
    }

    void spawnArmedMineClones(Explosive source, int count,
            List<Entity> entitiesToAdd) {
        if (count <= 0 || source.getEntityPosition() == null) {
            return;
        }
        List<EntityPosition> reserved = new ArrayList<>();
        for (int offset = 1; offset <= numberOfRows * numberOfColumns && count > 0; offset++) {
            int flatIndex = source.getEntityPosition().getRow() * numberOfColumns
                    + source.getEntityPosition().getColumn() + offset;
            int row = Math.floorMod(flatIndex / numberOfColumns, numberOfRows);
            int column = Math.floorMod(flatIndex, numberOfColumns);
            EntityPosition position = new EntityPosition(row, column);
            if (reserved.contains(position)) {
                continue;
            }
            Explosive clone = Explosive.createArmedClone(
                    source.getType(), source.getLevel(), position);
            if (canAddPlant(clone)) {
                reserved.add(position);
                entitiesToAdd.add(clone);
                count--;
            }
        }
    }

    void crushMultipleZombies(Explosive explosive, int targetCount) {
        if (targetCount <= 0) {
            return;
        }
        for (Zombie zombie : getZombies()) {
            if (targetCount <= 0) {
                break;
            }
            if (zombie.isHypnotized()
                    || zombie.isSubmerged()) {
                continue;
            }
            damageZombieOrFrozenShell(
                    zombie, explosive.getDamage(), false);
            if (zombie.isDead()) {
                reportZombieDeath(zombie);
            }
            targetCount--;
        }
        pendingResults.add(explosive.getName() + " crushed zombies with plant food.");
    }

    void drownMultipleZombies(Explosive explosive, int targetCount) {
        if (targetCount <= 0) {
            return;
        }
        for (Zombie zombie : getZombies()) {
            if (targetCount <= 0) {
                break;
            }
            if (zombie.isHypnotized() || !isWaterZombie(zombie)) {
                continue;
            }
            zombie.kill();
            reportZombieDeath(zombie);
            targetCount--;
        }
        pendingResults.add(explosive.getName() + " pulled water zombies underwater.");
    }

    void executeExplosiveActivation(Explosive explosive,
            List<Entity> entitiesToAdd) {
        EntityPosition center = explosive.getEntityPosition();
        if (center == null) {
            explosive.finishActivation();
            return;
        }
        switch (explosive.getType().getBehavior()) {
            case CONTACT_MINE:
                damageArea(center, 0, 0.75, explosive.getDamage(), false);
                break;
            case AREA_CONTACT_MINE:
            case INSTANT_AREA:
                damageArea(center, 1, 1.0, explosive.getDamage(), false);
                break;
            case SQUASH:
                damageExplosiveTriggerTarget(explosive);
                break;
            case GRAPESHOT:
                damageArea(center, 1, 1.0, explosive.getDamage(), false);
                addGrapeshotProjectiles(explosive, entitiesToAdd);
                break;
            case LANE_FIRE:
                damageLaneWithFire(center.getRow(), explosive.getDamage());
                meltLane(center.getRow());
                break;
            case WHOLE_BOARD:
                damageAllZombies(explosive.getDamage());
                setTileType(center, TileType.CRATER);
                break;
            case WATER_TRAP:
                killWaterTargets(explosive, explosive.getTargetCount());
                break;
            case FREEZE_TRAP:
                freezeExplosiveTriggerTarget(explosive);
                break;
            case WHOLE_BOARD_FREEZE:
                damageAllZombies(explosive.getDamage());
                freezeAllZombies(explosive.getFreezeDurationSeconds());
                break;
            case MELT_ICE:
                meltFrozenTiles(center, explosive.getMeltRadius());
                applyFinishExplosion(explosive);
                break;
            case CONSUME_GRAVE:
                removeGraveAt(center);
                applyFinishExplosion(explosive);
                break;
            case FAMILY_BOOST:
                break;
            default:
                throw new IllegalStateException("Unknown explosive behavior: "
                        + explosive.getType().getBehavior());
        }
        pendingResults.add(explosive.getName() + " activated at " + center + ".");
        explosive.finishActivation();
    }

    void damageExplosiveTriggerTarget(Explosive explosive) {
        Zombie target = findExplosiveTriggerTarget(explosive);
        if (target != null) {
            target.takeDamage(explosive.getDamage());
            if (target.isDead()) {
                reportZombieDeath(target);
            }
        }
    }

    void killWaterTargets(Explosive explosive, int targetCount) {
        Zombie firstTarget = findExplosiveTriggerTarget(explosive);
        if (firstTarget == null || targetCount <= 0) {
            return;
        }
        firstTarget.kill();
        reportZombieDeath(firstTarget);
        targetCount--;
        for (Zombie zombie : getZombies()) {
            if (targetCount <= 0) {
                break;
            }
            if (zombie != firstTarget && !zombie.isHypnotized()
                    && isWaterZombie(zombie)) {
                zombie.kill();
                reportZombieDeath(zombie);
                targetCount--;
            }
        }
    }

    void freezeExplosiveTriggerTarget(Explosive explosive) {
        Zombie target = findExplosiveTriggerTarget(explosive);
        if (target != null) {
            target.applyFreeze(explosive.getFreezeDurationSeconds());
        }
    }

    void damageArea(EntityPosition center, int rowRadius,
            double columnRadius, int damage, boolean fireDamage) {
        if (fireDamage) {
            meltFrozenPlantsInArea(center, rowRadius, columnRadius);
        }
        for (Zombie zombie : getZombies()) {
            if (zombie.isHypnotized() || zombie.isSubmerged()
                    || Math.abs(zombie.getLane() - center.getRow()) > rowRadius
                    || Math.abs(zombie.getColumnPosition() - center.getColumn()) > columnRadius) {
                continue;
            }
            damageZombieOrFrozenShell(
                    zombie, damage, fireDamage);
            if (zombie.isDead()) {
                reportZombieDeath(zombie);
            }
        }
    }

    void damageLaneWithFire(int lane, int damage) {
        meltFrozenPlantsInLane(lane);
        for (Zombie zombie : getZombies()) {
            if (!zombie.isHypnotized() && !zombie.isSubmerged()
                    && zombie.getLane() == lane) {
                damageZombieOrFrozenShell(
                        zombie, damage, true);
                if (zombie.isDead()) {
                    reportZombieDeath(zombie);
                }
            }
        }
    }

    void damageAllZombies(int damage) {
        if (damage <= 0) {
            return;
        }
        for (Zombie zombie : getZombies()) {
            if (zombie.isHypnotized() || zombie.isSubmerged()) {
                continue;
            }
            damageZombieOrFrozenShell(
                    zombie, damage, false);
            if (zombie.isDead()) {
                reportZombieDeath(zombie);
            }
        }
    }

    void freezeAllZombies(double durationSeconds) {
        for (Zombie zombie : getZombies()) {
            if (!zombie.isHypnotized() && !zombie.isSubmerged()) {
                zombie.applyFreeze(durationSeconds);
            }
        }
    }

    void addGrapeshotProjectiles(Explosive explosive,
            List<Entity> entitiesToAdd) {
        EntityPosition center = explosive.getEntityPosition();
        int grapeDamage = Math.max(1, explosive.getDamage() / 9);
        int maximumHits = 1 + explosive.getGrapeBounceCount();
        int[][] directions = {
                { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 },
                { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }
        };
        for (int index = 0; index < GRAPESHOT_PROJECTILE_COUNT; index++) {
            entitiesToAdd.add(new BouncingGrape(center.getRow(), center.getColumn(),
                    directions[index][0], directions[index][1], grapeDamage, maximumHits));
        }
    }

    void applyFinishExplosion(Explosive explosive) {
        if (explosive.explodesOnFinish()) {
            damageArea(explosive.getEntityPosition(), 1, 1.0,
                    FINISH_EXPLOSION_DAMAGE, false);
        }
    }
}
