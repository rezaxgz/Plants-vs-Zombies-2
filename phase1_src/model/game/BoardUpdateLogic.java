package model.game;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.other.Sun;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantFamily;
import model.game.entities.plants.modifier.Modifier;
import model.game.entities.plants.sunProducer.SunProducer;
import model.game.entities.projectile.Projectile;
import model.game.entities.zombies.Zombie;
import model.game.tile.Tile;
import model.game.tile.TileType;

abstract class BoardUpdateLogic extends BoardState {
    protected BoardUpdateLogic() {
        super();
    }

    protected BoardUpdateLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    public void update(float deltaSeconds) {
        validateDeltaSeconds(deltaSeconds);
        updateActiveFamilyBoosts(deltaSeconds);

        List<Entity> entitiesToAdd = new ArrayList<>();
        List<Entity> updateSnapshot = new ArrayList<>(allEntities);
        updateEntities(updateSnapshot, entitiesToAdd, deltaSeconds);
        applyTorchwoodProjectileEffects(updateSnapshot);
        meltFrozenTilesHitByFireProjectiles(updateSnapshot);
        resolveProjectileImpacts(updateSnapshot);
        resolveHomingProjectileImpacts(updateSnapshot);
        resolveLobbedProjectileImpacts(updateSnapshot);
        resolveGrapeImpacts(updateSnapshot);
        reportDeadZombies(updateSnapshot);
        activateReadyShooters(updateSnapshot, entitiesToAdd);
        activateReadyLobbers(updateSnapshot, entitiesToAdd);
        activateReadyStrikeThroughs(updateSnapshot, entitiesToAdd);
        activateReadyHomings(updateSnapshot, entitiesToAdd);
        applyPendingShooterBoardEffects(updateSnapshot, entitiesToAdd);
        applyPendingLobberBoardEffects(updateSnapshot, entitiesToAdd);
        applyPendingStrikeThroughBoardEffects(updateSnapshot, entitiesToAdd);
        applyPendingHomingBoardEffects(updateSnapshot, entitiesToAdd);
        applyPendingSunProducerBoardEffects(updateSnapshot, entitiesToAdd);
        applyPendingExplosiveBoardEffects(updateSnapshot, entitiesToAdd);
        applyPendingMeleeBoardEffects(updateSnapshot, entitiesToAdd);
        activateReadyMelee(updateSnapshot);
        applyPendingModifierBoardEffects(updateSnapshot, entitiesToAdd);
        applyPendingWallnutBoardEffects(updateSnapshot, entitiesToAdd);
        applyPendingWallnutPassiveEffects(updateSnapshot);
        updateZombies(updateSnapshot, deltaSeconds);
        applyPendingModifierDeathEffects(updateSnapshot);
        warmFrozenPlants(deltaSeconds);

        cleanupRemovedEntities();
        for (Entity entity : entitiesToAdd) {
            if (entity instanceof BasePlant) {
                addPlantInternal((BasePlant) entity, false);
            } else {
                addEntity(entity);
            }
        }
    }

    void updateActiveFamilyBoosts(float deltaSeconds) {
        for (ActiveFamilyBoost boost : activeFamilyBoosts) {
            boost.remainingSeconds -= deltaSeconds;
        }
        activeFamilyBoosts.removeIf(boost -> !boost.isActive());
    }

    void activateFamilyBoost(PlantFamily family, double durationSeconds,
            boolean resetCooldowns, BasePlant mint, List<Entity> entitiesToAdd,
            String resultMessage) {
        ActiveFamilyBoost boost = new ActiveFamilyBoost(family,
                Math.max(durationSeconds, POSITION_EPSILON), resetCooldowns);
        activeFamilyBoosts.add(boost);
        if (resetCooldowns && !pendingPlantCooldownResets.contains(family)) {
            pendingPlantCooldownResets.add(family);
        }
        for (BasePlant plant : getPlants()) {
            if (plant != mint) {
                applyFamilyBoostToPlant(boost, plant, entitiesToAdd);
            }
        }
        mint.markForRemoval();
        pendingResults.add(resultMessage + " The effect remains active for "
                + formatDuration(durationSeconds) + " seconds.");
    }

    void applyFamilyBoostToPlant(ActiveFamilyBoost boost, BasePlant plant,
            List<Entity> entitiesToAdd) {
        if (!boost.family.contains(plant) || boost.affectedPlants.contains(plant)) {
            return;
        }
        if (!applyPlantFoodToPlant(plant, entitiesToAdd)) {
            return;
        }
        boost.affectedPlants.add(plant);
        if (boost.resetCooldowns) {
            resetPlantActionTimer(plant);
        }
    }

    void applyActiveFamilyBoostsToPlant(BasePlant plant) {
        if (activeFamilyBoosts.isEmpty() || plant == null || PlantFamily.isMint(plant)) {
            return;
        }
        List<Entity> spawnedEntities = new ArrayList<>();
        for (ActiveFamilyBoost boost : activeFamilyBoosts) {
            applyFamilyBoostToPlant(boost, plant, spawnedEntities);
        }
        for (Entity entity : spawnedEntities) {
            addEntity(entity);
        }
    }

