package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import model.App;
import model.Constants;
import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.other.ArcadeMachine;
import model.game.entities.other.CollectibleDrop;
import model.game.entities.other.IceBlock;
import model.game.entities.other.PushedObstacle;
import model.game.entities.other.RollingBarrel;
import model.game.entities.other.Sun;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantFamily;
import model.game.entities.plants.PlantTag;
import model.game.entities.plants.explosive.Explosive;
import model.game.entities.plants.explosive.ExplosiveBehavior;
import model.game.entities.plants.explosive.ExplosivePlantType;
import model.game.entities.plants.homing.Homing;
import model.game.entities.plants.homing.HomingBehavior;
import model.game.entities.plants.homing.HomingPlantType;
import model.game.entities.plants.lobber.Lobber;
import model.game.entities.plants.lobber.LobberPlantType;
import model.game.entities.plants.melee.Melee;
import model.game.entities.plants.melee.MeleeBehavior;
import model.game.entities.plants.melee.MeleePlantType;
import model.game.entities.plants.modifier.Modifier;
import model.game.entities.plants.modifier.ModifierPlantType;
import model.game.entities.plants.shooter.Shooter;
import model.game.entities.plants.shooter.ShooterPlantType;
import model.game.entities.plants.strikeThrough.StrikeThrough;
import model.game.entities.plants.strikeThrough.StrikeThroughPlantType;
import model.game.entities.plants.sunProducer.SunProducer;
import model.game.entities.plants.sunProducer.SunProducerPlantType;
import model.game.entities.plants.wallnut.Wallnut;
import model.game.entities.plants.wallnut.WallnutPlantType;
import model.game.entities.projectile.BouncingGrape;
import model.game.entities.projectile.HomingProjectile;
import model.game.entities.projectile.LobbedProjectile;
import model.game.entities.projectile.PiercingProjectile;
import model.game.entities.projectile.Projectile;
import model.game.entities.projectile.effect.ProjectileEffect;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.ZombieType;
import model.game.entities.zombies.abilities.ArcadePushAbility;
import model.game.entities.zombies.abilities.BarrelPushAbility;
import model.game.entities.zombies.abilities.ChillOnHitAbility;
import model.game.entities.zombies.abilities.FlyAbility;
import model.game.entities.zombies.abilities.FastSwimAbility;
import model.game.entities.zombies.abilities.FishingHookAbility;
import model.game.entities.zombies.abilities.IceBlockPushAbility;
import model.game.entities.zombies.abilities.JuggleAbility;
import model.game.entities.zombies.abilities.KingBuffAbility;
import model.game.entities.zombies.abilities.LaunchAbility;
import model.game.entities.zombies.abilities.PianoCrushAbility;
import model.game.entities.zombies.abilities.SmashAbility;
import model.game.entities.zombies.abilities.SubmergeAbility;
import model.game.entities.zombies.abilities.SurfAbility;
import model.game.entities.zombies.abilities.TackleAbility;
import model.game.entities.zombies.abilities.TorchAbility;
import model.game.entities.zombies.abilities.UmbrellaBounceAbility;
import model.game.entities.zombies.abilities.WizardSpellAbility;
import model.game.entities.zombies.abilities.ZombieAbility;
import model.game.structure.BaseStructure;
import model.game.structure.Grave;
import model.game.tile.Tile;
import model.game.tile.TileType;

public class Board {
    private static final double POSITION_EPSILON = 0.000001;
    private static final double SWEET_POTATO_ATTRACTION_RANGE = 1.0;
    private static final double PROJECTILE_COLLISION_RADIUS = 0.35;
    private static final double PROJECTILE_BOARD_MARGIN = 0.5;
    private static final double GRAPE_COLLISION_RADIUS = 0.35;
    private static final int GRAPESHOT_PROJECTILE_COUNT = 8;
    private static final int FINISH_EXPLOSION_DAMAGE = 1800;
    private static final double FROZEN_SHELL_WARMING_DAMAGE_PER_SECOND = 60.0;

    private static final class ActiveFamilyBoost {
        private final PlantFamily family;
        private final boolean resetCooldowns;
        private final Set<BasePlant> affectedPlants;
        private double remainingSeconds;

        private ActiveFamilyBoost(PlantFamily family, double remainingSeconds,
                boolean resetCooldowns) {
            this.family = family;
            this.remainingSeconds = remainingSeconds;
            this.resetCooldowns = resetCooldowns;
            this.affectedPlants = Collections.newSetFromMap(new IdentityHashMap<>());
        }

        private boolean isActive() {
            return remainingSeconds > POSITION_EPSILON;
        }
    }

    private final int numberOfRows;
    private final int numberOfColumns;
    private final List<Tile> tiles;
    private final List<Entity> allEntities;
    private final List<BaseStructure> structures;
    private final List<String> pendingResults;
    private final List<Zombie> pendingSpawnedZombies;
    private final List<ActiveFamilyBoost> activeFamilyBoosts;
    private final List<PlantFamily> pendingPlantCooldownResets;
    private final Set<EntityPosition> lowBeachTiles;
    private boolean frostbiteCavesRules;
    private boolean bigWaveBeachRules;
    private int waterColumnCount;
    private int maximumWaterColumnCount;

    public Board() {
        this(Constants.DEFAULT_BOARD_ROWS, Constants.DEFAULT_BOARD_COLUMNS);
    }

    public Board(int numberOfRows, int numberOfColumns) {
        if (numberOfRows <= 0 || numberOfColumns <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }
        this.numberOfRows = numberOfRows;
        this.numberOfColumns = numberOfColumns;
        this.tiles = new ArrayList<>();
        this.allEntities = new ArrayList<>();
        this.structures = new ArrayList<>();
        this.pendingResults = new ArrayList<>();
        this.pendingSpawnedZombies = new ArrayList<>();
        this.activeFamilyBoosts = new ArrayList<>();
        this.pendingPlantCooldownResets = new ArrayList<>();
        this.lowBeachTiles = new LinkedHashSet<>();
        initializeTiles();
    }

