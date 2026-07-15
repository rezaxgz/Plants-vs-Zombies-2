package model.game.entities.plants.shooter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import model.game.entities.EntityPosition;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;
import model.game.entities.projectile.PiercingProjectile;
import model.game.entities.projectile.Projectile;
import model.game.entities.projectile.effect.ChillEffect;
import model.game.entities.projectile.effect.DamageEffect;
import model.game.entities.projectile.effect.FireEffect;
import model.game.entities.projectile.effect.PoisonEffect;
import model.game.entities.projectile.effect.ProjectileEffect;
import model.game.entities.projectile.movement.BouncingProjectileMovement;
import model.game.entities.projectile.movement.LinearProjectileMovement;
import model.game.entities.projectile.movement.ProjectileDirection;
import model.game.entities.zombies.Zombie;

public class Shooter extends BasePlant {
    private static final double TIMER_EPSILON = 0.000001;
    private static final double RAY_TOLERANCE = 0.35;
    private static final double PROJECTILE_SPEED_TILES_PER_SECOND = 4.0;
    private static final int[] BOWLING_DAMAGE_BONUSES = {0, 80, 140};

    private final ShooterPlantType type;
    private final List<Projectile> pendingProjectiles = new ArrayList<>();
    private final Random random;

    private double secondsSinceLastShot;
    private double lifeSeconds;
    private int bowlingShotIndex;
    private int projectileBoardRows = 5;
    private boolean bowlingUpwardNext = true;
    private boolean familyBoostPending;
    private boolean familyBoostActivated;
    private boolean plantFoodUsed;

    public Shooter() {
        this(ShooterPlantType.PEASHOOTER, 1, null);
    }

    public Shooter(ShooterPlantType type, EntityPosition entityPosition) {
        this(type, 1, entityPosition);
    }

    public Shooter(ShooterPlantType type, int level, EntityPosition entityPosition) {
        this(type, level, entityPosition, new Random());
    }

    public Shooter(ShooterPlantType type, int level, EntityPosition entityPosition,
            Random random) {
        super(requireType(type).getDisplayName(), PlantCategory.SHOOTER,
                type.getTags(), level, type.getCost(level), type.getBaseHP(level),
                type.getDamage(level), entityPosition);
        ShooterPlantType.validateLevel(level);
        if (random == null) {
            throw new IllegalArgumentException("random cannot be null");
        }
        this.type = type;
        this.random = random;
    }

    private static ShooterPlantType requireType(ShooterPlantType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        return type;
    }

    @Override
    public void update(float deltaSeconds) {
        if (isRemoved()) {
            return;
        }
        super.update(deltaSeconds);
        lifeSeconds += deltaSeconds;
        expireIfNeeded();
        if (type.getBehavior() == ShooterBehavior.FAMILY_BOOST) {
            activateFamilyBoost();
            return;
        }
        float interval = type.getActionIntervalSeconds(getLevel());
        secondsSinceLastShot = Math.min(interval, secondsSinceLastShot + deltaSeconds);
    }

    private void expireIfNeeded() {
        double lifespan = type.getLifespanSeconds(getLevel());
        if (lifespan > 0.0 && lifeSeconds + TIMER_EPSILON >= lifespan) {
            markForRemoval();
        }
    }

    private void activateFamilyBoost() {
        if (!familyBoostActivated) {
            familyBoostActivated = true;
            familyBoostPending = true;
        }
    }

    public boolean isReadyToShoot() {
        float interval = type.getActionIntervalSeconds(getLevel());
        return !isRemoved() && type.getBehavior() != ShooterBehavior.FAMILY_BOOST
                && interval > 0.0f && secondsSinceLastShot + TIMER_EPSILON >= interval;
    }

