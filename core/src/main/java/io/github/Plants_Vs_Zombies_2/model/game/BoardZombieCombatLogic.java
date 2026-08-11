package io.github.Plants_Vs_Zombies_2.model.game;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.Sun;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.modifier.Modifier;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.wallnut.Wallnut;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.FastSwimAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.FishingHookAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.FlyAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.KingBuffAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.PianoCrushAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.SmashAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.SubmergeAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.SurfAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.TackleAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.TorchAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.WizardSpellAbility;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.ZombieAbility;
import io.github.Plants_Vs_Zombies_2.view.game.ZombieView;

abstract class BoardZombieCombatLogic extends BoardZombieMovementLogic {
    protected BoardZombieCombatLogic() {
        super();
    }

    protected BoardZombieCombatLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    void updateZombie(Zombie zombie, float deltaSeconds) {
        finishDodoFlight(zombie);
        if (keepStationaryZombieAtRightEdge(zombie)) {
            return;
        }
        BasePlant blockingPlant = findNearestPlantAhead(zombie);
        if (tryTransformPlantWithWizard(
                zombie, blockingPlant)) {
            return;
        }
        updateBeachMovementStates(zombie, blockingPlant);
        Zombie blockingHypnotizedZombie = findNearestHypnotizedZombieAhead(zombie);
        if (blockingPlant != null
                && (blockingHypnotizedZombie == null
                        || blockingHypnotizedZombie.getColumnPosition() <= blockingPlant.getEntityPosition()
                                .getColumn())
                && tryCrushPlantWithPiano(zombie, blockingPlant)) {
            reportDestroyedPlant(blockingPlant);
            return;
        }
        if (blockingPlant != null
                && (blockingHypnotizedZombie == null
                        || blockingHypnotizedZombie.getColumnPosition() <= blockingPlant.getEntityPosition()
                                .getColumn())
                && tryCrushPlantWithSurfboard(zombie, blockingPlant)) {
            reportDestroyedPlant(blockingPlant);
            return;
        }
        if (blockingPlant != null
                && (blockingHypnotizedZombie == null
                        || blockingHypnotizedZombie.getColumnPosition() <= blockingPlant.getEntityPosition()
                                .getColumn())
                && tryFlyOverPlant(zombie, blockingPlant)) {
            return;
        }
        if (blockingPlant != null
                && (blockingHypnotizedZombie == null
                        || blockingHypnotizedZombie.getColumnPosition() <= blockingPlant.getEntityPosition()
                                .getColumn())
                && tryBurnPlant(zombie, blockingPlant)) {
            reportDestroyedPlant(blockingPlant);
            return;
        }
        if (blockingHypnotizedZombie != null
                && (blockingPlant == null
                        || blockingHypnotizedZombie.getColumnPosition() > blockingPlant.getEntityPosition()
                                .getColumn())) {
            updateZombieAgainstHypnotizedTarget(zombie,
                    blockingHypnotizedZombie, deltaSeconds);
            return;
        }
        if (blockingPlant == null) {
            zombie.move(deltaSeconds, 0.0);
            if (zombie.getColumnPosition() <= POSITION_EPSILON) {
                zombie.markReachedHouse();
            }
            return;
        }

        double attackColumn = blockingPlant.getEntityPosition().getColumn() + Zombie.ATTACK_REACH;
        if (zombie.getColumnPosition() <= attackColumn + POSITION_EPSILON) {
            if (tryTacklePlant(zombie, blockingPlant)) {
                reportDestroyedPlant(blockingPlant);
                return;
            }
            attackPlant(zombie, blockingPlant, deltaSeconds);
            handleWallnutAfterAttack(zombie, blockingPlant, deltaSeconds);
            reportDestroyedPlant(blockingPlant);
        } else {
            zombie.move(deltaSeconds, attackColumn);
        }
    }

