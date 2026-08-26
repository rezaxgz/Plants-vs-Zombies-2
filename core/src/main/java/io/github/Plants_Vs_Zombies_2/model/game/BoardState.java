package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.Constants;
import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.PushedObstacle;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFamily;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.explosive.Explosive;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.homing.Homing;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.lobber.Lobber;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.melee.Melee;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.modifier.Modifier;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.shooter.Shooter;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.shooter.ShooterPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.strikeThrough.StrikeThrough;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.sunProducer.SunProducer;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.wallnut.Wallnut;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.BouncingGrape;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.LobbedProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.PiercingProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.Projectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.structure.BaseStructure;
import io.github.Plants_Vs_Zombies_2.model.game.tile.Tile;
import io.github.Plants_Vs_Zombies_2.model.game.tile.TileType;

abstract class BoardState implements java.io.Serializable {
    static final double POSITION_EPSILON = 0.000001;
    static final double SWEET_POTATO_ATTRACTION_RANGE = 1.0;
    static final double PROJECTILE_COLLISION_RADIUS = 0.35;
    static final double PROJECTILE_BOARD_MARGIN = 0.5;
    static final double GRAPE_COLLISION_RADIUS = 0.35;
    static final int GRAPESHOT_PROJECTILE_COUNT = 8;
    static final int FINISH_EXPLOSION_DAMAGE = 1800;
    static final double FROZEN_SHELL_WARMING_DAMAGE_PER_SECOND = 60.0;

    static final class ActiveFamilyBoost implements java.io.Serializable {
        final PlantFamily family;
        final boolean resetCooldowns;
        final Set<BasePlant> affectedPlants;
        double remainingSeconds;

        ActiveFamilyBoost(PlantFamily family, double remainingSeconds,
                boolean resetCooldowns) {
            this.family = family;
            this.remainingSeconds = remainingSeconds;
            this.resetCooldowns = resetCooldowns;
            this.affectedPlants = Collections.newSetFromMap(new IdentityHashMap<>());
        }

        boolean isActive() {
            return remainingSeconds > POSITION_EPSILON;
        }
    }

    final int numberOfRows;
    final int numberOfColumns;
    final List<Tile> tiles;
    final List<Entity> allEntities;
    final List<BaseStructure> structures;
    final List<String> pendingResults;
    final List<Zombie> pendingSpawnedZombies;
    final List<ActiveFamilyBoost> activeFamilyBoosts;
    final List<PlantFamily> pendingPlantCooldownResets;
    final Set<EntityPosition> lowBeachTiles;
    final Map<EntityPosition, Double> burningTileSeconds;
    final Map<EntityPosition, TileType> burningTilePreviousTypes;
    boolean frostbiteCavesRules;
    boolean bigWaveBeachRules;
    int waterColumnCount;
    int maximumWaterColumnCount;

    protected BoardState() {
        this(Constants.DEFAULT_BOARD_ROWS, Constants.DEFAULT_BOARD_COLUMNS);
    }

    protected BoardState(int numberOfRows, int numberOfColumns) {
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
        this.burningTileSeconds = new LinkedHashMap<>();
        this.burningTilePreviousTypes = new LinkedHashMap<>();
        initializeTiles();
    }

    void initializeTiles() {
        for (int row = 0; row < numberOfRows; row++) {
            for (int column = 0; column < numberOfColumns; column++) {
                tiles.add(new Tile(new EntityPosition(row, column), TileType.NORMAL));
            }
        }
    }

    final Board asBoard() {
        return (Board) this;
    }

    static String formatDuration(double seconds) {
        return String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }

    static boolean isHomingTargetable(Zombie zombie) {
        return zombie != null && !zombie.isDead() && !zombie.isRemoved()
                && !zombie.isHypnotized() && !zombie.isSubmerged();
    }

    static boolean isZomboss(Zombie zombie) {
        return zombie != null && zombie.getType().isBoss();
    }

    static boolean zombieOccupiesLane(Zombie zombie, int lane) {
        if (zombie == null) {
            return false;
        }
        if (!zombie.getType().isBoss()) {
            return zombie.getLane() == lane;
        }
        return lane == zombie.getLane()
                || lane == zombie.getLane() - 1;
    }