    public boolean canTarget(Zombie zombie, int boardRows) {
        if (zombie == null || zombie.isDead() || zombie.isSubmerged()
                || getEntityPosition() == null) {
            return false;
        }
        int plantRow = getEntityPosition().getRow();
        double plantColumn = getEntityPosition().getColumn();
        double range = type.getRangeTiles(getLevel());
        switch (type.getBehavior()) {
        case THREE_LANES:
            return Math.abs(zombie.getLane() - plantRow) <= 1
                    && isOnRay(zombie, zombie.getLane(), plantColumn,
                            ProjectileDirection.RIGHT, range);
        case FOUR_DIAGONALS:
            return isOnAnyRay(zombie, plantRow, plantColumn, range,
                    ProjectileDirection.UP_RIGHT, ProjectileDirection.DOWN_RIGHT,
                    ProjectileDirection.UP_LEFT, ProjectileDirection.DOWN_LEFT);
        case SPLIT:
            return isOnAnyRay(zombie, plantRow, plantColumn, range,
                    ProjectileDirection.RIGHT, ProjectileDirection.LEFT);
        case FIVE_WAY:
            return isOnAnyRay(zombie, plantRow, plantColumn, range,
                    ProjectileDirection.RIGHT, ProjectileDirection.UP_RIGHT,
                    ProjectileDirection.DOWN_RIGHT, ProjectileDirection.UP_LEFT,
                    ProjectileDirection.DOWN_LEFT);
        case FORWARD:
        case BOWLING:
        case SHORT_RANGE:
            return plantRow >= 0 && plantRow < boardRows
                    && isOnRay(zombie, plantRow, plantColumn,
                            ProjectileDirection.RIGHT, range);
        case FAMILY_BOOST:
            return false;
        default:
            throw new IllegalStateException("Unknown shooter behavior: " + type.getBehavior());
        }
    }