    private void initializeTiles() {
        for (int row = 0; row < numberOfRows; row++) {
            for (int column = 0; column < numberOfColumns; column++) {
                tiles.add(new Tile(new EntityPosition(row, column), TileType.NORMAL));
            }
        }
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

    private void updateActiveFamilyBoosts(float deltaSeconds) {
        for (ActiveFamilyBoost boost : activeFamilyBoosts) {
            boost.remainingSeconds -= deltaSeconds;
        }
        activeFamilyBoosts.removeIf(boost -> !boost.isActive());
    }

    private void activateFamilyBoost(PlantFamily family, double durationSeconds,
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

    private static String formatDuration(double seconds) {
        return String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }

    private void applyFamilyBoostToPlant(ActiveFamilyBoost boost, BasePlant plant,
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

    private void applyActiveFamilyBoostsToPlant(BasePlant plant) {
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

    private void cleanupRemovedEntities() {
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

    private Set<EntityPosition> collectRemovedPlantPositions() {
        Set<EntityPosition> positions = new LinkedHashSet<>();
        for (Entity entity : allEntities) {
            if (entity.isRemoved() && entity instanceof BasePlant
                    && entity.getEntityPosition() != null) {
                positions.add(entity.getEntityPosition());
            }
        }
        return positions;
    }

    private void drownPlantWithoutLilyPad(
            EntityPosition position) {
        Tile tile = getTileAt(position);
        if (tile == null
                || tile.getTileType() != TileType.WATER) {
            return;
        }
        drownUnsupportedPlants(position,
                " because it no longer had a lily pad.");
    }

    private void refreshTilePlant(EntityPosition position) {
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

    private void updateEntities(List<Entity> updateSnapshot, List<Entity> entitiesToAdd,
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

    private void reportSunLanding(Entity entity, boolean sunWasDropping) {
        if (sunWasDropping && entity instanceof Sun && !((Sun) entity).isDropping()) {
            pendingResults.add("Sun reached the ground at position " + entity.getEntityPosition());
        }
    }

    private void collectProducedSuns(Entity entity, List<Entity> entitiesToAdd) {
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

    private void applyTorchwoodProjectileEffects(List<Entity> updateSnapshot) {
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

    private void meltFrozenTilesHitByFireProjectiles(
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

    private void activateReadyShooters(List<Entity> updateSnapshot,
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

    private boolean hasTarget(Shooter shooter) {
        for (Zombie zombie : getZombies()) {
            if (!zombie.isHypnotized() && shooter.canTarget(zombie, numberOfRows)) {
                return true;
            }
        }
        return false;
    }

    private void activateReadyLobbers(List<Entity> updateSnapshot,
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

    private Zombie findFirstLobberTarget(Lobber lobber) {
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

    private void activateReadyStrikeThroughs(List<Entity> updateSnapshot,
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

    private boolean hasStrikeThroughTarget(StrikeThrough plant) {
        for (Zombie zombie : getZombies()) {
            if (!zombie.isDead() && !zombie.isHypnotized()
                    && !zombie.isSubmerged()
                    && plant.canTarget(zombie.getColumnPosition(), zombie.getLane())) {
                return true;
            }
        }
        return false;
    }

    private void activateReadyHomings(List<Entity> updateSnapshot,
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

    private Zombie findHomingTarget(Homing plant) {
        switch (plant.getType().getBehavior()) {
        case HYPNOTIZE:
            return randomHomingTarget(plant, false);
        case LIGHTNING:
            return plant.hasTargetPriorityUp()
                    ? strongestHomingTarget(plant) : randomHomingTarget(plant, true);
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

    private void performHomingAttack(Homing plant, Zombie target,
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

    private Zombie randomHomingTarget(Homing plant, boolean allowBoss) {
        List<Zombie> candidates = homingCandidates(plant, allowBoss, false);
        if (candidates.isEmpty()) {
            return null;
        }
        Collections.shuffle(candidates);
        return candidates.get(0);
    }

    private Zombie nearestHomingTarget(Homing plant) {
        List<Zombie> candidates = homingCandidates(plant, true, false);
        return candidates.stream()
                .min(Comparator.comparingDouble(zombie -> distanceSquared(plant, zombie)))
                .orElse(null);
    }

    private Zombie strongestHomingTarget(Homing plant) {
        List<Zombie> candidates = homingCandidates(plant, true, false);
        return candidates.stream()
                .max(Comparator.comparingInt(Zombie::getCurrentDurability)
                        .thenComparingDouble(zombie -> -distanceSquared(plant, zombie)))
                .orElse(null);
    }

    private Zombie nearestMagnetTarget(Homing plant) {
        List<Zombie> candidates = homingCandidates(plant, true, true);
        return candidates.stream()
                .min(Comparator.comparingDouble(zombie -> distanceSquared(plant, zombie)))
                .orElse(null);
    }

    private List<Zombie> homingCandidates(Homing plant, boolean allowBoss,
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

    private static boolean isHomingTargetable(Zombie zombie) {
        return zombie != null && !zombie.isDead() && !zombie.isRemoved()
                && !zombie.isHypnotized() && !zombie.isSubmerged();
    }

    private static boolean isZomboss(Zombie zombie) {
        return zombie.getType().name().startsWith("ZOMBOSS_");
    }

    private static boolean isInsideHomingRange(Homing plant, Zombie zombie) {
        double range = plant.getRangeTiles();
        return Double.isInfinite(range)
                || distanceSquared(plant, zombie) <= range * range + POSITION_EPSILON;
    }

    private static double distanceSquared(Homing plant, Zombie zombie) {
        EntityPosition position = plant.getEntityPosition();
        if (position == null) {
            return Double.POSITIVE_INFINITY;
        }
        double rowDelta = zombie.getLane() - position.getRow();
        double columnDelta = zombie.getColumnPosition() - position.getColumn();
        return rowDelta * rowDelta + columnDelta * columnDelta;
    }

    private void applyPendingStrikeThroughBoardEffects(
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

    private void applyStrikeThroughFamilyBoosts(List<Entity> updateSnapshot,
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

    private void applyPendingLobberBoardEffects(List<Entity> updateSnapshot,
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

    private void applyLobberFamilyBoosts(List<Entity> updateSnapshot,
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

    private void warmTilesAroundPepperPult(Lobber lobber) {
        if (lobber.getType() == LobberPlantType.PEPPER_PULT
                && lobber.getEntityPosition() != null) {
            meltFrozenTiles(lobber.getEntityPosition(), lobber.getWarmthRadius());
        }
    }

    private void addLobberPlantFoodProjectiles(Lobber lobber,
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

    private List<Zombie> getLobberPlantFoodTargets(Lobber lobber) {
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

    private void applyPendingShooterBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Shooter) || entity.isRemoved() || ((BasePlant) entity).isDisabled()) {
                continue;
            }
            Shooter mint = (Shooter) entity;
            if (mint.drainFamilyBoostPending()) {
                applyShooterFamilyBoost(mint, entitiesToAdd);
            }
        }
    }

    private void applyShooterFamilyBoost(Shooter mint, List<Entity> entitiesToAdd) {
        activateFamilyBoost(PlantFamily.SHOOTER,
                mint.getFamilyBoostDurationSeconds(), mint.resetsFamilyCooldowns(),
                mint, entitiesToAdd,
                "Appease-mint applied plant food to every Shooter plant.");
    }

    private void resolveProjectileImpacts(List<Entity> updateSnapshot) {
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

    private void resolveProjectileHits(Projectile projectile) {
        while (!projectile.isRemoved()) {
            Grave grave = findFirstGraveHit(projectile);
            PushedObstacle pushedObstacle =
                    findFirstPushedObstacleHit(projectile);
            BasePlant octopusPlant =
                    findFirstOctopusCoveredPlantHit(projectile);
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
                boolean octopusDestroyed =
                        octopusPlant.damageOctopus(1);
                projectile.markForRemoval();
                pendingResults.add(octopusPlant.getName()
                        + "'s octopus cover was hit."
                        + (octopusDestroyed
                                ? " The plant is active again." : ""));
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
                                ? " It was destroyed." : ""));
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

    private Grave findFirstGraveHit(Projectile projectile) {
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

    private boolean isGraveBeforeOtherTargets(
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

    private double minimumTargetParameter(Projectile projectile,
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

    private static double collisionParameter(Projectile projectile,
            EntityPosition position) {
        if (position == null) {
            return Double.POSITIVE_INFINITY;
        }
        double parameter = projectile.getIntersectionParameter(
                position.getRow(), position.getColumn(),
                PROJECTILE_COLLISION_RADIUS);
        return Double.isNaN(parameter)
                ? Double.POSITIVE_INFINITY : parameter;
    }

    private void damageGrave(Grave grave, int damage) {
        grave.takeDamage(damage);
        if (grave.isRemoved()) {
            structures.remove(grave);
            setTileType(grave.getPosition(), TileType.NORMAL);
            pendingResults.add("Grave at " + grave.getPosition()
                    + " was destroyed.");
            return;
        }
        pendingResults.add("Grave at " + grave.getPosition()
                + " absorbed " + damage + " projectile damage; "
                + grave.getHitPoints() + " HP remains.");
    }

    private boolean resolveFrozenZombieImpact(
            Projectile projectile, Zombie zombie) {
        if (!damageFrozenZombieShell(zombie,
                Math.max(1, projectile.getImpactDamage()),
                projectile.hasFireEffect())) {
            return false;
        }
        projectile.markForRemoval();
        return true;
    }

    private boolean damageFrozenZombieShell(
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

    private void damageZombieOrFrozenShell(
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

    private boolean damageFrozenPlantIce(BasePlant plant,
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
                    ? "a fire attack" : damage + " damage";
            pendingResults.add("The ice around " + plant.getName()
                    + " absorbed " + impact + "; "
                    + plant.getIceShellHitPoints()
                    + " HP remains.");
        }
        return true;
    }

    private void reportFrozenZombieShellHit(
            Zombie zombie, int damage, boolean released,
            boolean fireDamage) {
        if (released) {
            releaseFrozenZombie(zombie);
            return;
        }
        String impact = fireDamage
                ? "a fire attack" : damage + " damage";
        pendingResults.add("The ice around " + zombie.getName()
                + " absorbed " + impact + "; "
                + zombie.getFrozenShellHitPoints()
                + " HP remains.");
    }

    private Zombie findEncasedZombieAt(EntityPosition position) {
        if (position == null) {
            return null;
        }
        for (Zombie zombie : getZombies()) {
            if (zombie.isEncasedInIce()
                    && zombie.getLane() == position.getRow()
                    && (int) Math.floor(zombie.getColumnPosition())
                            == position.getColumn()) {
                return zombie;
            }
        }
        return null;
    }

    private void releaseFrozenZombie(Zombie zombie) {
        EntityPosition position = zombie.getEntityPosition();
        Tile tile = getTileAt(position);
        if (tile != null && tile.getTileType() == TileType.FROZEN) {
            tile.setTileType(TileType.NORMAL);
        }
        pendingResults.add("The ice around " + zombie.getName()
                + " at " + position + " was destroyed; the zombie is active.");
    }

    private BasePlant findFirstOctopusCoveredPlantHit(
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

    private boolean isOctopusBeforeOtherTargets(
            Projectile projectile, BasePlant octopusPlant,
            PushedObstacle pushedObstacle,
            BasePlant frozenPlant, Zombie zombie) {
        EntityPosition octopusPosition =
                octopusPlant.getEntityPosition();
        double octopusParameter =
                projectile.getIntersectionParameter(
                        octopusPosition.getRow(),
                        octopusPosition.getColumn(),
                        PROJECTILE_COLLISION_RADIUS);
        if (Double.isNaN(octopusParameter)) {
            return false;
        }

        if (pushedObstacle != null) {
            double obstacleParameter =
                    projectile.getIntersectionParameter(
                            pushedObstacle.getLane(),
                            pushedObstacle.getColumnPosition(),
                            PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(obstacleParameter)
                    && obstacleParameter + POSITION_EPSILON
                            < octopusParameter) {
                return false;
            }
        }

        if (frozenPlant != null) {
            EntityPosition frozenPosition =
                    frozenPlant.getEntityPosition();
            double frozenParameter =
                    projectile.getIntersectionParameter(
                            frozenPosition.getRow(),
                            frozenPosition.getColumn(),
                            PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(frozenParameter)
                    && frozenParameter + POSITION_EPSILON
                            < octopusParameter) {
                return false;
            }
        }

        if (zombie != null) {
            double zombieParameter =
                    projectile.getIntersectionParameter(
                            zombie.getLane(),
                            zombie.getColumnPosition(),
                            PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(zombieParameter)
                    && zombieParameter + POSITION_EPSILON
                            < octopusParameter) {
                return false;
            }
        }
        return true;
    }

    private PushedObstacle findFirstPushedObstacleHit(
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

    private boolean isPushedObstacleBeforeOtherTargets(
            Projectile projectile, PushedObstacle obstacle,
            BasePlant frozenPlant, Zombie zombie) {
        double obstacleParameter =
                projectile.getIntersectionParameter(
                        obstacle.getLane(),
                        obstacle.getColumnPosition(),
                        PROJECTILE_COLLISION_RADIUS);
        if (Double.isNaN(obstacleParameter)) {
            return false;
        }

        if (frozenPlant != null) {
            EntityPosition position =
                    frozenPlant.getEntityPosition();
            double plantParameter =
                    projectile.getIntersectionParameter(
                            position.getRow(),
                            position.getColumn(),
                            PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(plantParameter)
                    && plantParameter + POSITION_EPSILON
                            < obstacleParameter) {
                return false;
            }
        }

        if (zombie != null) {
            double zombieParameter =
                    projectile.getIntersectionParameter(
                            zombie.getLane(),
                            zombie.getColumnPosition(),
                            PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(zombieParameter)
                    && zombieParameter + POSITION_EPSILON
                            < obstacleParameter) {
                return false;
            }
        }
        return true;
    }

    private void damagePushedObstacle(
            PushedObstacle obstacle, int damage) {
        if (obstacle == null || obstacle.isDestroyed()) {
            return;
        }
        obstacle.takeDamage(damage);
        handleDestroyedPushedObstacle(obstacle);
    }

    private void handleDestroyedPushedObstacle(
            PushedObstacle obstacle) {
        if (obstacle == null || !obstacle.isDestroyed()) {
            return;
        }
        if (obstacle instanceof RollingBarrel) {
            releaseBarrelImps((RollingBarrel) obstacle);
        }
    }

    private void releaseBarrelImps(RollingBarrel barrel) {
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

    private BasePlant findFirstFrozenPlantHit(Projectile projectile) {
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

    private BasePlant findFirstFrozenPlantHit(BouncingGrape grape) {
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

    private boolean isFrozenPlantBeforeZombie(BouncingGrape grape,
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

    private boolean isFrozenPlantBeforeZombie(Projectile projectile,
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

    private void extinguishProspectorDynamite(
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

    private boolean tryReflectProjectile(Projectile projectile,
            Zombie target) {
        if (projectile == null || target == null
                || projectile.isRemoved()) {
            return false;
        }
        for (ZombieAbility ability : target.getAbilities()) {
            if (!(ability instanceof JuggleAbility)
                    || !((JuggleAbility) ability)
                            .tryReflect(target, projectile, this)) {
                continue;
            }

            BasePlant source = findProjectileSourcePlant(projectile);
            int reflectedDamage =
                    Math.max(1, projectile.getImpactDamage());
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

    private void chillProjectileSourceIfBlockhead(Projectile projectile,
            Zombie blockhead) {
        for (ZombieAbility ability : blockhead.getAbilities()) {
            if (!(ability instanceof ChillOnHitAbility)
                    || !ability.tryUse(blockhead, this)) {
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

    private BasePlant findProjectileSourcePlant(Projectile projectile) {
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

    private Zombie findFirstZombieHit(Projectile projectile) {
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

    private static boolean canProjectileHit(Projectile projectile, Zombie zombie) {
        return !(projectile instanceof PiercingProjectile)
                || ((PiercingProjectile) projectile).canHit(zombie);
    }

    private boolean isProjectileOutsideBoard(Projectile projectile) {
        return projectile.getRowPosition() < -PROJECTILE_BOARD_MARGIN
                || projectile.getRowPosition() > numberOfRows - 1 + PROJECTILE_BOARD_MARGIN
                || projectile.getColumnPosition() < -PROJECTILE_BOARD_MARGIN
                || projectile.getColumnPosition() > numberOfColumns - 1 + PROJECTILE_BOARD_MARGIN;
    }

    private void resolveHomingProjectileImpacts(List<Entity> updateSnapshot) {
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

    private void resolveLobbedProjectileImpacts(List<Entity> updateSnapshot) {
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

    private Zombie findLobbedLandingTarget(LobbedProjectile projectile) {
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

    private static boolean isAtLobbedLandingPoint(Zombie zombie,
            LobbedProjectile projectile) {
        if (zombie == null || zombie.isDead() || zombie.isRemoved()
                || zombie.isHypnotized()) {
            return false;
        }
        double rowDelta = zombie.getLane() - projectile.getLandingRow();
        double columnDelta = zombie.getColumnPosition() - projectile.getLandingColumn();
        return rowDelta * rowDelta + columnDelta * columnDelta
                <= PROJECTILE_COLLISION_RADIUS * PROJECTILE_COLLISION_RADIUS;
    }

    private void applyLobbedSplash(LobbedProjectile projectile, Zombie directTarget) {
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
                effect.apply(zombie);
            }
        }
    }

    private boolean isProtectedFromLobbers(Zombie zombie) {
        if (zombie == null) {
            return false;
        }
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof UmbrellaBounceAbility
                    && ability.tryUse(zombie, this)) {
                pendingResults.add(zombie.getName()
                        + " deflected a lobbed projectile.");
                return true;
            }
        }
        return false;
    }

    private static boolean isInsideLobbedSplash(Zombie zombie,
            LobbedProjectile projectile) {
        double radius = projectile.getSplashRadiusTiles();
        return Math.abs(zombie.getLane() - projectile.getLandingRow()) <= radius
                && Math.abs(zombie.getColumnPosition() - projectile.getLandingColumn()) <= radius;
    }

    private void reportDeadLobberTargets() {
        for (Zombie zombie : getZombies()) {
            if (zombie.isDead()) {
                reportZombieDeath(zombie);
            }
        }
    }

    private void resolveGrapeImpacts(List<Entity> updateSnapshot) {
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

    private Zombie findFirstZombieHit(BouncingGrape grape) {
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

    private void reportDeadZombies(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (entity instanceof Zombie && ((Zombie) entity).isDead()) {
                reportZombieDeath((Zombie) entity);
            }
        }
    }


    private void applyPendingHomingBoardEffects(List<Entity> updateSnapshot,
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

    private void applyHomingFamilyBoosts(List<Entity> updateSnapshot,
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

    private void applyHomingPlantFood(Homing plant,
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

    private void addHomingPlantFoodProjectiles(Homing plant,
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

    private void strikeRandomHomingTargets(Homing plant) {
        List<Zombie> targets = randomHomingTargets(plant,
                plant.getPlantFoodTargetCount(), true);
        for (Zombie target : targets) {
            target.takeDamage(plant.getDamage());
            if (target.isDead()) {
                reportZombieDeath(target);
            }
        }
        reportHomingPlantFood(plant, targets.size());
    }

    private void removeAllMetalArmorInRange(Homing plant) {
        int removedArmorCount = 0;
        for (Zombie zombie : homingCandidates(plant, true, true)) {
            if (zombie.removeMagnetizableArmor()) {
                removedArmorCount++;
            }
        }
        reportHomingPlantFood(plant, removedArmorCount);
    }

    private void addCatTailBarrage(Homing plant,
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

    private List<Zombie> randomHomingTargets(Homing plant, int maximumTargets,
            boolean allowBoss) {
        List<Zombie> targets = homingCandidates(plant, allowBoss, false);
        Collections.shuffle(targets);
        int count = Math.min(maximumTargets, targets.size());
        return new ArrayList<>(targets.subList(0, count));
    }

    private void reportHomingPlantFood(Homing plant, int affectedCount) {
        if (affectedCount > 0) {
            pendingResults.add(plant.getName() + " used its plant food effect on "
                    + affectedCount + " target(s).");
        }
    }

    private void applyPendingSunProducerBoardEffects(List<Entity> updateSnapshot,
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

    private void applySunProducerFamilyBoost(SunProducer mint,
            List<Entity> entitiesToAdd) {
        activateFamilyBoost(PlantFamily.SUN_PRODUCER,
                mint.getFamilyBoostDurationSeconds(), mint.resetsFamilyCooldowns(),
                mint, entitiesToAdd,
                "Enlighten-mint applied plant food to every Sun Producer plant.");
    }

    private void applyPendingExplosiveBoardEffects(List<Entity> updateSnapshot,
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

    private void applyExplosiveFamilyBoosts(List<Entity> updateSnapshot,
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

    private void triggerContactExplosive(Explosive explosive) {
        if (!explosive.canTriggerOnContact()) {
            return;
        }
        if (findExplosiveTriggerTarget(explosive) != null) {
            explosive.trigger();
        }
    }

    private Zombie findExplosiveTriggerTarget(Explosive explosive) {
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
            if (explosive.getType().getBehavior()
                    == ExplosiveBehavior.WATER_TRAP
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

    private boolean canDodoFlyOverExplosive(
            Zombie zombie, Explosive explosive) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof FlyAbility
                    && ability.tryUse(zombie, this)
                    && ((FlyAbility) ability)
                            .canFlyOver(explosive)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWaterZombie(Zombie zombie) {
        if (zombie.isSubmerged()) {
            return true;
        }
        int column = Math.max(0, Math.min(numberOfColumns - 1,
                (int) Math.floor(zombie.getColumnPosition())));
        Tile tile = getTileAt(new EntityPosition(zombie.getLane(), column));
        return tile != null && tile.getTileType() == TileType.WATER;
    }

    private void applyExplosivePlantFoodEffects(Explosive explosive,
            List<Entity> entitiesToAdd) {
        spawnArmedMineClones(explosive, explosive.drainCloneMineCount(), entitiesToAdd);
        if (explosive.drainGlobalFreezePending()) {
            freezeAllZombies(explosive.getFreezeDurationSeconds());
            pendingResults.add(explosive.getName() + " froze every zombie with plant food.");
        }
        crushMultipleZombies(explosive, explosive.drainPlantFoodSquashTargetCount());
        drownMultipleZombies(explosive, explosive.drainPlantFoodKelpTargetCount());
    }

    private void spawnArmedMineClones(Explosive source, int count,
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

    private void crushMultipleZombies(Explosive explosive, int targetCount) {
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

    private void drownMultipleZombies(Explosive explosive, int targetCount) {
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

    private void executeExplosiveActivation(Explosive explosive,
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

    private void damageExplosiveTriggerTarget(Explosive explosive) {
        Zombie target = findExplosiveTriggerTarget(explosive);
        if (target != null) {
            target.takeDamage(explosive.getDamage());
            if (target.isDead()) {
                reportZombieDeath(target);
            }
        }
    }

    private void killWaterTargets(Explosive explosive, int targetCount) {
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

    private void freezeExplosiveTriggerTarget(Explosive explosive) {
        Zombie target = findExplosiveTriggerTarget(explosive);
        if (target != null) {
            target.applyFreeze(explosive.getFreezeDurationSeconds());
        }
    }

    private void damageArea(EntityPosition center, int rowRadius,
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

    private void damageLaneWithFire(int lane, int damage) {
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

    private void damageAllZombies(int damage) {
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

    private void freezeAllZombies(double durationSeconds) {
        for (Zombie zombie : getZombies()) {
            if (!zombie.isHypnotized() && !zombie.isSubmerged()) {
                zombie.applyFreeze(durationSeconds);
            }
        }
    }

    private void addGrapeshotProjectiles(Explosive explosive,
            List<Entity> entitiesToAdd) {
        EntityPosition center = explosive.getEntityPosition();
        int grapeDamage = Math.max(1, explosive.getDamage() / 9);
        int maximumHits = 1 + explosive.getGrapeBounceCount();
        int[][] directions = {
            {-1, -1}, {-1, 0}, {-1, 1}, {0, -1},
            {0, 1}, {1, -1}, {1, 0}, {1, 1}
        };
        for (int index = 0; index < GRAPESHOT_PROJECTILE_COUNT; index++) {
            entitiesToAdd.add(new BouncingGrape(center.getRow(), center.getColumn(),
                    directions[index][0], directions[index][1], grapeDamage, maximumHits));
        }
    }

    private void applyFinishExplosion(Explosive explosive) {
        if (explosive.explodesOnFinish()) {
            damageArea(explosive.getEntityPosition(), 1, 1.0,
                    FINISH_EXPLOSION_DAMAGE, false);
        }
    }

    private void applyPendingMeleeBoardEffects(List<Entity> updateSnapshot,
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

    private void applyMeleeFamilyBoosts(List<Entity> updateSnapshot,
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

    private void warmTilesAroundWasabiWhip(Melee melee) {
        if (melee.getType() == MeleePlantType.WASABI_WHIP
                && melee.getEntityPosition() != null) {
            meltFrozenTiles(melee.getEntityPosition(), 1);
        }
    }

    private void activateReadyMelee(List<Entity> updateSnapshot) {
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

    private void performReadyMeleeAttacks(Melee melee) {
        while (melee.isReadyToAttack()) {
            if (!performMeleeAttack(melee)) {
                melee.retainSingleReadyAttack();
                return;
            }
            melee.consumeAttack();
        }
    }

    private boolean performMeleeAttack(Melee melee) {
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

    private boolean attackFrontAndBack(Melee melee) {
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

    private Zombie findDirectionalMeleeTarget(Melee melee, boolean front) {
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

    private static boolean isCorrectMeleeDirection(double delta, boolean front) {
        return front ? delta > POSITION_EPSILON : delta < -POSITION_EPSILON;
    }

    private boolean attackMeleeArea(Melee melee, double radius,
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

    private static boolean isInsideMeleeArea(Zombie zombie,
            EntityPosition center, double radius) {
        return Math.abs(zombie.getLane() - center.getRow()) <= radius
                && Math.abs(zombie.getColumnPosition() - center.getColumn()) <= radius;
    }

    private static boolean isMeleeTargetable(Zombie zombie) {
        return zombie != null && !zombie.isDead() && !zombie.isHypnotized()
                && !zombie.isSubmerged() && !zombie.isFlying();
    }

    private void damageMeleeTarget(Zombie zombie, int damage, boolean fireDamage) {
        if (zombie == null || damage <= 0 || zombie.isDead()) {
            return;
        }
        damageZombieOrFrozenShell(
                zombie, damage, fireDamage);
        if (zombie.isDead()) {
            reportZombieDeath(zombie);
        }
    }

    private void attackWithChomper(Melee chomper) {
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

    private Zombie findNearestChomperTarget(Melee chomper, double range) {
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

    private void applyMeleePlantFood(Melee melee) {
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

    private void swallowPlantFoodTargets(Melee chomper, int targetCount) {
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

    private List<Zombie> getChomperPlantFoodTargets(Melee chomper) {
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

    private void meltLane(int lane) {
        for (int column = 0; column < numberOfColumns; column++) {
            EntityPosition position = new EntityPosition(lane, column);
            Tile tile = getTileAt(position);
            if (tile != null && tile.getTileType() == TileType.FROZEN) {
                tile.setTileType(TileType.NORMAL);
            }
        }
    }

    private void meltFrozenTiles(EntityPosition center, int radius) {
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

    private void applyPendingModifierBoardEffects(List<Entity> updateSnapshot,
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

    private void applyEnchantMint(Modifier mint, List<Entity> entitiesToAdd) {
        activateFamilyBoost(PlantFamily.MODIFIER,
                mint.getFamilyBoostDurationSeconds(), mint.resetsFamilyCooldowns(),
                mint, entitiesToAdd,
                "Enchant-mint applied plant food to every Modifier plant.");
    }

    private boolean applyPlantFoodToPlant(BasePlant plant,
            List<Entity> entitiesToAdd) {
        if (!supportsPlantFood(plant)) {
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

    private void applyShooterPlantFood(Shooter shooter,
            List<Entity> entitiesToAdd) {
        shooter.usePlantFood(numberOfRows);
        entitiesToAdd.addAll(shooter.drainProjectiles());
        resetTemporaryShooterFamily(shooter);
        freezeSnowPeaLane(shooter);
    }

    private void resetTemporaryShooterFamily(Shooter source) {
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

    private void freezeSnowPeaLane(Shooter shooter) {
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

    private static boolean supportsPlantFood(BasePlant plant) {
        if (plant == null || plant.isRemoved()
                || plant.isDisabled() || PlantFamily.isMint(plant)) {
            return false;
        }
        if (plant instanceof SunProducer) {
            SunProducerPlantType type = ((SunProducer) plant).getType();
            return type != SunProducerPlantType.GOLD_BLOOM;
        }
        if (plant instanceof Explosive) {
            ExplosivePlantType type = ((Explosive) plant).getType();
            return type == ExplosivePlantType.POTATO_MINE
                    || type == ExplosivePlantType.PRIMAL_POTATO_MINE
                    || type == ExplosivePlantType.SQUASH
                    || type == ExplosivePlantType.TANGLE_KELP
                    || type == ExplosivePlantType.ICEBERG_LETTUCE;
        }
        if (plant instanceof Modifier) {
            ModifierPlantType type = ((Modifier) plant).getType();
            return type == ModifierPlantType.TORCHWOOD
                    || type == ModifierPlantType.HYPNO_SHROOM
                    || type == ModifierPlantType.LILY_PAD;
        }
        if (plant instanceof Shooter) {
            return ((Shooter) plant).getType() != ShooterPlantType.APPEASE_MINT;
        }
        if (plant instanceof Lobber) {
            return ((Lobber) plant).getType() != LobberPlantType.ARMA_MINT;
        }
        if (plant instanceof StrikeThrough) {
            return ((StrikeThrough) plant).getType()
                    != StrikeThroughPlantType.PIERCE_MINT;
        }
        if (plant instanceof Homing) {
            return ((Homing) plant).getType() != HomingPlantType.CAT_TAIL_MINT;
        }
        if (plant instanceof Melee) {
            return ((Melee) plant).getType() != MeleePlantType.ENFORCE_MINT;
        }
        return plant instanceof Wallnut
                && ((Wallnut) plant).getType() != WallnutPlantType.REINFORCE_MINT;
    }

    private static void resetPlantActionTimer(BasePlant plant) {
        if (plant instanceof Shooter) {
            ((Shooter) plant).resetActionTimer();
        } else if (plant instanceof SunProducer) {
            ((SunProducer) plant).resetActionTimer();
        } else if (plant instanceof Explosive) {
            ((Explosive) plant).resetActionTimer();
        } else if (plant instanceof Melee) {
            ((Melee) plant).resetActionTimer();
        } else if (plant instanceof Lobber) {
            ((Lobber) plant).resetActionTimer();
        } else if (plant instanceof StrikeThrough) {
            ((StrikeThrough) plant).resetActionTimer();
        } else if (plant instanceof Homing) {
            ((Homing) plant).resetActionTimer();
        }
    }

    private void createLilyPadCopies(Modifier source, int copyCount,
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

    private static boolean containsPendingPlantAt(List<Entity> entities,
            EntityPosition position) {
        for (Entity entity : entities) {
            if (entity instanceof BasePlant
                    && position.equals(entity.getEntityPosition())) {
                return true;
            }
        }
        return false;
    }

    private void applyPendingModifierDeathEffects(
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

    private void damageAreaWithFire(EntityPosition center, int radius,
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

    private void applyPendingWallnutPassiveEffects(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (entity instanceof Wallnut && !((Wallnut) entity).isDisabled()) {
                Wallnut wallnut = (Wallnut) entity;
                releaseSunBeanSun(wallnut);
                applyWallnutExplosion(wallnut);
            }
        }
    }

    private void applyPendingWallnutBoardEffects(List<Entity> updateSnapshot,
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

    private void applyFamilyBoost(Wallnut mint, List<Entity> entitiesToAdd) {
        if (!mint.drainFamilyBoostPending()) {
            return;
        }
        activateFamilyBoost(PlantFamily.WALL_NUT,
                mint.getFamilyBoostDurationSeconds(), mint.resetsFamilyCooldowns(),
                mint, entitiesToAdd,
                "Reinforce-mint applied plant food to every Wall-nut family plant.");
    }

    private void applyGarlicPlantFood(Wallnut garlic) {
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

    private void applySweetPotatoPlantFood(Wallnut sweetPotato) {
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

    private void warmFrozenPlants(float deltaSeconds) {
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

    private boolean hasAdjacentActiveFirePlant(
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

    private void meltFrozenPlantsInLane(int lane) {
        for (BasePlant plant : getPlants()) {
            if (plant.isFrozen() && plant.getEntityPosition() != null
                    && plant.getEntityPosition().getRow() == lane) {
                damageFrozenPlantIce(plant, 1, true);
            }
        }
    }

    private void meltFrozenPlantsInArea(EntityPosition center,
            int rowRadius, double columnRadius) {
        if (center == null) {
            return;
        }
        for (BasePlant plant : getPlants()) {
            EntityPosition position = plant.getEntityPosition();
            if (!plant.isFrozen() || position == null
                    || Math.abs(position.getRow() - center.getRow()) > rowRadius
                    || Math.abs(position.getColumn() - center.getColumn())
                            > columnRadius) {
                continue;
            }
            damageFrozenPlantIce(plant, 1, true);
        }
    }

    private void updateZombies(List<Entity> updateSnapshot, float deltaSeconds) {
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

    private void warmFrozenZombie(Zombie zombie,
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

    private boolean hasAdjacentFirePlant(Zombie zombie) {
        return zombie != null
                && hasAdjacentActiveFirePlant(
                        zombie.getEntityPosition(), null);
    }

    private void applySliderTile(Zombie zombie) {
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

    private void updatePushedObjects(Zombie zombie) {
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

    private void updateArcadeMachine(
            Zombie zombie, ArcadePushAbility ability) {
        if (!ability.tryUse(zombie, this)) {
            return;
        }
        ArcadeMachine machine = ability.getMachine();
        if (ability.didSpawnThisUse()) {
            pendingResults.add(zombie.getName()
                    + " started pushing an arcade machine.");
        }
        crushWithPushedObstacle(machine);
    }

    private void updateRollingBarrel(
            Zombie zombie, BarrelPushAbility ability) {
        if (!ability.tryUse(zombie, this)) {
            return;
        }
        RollingBarrel barrel = ability.getBarrel();
        if (ability.didSpawnThisUse()) {
            pendingResults.add(zombie.getName()
                    + " started pushing a rolling barrel.");
        }
        crushWithPushedObstacle(barrel);
    }

    private void crushWithPushedObstacle(
            PushedObstacle obstacle) {
        if (obstacle == null || obstacle.isDestroyed()) {
            return;
        }
        crushPlantsWithPushedObstacle(obstacle);
        crushHypnotizedZombiesWithPushedObstacle(obstacle);
    }

    private void crushPlantsWithPushedObstacle(
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

    private void crushHypnotizedZombiesWithPushedObstacle(
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

    private void updatePianoMusic(Zombie pianoZombie) {
        for (ZombieAbility ability : pianoZombie.getAbilities()) {
            if (!(ability instanceof PianoCrushAbility)
                    || !ability.tryUse(pianoZombie, this)) {
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

    private boolean updateProspector(
            Zombie prospector, float deltaSeconds) {
        for (ZombieAbility ability : prospector.getAbilities()) {
            if (!(ability instanceof LaunchAbility)) {
                continue;
            }

            LaunchAbility launch = (LaunchAbility) ability;
            launch.tryUse(prospector, this);
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

    private void updateReverseMovingProspector(
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

    private BasePlant findNearestPlantToRight(Zombie zombie) {
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

    private void updateTroglobiteIceBlocks(Zombie zombie) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (!(ability instanceof IceBlockPushAbility)
                    || !ability.tryUse(zombie, this)) {
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

    private void crushPlantsWithIceBlock(IceBlock block) {
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

    private void crushHypnotizedZombiesWithIceBlock(IceBlock block) {
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

    private void attractZombieToSweetPotato(Zombie zombie) {
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

    private void updateHypnotizedZombie(
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

    private PushedObstacle findNearestPushedObstacleToRight(
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

    private void updateHypnotizedZombieAgainstObstacle(
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

    private Zombie findNearestHostileZombieToRight(Zombie hypnotizedZombie) {
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

    private Zombie findNearestHypnotizedZombieAhead(Zombie zombie) {
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

    private void updateZombieAgainstHypnotizedTarget(Zombie zombie,
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

    private void updateZombie(Zombie zombie, float deltaSeconds) {
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
                || blockingHypnotizedZombie.getColumnPosition()
                <= blockingPlant.getEntityPosition().getColumn())
                && tryCrushPlantWithPiano(zombie, blockingPlant)) {
            reportDestroyedPlant(blockingPlant);
            return;
        }
        if (blockingPlant != null
                && (blockingHypnotizedZombie == null
                || blockingHypnotizedZombie.getColumnPosition()
                <= blockingPlant.getEntityPosition().getColumn())
                && tryCrushPlantWithSurfboard(zombie, blockingPlant)) {
            reportDestroyedPlant(blockingPlant);
            return;
        }
        if (blockingPlant != null
                && (blockingHypnotizedZombie == null
                || blockingHypnotizedZombie.getColumnPosition()
                <= blockingPlant.getEntityPosition().getColumn())
                && tryFlyOverPlant(zombie, blockingPlant)) {
            return;
        }
        if (blockingPlant != null
                && (blockingHypnotizedZombie == null
                || blockingHypnotizedZombie.getColumnPosition()
                <= blockingPlant.getEntityPosition().getColumn())
                && tryBurnPlant(zombie, blockingPlant)) {
            reportDestroyedPlant(blockingPlant);
            return;
        }
        if (blockingHypnotizedZombie != null
                && (blockingPlant == null
                || blockingHypnotizedZombie.getColumnPosition()
                > blockingPlant.getEntityPosition().getColumn())) {
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

    private boolean tryCrushPlantWithPiano(
            Zombie zombie, BasePlant plant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (!(ability instanceof PianoCrushAbility)) {
                continue;
            }
            PianoCrushAbility piano =
                    (PianoCrushAbility) ability;
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

    private boolean tryTransformPlantWithWizard(
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
            WizardSpellAbility spell =
                    (WizardSpellAbility) ability;
            boolean transformed =
                    spell.transformReachedPlant(
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

    private boolean keepStationaryZombieAtRightEdge(
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

    private void updateBeachMovementStates(Zombie zombie,
            BasePlant blockingPlant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof SubmergeAbility) {
                ((SubmergeAbility) ability).updateState(
                        zombie, this, blockingPlant);
            } else if (ability instanceof FastSwimAbility) {
                ability.tryUse(zombie, this);
            } else if (ability instanceof SurfAbility) {
                ability.tryUse(zombie, this);
            }
        }
    }

    private boolean tryCrushPlantWithSurfboard(Zombie zombie,
            BasePlant plant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (!(ability instanceof SurfAbility)) {
                continue;
            }
            SurfAbility surf = (SurfAbility) ability;
            if (!surf.tryCrush(zombie, plant, this)) {
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

    private boolean tryFlyOverPlant(Zombie zombie, BasePlant plant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof FlyAbility
                    && ((FlyAbility) ability).tryFlyOver(zombie, plant, this)) {
                pendingResults.add(zombie.getName() + " flew over "
                        + plant.getName() + ".");
                return true;
            }
        }
        return false;
    }

    private void finishDodoFlight(Zombie zombie) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof FlyAbility
                    && ((FlyAbility) ability).isFlying()) {
                ((FlyAbility) ability).finishFlight(zombie);
            }
        }
    }

    private boolean tryBurnPlant(Zombie zombie, BasePlant plant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (!(ability instanceof TorchAbility)) {
                continue;
            }
            TorchAbility torch = (TorchAbility) ability;
            if (!torch.canBurn(zombie, plant)
                    || !torch.tryUse(zombie, this)) {
                continue;
            }
            plant.takeDamage(Integer.MAX_VALUE);
            pendingResults.add(zombie.getName() + " burned "
                    + plant.getName() + " at " + plant.getEntityPosition() + ".");
            return true;
        }
        return false;
    }

    private boolean tryTacklePlant(Zombie zombie, BasePlant plant) {
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

    private boolean tryTackleZombie(Zombie zombie, Zombie target) {
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

    private TackleAbility useTackle(Zombie zombie) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof TackleAbility
                    && ability.tryUse(zombie, this)) {
                return (TackleAbility) ability;
            }
        }
        return null;
    }

    private void attackPlant(Zombie zombie, BasePlant plant, float deltaSeconds) {
        boolean eaten = !trySmashPlant(zombie, plant);
        if (eaten) {
            zombie.eat(plant, deltaSeconds);
        }
        handleModifierAfterAttack(zombie, plant, eaten);
    }

    private void handleModifierAfterAttack(Zombie zombie, BasePlant plant,
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

    private boolean trySmashPlant(Zombie zombie, BasePlant plant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof SmashAbility && ability.tryUse(zombie, this)) {
                plant.takeDamage(Integer.MAX_VALUE);
                return true;
            }
        }
        return false;
    }

    private void handleWallnutAfterAttack(Zombie zombie, BasePlant plant, float deltaSeconds) {
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

    private void reflectEndurianDamage(Zombie zombie, Wallnut wallnut, float deltaSeconds) {
        int reflectedDamage = wallnut.calculateReflectedDamage(deltaSeconds);
        if (reflectedDamage > 0) {
            damageZombieOrFrozenShell(
                    zombie, reflectedDamage, false);
        }
    }

    private void divertZombieAfterGarlicBite(Zombie zombie, Wallnut wallnut) {
        if (!wallnut.drainDivertLanePending()) {
            return;
        }
        int targetLane = wallnut.chooseAdjacentLane(zombie.getLane(), numberOfRows);
        zombie.moveToLane(targetLane);
        pendingResults.add("Garlic diverted " + zombie.getName() + " into lane " + targetLane + ".");
    }

    private void releaseSunBeanSun(Wallnut wallnut) {
        int sunAmount = wallnut.drainPendingSunAmount();
        if (sunAmount <= 0) {
            return;
        }
        addEntity(Sun.createPlantSun(sunAmount, wallnut.getEntityPosition()));
        pendingResults.add("plant Sun Bean produced " + sunAmount + " sun at "
                + wallnut.getEntityPosition());
    }

    private void applyWallnutExplosion(Wallnut wallnut) {
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

    private static boolean isInsideThreeByThree(Zombie zombie, EntityPosition center) {
        return Math.abs(zombie.getLane() - center.getRow()) <= 1
                && Math.abs(zombie.getColumnPosition() - center.getColumn()) <= 1.0;
    }

    private BasePlant findNearestPlantAhead(Zombie zombie) {
        BasePlant nearestPlant = null;
        int nearestColumn = -1;
        for (BasePlant plant : getPlants()) {
            if (plant.isRemoved()
                    || plant.isTransformedToSheep()
                    || plant.getEntityPosition().getRow()
                            != zombie.getLane()) {
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

    private static boolean isBetterBlocker(BasePlant candidate, BasePlant current,
            int candidateColumn, int currentColumn) {
        if (candidateColumn > currentColumn) {
            return true;
        }
        if (candidateColumn != currentColumn) {
            return false;
        }
        if (isCover(candidate) && !isCover(current)) {
            return true;
        }
        return isLilyPad(current) && !isLilyPad(candidate);
    }

    private static boolean isCover(BasePlant plant) {
        return plant instanceof Wallnut && ((Wallnut) plant).isCoverPlant();
    }

    private void reportDestroyedPlant(BasePlant plant) {
        if (plant.isDestroyed()) {
            pendingResults.add("Plant " + plant.getName() + " at "
                    + plant.getEntityPosition() + " is destroyed.");
        }
    }

    private void reportZombieDeath(Zombie zombie) {
        if (zombie.isDeathReported()) {
            return;
        }
        zombie.markDeathReported();
        zombie.markForRemoval();
        pendingResults.add("Zombie of type " + zombie.getName() + " is dead at ("
                + formatColumn(zombie.getColumnPosition()) + ", " + zombie.getLane() + ")");
    }

    private static String formatColumn(double column) {
        return String.format(java.util.Locale.ROOT, "%.2f", column);
    }

    public List<String> drainResults() {
        if (pendingResults.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>(pendingResults);
        pendingResults.clear();
        return Collections.unmodifiableList(results);
    }

    private static String buildSunProductionResult(SunProducer producer) {
        return "plant " + producer.getType().getDisplayName()
                + " produced a sun at " + producer.getEntityPosition();
    }

    public void addEntity(Entity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null");
        }
        validatePosition(entity.getEntityPosition());
        allEntities.add(entity);
    }

    public void addZombie(Zombie zombie) {
        if (zombie != null && frostbiteCavesRules) {
            zombie.setChapterColdImmune(true);
        }
        addEntity(zombie);
        if (App.getInstance().getLoggedInUser() != null) {
            App.getInstance().getLoggedInUser().unlockZombie(zombie.getType().getAlias());
        }
    }

    public boolean canAddPlant(BasePlant requestedPlant) {
        BasePlant plant = resolvePlacedPlant(requestedPlant);
        if (plant == null
                || !isPositionInsideBoard(plant.getEntityPosition())
                || !canPlantOnTerrain(plant)) {
            return false;
        }
        EntityPosition position = plant.getEntityPosition();
        List<BasePlant> plantsAtPosition = getPlantsAt(position);
        Tile tile = getTileAt(position);
        if (tile != null
                && tile.getTileType() == TileType.WATER) {
            return canAddPlantInWater(plant, plantsAtPosition);
        }
        return canAddPlantOnLand(plant, plantsAtPosition);
    }

    private boolean canAddPlantInWater(BasePlant plant,
            List<BasePlant> plantsAtPosition) {
        if (isLilyPad(plant)) {
            return plantsAtPosition.isEmpty();
        }
        boolean hasLilyPad = plantsAtPosition.stream()
                .anyMatch(Board::isLilyPad);
        List<BasePlant> plantsAbovePad = new ArrayList<>();
        for (BasePlant existing : plantsAtPosition) {
            if (!isLilyPad(existing)) {
                plantsAbovePad.add(existing);
            }
        }
        if (plant.hasTag(PlantTag.WATER)) {
            return plantsAtPosition.isEmpty();
        }
        if (!hasLilyPad) {
            return false;
        }
        if (plantsAbovePad.isEmpty()) {
            return !isCover(plant);
        }
        if (isPeaPod(plant) && plantsAbovePad.size() < 5) {
            return plantsAbovePad.stream()
                    .allMatch(Board::isPeaPod);
        }
        return isCover(plant)
                && plantsAbovePad.size() == 1
                && !isCover(plantsAbovePad.get(0));
    }

    private static boolean canAddPlantOnLand(BasePlant plant,
            List<BasePlant> plantsAtPosition) {
        if (isLilyPad(plant)) {
            return false;
        }
        if (plantsAtPosition.isEmpty()) {
            return !isCover(plant);
        }
        if (isPeaPod(plant) && plantsAtPosition.size() < 5) {
            return plantsAtPosition.stream()
                    .allMatch(Board::isPeaPod);
        }
        return isCover(plant)
                && plantsAtPosition.size() == 1
                && !isCover(plantsAtPosition.get(0));
    }

    private BasePlant resolvePlacedPlant(BasePlant plant) {
        if (!(plant instanceof Modifier) || !((Modifier) plant).isImitater()) {
            return plant;
        }
        Modifier imitater = (Modifier) plant;
        return imitater.hasValidImitatedPlant()
                ? imitater.getImitatedPlant() : null;
    }

    private boolean canPlantOnTerrain(BasePlant plant) {
        Tile tile = getTileAt(plant.getEntityPosition());
        if (tile == null) {
            return false;
        }
        if (plant instanceof Explosive) {
            ExplosivePlantType type = ((Explosive) plant).getType();
            if (type == ExplosivePlantType.HOT_POTATO) {
                return tile.getTileType() == TileType.FROZEN;
            }
            if (type == ExplosivePlantType.GRAVE_BUSTER) {
                return hasGraveAt(plant.getEntityPosition());
            }
            if (type == ExplosivePlantType.TANGLE_KELP) {
                return tile.getTileType() == TileType.WATER;
            }
        }
        if (tile.getTileType() == TileType.WATER) {
            return plant.getTags().contains(PlantTag.WATER)
                    || getPlantsAt(plant.getEntityPosition()).stream()
                            .anyMatch(Board::isLilyPad);
        }
        return tile.isPlantableTerrain();
    }

    private static boolean isPeaPod(BasePlant plant) {
        return plant instanceof Shooter
                && ((Shooter) plant).getType() == ShooterPlantType.PEA_POD;
    }

    private static boolean isLilyPad(BasePlant plant) {
        return plant instanceof Modifier && ((Modifier) plant).isLilyPad();
    }

    public boolean movePlant(BasePlant plant,
            EntityPosition destination) {
        if (plant == null || plant.isRemoved()
                || plant.getEntityPosition() == null
                || !isPositionInsideBoard(destination)
                || destination.equals(plant.getEntityPosition())
                || !getPlantsAt(destination).isEmpty()
                || getStructureAt(destination) != null) {
            return false;
        }

        Tile destinationTile = getTileAt(destination);
        if (destinationTile == null
                || !canMovePlantOntoTile(plant, destinationTile)) {
            return false;
        }

        EntityPosition oldPosition = plant.getEntityPosition();
        plant.setEntityPosition(destination);
        refreshTilePlant(oldPosition);
        destinationTile.setPlant(plant);
        return true;
    }

    private boolean canMovePlantOntoTile(BasePlant plant, Tile tile) {
        if (tile.getTileType() == TileType.WATER) {
            return plant.getTags().contains(PlantTag.WATER);
        }
        return tile.isPlantableTerrain();
    }

    public boolean addPlant(BasePlant requestedPlant) {
        return addPlantInternal(requestedPlant, true);
    }

    private boolean addPlantInternal(BasePlant requestedPlant,
            boolean applyActiveFamilyBoosts) {
        if (!canAddPlant(requestedPlant)) {
            return false;
        }
        BasePlant plant = resolvePlacedPlant(requestedPlant);
        addEntity(plant);
        Tile tile = getTileAt(plant.getEntityPosition());
        if (tile != null) {
            tile.setPlant(plant);
        }
        applyImitaterEntranceEffect(requestedPlant, plant);
        if (applyActiveFamilyBoosts) {
            applyActiveFamilyBoostsToPlant(plant);
        }
        return true;
    }

    private void applyImitaterEntranceEffect(BasePlant requestedPlant,
            BasePlant placedPlant) {
        if (!(requestedPlant instanceof Modifier)) {
            return;
        }
        Modifier imitater = (Modifier) requestedPlant;
        if (!imitater.isImitater() || !imitater.appliesPlantFoodOnEntrance()) {
            return;
        }
        List<Entity> spawnedEntities = new ArrayList<>();
        applyPlantFoodToPlant(placedPlant, spawnedEntities);
        for (Entity entity : spawnedEntities) {
            addEntity(entity);
        }
    }

    public PlantFoodResult usePlantFoodAt(EntityPosition position) {
        BasePlant plant = getPlantAt(position);
        if (plant == null) {
            return PlantFoodResult.NO_PLANT;
        }
        List<Entity> spawnedEntities = new ArrayList<>();
        boolean applied = applyPlantFoodAtPosition(plant, position, spawnedEntities);
        if (!applied) {
            return PlantFoodResult.NO_EFFECT;
        }
        for (Entity entity : spawnedEntities) {
            addEntity(entity);
        }
        pendingResults.add(plant.getName() + " received plant food.");
        return PlantFoodResult.SUCCESS;
    }

    private boolean applyPlantFoodAtPosition(BasePlant plant, EntityPosition position,
            List<Entity> spawnedEntities) {
        if (!(plant instanceof Shooter)
                || ((Shooter) plant).getType() != ShooterPlantType.PEA_POD) {
            return applyPlantFoodToPlant(plant, spawnedEntities);
        }
        boolean applied = false;
        for (BasePlant stackedPlant : getPlantsAt(position)) {
            if (stackedPlant instanceof Shooter
                    && ((Shooter) stackedPlant).getType() == ShooterPlantType.PEA_POD) {
                applied |= applyPlantFoodToPlant(stackedPlant, spawnedEntities);
            }
        }
        return applied;
    }

    public boolean removeEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        EntityPosition plantPosition = entity instanceof BasePlant
                ? entity.getEntityPosition() : null;
        entity.markForRemoval();
        boolean removed = allEntities.remove(entity);
        if (plantPosition != null) {
            refreshTilePlant(plantPosition);
        }
        return removed;
    }

    public boolean containsEntity(Entity entity) {
        return entity != null && allEntities.contains(entity) && !entity.isRemoved();
    }

    public List<BasePlant> getPlants() {
        List<BasePlant> plants = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof BasePlant && !entity.isRemoved()) {
                plants.add((BasePlant) entity);
            }
        }
        return Collections.unmodifiableList(plants);
    }

    public List<Zombie> getZombies() {
        List<Zombie> zombies = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof Zombie && !entity.isRemoved()) {
                zombies.add((Zombie) entity);
            }
        }
        return Collections.unmodifiableList(zombies);
    }

    public List<IceBlock> getIceBlocks() {
        List<IceBlock> iceBlocks = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof IceBlock && !entity.isRemoved()) {
                iceBlocks.add((IceBlock) entity);
            }
        }
        return Collections.unmodifiableList(iceBlocks);
    }

    public List<PushedObstacle> getPushedObstacles() {
        List<PushedObstacle> obstacles = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof PushedObstacle
                    && !entity.isRemoved()) {
                obstacles.add((PushedObstacle) entity);
            }
        }
        return Collections.unmodifiableList(obstacles);
    }

    public List<Sun> getSuns() {
        List<Sun> suns = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof Sun && !entity.isRemoved()) {
                suns.add((Sun) entity);
            }
        }
        return Collections.unmodifiableList(suns);
    }

    public List<CollectibleDrop> getCollectibleDrops() {
        List<CollectibleDrop> drops = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof CollectibleDrop && !entity.isRemoved()) {
                drops.add((CollectibleDrop) entity);
            }
        }
        return Collections.unmodifiableList(drops);
    }

    public List<CollectibleDrop> getCollectibleDropsAt(EntityPosition position) {
        if (position == null) {
            return Collections.emptyList();
        }
        List<CollectibleDrop> drops = new ArrayList<>();
        for (CollectibleDrop drop : getCollectibleDrops()) {
            if (position.equals(drop.getEntityPosition())) {
                drops.add(drop);
            }
        }
        return Collections.unmodifiableList(drops);
    }

    public List<Projectile> getProjectiles() {
        List<Projectile> projectiles = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof Projectile && !entity.isRemoved()) {
                projectiles.add((Projectile) entity);
            }
        }
        return Collections.unmodifiableList(projectiles);
    }

    public List<BouncingGrape> getBouncingGrapes() {
        List<BouncingGrape> grapes = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof BouncingGrape && !entity.isRemoved()) {
                grapes.add((BouncingGrape) entity);
            }
        }
        return Collections.unmodifiableList(grapes);
    }

    public List<BasePlant> getPlantsAt(EntityPosition position) {
        if (position == null) {
            return Collections.emptyList();
        }
        List<BasePlant> plants = new ArrayList<>();
        for (BasePlant plant : getPlants()) {
            if (position.equals(plant.getEntityPosition())) {
                plants.add(plant);
            }
        }
        return Collections.unmodifiableList(plants);
    }

    public BasePlant getPlantAt(EntityPosition position) {
        BasePlant fallback = null;
        for (BasePlant plant : getPlantsAt(position)) {
            if (isCover(plant)) {
                return plant;
            }
            fallback = plant;
        }
        return fallback;
    }

    public BasePlant removePlantAt(EntityPosition position) {
        BasePlant plant = getPlantAt(position);
        if (plant != null) {
            removeEntity(plant);
        }
        return plant;
    }

    public boolean isPositionInsideBoard(EntityPosition position) {
        return position != null
                && position.getRow() >= 0
                && position.getColumn() >= 0
                && position.getRow() < numberOfRows
                && position.getColumn() < numberOfColumns;
    }

    public List<Sun> getSunsAt(EntityPosition position) {
        if (position == null) {
            return Collections.emptyList();
        }
        List<Sun> suns = new ArrayList<>();
        for (Sun sun : getSuns()) {
            if (position.equals(sun.getEntityPosition())) {
                suns.add(sun);
            }
        }
        return Collections.unmodifiableList(suns);
    }

    private void validatePosition(EntityPosition position) {
        if (position == null) {
            return;
        }
        if (position.getRow() >= numberOfRows || position.getColumn() >= numberOfColumns) {
            throw new IllegalArgumentException("Entity position is outside the board: " + position);
        }
    }

    private static void validateDeltaSeconds(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }

    public int getNumberOfColumns() {
        return numberOfColumns;
    }

    public List<Tile> getTiles() {
        return Collections.unmodifiableList(tiles);
    }

    public Tile getTileAt(EntityPosition position) {
        if (!isPositionInsideBoard(position)) {
            return null;
        }
        return tiles.get(position.getRow() * numberOfColumns + position.getColumn());
    }

    public void configureBigWaveBeach(int initialWaterColumns,
            int maximumWaterColumns,
            List<EntityPosition> lowBeachPositions) {
        validateBeachWaterColumns(initialWaterColumns,
                maximumWaterColumns);
        if (lowBeachPositions == null) {
            throw new IllegalArgumentException(
                    "lowBeachPositions cannot be null");
        }
        bigWaveBeachRules = true;
        maximumWaterColumnCount = maximumWaterColumns;
        waterColumnCount = 0;
        lowBeachTiles.clear();
        for (EntityPosition position : lowBeachPositions) {
            if (!isPositionInsideBoard(position)) {
                throw new IllegalArgumentException(
                        "low-beach position is outside the board");
            }
            lowBeachTiles.add(position);
        }
        setBeachWaterColumns(initialWaterColumns, false);
    }

    private void validateBeachWaterColumns(int initialWaterColumns,
            int maximumWaterColumns) {
        if (initialWaterColumns < 0
                || maximumWaterColumns < initialWaterColumns
                || maximumWaterColumns > numberOfColumns) {
            throw new IllegalArgumentException(
                    "beach water columns are invalid");
        }
    }

    public List<EntityPosition> raiseBigWaveBeachTide() {
        if (!bigWaveBeachRules
                || waterColumnCount >= maximumWaterColumnCount) {
            return Collections.emptyList();
        }
        return setBeachWaterColumns(waterColumnCount + 1, true);
    }

    private List<EntityPosition> setBeachWaterColumns(
            int requestedWaterColumns, boolean drownPlants) {
        int oldWaterColumns = waterColumnCount;
        int newWaterColumns = Math.min(
                requestedWaterColumns, maximumWaterColumnCount);
        int oldWaterStart = numberOfColumns - oldWaterColumns;
        int newWaterStart = numberOfColumns - newWaterColumns;
        List<EntityPosition> newlyFloodedLowBeach = new ArrayList<>();
        for (int row = 0; row < numberOfRows; row++) {
            for (int column = 0; column < numberOfColumns; column++) {
                EntityPosition position =
                        new EntityPosition(row, column);
                boolean isWater = column >= newWaterStart;
                boolean wasWater = column >= oldWaterStart;
                updateBeachTile(position, isWater);
                if (isWater && !wasWater) {
                    recordNewlyFloodedTile(position, drownPlants,
                            newlyFloodedLowBeach);
                }
            }
        }
        waterColumnCount = newWaterColumns;
        if (drownPlants) {
            cleanupRemovedEntities();
        }
        return Collections.unmodifiableList(newlyFloodedLowBeach);
    }

    private void updateBeachTile(EntityPosition position,
            boolean water) {
        Tile tile = getTileAt(position);
        if (water) {
            tile.setTileType(TileType.WATER);
        } else if (lowBeachTiles.contains(position)) {
            tile.setTileType(TileType.LOW_BEACH);
        } else {
            tile.setTileType(TileType.NORMAL);
        }
    }

    private void recordNewlyFloodedTile(EntityPosition position,
            boolean drownPlants,
            List<EntityPosition> newlyFloodedLowBeach) {
        if (lowBeachTiles.contains(position)) {
            newlyFloodedLowBeach.add(position);
        }
        if (drownPlants) {
            drownUnsupportedPlants(position,
                    " when the tide rose.");
        }
    }

    private void drownUnsupportedPlants(EntityPosition position,
            String reason) {
        List<BasePlant> plantsAtPosition =
                new ArrayList<>(getPlantsAt(position));
        boolean hasLilyPad = plantsAtPosition.stream()
                .anyMatch(Board::isLilyPad);
        for (BasePlant plant : plantsAtPosition) {
            if (isLilyPad(plant)
                    || plant.hasTag(PlantTag.WATER)
                    || hasLilyPad) {
                continue;
            }
            plant.takeDamage(Integer.MAX_VALUE);
            pendingResults.add("Plant " + plant.getName()
                    + " at " + position + " drowned" + reason);
        }
    }

    public boolean isBigWaveBeachRulesEnabled() {
        return bigWaveBeachRules;
    }

    public int getWaterColumnCount() {
        return waterColumnCount;
    }

    public int getMaximumWaterColumnCount() {
        return maximumWaterColumnCount;
    }

    public int getWaterBoundaryColumn() {
        return numberOfColumns - maximumWaterColumnCount;
    }

    public boolean isLowBeachTile(EntityPosition position) {
        return position != null && lowBeachTiles.contains(position);
    }

    public boolean isSubmergedLowBeachTile(EntityPosition position) {
        Tile tile = getTileAt(position);
        return isLowBeachTile(position)
                && tile != null
                && tile.getTileType() == TileType.WATER;
    }

    public void enableFrostbiteCavesRules() {
        frostbiteCavesRules = true;
        for (Zombie zombie : getZombies()) {
            zombie.setChapterColdImmune(true);
        }
    }

    public boolean isFrostbiteCavesRulesEnabled() {
        return frostbiteCavesRules;
    }

    public int applyIcyWind(List<Integer> lanes) {
        if (!frostbiteCavesRules || lanes == null || lanes.isEmpty()) {
            return 0;
        }
        Set<Integer> affectedLanes = new LinkedHashSet<>();
        for (Integer lane : lanes) {
            if (lane != null && lane >= 0 && lane < numberOfRows) {
                affectedLanes.add(lane);
            }
        }
        int affectedPlants = 0;
        for (BasePlant plant : getPlants()) {
            EntityPosition position = plant.getEntityPosition();
            if (position == null || plant.isDestroyed()
                    || plant.isFrozen()
                    || plant.hasTag(PlantTag.FIRE)
                    || !affectedLanes.contains(position.getRow())) {
                continue;
            }
            boolean frozenNow = plant.increaseFreezeLevel();
            affectedPlants++;
            pendingResults.add("Icy wind raised " + plant.getName()
                    + " at " + position + " to freeze level "
                    + plant.getFreezeLevel() + "/"
                    + BasePlant.MAX_FREEZE_LEVEL + "."
                    + (frozenNow
                            ? " The plant is frozen inside a "
                                    + BasePlant.ICE_SHELL_HIT_POINTS
                                    + " HP ice shell."
                            : ""));
        }
        return affectedPlants;
    }

    public void setSliderTile(EntityPosition position, int laneDelta) {
        if (!isPositionInsideBoard(position)) {
            throw new IllegalArgumentException(
                    "slider position is outside the board");
        }
        if (laneDelta != -1 && laneDelta != 1) {
            throw new IllegalArgumentException(
                    "slider lane delta must be -1 or 1");
        }
        setTileType(position, laneDelta < 0
                ? TileType.SLIDER_UP : TileType.SLIDER_DOWN);
    }

    public boolean addGrave(EntityPosition position) {
        return addStructure(new Grave(position));
    }

    private boolean hasZombieAt(EntityPosition position) {
        for (Zombie zombie : getZombies()) {
            if (zombie.getLane() == position.getRow()
                    && (int) Math.floor(zombie.getColumnPosition())
                            == position.getColumn()) {
                return true;
            }
        }
        return false;
    }

    public Zombie addFrozenZombie(ZombieType type,
            EntityPosition position) {
        if (type == null || !isPositionInsideBoard(position)) {
            throw new IllegalArgumentException(
                    "frozen zombie type and position are required");
        }
        if (hasZombieAt(position)
                || getStructureAt(position) != null) {
            throw new IllegalArgumentException(
                    "frozen zombie position is occupied");
        }
        Zombie zombie = new Zombie(type, 0,
                position.getRow(), position.getColumn(), false);
        zombie.encaseInIce();
        addZombie(zombie);
        setTileType(position, TileType.FROZEN);
        pendingResults.add("Frozen " + zombie.getName()
                + " is encased at " + position + " with "
                + zombie.getFrozenShellHitPoints() + " ice HP.");
        return zombie;
    }

    public void setTileType(EntityPosition position, TileType tileType) {
        Tile tile = getTileAt(position);
        if (tile == null) {
            throw new IllegalArgumentException("tile position is outside the board");
        }
        tile.setTileType(tileType);
    }

    public boolean addStructure(BaseStructure structure) {
        if (structure == null || !isPositionInsideBoard(structure.getPosition())) {
            return false;
        }
        if (getStructureAt(structure.getPosition()) != null) {
            return false;
        }
        structures.add(structure);
        if (structure instanceof Grave) {
            setTileType(structure.getPosition(), TileType.GRAVESTONE);
        }
        return true;
    }

    public BaseStructure getStructureAt(EntityPosition position) {
        for (BaseStructure structure : structures) {
            if (!structure.isRemoved() && position != null
                    && position.equals(structure.getPosition())) {
                return structure;
            }
        }
        return null;
    }

    public boolean hasGraveAt(EntityPosition position) {
        return getStructureAt(position) instanceof Grave;
    }

    public BaseStructure removeStructureAt(EntityPosition position) {
        BaseStructure structure = getStructureAt(position);
        if (structure == null) {
            return null;
        }
        structure.markForRemoval();
        structures.remove(structure);
        if (structure instanceof Grave) {
            setTileType(position, TileType.NORMAL);
        }
        return structure;
    }

    private void removeGraveAt(EntityPosition position) {
        BaseStructure removed = removeStructureAt(position);
        if (removed instanceof Grave) {
            pendingResults.add("Grave at " + position + " was destroyed.");
        }
    }

    public List<Zombie> drainSpawnedZombies() {
        if (pendingSpawnedZombies.isEmpty()) {
            return Collections.emptyList();
        }
        List<Zombie> result =
                new ArrayList<>(pendingSpawnedZombies);
        pendingSpawnedZombies.clear();
        return Collections.unmodifiableList(result);
    }

    public List<PlantFamily> drainPlantCooldownResetRequests() {
        if (pendingPlantCooldownResets.isEmpty()) {
            return Collections.emptyList();
        }
        List<PlantFamily> result = new ArrayList<>(pendingPlantCooldownResets);
        pendingPlantCooldownResets.clear();
        return Collections.unmodifiableList(result);
    }

    public List<Entity> getAllEntities() {
        return Collections.unmodifiableList(new ArrayList<>(allEntities));
    }

    public List<BaseStructure> getStructures() {
        List<BaseStructure> activeStructures = new ArrayList<>();
        for (BaseStructure structure : structures) {
            if (!structure.isRemoved()) {
                activeStructures.add(structure);
            }
        }
        return Collections.unmodifiableList(activeStructures);
    }
}
