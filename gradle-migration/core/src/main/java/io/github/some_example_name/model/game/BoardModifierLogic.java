package io.github.some_example_name.model.game;

import java.util.List;

import io.github.some_example_name.model.game.entities.Entity;
import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.plants.PlantFamily;
import io.github.some_example_name.model.game.entities.plants.PlantFoodSupport;
import io.github.some_example_name.model.game.entities.plants.PlantTag;
import io.github.some_example_name.model.game.entities.plants.explosive.Explosive;
import io.github.some_example_name.model.game.entities.plants.homing.Homing;
import io.github.some_example_name.model.game.entities.plants.lobber.Lobber;
import io.github.some_example_name.model.game.entities.plants.melee.Melee;
import io.github.some_example_name.model.game.entities.plants.modifier.Modifier;
import io.github.some_example_name.model.game.entities.plants.modifier.ModifierPlantType;
import io.github.some_example_name.model.game.entities.plants.shooter.Shooter;
import io.github.some_example_name.model.game.entities.plants.shooter.ShooterPlantType;
import io.github.some_example_name.model.game.entities.plants.strikeThrough.StrikeThrough;
import io.github.some_example_name.model.game.entities.plants.sunProducer.SunProducer;
import io.github.some_example_name.model.game.entities.plants.wallnut.Wallnut;
import io.github.some_example_name.model.game.entities.zombies.Zombie;
import io.github.some_example_name.model.game.tile.Tile;
import io.github.some_example_name.model.game.tile.TileType;

abstract class BoardModifierLogic extends BoardMeleeLogic {
    protected BoardModifierLogic() {
        super();
    }

