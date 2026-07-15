package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import model.Constants;
import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.other.Sun;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantTag;
import model.game.entities.plants.explosive.Explosive;
import model.game.entities.plants.explosive.ExplosiveBehavior;
import model.game.entities.plants.explosive.ExplosivePlantType;
import model.game.entities.plants.homing.Homing;
import model.game.entities.plants.homing.HomingBehavior;
import model.game.entities.plants.lobber.Lobber;
import model.game.entities.plants.lobber.LobberPlantType;
import model.game.entities.plants.melee.Melee;
import model.game.entities.plants.melee.MeleeBehavior;
import model.game.entities.plants.melee.MeleePlantType;
import model.game.entities.plants.shooter.Shooter;
import model.game.entities.plants.shooter.ShooterPlantType;
import model.game.entities.plants.strikeThrough.StrikeThrough;
import model.game.entities.plants.sunProducer.SunProducer;
import model.game.entities.plants.wallnut.Wallnut;
import model.game.entities.projectile.BouncingGrape;
import model.game.entities.projectile.HomingProjectile;
import model.game.entities.projectile.LobbedProjectile;
import model.game.entities.projectile.PiercingProjectile;
import model.game.entities.projectile.Projectile;
import model.game.entities.projectile.effect.ProjectileEffect;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.ZombieType;
import model.game.entities.zombies.abilities.SmashAbility;
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

    private final int numberOfRows;
    private final int numberOfColumns;
    private final List<Tile> tiles;
    private final List<Entity> allEntities;
    private final List<BaseStructure> structures;
    private final List<String> pendingResults;

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

        List<Entity> entitiesToAdd = new ArrayList<>();
        List<Entity> updateSnapshot = new ArrayList<>(allEntities);
        updateEntities(updateSnapshot, entitiesToAdd, deltaSeconds);
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
        applyPendingMeleeBoardEffects(updateSnapshot);
        activateReadyMelee(updateSnapshot);
        applyPendingWallnutBoardEffects(updateSnapshot);
        applyPendingWallnutPassiveEffects(updateSnapshot);
        updateZombies(updateSnapshot, deltaSeconds);

        cleanupRemovedEntities();
        for (Entity entity : entitiesToAdd) {
            addEntity(entity);
        }
    }

    private void cleanupRemovedEntities() {
        for (Entity entity : allEntities) {
            if (entity.isRemoved() && entity instanceof BasePlant) {
                Tile tile = getTileAt(entity.getEntityPosition());
                if (tile != null && tile.getPlant() == entity) {
                    tile.clearPlant();
                }
            }
        }
        allEntities.removeIf(Entity::isRemoved);
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

    private void activateReadyShooters(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Shooter) || entity.isRemoved()) {
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
            if (!(entity instanceof Lobber) || entity.isRemoved()) {
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
            if (!(entity instanceof StrikeThrough) || entity.isRemoved()) {
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
            if (!(entity instanceof Homing) || entity.isRemoved()) {
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
        applyStrikeThroughFamilyBoosts(updateSnapshot);
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof StrikeThrough) || entity.isRemoved()) {
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

    private void applyStrikeThroughFamilyBoosts(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof StrikeThrough) || entity.isRemoved()) {
                continue;
            }
            StrikeThrough mint = (StrikeThrough) entity;
            if (mint.drainFamilyBoostPending()) {
                boostStrikeThroughFamily(mint);
            }
        }
    }

    private void boostStrikeThroughFamily(StrikeThrough mint) {
        for (BasePlant plant : getPlants()) {
            if (!(plant instanceof StrikeThrough) || plant == mint) {
                continue;
            }
            StrikeThrough strikeThrough = (StrikeThrough) plant;
            strikeThrough.usePlantFood();
            if (mint.resetsFamilyCooldowns()) {
                strikeThrough.resetActionTimer();
            }
        }
        mint.markForRemoval();
        pendingResults.add(
                "Pierce-mint applied plant food to every Strike-through plant.");
    }

    private void applyPendingLobberBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        applyLobberFamilyBoosts(updateSnapshot);
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Lobber) || entity.isRemoved()) {
                continue;
            }
            Lobber lobber = (Lobber) entity;
            warmTilesAroundPepperPult(lobber);
            if (lobber.drainPlantFoodPending()) {
                addLobberPlantFoodProjectiles(lobber, entitiesToAdd);
            }
        }
    }

    private void applyLobberFamilyBoosts(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Lobber) || entity.isRemoved()) {
                continue;
            }
            Lobber mint = (Lobber) entity;
            if (!mint.drainFamilyBoostPending()) {
                continue;
            }
            boostLobberFamily(mint);
        }
    }

    private void boostLobberFamily(Lobber mint) {
        for (BasePlant plant : getPlants()) {
            if (!(plant instanceof Lobber) || plant == mint) {
                continue;
            }
            Lobber lobber = (Lobber) plant;
            lobber.usePlantFood();
            if (mint.resetsFamilyCooldowns()) {
                lobber.resetActionTimer();
            }
        }
        mint.markForRemoval();
        pendingResults.add("Arma-mint applied plant food to every Lobber plant.");
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
            if (!(entity instanceof Shooter) || entity.isRemoved()) {
                continue;
            }
            Shooter mint = (Shooter) entity;
            if (mint.drainFamilyBoostPending()) {
                applyShooterFamilyBoost(mint, entitiesToAdd);
            }
        }
    }

    private void applyShooterFamilyBoost(Shooter mint, List<Entity> entitiesToAdd) {
        for (BasePlant plant : getPlants()) {
            if (!(plant instanceof Shooter) || plant == mint) {
                continue;
            }
            Shooter shooter = (Shooter) plant;
            shooter.usePlantFood(numberOfRows);
            entitiesToAdd.addAll(shooter.drainProjectiles());
        }
        mint.markForRemoval();
        pendingResults.add("Appease-mint applied plant food to every Shooter plant.");
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
        Zombie target = findFirstZombieHit(projectile);
        while (target != null && !projectile.isRemoved()) {
            projectile.hit(target);
            if (target.isDead()) {
                reportZombieDeath(target);
            }
            if (!(projectile instanceof PiercingProjectile)) {
                return;
            }
            target = findFirstZombieHit(projectile);
        }
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
            if (!projectile.isTargetAvailable()) {
                projectile.markForRemoval();
                continue;
            }
            if (projectile.hasReachedTarget()) {
                Zombie target = projectile.getLockedTarget();
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

    private static boolean isProtectedFromLobbers(Zombie zombie) {
        return zombie != null && zombie.getType() == ZombieType.LOST_CITY_JANE;
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
            Zombie target = findFirstZombieHit(grape);
            if (target != null) {
                grape.hit(target);
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
        applyHomingFamilyBoosts(updateSnapshot);
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Homing) || entity.isRemoved()) {
                continue;
            }
            Homing plant = (Homing) entity;
            if (plant.drainPlantFoodPending()) {
                applyHomingPlantFood(plant, entitiesToAdd);
            }
        }
    }

    private void applyHomingFamilyBoosts(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Homing) || entity.isRemoved()) {
                continue;
            }
            Homing mint = (Homing) entity;
            if (mint.drainFamilyBoostPending()) {
                boostHomingFamily(mint);
            }
        }
    }

    private void boostHomingFamily(Homing mint) {
        for (BasePlant plant : getPlants()) {
            if (!(plant instanceof Homing) || plant == mint) {
                continue;
            }
            Homing homing = (Homing) plant;
            homing.usePlantFood();
            if (mint.resetsFamilyCooldowns()) {
                homing.resetActionTimer();
            }
        }
        mint.markForRemoval();
        pendingResults.add("catTail-mint applied plant food to every Homing plant.");
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
            if (!(entity instanceof SunProducer) || entity.isRemoved()) {
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
        for (BasePlant plant : getPlants()) {
            if (!(plant instanceof SunProducer) || plant == mint) {
                continue;
            }
            SunProducer producer = (SunProducer) plant;
            producer.usePlantFood();
            collectProducedSuns(producer, entitiesToAdd);
        }
        mint.markForRemoval();
        pendingResults.add("Enlighten-mint applied plant food to every Sun Producer plant.");
    }

    private void applyPendingExplosiveBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        applyExplosiveFamilyBoosts(updateSnapshot);
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Explosive) || entity.isRemoved()) {
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

    private void applyExplosiveFamilyBoosts(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Explosive) || entity.isRemoved()) {
                continue;
            }
            Explosive mint = (Explosive) entity;
            if (!mint.drainFamilyBoostPending()) {
                continue;
            }
            for (BasePlant plant : getPlants()) {
                if (plant instanceof Explosive && plant != mint) {
                    Explosive explosive = (Explosive) plant;
                    explosive.usePlantFood();
                    if (mint.resetsFamilyCooldowns()) {
                        explosive.resetActionTimer();
                    }
                }
            }
            mint.markForRemoval();
            pendingResults.add("Bombard-mint applied plant food to every Explosive plant.");
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
                    || zombie.getLane() != lane) {
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
            if (zombie.isHypnotized()) {
                continue;
            }
            zombie.takeDamage(explosive.getDamage());
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
        for (Zombie zombie : getZombies()) {
            if (zombie.isHypnotized()
                    || Math.abs(zombie.getLane() - center.getRow()) > rowRadius
                    || Math.abs(zombie.getColumnPosition() - center.getColumn()) > columnRadius) {
                continue;
            }
            if (fireDamage) {
                zombie.applyFireDamage(damage);
            } else {
                zombie.takeDamage(damage);
            }
            if (zombie.isDead()) {
                reportZombieDeath(zombie);
            }
        }
    }

    private void damageLaneWithFire(int lane, int damage) {
        for (Zombie zombie : getZombies()) {
            if (!zombie.isHypnotized() && zombie.getLane() == lane) {
                zombie.applyFireDamage(damage);
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
            if (zombie.isHypnotized()) {
                continue;
            }
            zombie.takeDamage(damage);
            if (zombie.isDead()) {
                reportZombieDeath(zombie);
            }
        }
    }

    private void freezeAllZombies(double durationSeconds) {
        for (Zombie zombie : getZombies()) {
            if (!zombie.isHypnotized()) {
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

    private void applyPendingMeleeBoardEffects(List<Entity> updateSnapshot) {
        applyMeleeFamilyBoosts(updateSnapshot);
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Melee) || entity.isRemoved()) {
                continue;
            }
            Melee melee = (Melee) entity;
            warmTilesAroundWasabiWhip(melee);
            if (melee.drainPlantFoodPending()) {
                applyMeleePlantFood(melee);
            }
        }
    }

    private void applyMeleeFamilyBoosts(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Melee) || entity.isRemoved()) {
                continue;
            }
            Melee mint = (Melee) entity;
            if (!mint.drainFamilyBoostPending()) {
                continue;
            }
            boostMeleeFamily(mint);
        }
    }

    private void boostMeleeFamily(Melee mint) {
        for (BasePlant plant : getPlants()) {
            if (!(plant instanceof Melee) || plant == mint) {
                continue;
            }
            Melee melee = (Melee) plant;
            melee.usePlantFood();
            if (mint.resetsFamilyCooldowns()) {
                melee.resetActionTimer();
            }
        }
        mint.markForRemoval();
        pendingResults.add("Enforce-mint applied plant food to every Melee plant.");
    }

    private void warmTilesAroundWasabiWhip(Melee melee) {
        if (melee.getType() == MeleePlantType.WASABI_WHIP
                && melee.getEntityPosition() != null) {
            meltFrozenTiles(melee.getEntityPosition(), 1);
        }
    }

    private void activateReadyMelee(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Melee) || entity.isRemoved()) {
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
        if (fireDamage) {
            zombie.applyFireDamage(damage);
        } else {
            zombie.takeDamage(damage);
        }
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
    }

    private void applyPendingWallnutPassiveEffects(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (entity instanceof Wallnut) {
                Wallnut wallnut = (Wallnut) entity;
                releaseSunBeanSun(wallnut);
                applyWallnutExplosion(wallnut);
            }
        }
    }

    private void applyPendingWallnutBoardEffects(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Wallnut) || entity.isRemoved()) {
                continue;
            }
            Wallnut wallnut = (Wallnut) entity;
            applyFamilyBoost(wallnut);
            applyGarlicPlantFood(wallnut);
            applySweetPotatoPlantFood(wallnut);
        }
    }

    private void applyFamilyBoost(Wallnut mint) {
        if (!mint.drainFamilyBoostPending()) {
            return;
        }
        for (BasePlant plant : getPlants()) {
            if (plant instanceof Wallnut && plant != mint) {
                ((Wallnut) plant).usePlantFood();
            }
        }
        mint.markForRemoval();
        pendingResults.add("Reinforce-mint applied plant food to every Wall-nut family plant.");
    }

    private void applyGarlicPlantFood(Wallnut garlic) {
        if (!garlic.drainDivertAllPending() || garlic.getEntityPosition() == null) {
            return;
        }
        int sourceLane = garlic.getEntityPosition().getRow();
        for (Zombie zombie : getZombies()) {
            if (!zombie.isHypnotized() && zombie.getLane() == sourceLane) {
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
                    && Math.abs(zombie.getLane() - targetLane) == 1) {
                zombie.moveToLane(targetLane);
            }
        }
        pendingResults.add("Sweet Potato pulled adjacent-lane zombies into lane " + targetLane + ".");
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
            if (zombie.isHypnotized()) {
                updateHypnotizedZombie(zombie, deltaSeconds);
            } else {
                attractZombieToSweetPotato(zombie);
                updateZombie(zombie, deltaSeconds);
            }
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

    private void updateHypnotizedZombie(Zombie zombie, float deltaSeconds) {
        Zombie target = findNearestHostileZombieToRight(zombie);
        if (target == null) {
            zombie.moveRight(deltaSeconds, numberOfColumns + PROJECTILE_BOARD_MARGIN);
            if (zombie.getColumnPosition() >= numberOfColumns - POSITION_EPSILON) {
                zombie.markForRemoval();
            }
            return;
        }
        double attackColumn = target.getColumnPosition() - Zombie.ATTACK_REACH;
        if (zombie.getColumnPosition() + POSITION_EPSILON >= attackColumn) {
            zombie.attackZombie(target, deltaSeconds);
            if (target.isDead()) {
                reportZombieDeath(target);
            }
        } else {
            zombie.moveRight(deltaSeconds, attackColumn);
        }
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
            zombie.attackZombie(target, deltaSeconds);
            if (target.isDead()) {
                reportZombieDeath(target);
            }
        } else {
            zombie.move(deltaSeconds, attackColumn);
        }
    }

    private void updateZombie(Zombie zombie, float deltaSeconds) {
        BasePlant blockingPlant = findNearestPlantAhead(zombie);
        Zombie blockingHypnotizedZombie = findNearestHypnotizedZombieAhead(zombie);
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
            attackPlant(zombie, blockingPlant, deltaSeconds);
            handleWallnutAfterAttack(zombie, blockingPlant, deltaSeconds);
            reportDestroyedPlant(blockingPlant);
        } else {
            zombie.move(deltaSeconds, attackColumn);
        }
    }

    private void attackPlant(Zombie zombie, BasePlant plant, float deltaSeconds) {
        if (!trySmashPlant(zombie, plant)) {
            zombie.eat(plant, deltaSeconds);
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
            zombie.takeDamage(reflectedDamage);
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
            if (!target.isHypnotized() && isInsideThreeByThree(target, center)) {
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
            if (plant.isRemoved() || plant.getEntityPosition().getRow() != zombie.getLane()) {
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
        return candidateColumn == currentColumn && isCover(candidate) && !isCover(current);
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
        addEntity(zombie);
    }

    public boolean canAddPlant(BasePlant plant) {
        if (plant == null || !isPositionInsideBoard(plant.getEntityPosition())
                || !canPlantOnTerrain(plant)) {
            return false;
        }
        List<BasePlant> plantsAtPosition = getPlantsAt(plant.getEntityPosition());
        if (plantsAtPosition.isEmpty()) {
            return !isCover(plant);
        }
        if (isPeaPod(plant) && plantsAtPosition.size() < 5) {
            return plantsAtPosition.stream().allMatch(Board::isPeaPod);
        }
        return isCover(plant) && plantsAtPosition.size() == 1
                && !isCover(plantsAtPosition.get(0));
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
            return plant.getTags().contains(PlantTag.WATER);
        }
        return tile.isPlantableTerrain();
    }

    private static boolean isPeaPod(BasePlant plant) {
        return plant instanceof Shooter
                && ((Shooter) plant).getType() == ShooterPlantType.PEA_POD;
    }

    public boolean addPlant(BasePlant plant) {
        if (!canAddPlant(plant)) {
            return false;
        }
        addEntity(plant);
        Tile tile = getTileAt(plant.getEntityPosition());
        if (tile != null) {
            tile.setPlant(plant);
        }
        return true;
    }

    public boolean removeEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        entity.markForRemoval();
        if (entity instanceof BasePlant) {
            Tile tile = getTileAt(entity.getEntityPosition());
            if (tile != null && tile.getPlant() == entity) {
                tile.clearPlant();
            }
        }
        return allEntities.remove(entity);
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

    public List<Sun> getSuns() {
        List<Sun> suns = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof Sun && !entity.isRemoved()) {
                suns.add((Sun) entity);
            }
        }
        return Collections.unmodifiableList(suns);
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