    void cleanupRemovedEntities() {
        Set<EntityPosition> affectedPlantPositions =
                collectRemovedPlantPositions();
        while (!affectedPlantPositions.isEmpty()) {
            allEntities.removeIf(Entity::isRemoved);
            for (EntityPosition position : affectedPlantPositions) {
                refreshTilePlant(position);
                drownPlantWithoutLilyPad(position);
            }
            affectedPlantPositions = collectRemovedPlantPositions();
        }
        allEntities.removeIf(Entity::isRemoved);
    }

    Set<EntityPosition> collectRemovedPlantPositions() {
        Set<EntityPosition> positions = new LinkedHashSet<>();
        for (Entity entity : allEntities) {
            if (entity.isRemoved() && entity instanceof BasePlant
                    && entity.getEntityPosition() != null) {
                positions.add(entity.getEntityPosition());
            }
        }
        return positions;
    }

    void drownPlantWithoutLilyPad(
            EntityPosition position) {
        Tile tile = getTileAt(position);
        if (tile == null
                || tile.getTileType() != TileType.WATER) {
            return;
        }
        drownUnsupportedPlants(position,
                " because it no longer had a lily pad.");
    }

    void refreshTilePlant(EntityPosition position) {
        Tile tile = getTileAt(position);
        if (tile == null) {
            return;
        }
        BasePlant remainingPlant = getPlantAt(position);
        if (remainingPlant == null) {
            tile.clearPlant();
        } else {
            tile.setPlant(remainingPlant);
        }
    }

    void updateEntities(List<Entity> updateSnapshot, List<Entity> entitiesToAdd,
            float deltaSeconds) {
        for (Entity entity : updateSnapshot) {
            if (entity.isRemoved()) {
                continue;
            }
            if (entity instanceof Zombie && ((Zombie) entity).isDead()) {
                reportZombieDeath((Zombie) entity);
                continue;
            }
            if (entity instanceof BasePlant
                    && ((BasePlant) entity).isDisabled()) {
                continue;
            }

            boolean sunWasDropping = entity instanceof Sun && ((Sun) entity).isDropping();
            entity.update(deltaSeconds);
            reportSunLanding(entity, sunWasDropping);
            collectProducedSuns(entity, entitiesToAdd);
        }
    }

    void reportSunLanding(Entity entity, boolean sunWasDropping) {
        if (sunWasDropping && entity instanceof Sun && !((Sun) entity).isDropping()) {
            pendingResults.add("Sun reached the ground at position " + entity.getEntityPosition());
        }
    }

    void collectProducedSuns(Entity entity, List<Entity> entitiesToAdd) {
        if (!(entity instanceof SunProducer)) {
            return;
        }
        SunProducer producer = (SunProducer) entity;
        List<Sun> producedSuns = producer.drainProducedSuns();
        entitiesToAdd.addAll(producedSuns);
        for (int i = 0; i < producedSuns.size(); i++) {
            pendingResults.add(buildSunProductionResult(producer));
        }
    }

    void applyTorchwoodProjectileEffects(List<Entity> updateSnapshot) {
        List<Modifier> torchwoods = new ArrayList<>();
        for (BasePlant plant : getPlants()) {
            if (plant instanceof Modifier && ((Modifier) plant).isTorchwood()
                    && !plant.isRemoved() && !plant.isDisabled()) {
                torchwoods.add((Modifier) plant);
            }
        }
        if (torchwoods.isEmpty()) {
            return;
        }
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Projectile) || entity.isRemoved()) {
                continue;
            }
            Projectile projectile = (Projectile) entity;
            if (!projectile.isTorchwoodEligible()) {
                continue;
            }
            for (Modifier torchwood : torchwoods) {
                EntityPosition position = torchwood.getEntityPosition();
                double parameter = projectile.getIntersectionParameter(
                        position.getRow(), position.getColumn(),
                        PROJECTILE_COLLISION_RADIUS);
                if (!Double.isNaN(parameter)) {
                    projectile.igniteByTorchwood(
                            torchwood.getTorchwoodDamageMultiplier());
                }
            }
        }
    }

    void meltFrozenTilesHitByFireProjectiles(
            List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Projectile) || entity.isRemoved()) {
                continue;
            }
            Projectile projectile = (Projectile) entity;
            if (!projectile.hasFireEffect()) {
                continue;
            }
            for (Tile tile : tiles) {
                if (tile.getTileType() != TileType.FROZEN) {
                    continue;
                }
                EntityPosition position = tile.getPosition();
                if (findEncasedZombieAt(position) != null) {
                    continue;
                }
                double parameter = projectile.getIntersectionParameter(
                        position.getRow(), position.getColumn(), 0.45);
                if (!Double.isNaN(parameter)) {
                    tile.setTileType(TileType.NORMAL);
                }
            }
        }
    }
}