    protected BoardModifierLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    void applyPendingModifierBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Modifier) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Modifier modifier = (Modifier) entity;
            if (modifier.drainFamilyBoostPending()) {
                applyEnchantMint(modifier, entitiesToAdd);
            }
            int copyCount = modifier.drainLilyPadCopiesPending();
            if (copyCount > 0) {
                createLilyPadCopies(modifier, copyCount, entitiesToAdd);
            }
        }
    }

    void applyEnchantMint(Modifier mint, List<Entity> entitiesToAdd) {
        activateFamilyBoost(PlantFamily.MODIFIER,
                mint.getFamilyBoostDurationSeconds(), mint.resetsFamilyCooldowns(),
                mint, entitiesToAdd,
                "Enchant-mint applied plant food to every Modifier plant.");
    }

    boolean applyPlantFoodToPlant(BasePlant plant,
            List<Entity> entitiesToAdd) {
        if (!PlantFoodSupport.supports(plant)) {
            return false;
        }
        if (plant instanceof Shooter) {
            applyShooterPlantFood((Shooter) plant, entitiesToAdd);
        } else if (plant instanceof SunProducer) {
            SunProducer producer = (SunProducer) plant;
            producer.usePlantFood();
            collectProducedSuns(producer, entitiesToAdd);
        } else if (plant instanceof Wallnut) {
            ((Wallnut) plant).usePlantFood();
        } else if (plant instanceof Explosive) {
            ((Explosive) plant).usePlantFood();
        } else if (plant instanceof Melee) {
            ((Melee) plant).usePlantFood();
        } else if (plant instanceof Lobber) {
            ((Lobber) plant).usePlantFood();
        } else if (plant instanceof StrikeThrough) {
            ((StrikeThrough) plant).usePlantFood();
        } else if (plant instanceof Homing) {
            ((Homing) plant).usePlantFood();
        } else if (plant instanceof Modifier) {
            ((Modifier) plant).usePlantFood();
        }
        return true;
    }

    void applyShooterPlantFood(Shooter shooter,
            List<Entity> entitiesToAdd) {
        shooter.usePlantFood(numberOfRows);
        entitiesToAdd.addAll(shooter.drainProjectiles());
        resetTemporaryShooterFamily(shooter);
        freezeSnowPeaLane(shooter);
    }

    void resetTemporaryShooterFamily(Shooter source) {
        ShooterPlantType type = source.getType();
        if (type != ShooterPlantType.SEA_SHROOM
                && type != ShooterPlantType.PUFF_SHROOM) {
            return;
        }
        for (BasePlant plant : getPlants()) {
            if (plant instanceof Shooter && ((Shooter) plant).getType() == type) {
                ((Shooter) plant).resetLifespan();
            }
        }
    }

    void freezeSnowPeaLane(Shooter shooter) {
        if (shooter.getType() != ShooterPlantType.SNOW_PEA
                || shooter.getEntityPosition() == null) {
            return;
        }
        int lane = shooter.getEntityPosition().getRow();
        for (Zombie zombie : getZombies()) {
            if (!zombie.isDead() && !zombie.isHypnotized()
                    && zombie.getLane() == lane) {
                zombie.applyFreeze(shooter.getChillDurationSeconds());
            }
        }
    }

    void createLilyPadCopies(Modifier source, int copyCount,
            List<Entity> entitiesToAdd) {
        int created = 0;
        for (Tile tile : tiles) {
            if (created >= copyCount) {
                break;
            }
            EntityPosition position = tile.getPosition();
            if (tile.getTileType() != TileType.WATER
                    || !getPlantsAt(position).isEmpty()
                    || containsPendingPlantAt(entitiesToAdd, position)) {
                continue;
            }
            entitiesToAdd.add(new Modifier(ModifierPlantType.LILY_PAD,
                    source.getLevel(), position));
            created++;
        }
        pendingResults.add(source.getName() + " created " + created
                + " Lily Pad copy/copies.");
    }

    void applyPendingModifierDeathEffects(
            List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Modifier)) {
                continue;
            }
            Modifier modifier = (Modifier) entity;
            if (!modifier.drainDeathAreaEffectPending()
                    || modifier.getEntityPosition() == null) {
                continue;
            }
            damageAreaWithFire(modifier.getEntityPosition(), 1,
                    ModifierPlantType.TORCHWOOD_DEATH_DAMAGE);
            pendingResults.add(modifier.getName()
                    + " released a fire explosion when destroyed.");
        }
    }

    void damageAreaWithFire(EntityPosition center, int radius,
            int damage) {
        meltFrozenPlantsInArea(center, radius, radius);
        for (Zombie zombie : getZombies()) {
            if (zombie.isDead() || zombie.isHypnotized()
                    || zombie.isSubmerged()
                    || Math.abs(zombie.getLane() - center.getRow()) > radius
                    || Math.abs(zombie.getColumnPosition()
                            - center.getColumn()) > radius) {
                continue;
            }
            damageZombieOrFrozenShell(
                    zombie, damage, true);
            if (zombie.isDead()) {
                reportZombieDeath(zombie);
            }
        }
    }

    void applyPendingWallnutPassiveEffects(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (entity instanceof Wallnut && !((Wallnut) entity).isDisabled()) {
                Wallnut wallnut = (Wallnut) entity;
                releaseSunBeanSun(wallnut);
                applyWallnutExplosion(wallnut);
            }
        }
    }

    void applyPendingWallnutBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Wallnut) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Wallnut wallnut = (Wallnut) entity;
            applyFamilyBoost(wallnut, entitiesToAdd);
            applyGarlicPlantFood(wallnut);
            applySweetPotatoPlantFood(wallnut);
        }
    }

    void applyFamilyBoost(Wallnut mint, List<Entity> entitiesToAdd) {
        if (!mint.drainFamilyBoostPending()) {
            return;
        }
        activateFamilyBoost(PlantFamily.WALL_NUT,
                mint.getFamilyBoostDurationSeconds(), mint.resetsFamilyCooldowns(),
                mint, entitiesToAdd,
                "Reinforce-mint applied plant food to every Wall-nut family plant.");
    }

    void applyGarlicPlantFood(Wallnut garlic) {
        if (!garlic.drainDivertAllPending() || garlic.getEntityPosition() == null) {
            return;
        }
        int sourceLane = garlic.getEntityPosition().getRow();
        for (Zombie zombie : getZombies()) {
            if (!zombie.isHypnotized()
                    && !zombie.getType().isBoss()
                    && zombie.getLane() == sourceLane) {
                int targetLane = garlic.chooseAdjacentLane(sourceLane, numberOfRows);
                zombie.moveToLane(targetLane);
            }
        }
        pendingResults.add("Garlic diverted every zombie in lane " + sourceLane + ".");
    }

    void applySweetPotatoPlantFood(Wallnut sweetPotato) {
        if (!sweetPotato.drainAttractAllPending() || sweetPotato.getEntityPosition() == null) {
            return;
        }
        int targetLane = sweetPotato.getEntityPosition().getRow();
        for (Zombie zombie : getZombies()) {
            if (!zombie.isHypnotized()
                    && !zombie.getType().isBoss()
                    && Math.abs(zombie.getLane() - targetLane) == 1) {
                zombie.moveToLane(targetLane);
            }
        }
        pendingResults.add("Sweet Potato pulled adjacent-lane zombies into lane " + targetLane + ".");
    }

    void warmFrozenPlants(float deltaSeconds) {
        if (deltaSeconds <= 0.0f) {
            return;
        }
        for (BasePlant plant : getPlants()) {
            if (!plant.isFrozen() || plant.getEntityPosition() == null
                    || !hasAdjacentActiveFirePlant(
                            plant.getEntityPosition(), plant)) {
                continue;
            }
            boolean released = plant.meltIce(
                    BasePlant.ICE_WARMING_DAMAGE_PER_SECOND
                            * deltaSeconds);
            if (released) {
                pendingResults.add("The ice around " + plant.getName()
                        + " at " + plant.getEntityPosition()
                        + " melted; the plant is active again.");
            }
        }
    }

    boolean hasAdjacentActiveFirePlant(
            EntityPosition center, BasePlant excludedPlant) {
        if (center == null) {
            return false;
        }
        for (BasePlant plant : getPlants()) {
            EntityPosition position = plant.getEntityPosition();
            if (plant == excludedPlant || position == null
                    || plant.isDestroyed() || plant.isDisabled()
                    || !plant.hasTag(PlantTag.FIRE)) {
                continue;
            }
            int rowDistance = Math.abs(
                    position.getRow() - center.getRow());
            int columnDistance = Math.abs(
                    position.getColumn() - center.getColumn());
            if (rowDistance <= 1 && columnDistance <= 1
                    && rowDistance + columnDistance > 0) {
                return true;
            }
        }
        return false;
    }

    void meltFrozenPlantsInLane(int lane) {
        for (BasePlant plant : getPlants()) {
            if (plant.isFrozen() && plant.getEntityPosition() != null
                    && plant.getEntityPosition().getRow() == lane) {
                damageFrozenPlantIce(plant, 1, true);
            }
        }
    }

    void meltFrozenPlantsInArea(EntityPosition center,
            int rowRadius, double columnRadius) {
        if (center == null) {
            return;
        }
        for (BasePlant plant : getPlants()) {
            EntityPosition position = plant.getEntityPosition();
            if (!plant.isFrozen() || position == null
                    || Math.abs(position.getRow() - center.getRow()) > rowRadius
                    || Math.abs(position.getColumn() - center.getColumn()) > columnRadius) {
                continue;
            }
            damageFrozenPlantIce(plant, 1, true);
        }
    }
}