    static double zombieIntersectionParameter(Projectile projectile,
            Zombie zombie, double radius) {
        if (projectile == null || zombie == null) {
            return Double.NaN;
        }
        double parameter = projectile.getIntersectionParameter(
                zombie.getLane(), zombie.getColumnPosition(), radius);
        if (!zombie.getType().isBoss() || zombie.getLane() <= 0) {
            return parameter;
        }
        double second = projectile.getIntersectionParameter(
                zombie.getLane() - 1, zombie.getColumnPosition(), radius);
        if (Double.isNaN(parameter)) {
            return second;
        }
        if (Double.isNaN(second)) {
            return parameter;
        }
        return Math.min(parameter, second);
    }

    void updateBurningTiles(float deltaSeconds) {
        if (deltaSeconds <= 0.0f || burningTileSeconds.isEmpty()) {
            return;
        }
        List<EntityPosition> expired = new ArrayList<>();
        for (Map.Entry<EntityPosition, Double> entry
                : burningTileSeconds.entrySet()) {
            double remaining = Math.max(0.0,
                    entry.getValue() - deltaSeconds);
            entry.setValue(remaining);
            if (remaining <= POSITION_EPSILON) {
                expired.add(entry.getKey());
            }
        }
        for (EntityPosition position : expired) {
            burningTileSeconds.remove(position);
            TileType previous = burningTilePreviousTypes.remove(position);
            Tile tile = getTileAt(position);
            if (tile != null && tile.getTileType() == TileType.BURNING) {
                tile.setTileType(previous == null ? TileType.NORMAL : previous);
            }
            pendingResults.add("The fire at " + position + " went out.");
        }
    }

    static double zombieIntersectionParameter(BouncingGrape grape,
            Zombie zombie, double radius) {
        if (grape == null || zombie == null) {
            return Double.NaN;
        }
        double parameter = grape.getIntersectionParameter(
                zombie.getLane(), zombie.getColumnPosition(), radius);
        if (!zombie.getType().isBoss() || zombie.getLane() <= 0) {
            return parameter;
        }
        double second = grape.getIntersectionParameter(
                zombie.getLane() - 1, zombie.getColumnPosition(), radius);
        if (Double.isNaN(parameter)) {
            return second;
        }
        if (Double.isNaN(second)) {
            return parameter;
        }
        return Math.min(parameter, second);
    }

    static boolean isInsideHomingRange(Homing plant, Zombie zombie) {
        double range = plant.getRangeTiles();
        return Double.isInfinite(range)
                || distanceSquared(plant, zombie) <= range * range + POSITION_EPSILON;
    }

    static double distanceSquared(Homing plant, Zombie zombie) {
        EntityPosition position = plant.getEntityPosition();
        if (position == null) {
            return Double.POSITIVE_INFINITY;
        }
        double rowDelta = zombie.getLane() - position.getRow();
        double columnDelta = zombie.getColumnPosition() - position.getColumn();
        return rowDelta * rowDelta + columnDelta * columnDelta;
    }

    static double collisionParameter(Projectile projectile,
            EntityPosition position) {
        if (position == null) {
            return Double.POSITIVE_INFINITY;
        }
        double parameter = projectile.getIntersectionParameter(
                position.getRow(), position.getColumn(),
                PROJECTILE_COLLISION_RADIUS);
        return Double.isNaN(parameter)
                ? Double.POSITIVE_INFINITY
                : parameter;
    }

    static boolean canProjectileHit(Projectile projectile, Zombie zombie) {
        return !(projectile instanceof PiercingProjectile)
                || ((PiercingProjectile) projectile).canHit(zombie);
    }

