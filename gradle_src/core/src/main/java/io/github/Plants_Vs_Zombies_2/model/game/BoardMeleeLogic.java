package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.ArrayList;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFamily;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.melee.Melee;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.melee.MeleeBehavior;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.melee.MeleePlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.tile.Tile;
import io.github.Plants_Vs_Zombies_2.model.game.tile.TileType;

abstract class BoardMeleeLogic extends BoardExplosiveLogic {
    protected BoardMeleeLogic() {
        super();
    }

    protected BoardMeleeLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    void applyPendingMeleeBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        applyMeleeFamilyBoosts(updateSnapshot, entitiesToAdd);
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Melee) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Melee melee = (Melee) entity;
            warmTilesAroundWasabiWhip(melee);
            if (melee.drainPlantFoodPending()) {
                applyMeleePlantFood(melee);
            }
        }
    }

    void applyMeleeFamilyBoosts(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Melee) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Melee mint = (Melee) entity;
            if (!mint.drainFamilyBoostPending()) {
                continue;
            }
            activateFamilyBoost(PlantFamily.MELEE,
                    mint.getFamilyBoostDurationSeconds(),
                    mint.resetsFamilyCooldowns(), mint, entitiesToAdd,
                    "Enforce-mint applied plant food to every Melee plant.");
        }
    }

    void warmTilesAroundWasabiWhip(Melee melee) {
        if (melee.getType() == MeleePlantType.WASABI_WHIP
                && melee.getEntityPosition() != null) {
            meltFrozenTiles(melee.getEntityPosition(), 1);
        }
    }

    void activateReadyMelee(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Melee) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Melee melee = (Melee) entity;
            if (melee.getType() == MeleePlantType.CHOMPER) {
                attackWithChomper(melee);
            } else {
                performReadyMeleeAttacks(melee);
            }
        }
    }

    void performReadyMeleeAttacks(Melee melee) {
        while (melee.isReadyToAttack()) {
            if (!performMeleeAttack(melee)) {
                melee.retainSingleReadyAttack();
                return;
            }
            melee.consumeAttack();
        }
    }

    boolean performMeleeAttack(Melee melee) {
        MeleeBehavior behavior = melee.getType().getBehavior();
        switch (behavior) {
            case FRONT_AND_BACK:
                return attackFrontAndBack(melee);
            case AREA:
            case GROWING_AREA:
                return attackMeleeArea(melee, melee.getCurrentAttackRangeTiles(),
                        melee.getCurrentDamage(), false);
            case CHOMPER:
            case FAMILY_BOOST:
                return false;
            default:
                throw new IllegalStateException("Unknown melee behavior: " + behavior);
        }
    }

    boolean attackFrontAndBack(Melee melee) {
        Zombie front = findDirectionalMeleeTarget(melee, true);
        Zombie back = findDirectionalMeleeTarget(melee, false);
        if (front == null && back == null) {
            return false;
        }
        boolean fireDamage = melee.getType() == MeleePlantType.WASABI_WHIP;
        damageMeleeTarget(front, melee.getCurrentDamage(), fireDamage);
        damageMeleeTarget(back, melee.getCurrentDamage(), fireDamage);
        return true;
    }

    Zombie findDirectionalMeleeTarget(Melee melee, boolean front) {
        EntityPosition position = melee.getEntityPosition();
        if (position == null) {
            return null;
        }
        Zombie nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Zombie zombie : getZombies()) {
            if (!isMeleeTargetable(zombie) || zombie.getLane() != position.getRow()) {
                continue;
            }
            double delta = zombie.getColumnPosition() - position.getColumn();
            if (!isCorrectMeleeDirection(delta, front)) {
                continue;
            }
            double distance = Math.abs(delta);
            if (distance <= melee.getCurrentAttackRangeTiles() + POSITION_EPSILON
                    && distance < nearestDistance) {
                nearest = zombie;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    boolean attackMeleeArea(Melee melee, double radius,
            int damage, boolean fireDamage) {
        if (melee.getEntityPosition() == null) {
            return false;
        }
        boolean attacked = false;
        EntityPosition center = melee.getEntityPosition();
        for (Zombie zombie : getZombies()) {
            if (isMeleeTargetable(zombie) && isInsideMeleeArea(zombie, center, radius)) {
                damageMeleeTarget(zombie, damage, fireDamage);
                attacked = true;
            }
        }
        return attacked;
    }

    void damageMeleeTarget(Zombie zombie, int damage, boolean fireDamage) {
        if (zombie == null || damage <= 0 || zombie.isDead()) {
            return;
        }
        damageZombieOrFrozenShell(
                zombie, damage, fireDamage);
        if (zombie.isDead()) {
            reportZombieDeath(zombie);
        }
    }

    void attackWithChomper(Melee chomper) {
        if (!chomper.isReadyToBite()) {
            return;
        }
        Zombie target = findNearestChomperTarget(chomper, chomper.getAttackRangeTiles());
        if (target == null) {
            return;
        }
        target.kill();
        reportZombieDeath(target);
        chomper.startDigesting();
    }

    Zombie findNearestChomperTarget(Melee chomper, double range) {
        EntityPosition position = chomper.getEntityPosition();
        if (position == null) {
            return null;
        }
        Zombie nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Zombie zombie : getZombies()) {
            if (!isMeleeTargetable(zombie) || zombie.getLane() != position.getRow()) {
                continue;
            }
            double distance = zombie.getColumnPosition() - position.getColumn();
            if (distance > POSITION_EPSILON && distance <= range + POSITION_EPSILON
                    && distance < nearestDistance) {
                nearest = zombie;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    void applyMeleePlantFood(Melee melee) {
        switch (melee.getType()) {
            case BONK_CHOY:
                attackMeleeArea(melee, 1.0, melee.getCurrentDamage() * 10, false);
                break;
            case PHAT_BEET:
                attackMeleeArea(melee, 1.0, melee.getCurrentDamage() * 12, false);
                break;
            case CHOMPER:
                swallowPlantFoodTargets(melee, 3);
                break;
            case WASABI_WHIP:
                attackMeleeArea(melee, 1.0, melee.getCurrentDamage() * 10, true);
                break;
            case KIWIBEAST:
                attackMeleeArea(melee, melee.getCurrentAttackRangeTiles(),
                        melee.getCurrentDamage() * 10, false);
                break;
            case ENFORCE_MINT:
                return;
            default:
                throw new IllegalStateException("Unknown melee type: " + melee.getType());
        }
        pendingResults.add(melee.getName() + " used its plant food effect.");
    }

    void swallowPlantFoodTargets(Melee chomper, int targetCount) {
        List<Zombie> candidates = getChomperPlantFoodTargets(chomper);
        int swallowed = 0;
        for (Zombie zombie : candidates) {
            if (swallowed >= targetCount) {
                break;
            }
            zombie.kill();
            reportZombieDeath(zombie);
            swallowed++;
        }
        if (swallowed > 0) {
            chomper.startDigesting();
        }
    }

    List<Zombie> getChomperPlantFoodTargets(Melee chomper) {
        List<Zombie> candidates = new ArrayList<>();
        EntityPosition position = chomper.getEntityPosition();
        if (position == null) {
            return candidates;
        }
        for (Zombie zombie : getZombies()) {
            if (isMeleeTargetable(zombie) && zombie.getLane() == position.getRow()
                    && zombie.getColumnPosition() > position.getColumn() + POSITION_EPSILON) {
                candidates.add(zombie);
            }
        }
        candidates.sort((first, second) -> Double.compare(
                first.getColumnPosition(), second.getColumnPosition()));
        return candidates;
    }

    void meltLane(int lane) {
        for (int column = 0; column < numberOfColumns; column++) {
            EntityPosition position = new EntityPosition(lane, column);
            Tile tile = getTileAt(position);
            if (tile != null && tile.getTileType() == TileType.FROZEN) {
                tile.setTileType(TileType.NORMAL);
            }
        }
    }

    void meltFrozenTiles(EntityPosition center, int radius) {
        for (Tile tile : tiles) {
            EntityPosition position = tile.getPosition();
            if (tile.getTileType() == TileType.FROZEN
                    && Math.abs(position.getRow() - center.getRow()) <= radius
                    && Math.abs(position.getColumn() - center.getColumn()) <= radius) {
                tile.setTileType(TileType.NORMAL);
            }
        }
        meltFrozenPlantsInArea(center, radius, radius);
    }
}