    boolean tryCrushPlantWithPiano(
            Zombie zombie, BasePlant plant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (!(ability instanceof PianoCrushAbility)) {
                continue;
            }
            PianoCrushAbility piano = (PianoCrushAbility) ability;
            if (!piano.canCrush(zombie, plant)) {
                continue;
            }
            plant.takeDamage(Integer.MAX_VALUE);
            pendingResults.add(zombie.getName()
                    + " crushed " + plant.getName()
                    + " with its piano at "
                    + plant.getEntityPosition() + ".");
            return true;
        }
        return false;
    }

    boolean tryTransformPlantWithWizard(
            Zombie zombie, BasePlant plant) {
        if (plant == null
                || plant.getEntityPosition() == null) {
            return false;
        }

        double distance = zombie.getColumnPosition()
                - plant.getEntityPosition().getColumn();
        if (distance < 0.0
                || distance > Zombie.ATTACK_REACH
                        + POSITION_EPSILON) {
            return false;
        }

        for (ZombieAbility ability : zombie.getAbilities()) {
            if (!(ability instanceof WizardSpellAbility)) {
                continue;
            }
            WizardSpellAbility spell = (WizardSpellAbility) ability;
            boolean transformed = spell.transformReachedPlant(
                    zombie, plant);
            if (transformed) {
                pendingResults.add(zombie.getName()
                        + " transformed "
                        + plant.getName()
                        + " into a cat on contact.");
            }
            return true;
        }
        return false;
    }

    boolean keepStationaryZombieAtRightEdge(
            Zombie zombie) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof FishingHookAbility
                    || ability instanceof KingBuffAbility) {
                zombie.moveTo(numberOfColumns - 1.0);
                return true;
            }
        }
        return false;
    }

    void updateBeachMovementStates(Zombie zombie,
            BasePlant blockingPlant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof SubmergeAbility) {
                ((SubmergeAbility) ability).updateState(
                        zombie, asBoard(), blockingPlant);
            } else if (ability instanceof FastSwimAbility) {
                ability.tryUse(zombie, asBoard());
            } else if (ability instanceof SurfAbility) {
                ability.tryUse(zombie, asBoard());
            }
        }
    }

    boolean tryCrushPlantWithSurfboard(Zombie zombie,
            BasePlant plant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (!(ability instanceof SurfAbility)) {
                continue;
            }
            SurfAbility surf = (SurfAbility) ability;
            if (!surf.tryCrush(zombie, plant, asBoard())) {
                continue;
            }
            plant.takeDamage(Integer.MAX_VALUE);
            pendingResults.add(zombie.getName() + " crushed "
                    + plant.getName() + " with its surfboard at "
                    + plant.getEntityPosition() + ".");
            return true;
        }
        return false;
    }

    boolean tryFlyOverPlant(Zombie zombie, BasePlant plant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof FlyAbility
                    && ((FlyAbility) ability).tryFlyOver(zombie, plant, asBoard())) {
                pendingResults.add(zombie.getName() + " flew over "
                        + plant.getName() + ".");
                return true;
            }
        }
        return false;
    }

    void finishDodoFlight(Zombie zombie) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof FlyAbility
                    && ((FlyAbility) ability).isFlying()) {
                ((FlyAbility) ability).finishFlight(zombie);
            }
        }
    }

    boolean tryBurnPlant(Zombie zombie, BasePlant plant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (!(ability instanceof TorchAbility)) {
                continue;
            }
            TorchAbility torch = (TorchAbility) ability;
            if (!torch.canBurn(zombie, plant)
                    || !torch.tryUse(zombie, asBoard())) {
                continue;
            }
            plant.takeDamage(Integer.MAX_VALUE);
            pendingResults.add(zombie.getName() + " burned "
                    + plant.getName() + " at " + plant.getEntityPosition() + ".");
            return true;
        }
        return false;
    }

    boolean tryTacklePlant(Zombie zombie, BasePlant plant) {
        TackleAbility tackle = useTackle(zombie);
        if (tackle == null) {
            return false;
        }
        int lethalDamage = Math.max(tackle.getSmashDamage(), plant.getCurrentHP());
        plant.takeDamage(lethalDamage);
        pendingResults.add(zombie.getName() + " tackled and destroyed "
                + plant.getName() + " at " + plant.getEntityPosition() + ".");
        return true;
    }

    boolean tryTackleZombie(Zombie zombie, Zombie target) {
        TackleAbility tackle = useTackle(zombie);
        if (tackle == null) {
            return false;
        }
        int lethalDamage = Math.max(tackle.getSmashDamage(), target.getHitPoints());
        target.takeDirectDamage(lethalDamage);
        pendingResults.add(zombie.getName() + " tackled and destroyed hypnotized "
                + target.getName() + ".");
        return true;
    }

    TackleAbility useTackle(Zombie zombie) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof TackleAbility
                    && ability.tryUse(zombie, asBoard())) {
                return (TackleAbility) ability;
            }
        }
        return null;
    }

    void attackPlant(Zombie zombie, BasePlant plant, float deltaSeconds) {
        boolean eaten = !trySmashPlant(zombie, plant);
        if (eaten) {
            zombie.eat(plant, deltaSeconds);
        }
        handleModifierAfterAttack(zombie, plant, eaten);
    }

    void handleModifierAfterAttack(Zombie zombie, BasePlant plant,
            boolean eaten) {
        if (!eaten || !(plant instanceof Modifier)) {
            return;
        }
        Modifier modifier = (Modifier) plant;
        if (modifier.onEatenBy(zombie)) {
            pendingResults.add("Zombie " + zombie.getName()
                    + " was hypnotized by " + modifier.getName() + ".");
        }
    }

    boolean trySmashPlant(Zombie zombie, BasePlant plant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof SmashAbility && ability.tryUse(zombie, asBoard())) {
                plant.takeDamage(Integer.MAX_VALUE);
                return true;
            }
        }
        return false;
    }

    void handleWallnutAfterAttack(Zombie zombie, BasePlant plant, float deltaSeconds) {
        if (!(plant instanceof Wallnut)) {
            return;
        }
        Wallnut wallnut = (Wallnut) plant;
        reflectEndurianDamage(zombie, wallnut, deltaSeconds);
        divertZombieAfterGarlicBite(zombie, wallnut);
        releaseSunBeanSun(wallnut);
        applyWallnutExplosion(wallnut);
        if (zombie.isDead()) {
            reportZombieDeath(zombie);
        }
    }

    void reflectEndurianDamage(Zombie zombie, Wallnut wallnut, float deltaSeconds) {
        int reflectedDamage = wallnut.calculateReflectedDamage(deltaSeconds);
        if (reflectedDamage > 0) {
            damageZombieOrFrozenShell(
                    zombie, reflectedDamage, false);
        }
    }

    void divertZombieAfterGarlicBite(Zombie zombie, Wallnut wallnut) {
        if (!wallnut.drainDivertLanePending()) {
            return;
        }
        int targetLane = wallnut.chooseAdjacentLane(zombie.getLane(), numberOfRows);
        zombie.moveToLane(targetLane);
        pendingResults.add("Garlic diverted " + zombie.getName() + " into lane " + targetLane + ".");
    }

    void releaseSunBeanSun(Wallnut wallnut) {
        int sunAmount = wallnut.drainPendingSunAmount();
        if (sunAmount <= 0) {
            return;
        }
        addEntity(Sun.createPlantSun(sunAmount, wallnut.getEntityPosition()));
        pendingResults.add("plant Sun Bean produced " + sunAmount + " sun at "
                + wallnut.getEntityPosition());
    }

    void applyWallnutExplosion(Wallnut wallnut) {
        int explosionDamage = wallnut.drainPendingExplosionDamage();
        if (explosionDamage <= 0 || wallnut.getEntityPosition() == null) {
            return;
        }
        EntityPosition center = wallnut.getEntityPosition();
        for (Zombie target : getZombies()) {
            if (!target.isHypnotized()
                    && !target.isSubmerged()
                    && isInsideThreeByThree(target, center)) {
                target.takeDamage(explosionDamage);
            }
        }
        pendingResults.add(wallnut.getName() + " exploded for " + explosionDamage
                + " damage around " + center + ".");
    }

    BasePlant findNearestPlantAhead(Zombie zombie) {
        BasePlant nearestPlant = null;
        int nearestColumn = -1;
        for (BasePlant plant : getPlants()) {
            if (plant.isRemoved()
                    || plant.isTransformedToSheep()
                    || plant.getEntityPosition().getRow() != zombie.getLane()) {
                continue;
            }
            int plantColumn = plant.getEntityPosition().getColumn();
            if (plantColumn <= zombie.getColumnPosition() + POSITION_EPSILON
                    && isBetterBlocker(plant, nearestPlant, plantColumn, nearestColumn)) {
                nearestPlant = plant;
                nearestColumn = plantColumn;
            }
        }
        return nearestPlant;
    }

    void reportDestroyedPlant(BasePlant plant) {
        if (plant.isDestroyed()) {
            pendingResults.add("Plant " + plant.getName() + " at "
                    + plant.getEntityPosition() + " is destroyed.");
        }
    }

    void reportZombieDeath(Zombie zombie) {
        if (zombie.isDeathReported()) {
            return;
        }
        zombie.markDeathReported();
        zombie.markForRemoval();
        pendingResults.add(ZombieView.buildDeathMessage(zombie));
    }
}