    static boolean isAtLobbedLandingPoint(Zombie zombie,
            LobbedProjectile projectile) {
        if (zombie == null || zombie.isDead() || zombie.isRemoved()
                || zombie.isHypnotized()) {
            return false;
        }
        double rowDelta = zombie.getLane() - projectile.getLandingRow();
        if (zombie.getType().isBoss() && zombie.getLane() > 0) {
            double secondRowDelta = zombie.getLane() - 1
                    - projectile.getLandingRow();
            if (Math.abs(secondRowDelta) < Math.abs(rowDelta)) {
                rowDelta = secondRowDelta;
            }
        }
        double columnDelta = zombie.getColumnPosition()
                - projectile.getLandingColumn();
        return rowDelta * rowDelta + columnDelta * columnDelta
                <= PROJECTILE_COLLISION_RADIUS * PROJECTILE_COLLISION_RADIUS;
    }

    static boolean isInsideLobbedSplash(Zombie zombie,
            LobbedProjectile projectile) {
        double radius = projectile.getSplashRadiusTiles();
        return Math.abs(zombie.getLane() - projectile.getLandingRow()) <= radius
                && Math.abs(zombie.getColumnPosition() - projectile.getLandingColumn()) <= radius;
    }

    static boolean isCorrectMeleeDirection(double delta, boolean front) {
        return front ? delta > POSITION_EPSILON : delta < -POSITION_EPSILON;
    }

    static boolean isInsideMeleeArea(Zombie zombie,
            EntityPosition center, double radius) {
        return Math.abs(zombie.getLane() - center.getRow()) <= radius
                && Math.abs(zombie.getColumnPosition() - center.getColumn()) <= radius;
    }

    static boolean isMeleeTargetable(Zombie zombie) {
        return zombie != null && !zombie.isDead() && !zombie.isHypnotized()
                && !zombie.isSubmerged() && !zombie.isFlying();
    }

