package io.github.Plants_Vs_Zombies_2.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFactory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.armor.ArmorType;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchEntitySnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchProjectileSnapshot;

/**
 * Small deterministic server-side I, Zombie combat model. It deliberately
 * depends only on headless model metadata/factories, never LibGDX rendering.
 * Stage 6 adapts the existing plant damage/HP values and ZombieType
 * speed/eat-DPS/health values to a fixed-step network simulation.
 */
final class AuthoritativeIZombieSimulation {
    private static final double PLANT_ATTACK_INTERVAL_SECONDS = 1.5;
    private static final double PROJECTILE_SPEED_COLUMNS_PER_SECOND = 4.0;
    private static final double ZOMBIE_BLOCK_DISTANCE = 0.58;
    private static final double BRAIN_COLUMN = -0.25;

    private final int rows;
    private final int columns;
    private final Map<String, SimEntity> plants = new LinkedHashMap<>();
    private final Map<String, SimEntity> zombies = new LinkedHashMap<>();
    private final Map<String, Projectile> projectiles = new LinkedHashMap<>();
    private final List<Boolean> brainsAvailable;
    private long nextProjectileNumber = 1L;

    AuthoritativeIZombieSimulation(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.brainsAvailable = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            brainsAvailable.add(Boolean.TRUE);
        }
    }

    void addPlant(String id, String requestedType, int row, int column) {
        BasePlant plant = PlantFactory.createPlant(requestedType,
                new EntityPosition(row, column));
        if (plant == null) {
            throw new IllegalStateException("Accepted plant cannot be recreated: "
                    + requestedType);
        }
        plants.put(id, SimEntity.plant(id, plant.getName(), row, column,
                plant.getBaseHP(), plant.getDamage()));
    }

    void addZombie(String id, String typeName, int row, int column) {
        ZombieType type = ZombieType.valueOf(typeName);
        ArmorType armor = type.getDefaultArmor();
        int armorHealth = armor == null ? 0 : armor.getBaseHealth();
        int totalHealth = Math.max(1, type.getHitpoints() + armorHealth);
        zombies.put(id, SimEntity.zombie(id, type.name(), row, column,
                totalHealth, type.getSpeed(), type.getEatDPS()));
    }

    void removePlant(String id) {
        plants.remove(id);
    }

    TickResult tick(double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds <= 0.0) {
            throw new IllegalArgumentException("deltaSeconds must be positive");
        }
        List<String> removed = new ArrayList<>();
        updatePlants(deltaSeconds);
        updateProjectiles(deltaSeconds, removed);
        updateZombies(deltaSeconds, removed);
        removeDeadEntities(removed);
        return new TickResult(List.copyOf(removed));
    }

    private void updatePlants(double deltaSeconds) {
        for (SimEntity plant : plants.values()) {
            if (plant.health <= 0 || plant.attackDamage <= 0) {
                continue;
            }
            plant.attackCooldown -= deltaSeconds;
            if (plant.attackCooldown > 0.0) {
                continue;
            }
            SimEntity target = zombies.values().stream()
                    .filter(zombie -> zombie.health > 0 && zombie.row == plant.row
                            && zombie.columnPosition + 0.001 >= plant.columnPosition)
                    .min(Comparator.comparingDouble(zombie -> zombie.columnPosition))
                    .orElse(null);
            if (target != null) {
                String projectileId = "projectile-" + nextProjectileNumber++;
                projectiles.put(projectileId, new Projectile(projectileId,
                        plant.type + "_PROJECTILE", plant.row,
                        plant.columnPosition + 0.35,
                        PROJECTILE_SPEED_COLUMNS_PER_SECOND, plant.attackDamage));
                plant.attackCooldown = PLANT_ATTACK_INTERVAL_SECONDS;
            } else {
                // Retry promptly once a target enters the lane.
                plant.attackCooldown = Math.min(0.25,
                        PLANT_ATTACK_INTERVAL_SECONDS);
            }
        }
    }

    private void updateProjectiles(double deltaSeconds, List<String> removed) {
        Iterator<Projectile> iterator = projectiles.values().iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            double from = projectile.columnPosition;
            double to = from + projectile.velocity * deltaSeconds;
            SimEntity hit = zombies.values().stream()
                    .filter(zombie -> zombie.health > 0
                            && zombie.row == projectile.lane
                            && zombie.columnPosition >= from - 0.12
                            && zombie.columnPosition <= to + 0.12)
                    .min(Comparator.comparingDouble(zombie -> zombie.columnPosition))
                    .orElse(null);
            if (hit != null) {
                hit.health = Math.max(0, hit.health - projectile.damage);
                if (hit.health == 0) {
                    removed.add(hit.id);
                }
                iterator.remove();
            } else if (to > columns + 1.0) {
                iterator.remove();
            } else {
                projectile.columnPosition = to;
            }
        }
    }

    private void updateZombies(double deltaSeconds, List<String> removed) {
        for (SimEntity zombie : zombies.values()) {
            if (zombie.health <= 0) {
                continue;
            }
            SimEntity blocker = plants.values().stream()
                    .filter(plant -> plant.health > 0 && plant.row == zombie.row
                            && plant.columnPosition <= zombie.columnPosition + 0.001
                            && zombie.columnPosition - plant.columnPosition
                                    <= ZOMBIE_BLOCK_DISTANCE)
                    .max(Comparator.comparingDouble(plant -> plant.columnPosition))
                    .orElse(null);
            if (blocker != null) {
                zombie.damageRemainder += zombie.eatDamagePerSecond * deltaSeconds;
                int wholeDamage = (int) Math.floor(zombie.damageRemainder);
                if (wholeDamage > 0) {
                    zombie.damageRemainder -= wholeDamage;
                    blocker.health = Math.max(0, blocker.health - wholeDamage);
                    if (blocker.health == 0) {
                        removed.add(blocker.id);
                    }
                }
                continue;
            }

            zombie.damageRemainder = 0.0;
            zombie.columnPosition -= zombie.speedColumnsPerSecond * deltaSeconds;
            if (zombie.columnPosition <= BRAIN_COLUMN) {
                if (zombie.row >= 0 && zombie.row < brainsAvailable.size()
                        && brainsAvailable.get(zombie.row)) {
                    brainsAvailable.set(zombie.row, Boolean.FALSE);
                }
                zombie.health = 0;
                removed.add(zombie.id);
            }
        }
    }

    private void removeDeadEntities(List<String> removed) {
        for (String id : List.copyOf(removed)) {
            plants.remove(id);
            zombies.remove(id);
        }
    }

    boolean allBrainsEaten() {
        return brainsAvailable.stream().noneMatch(Boolean::booleanValue);
    }

    List<Boolean> brainsAvailable() {
        return List.copyOf(brainsAvailable);
    }

    List<MatchEntitySnapshot> plantSnapshots() {
        return snapshots(plants, MatchRole.PLANTS);
    }

    List<MatchEntitySnapshot> zombieSnapshots() {
        return snapshots(zombies, MatchRole.ZOMBIES);
    }

    private static List<MatchEntitySnapshot> snapshots(
            Map<String, SimEntity> source, MatchRole role) {
        List<MatchEntitySnapshot> result = new ArrayList<>();
        for (SimEntity entity : source.values()) {
            result.add(new MatchEntitySnapshot(entity.id, entity.type, role,
                    entity.row, (int) Math.floor(entity.columnPosition),
                    entity.columnPosition, entity.health, entity.maximumHealth));
        }
        return List.copyOf(result);
    }

    List<MatchProjectileSnapshot> projectileSnapshots() {
        return projectiles.values().stream()
                .map(projectile -> new MatchProjectileSnapshot(
                        projectile.id, projectile.type, projectile.lane,
                        projectile.columnPosition, projectile.velocity,
                        projectile.damage))
                .toList();
    }

    List<ZombiePosition> zombiePositions() {
        return zombies.values().stream()
                .map(zombie -> new ZombiePosition(zombie.id, zombie.row,
                        zombie.columnPosition))
                .toList();
    }

    record TickResult(List<String> removedEntityIds) { }
    record ZombiePosition(String entityId, int row, double columnPosition) { }

    private static final class SimEntity {
        private final String id;
        private final String type;
        private final int row;
        private final int maximumHealth;
        private final int attackDamage;
        private final double speedColumnsPerSecond;
        private final int eatDamagePerSecond;
        private double columnPosition;
        private int health;
        private double attackCooldown;
        private double damageRemainder;

        private SimEntity(String id, String type, int row, double columnPosition,
                int maximumHealth, int attackDamage,
                double speedColumnsPerSecond, int eatDamagePerSecond) {
            this.id = id;
            this.type = type;
            this.row = row;
            this.columnPosition = columnPosition;
            this.maximumHealth = maximumHealth;
            this.health = maximumHealth;
            this.attackDamage = attackDamage;
            this.speedColumnsPerSecond = speedColumnsPerSecond;
            this.eatDamagePerSecond = eatDamagePerSecond;
            this.attackCooldown = attackDamage > 0 ? 0.35 : Double.POSITIVE_INFINITY;
        }

        static SimEntity plant(String id, String type, int row, int column,
                int health, int damage) {
            return new SimEntity(id, type, row, column,
                    Math.max(1, health), damage, 0.0, 0);
        }

        static SimEntity zombie(String id, String type, int row, int column,
                int health, double speed, int eatDps) {
            return new SimEntity(id, type, row, column,
                    health, 0, Math.max(0.0, speed), Math.max(0, eatDps));
        }
    }

    private static final class Projectile {
        private final String id;
        private final String type;
        private final int lane;
        private final double velocity;
        private final int damage;
        private double columnPosition;

        private Projectile(String id, String type, int lane,
                double columnPosition, double velocity, int damage) {
            this.id = id;
            this.type = type;
            this.lane = lane;
            this.columnPosition = columnPosition;
            this.velocity = velocity;
            this.damage = damage;
        }
    }
}
