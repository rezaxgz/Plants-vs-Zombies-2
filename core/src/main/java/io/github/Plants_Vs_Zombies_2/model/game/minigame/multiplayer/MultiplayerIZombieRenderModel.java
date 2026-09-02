package io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.Game;
import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFactory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.Projectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.DamageEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.movement.LinearProjectileMovement;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.movement.ProjectileDirection;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.IZombieCard;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.IZombieLevel;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.IZombiePresentationModel;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchEntitySnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchProjectileSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;

/**
 * Client-side, non-simulating mirror of an authoritative multiplayer match.
 * It deliberately contains real Phase-1 entities so the existing
 * {@code GameScreen} animation and effect pipeline can render network state.
 */
public final class MultiplayerIZombieRenderModel extends Game
        implements IZombiePresentationModel {
    private static final double PROJECTILE_RANGE = 1_000.0;
    private static final double PROJECTILE_LIFETIME_SECONDS = 3_600.0;
    private static final double DEFAULT_PRESENTATION_INTERVAL_SECONDS = 0.20;
    private static final double MIN_PRESENTATION_INTERVAL_SECONDS = 0.05;
    private static final double MAX_PRESENTATION_INTERVAL_SECONDS = 0.40;

    private final MatchRole localRole;
    private final IZombieLevel level;
    private final int redLineColumn;
    private final Map<String, Integer> plantLoadoutLevels;
    private final Map<String, BasePlant> plantsById = new LinkedHashMap<>();
    private final Map<BasePlant, String> plantIds = new HashMap<>();
    private final Map<String, Zombie> zombiesById = new LinkedHashMap<>();
    private final Map<String, Projectile> projectilesById =
            new LinkedHashMap<>();
    private final Map<String, ColumnMotion> zombieMotions = new HashMap<>();
    private final Map<String, ProjectileMotion> projectileMotions =
            new HashMap<>();
    private List<Boolean> brainsAvailable;
    private double remainingSeconds;
    private long lastSnapshotTimestampMillis = Long.MIN_VALUE;
    private long lastSimulationTick = Long.MIN_VALUE;
    private long lastRevision = Long.MIN_VALUE;

    public MultiplayerIZombieRenderModel(MatchStateSnapshot initialSnapshot,
            MatchRole localRole, Map<String, Integer> plantLoadoutLevels) {
        super(boardFor(initialSnapshot), null, initialResource(initialSnapshot,
                localRole), List.of(), false);
        if (localRole == null) {
            throw new IllegalArgumentException("local role is required");
        }
        this.localRole = localRole;
        this.level = resolveLevel(initialSnapshot == null
                ? null : initialSnapshot.getLevel());
        this.redLineColumn = initialSnapshot.getRedLineColumn();
        this.plantLoadoutLevels = plantLoadoutLevels == null
                ? Map.of() : Map.copyOf(plantLoadoutLevels);
        this.brainsAvailable = initialBrains(initialSnapshot);
        disableSkySuns("Multiplayer I, Zombie is synchronized by the server");
        if (localRole == MatchRole.PLANTS && plantLoadoutLevels != null
                && !plantLoadoutLevels.isEmpty()) {
            configurePlantLoadout(plantLoadoutLevels, List.of());
        }
        if (initialSnapshot != null) {
            applySnapshot(initialSnapshot);
        }
    }

    public List<RemoteProjectileLaunch> applySnapshot(
            MatchStateSnapshot snapshot) {
        requireCompatible(snapshot);
        if (snapshot.getSimulationTick() == lastSimulationTick
                && snapshot.getRevision() == lastRevision) {
            return List.of();
        }
        double presentationInterval = presentationInterval(snapshot);
        synchronizeResource(localRole == MatchRole.PLANTS
                ? snapshot.getPlantResource() : snapshot.getZombieResource());
        brainsAvailable = List.copyOf(snapshot.getBrainsAvailable());
        remainingSeconds = Math.max(0.0, snapshot.getRemainingSeconds());
        synchronizePlants(snapshot.getPlants());
        synchronizeZombies(snapshot.getZombies(), presentationInterval);
        List<RemoteProjectileLaunch> launches = synchronizeProjectiles(
                snapshot.getProjectiles(), presentationInterval);
        lastSimulationTick = snapshot.getSimulationTick();
        lastRevision = snapshot.getRevision();
        return launches;
    }

    /**
     * Advances display-only interpolation between authoritative snapshots.
     * No combat, damage, resources, or win conditions are simulated here.
     */
    public void advancePresentation(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds <= 0f) return;
        double delta = Math.min(deltaSeconds,
                MAX_PRESENTATION_INTERVAL_SECONDS);
        remainingSeconds = Math.max(0.0, remainingSeconds - delta);

        var zombieIterator = zombieMotions.entrySet().iterator();
        while (zombieIterator.hasNext()) {
            Map.Entry<String, ColumnMotion> entry = zombieIterator.next();
            Zombie zombie = zombiesById.get(entry.getKey());
            if (zombie == null) {
                zombieIterator.remove();
                continue;
            }
            ColumnMotion motion = entry.getValue();
            zombie.moveTo(motion.advance(delta));
            if (motion.isComplete()) zombieIterator.remove();
        }

        var projectileIterator = projectileMotions.entrySet().iterator();
        while (projectileIterator.hasNext()) {
            Map.Entry<String, ProjectileMotion> entry =
                    projectileIterator.next();
            Projectile projectile = projectilesById.get(entry.getKey());
            if (projectile == null) {
                projectileIterator.remove();
                continue;
            }
            ProjectileMotion motion = entry.getValue();
            double nextRow = motion.advanceRow(delta);
            double nextColumn = motion.currentColumn();
            projectile.translate(nextRow - projectile.getRowPosition(),
                    nextColumn - projectile.getColumnPosition());
            if (motion.isComplete()) projectileIterator.remove();
        }
    }

    private void synchronizePlants(List<MatchEntitySnapshot> snapshots) {
        Set<String> incoming = new HashSet<>();
        for (MatchEntitySnapshot snapshot : snapshots) {
            incoming.add(snapshot.getEntityId());
            BasePlant plant = plantsById.get(snapshot.getEntityId());
            if (plant == null || !plant.getName().equalsIgnoreCase(
                    snapshot.getEntityType())) {
                removePlant(snapshot.getEntityId());
                plant = PlantFactory.createPlant(snapshot.getEntityType(),
                        plantLevel(snapshot.getEntityType()),
                        new EntityPosition(snapshot.getRow(),
                                snapshot.getColumn()));
                if (plant == null) continue;
                getBoard().addEntity(plant);
                plantsById.put(snapshot.getEntityId(), plant);
                plantIds.put(plant, snapshot.getEntityId());
            } else {
                plant.setEntityPosition(new EntityPosition(snapshot.getRow(),
                        snapshot.getColumn()));
            }
            synchronizePlantHealth(plant, snapshot.getHealth());
        }
        for (String id : new ArrayList<>(plantsById.keySet())) {
            if (!incoming.contains(id)) removePlant(id);
        }
    }

    private void synchronizeZombies(List<MatchEntitySnapshot> snapshots,
            double presentationInterval) {
        Set<String> incoming = new HashSet<>();
        for (MatchEntitySnapshot snapshot : snapshots) {
            incoming.add(snapshot.getEntityId());
            Zombie zombie = zombiesById.get(snapshot.getEntityId());
            ZombieType type = zombieType(snapshot.getEntityType());
            if (type == null) continue;
            if (zombie == null || zombie.getType() != type) {
                removeZombie(snapshot.getEntityId());
                zombie = new Zombie(type, 0, snapshot.getRow(),
                        Math.max(0.0, snapshot.getColumnPosition()), false);
                getBoard().addEntity(zombie);
                zombiesById.put(snapshot.getEntityId(), zombie);
            } else {
                zombie.moveToLane(snapshot.getRow());
                double target = Math.max(0.0,
                        snapshot.getColumnPosition());
                zombieMotions.put(snapshot.getEntityId(), new ColumnMotion(
                        zombie.getColumnPosition(), target,
                        presentationInterval));
            }
            synchronizeZombieHealth(zombie, snapshot.getHealth());
        }
        for (String id : new ArrayList<>(zombiesById.keySet())) {
            if (!incoming.contains(id)) removeZombie(id);
        }
    }

    private List<RemoteProjectileLaunch> synchronizeProjectiles(
            List<MatchProjectileSnapshot> snapshots,
            double presentationInterval) {
        Set<String> incoming = new HashSet<>();
        List<RemoteProjectileLaunch> launches = new ArrayList<>();
        for (MatchProjectileSnapshot snapshot : snapshots) {
            incoming.add(snapshot.getProjectileId());
            Projectile projectile = projectilesById.get(
                    snapshot.getProjectileId());
            if (projectile == null) {
                String source = sourcePlant(snapshot.getProjectileType());
                projectile = new Projectile(source, snapshot.getLane(),
                        snapshot.getColumnPosition(),
                        List.of(new DamageEffect(Math.max(0,
                                snapshot.getDamage()))),
                        new LinearProjectileMovement(ProjectileDirection.RIGHT,
                                Math.max(0.01,
                                        Math.abs(snapshot
                                                .getVelocityColumnsPerSecond()))),
                        PROJECTILE_RANGE, PROJECTILE_LIFETIME_SECONDS);
                getBoard().addEntity(projectile);
                projectilesById.put(snapshot.getProjectileId(), projectile);
                launches.add(new RemoteProjectileLaunch(source,
                        snapshot.getLane()));
            }
            double predictedColumn = snapshot.getColumnPosition()
                    + snapshot.getVelocityColumnsPerSecond()
                            * presentationInterval;
            projectileMotions.put(snapshot.getProjectileId(),
                    new ProjectileMotion(projectile.getRowPosition(),
                            projectile.getColumnPosition(), snapshot.getLane(),
                            predictedColumn, presentationInterval));
        }
        for (String id : new ArrayList<>(projectilesById.keySet())) {
            if (!incoming.contains(id)) removeProjectile(id);
        }
        return List.copyOf(launches);
    }

    private void synchronizeResource(int requested) {
        int resource = Math.max(0, requested);
        int current = getSunCount();
        if (resource > current) {
            addSun(resource - current);
        } else if (resource < current) {
            spendSun(current - resource);
        }
    }

    private void removePlant(String id) {
        BasePlant removed = plantsById.remove(id);
        if (removed != null) {
            plantIds.remove(removed);
            getBoard().removeEntity(removed);
        }
    }

    private void removeZombie(String id) {
        Zombie removed = zombiesById.remove(id);
        zombieMotions.remove(id);
        if (removed != null) getBoard().removeEntity(removed);
    }

    private void removeProjectile(String id) {
        Projectile removed = projectilesById.remove(id);
        projectileMotions.remove(id);
        if (removed != null) getBoard().removeEntity(removed);
    }

    private double presentationInterval(MatchStateSnapshot snapshot) {
        long timestamp = snapshot.getServerTimestampEpochMillis();
        double interval = DEFAULT_PRESENTATION_INTERVAL_SECONDS;
        if (lastSnapshotTimestampMillis != Long.MIN_VALUE
                && timestamp > lastSnapshotTimestampMillis) {
            interval = (timestamp - lastSnapshotTimestampMillis) / 1_000.0;
        }
        if (timestamp > lastSnapshotTimestampMillis) {
            lastSnapshotTimestampMillis = timestamp;
        }
        return Math.max(MIN_PRESENTATION_INTERVAL_SECONDS,
                Math.min(MAX_PRESENTATION_INTERVAL_SECONDS, interval));
    }

    public String getPlantEntityId(BasePlant plant) {
        return plantIds.get(plant);
    }

    public double getRemainingSeconds() {
        return remainingSeconds;
    }

    public MatchRole getLocalRole() {
        return localRole;
    }

    @Override public IZombieLevel getLevel() { return level; }
    @Override public int getRedLineColumn() { return redLineColumn; }

    @Override
    public boolean isBrainAvailable(int row) {
        if (row < 0 || row >= brainsAvailable.size()) {
            throw new IllegalArgumentException("brain row is outside the board");
        }
        return brainsAvailable.get(row);
    }

    @Override
    public double getCardCooldownRemainingSeconds(IZombieCard card) {
        return 0.0;
    }

    @Override public void update(float deltaSeconds) {
        // Never simulate on the client; authoritative snapshots own all state.
    }

    @Override public boolean allowsDirectPlanting() {
        return localRole == MatchRole.PLANTS;
    }

    @Override public String getDirectPlantingDisabledMessage() {
        return "Only the plant player can place plants in this match.";
    }

    @Override protected boolean usesLawnMowers() { return false; }

    private static Board boardFor(MatchStateSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("initial snapshot is required");
        }
        return new Board(snapshot.getBoardRows(), snapshot.getBoardColumns());
    }

    private static int initialResource(MatchStateSnapshot snapshot,
            MatchRole role) {
        if (snapshot == null || role == null) return 0;
        return role == MatchRole.PLANTS
                ? snapshot.getPlantResource() : snapshot.getZombieResource();
    }

    private static List<Boolean> initialBrains(MatchStateSnapshot snapshot) {
        return snapshot == null ? List.of()
                : List.copyOf(snapshot.getBrainsAvailable());
    }

    private void requireCompatible(MatchStateSnapshot snapshot) {
        if (snapshot == null
                || snapshot.getBoardRows() != getBoard().getNumberOfRows()
                || snapshot.getBoardColumns()
                        != getBoard().getNumberOfColumns()) {
            throw new IllegalArgumentException(
                    "snapshot board does not match the render model");
        }
    }

    private static IZombieLevel resolveLevel(String requested) {
        for (IZombieLevel candidate : IZombieLevel.values()) {
            if (candidate.name().equalsIgnoreCase(requested)
                    || candidate.getName().equalsIgnoreCase(requested)) {
                return candidate;
            }
        }
        return IZombieLevel.FIRST_BITE;
    }

    private static ZombieType zombieType(String requested) {
        if (requested == null) return null;
        try {
            return ZombieType.valueOf(requested);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private int plantLevel(String plantName) {
        if (plantName == null) return 1;
        for (Map.Entry<String, Integer> entry
                : plantLoadoutLevels.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(plantName)) {
                return Math.max(1, entry.getValue());
            }
        }
        return 1;
    }

    private static String sourcePlant(String projectileType) {
        if (projectileType == null || projectileType.isBlank()) {
            return "Peashooter";
        }
        String suffix = "_PROJECTILE";
        return projectileType.endsWith(suffix)
                ? projectileType.substring(0,
                        projectileType.length() - suffix.length())
                : projectileType;
    }

    private static void synchronizePlantHealth(BasePlant plant, int requested) {
        int target = Math.max(0, requested);
        int current = plant.getCurrentHP();
        if (target < current) plant.takeDamage(current - target);
        else if (target > current) plant.heal(target - current);
    }

    private static void synchronizeZombieHealth(Zombie zombie, int requested) {
        int target = Math.max(0, requested);
        int current = zombie.getCurrentDurability();
        if (target < current) zombie.takeDamage(current - target);
    }

    private static final class ColumnMotion {
        private final double start;
        private final double target;
        private final double duration;
        private double elapsed;

        private ColumnMotion(double start, double target, double duration) {
            this.start = start;
            this.target = target;
            this.duration = Math.max(MIN_PRESENTATION_INTERVAL_SECONDS,
                    duration);
        }

        private double advance(double delta) {
            elapsed = Math.min(duration, elapsed + delta);
            double progress = elapsed / duration;
            return start + (target - start) * progress;
        }

        private boolean isComplete() {
            return elapsed + 0.000001 >= duration;
        }
    }

    private static final class ProjectileMotion {
        private final double startRow;
        private final double startColumn;
        private final double targetRow;
        private final double targetColumn;
        private final double duration;
        private double elapsed;
        private double currentColumn;

        private ProjectileMotion(double startRow, double startColumn,
                double targetRow, double targetColumn, double duration) {
            this.startRow = startRow;
            this.startColumn = startColumn;
            this.targetRow = targetRow;
            this.targetColumn = targetColumn;
            this.duration = Math.max(MIN_PRESENTATION_INTERVAL_SECONDS,
                    duration);
            this.currentColumn = startColumn;
        }

        private double advanceRow(double delta) {
            elapsed = Math.min(duration, elapsed + delta);
            double progress = elapsed / duration;
            currentColumn = startColumn
                    + (targetColumn - startColumn) * progress;
            return startRow + (targetRow - startRow) * progress;
        }

        private double currentColumn() {
            return currentColumn;
        }

        private boolean isComplete() {
            return elapsed + 0.000001 >= duration;
        }
    }

    public record RemoteProjectileLaunch(String sourcePlant, int lane) { }
}