    private static boolean isOnAnyRay(Zombie zombie, double startRow, double startColumn,
            double range, ProjectileDirection... directions) {
        for (ProjectileDirection direction : directions) {
            if (isOnRay(zombie, startRow, startColumn, direction, range)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOnRay(Zombie zombie, double startRow, double startColumn,
            ProjectileDirection direction, double range) {
        double rowDelta = zombie.getLane() - startRow;
        double columnDelta = zombie.getColumnPosition() - startColumn;
        double projection = rowDelta * direction.getRowComponent()
                + columnDelta * direction.getColumnComponent();
        if (projection <= 0.0 || projection > range) {
            return false;
        }
        double perpendicular = Math.abs(rowDelta * direction.getColumnComponent()
                - columnDelta * direction.getRowComponent());
        return perpendicular <= RAY_TOLERANCE;
    }

    public List<Projectile> shoot(int boardRows) {
        if (!isReadyToShoot()) {
            return Collections.emptyList();
        }
        secondsSinceLastShot = 0.0;
        projectileBoardRows = boardRows;
        int damage = getNextShotDamage();
        addPatternProjectiles(boardRows, type.getShotsPerDirection(), damage);
        addRandomMegaGatlingPlantFood(boardRows);
        return drainProjectiles();
    }

    private void addRandomMegaGatlingPlantFood(int boardRows) {
        if (type == ShooterPlantType.MEGA_GATLING_PEA
                && random.nextDouble() < getPlantFoodChance()) {
            plantFoodUsed = true;
            addPlantFoodProjectiles(boardRows);
        }
    }

    private int getNextShotDamage() {
        int damage = type.getDamage(getLevel());
        if (type == ShooterPlantType.BOWLING_BULB) {
            damage += BOWLING_DAMAGE_BONUSES[bowlingShotIndex];
            bowlingShotIndex = (bowlingShotIndex + 1) % BOWLING_DAMAGE_BONUSES.length;
        }
        return damage;
    }

    public void usePlantFood(int boardRows) {
        if (isRemoved() || type == ShooterPlantType.APPEASE_MINT) {
            return;
        }
        plantFoodUsed = true;
        projectileBoardRows = boardRows;
        if (type == ShooterPlantType.SEA_SHROOM || type == ShooterPlantType.PUFF_SHROOM) {
            resetLifespan();
        }
        addPlantFoodProjectiles(boardRows);
    }

    private void addPlantFoodProjectiles(int boardRows) {
        int regularDamage = type.getDamage(getLevel());
        if (type == ShooterPlantType.PEA_POD) {
            addPatternProjectiles(boardRows, 1,
                    regularDamage * type.getPlantFoodDamageMultiplier());
            return;
        }
        if (type == ShooterPlantType.CITRON) {
            addCitronPlantFoodProjectile(regularDamage);
            return;
        }
        if (type == ShooterPlantType.THREEPEATER) {
            addAllLaneProjectiles(boardRows, type.getPlantFoodShotCount(), regularDamage);
            return;
        }
        if (type == ShooterPlantType.BOWLING_BULB) {
            addPatternProjectiles(boardRows, 3, regularDamage + BOWLING_DAMAGE_BONUSES[2]);
            return;
        }
        addPatternProjectiles(boardRows, type.getPlantFoodShotCount(), regularDamage);
        addPlantFoodGiantProjectiles(boardRows, regularDamage);
    }

    private void addCitronPlantFoodProjectile(int regularDamage) {
        if (getEntityPosition() == null) {
            return;
        }
        pendingProjectiles.add(new PiercingProjectile(getName(),
                getEntityPosition().getRow(), getEntityPosition().getColumn() + 0.2,
                Collections.singletonList(new DamageEffect(
                        regularDamage * type.getPlantFoodDamageMultiplier())),
                new LinearProjectileMovement(ProjectileDirection.RIGHT,
                        PROJECTILE_SPEED_TILES_PER_SECOND),
                Double.POSITIVE_INFINITY, Integer.MAX_VALUE));
    }

    private void addAllLaneProjectiles(int boardRows, int count, int damage) {
        for (int row = 0; row < boardRows; row++) {
            addDirectionalProjectiles(row, count, damage, ProjectileDirection.RIGHT);
        }
    }

    private void addPlantFoodGiantProjectiles(int boardRows, int regularDamage) {
        if (type == ShooterPlantType.REPEATER) {
            addPatternProjectiles(boardRows, 1,
                    regularDamage * type.getPlantFoodDamageMultiplier());
        } else if (type == ShooterPlantType.MEGA_GATLING_PEA) {
            addPatternProjectiles(boardRows, 4,
                    regularDamage * type.getPlantFoodDamageMultiplier());
        }
    }

    private void addPatternProjectiles(int boardRows, int count, int damage) {
        if (count <= 0 || getEntityPosition() == null) {
            return;
        }
        int row = getEntityPosition().getRow();
        switch (type.getBehavior()) {
        case THREE_LANES:
            addThreeLaneProjectiles(boardRows, count, damage);
            break;
        case FOUR_DIAGONALS:
            addDirectionalProjectiles(row, count, damage,
                    ProjectileDirection.UP_RIGHT, ProjectileDirection.DOWN_RIGHT,
                    ProjectileDirection.UP_LEFT, ProjectileDirection.DOWN_LEFT);
            break;
        case SPLIT:
            addDirectionalProjectiles(row, count, damage, ProjectileDirection.RIGHT);
            addDirectionalProjectiles(row, count * 2, damage, ProjectileDirection.LEFT);
            break;
        case FIVE_WAY:
            addDirectionalProjectiles(row, count, damage,
                    ProjectileDirection.RIGHT, ProjectileDirection.UP_RIGHT,
                    ProjectileDirection.DOWN_RIGHT, ProjectileDirection.UP_LEFT,
                    ProjectileDirection.DOWN_LEFT);
            break;
        case FORWARD:
        case BOWLING:
        case SHORT_RANGE:
            addDirectionalProjectiles(row, count, damage, ProjectileDirection.RIGHT);
            break;
        case FAMILY_BOOST:
            break;
        default:
            throw new IllegalStateException("Unknown shooter behavior: " + type.getBehavior());
        }
    }

    private void addThreeLaneProjectiles(int boardRows, int count, int damage) {
        int centerRow = getEntityPosition().getRow();
        for (int row = centerRow - 1; row <= centerRow + 1; row++) {
            if (row >= 0 && row < boardRows) {
                addDirectionalProjectiles(row, count, damage, ProjectileDirection.RIGHT);
            }
        }
    }

    private void addDirectionalProjectiles(int row, int count, int damage,
            ProjectileDirection... directions) {
        for (ProjectileDirection direction : directions) {
            for (int shot = 0; shot < count; shot++) {
                pendingProjectiles.add(createProjectile(row, direction, damage));
            }
        }
    }

    private Projectile createProjectile(int row, ProjectileDirection direction, int damage) {
        double column = getEntityPosition().getColumn();
        double startColumn = column + direction.getColumnComponent() * 0.2;
        double startRow = row + direction.getRowComponent() * 0.2;
        if (type == ShooterPlantType.BOWLING_BULB) {
            boolean upward = bowlingUpwardNext;
            bowlingUpwardNext = !bowlingUpwardNext;
            return new PiercingProjectile(type.getDisplayName(), startRow, startColumn,
                    createEffects(damage),
                    new BouncingProjectileMovement(PROJECTILE_SPEED_TILES_PER_SECOND,
                            projectileBoardRows, upward),
                    type.getRangeTiles(getLevel()), 3);
        }
        return new Projectile(type.getDisplayName(), startRow, startColumn,
                createEffects(damage),
                new LinearProjectileMovement(direction, PROJECTILE_SPEED_TILES_PER_SECOND),
                type.getRangeTiles(getLevel()), 5.0,
                type.getTags().contains(model.game.entities.plants.PlantTag.PEA));
    }

    private List<ProjectileEffect> createEffects(int damage) {
        List<ProjectileEffect> effects = new ArrayList<>();
        switch (type.getProjectileType()) {
        case NORMAL:
            effects.add(new DamageEffect(damage));
            break;
        case ICE:
            effects.add(new DamageEffect(damage));
            effects.add(new ChillEffect(type.getChillDurationSeconds(getLevel())));
            break;
        case FIRE:
            effects.add(new FireEffect(damage));
            break;
        case POISON:
            effects.add(new PoisonEffect(damage, type.getPoisonDamagePerTick(getLevel()),
                    ShooterPlantType.POISON_TICK_INTERVAL_SECONDS,
                    ShooterPlantType.POISON_DURATION_SECONDS));
            break;
        default:
            throw new IllegalStateException("Unknown projectile type: " + type.getProjectileType());
        }
        return effects;
    }

    public List<Projectile> drainProjectiles() {
        if (pendingProjectiles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Projectile> projectiles = new ArrayList<>(pendingProjectiles);
        pendingProjectiles.clear();
        return projectiles;
    }

    public void resetLifespan() {
        lifeSeconds = 0.0;
    }

    public void resetActionTimer() {
        secondsSinceLastShot = type.getActionIntervalSeconds(getLevel());
    }

    public boolean drainFamilyBoostPending() {
        boolean result = familyBoostPending;
        familyBoostPending = false;
        return result;
    }

    public ShooterPlantType getType() {
        return type;
    }

    public float getRechargeSeconds() {
        return type.getRechargeSeconds(getLevel());
    }

    public float getActionIntervalSeconds() {
        return type.getActionIntervalSeconds(getLevel());
    }

    public double getRangeTiles() {
        return type.getRangeTiles(getLevel());
    }

    public double getLifespanSeconds() {
        return type.getLifespanSeconds(getLevel());
    }

    public double getChillDurationSeconds() {
        return type.getChillDurationSeconds(getLevel());
    }

    public int getPoisonDamagePerTick() {
        return type.getPoisonDamagePerTick(getLevel());
    }

    public float getFamilyBoostDurationSeconds() {
        return type.getFamilyBoostDurationSeconds(getLevel());
    }

    public double getPlantFoodChance() {
        return type.getPlantFoodChance(getLevel());
    }

    public boolean resetsFamilyCooldowns() {
        return type.resetsFamilyCooldowns(getLevel());
    }

    public boolean wasPlantFoodUsed() {
        return plantFoodUsed;
    }
}