    static void resetPlantActionTimer(BasePlant plant) {
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

    static boolean containsPendingPlantAt(List<Entity> entities,
            EntityPosition position) {
        for (Entity entity : entities) {
            if (entity instanceof BasePlant
                    && position.equals(entity.getEntityPosition())) {
                return true;
            }
        }
        return false;
    }

    static boolean isInsideThreeByThree(Zombie zombie, EntityPosition center) {
        return Math.abs(zombie.getLane() - center.getRow()) <= 1
                && Math.abs(zombie.getColumnPosition() - center.getColumn()) <= 1.0;
    }

    static boolean isBetterBlocker(BasePlant candidate, BasePlant current,
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

    static boolean isCover(BasePlant plant) {
        return plant instanceof Wallnut && ((Wallnut) plant).isCoverPlant();
    }

    static String buildSunProductionResult(SunProducer producer) {
        return "plant " + producer.getType().getDisplayName()
                + " produced a sun at " + producer.getEntityPosition();
    }

    static boolean canAddPlantOnLand(BasePlant plant,
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

    static boolean isPeaPod(BasePlant plant) {
        return plant instanceof Shooter
                && ((Shooter) plant).getType() == ShooterPlantType.PEA_POD;
    }

    static boolean isLilyPad(BasePlant plant) {
        return plant instanceof Modifier && ((Modifier) plant).isLilyPad();
    }

    static void validateDeltaSeconds(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
    }

    abstract void activateReadyShooters(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract void activateReadyLobbers(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract void activateReadyStrikeThroughs(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract void activateReadyHomings(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract void applyPendingStrikeThroughBoardEffects(
            List<Entity> updateSnapshot, List<Entity> entitiesToAdd);

    abstract void applyPendingLobberBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract void applyPendingShooterBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract void resolveProjectileImpacts(List<Entity> updateSnapshot);

    abstract Zombie findEncasedZombieAt(EntityPosition position);

    abstract BasePlant findFirstOctopusCoveredPlantHit(
            Projectile projectile);

    abstract boolean isOctopusBeforeOtherTargets(
            Projectile projectile, BasePlant octopusPlant,
            PushedObstacle pushedObstacle,
            BasePlant frozenPlant, Zombie zombie);

    abstract PushedObstacle findFirstPushedObstacleHit(
            Projectile projectile);

    abstract boolean isPushedObstacleBeforeOtherTargets(
            Projectile projectile, PushedObstacle obstacle,
            BasePlant frozenPlant, Zombie zombie);

    abstract void damagePushedObstacle(
            PushedObstacle obstacle, int damage);

    abstract BasePlant findFirstFrozenPlantHit(Projectile projectile);

    abstract BasePlant findFirstFrozenPlantHit(BouncingGrape grape);

    abstract boolean isFrozenPlantBeforeZombie(BouncingGrape grape,
            BasePlant frozenPlant, Zombie zombie);

    abstract boolean isFrozenPlantBeforeZombie(Projectile projectile,
            BasePlant frozenPlant, Zombie zombie);

    abstract void extinguishProspectorDynamite(
            Projectile projectile, Zombie target);

    abstract boolean tryReflectProjectile(Projectile projectile,
            Zombie target);

    abstract void chillProjectileSourceIfBlockhead(Projectile projectile,
            Zombie blockhead);

    abstract Zombie findFirstZombieHit(Projectile projectile);

    abstract boolean isProjectileOutsideBoard(Projectile projectile);

    abstract void resolveHomingProjectileImpacts(List<Entity> updateSnapshot);

    abstract void resolveLobbedProjectileImpacts(List<Entity> updateSnapshot);

    abstract void resolveGrapeImpacts(List<Entity> updateSnapshot);

    abstract Zombie findFirstZombieHit(BouncingGrape grape);

    abstract void reportDeadZombies(List<Entity> updateSnapshot);

    abstract void applyPendingHomingBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract void applyPendingSunProducerBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract void applyPendingExplosiveBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract void applyPendingMeleeBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract void activateReadyMelee(List<Entity> updateSnapshot);

    abstract void meltLane(int lane);

    abstract void meltFrozenTiles(EntityPosition center, int radius);

    abstract void applyPendingModifierBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract boolean applyPlantFoodToPlant(BasePlant plant,
            List<Entity> entitiesToAdd);

    abstract void applyPendingModifierDeathEffects(
            List<Entity> updateSnapshot);

    abstract void applyPendingWallnutPassiveEffects(List<Entity> updateSnapshot);

    abstract void applyPendingWallnutBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd);

    abstract void warmFrozenPlants(float deltaSeconds);

    abstract void meltFrozenPlantsInLane(int lane);

    abstract void meltFrozenPlantsInArea(EntityPosition center,
            int rowRadius, double columnRadius);

    abstract void updateZombies(List<Entity> updateSnapshot, float deltaSeconds);

    abstract void updateZombie(Zombie zombie, float deltaSeconds);

    abstract boolean tryTackleZombie(Zombie zombie, Zombie target);

    abstract void attackPlant(Zombie zombie, BasePlant plant, float deltaSeconds);

    abstract void handleWallnutAfterAttack(Zombie zombie, BasePlant plant, float deltaSeconds);

    abstract void releaseSunBeanSun(Wallnut wallnut);

    abstract void applyWallnutExplosion(Wallnut wallnut);

    abstract void reportDestroyedPlant(BasePlant plant);

    abstract void reportZombieDeath(Zombie zombie);

    public abstract void addEntity(Entity entity);

    public abstract void addZombie(Zombie zombie);

    public abstract boolean canAddPlant(BasePlant requestedPlant);

    abstract boolean addPlantInternal(BasePlant requestedPlant,
            boolean applyActiveFamilyBoosts);

    public abstract List<BasePlant> getPlants();

    public abstract List<Zombie> getZombies();

    public abstract List<PushedObstacle> getPushedObstacles();

    public abstract List<BasePlant> getPlantsAt(EntityPosition position);

    public abstract BasePlant getPlantAt(EntityPosition position);

    public abstract Tile getTileAt(EntityPosition position);

    abstract void drownUnsupportedPlants(EntityPosition position,
            String reason);

    public abstract void setTileType(EntityPosition position, TileType tileType);

    public abstract BaseStructure getStructureAt(EntityPosition position);

    public abstract boolean hasGraveAt(EntityPosition position);

    abstract void removeGraveAt(EntityPosition position);

    public abstract List<BaseStructure> getStructures();
}
