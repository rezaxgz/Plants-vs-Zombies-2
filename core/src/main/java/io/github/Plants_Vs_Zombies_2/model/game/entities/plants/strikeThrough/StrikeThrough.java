package io.github.Plants_Vs_Zombies_2.model.game.entities.plants.strikeThrough;

import java.util.ArrayList;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantCategory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.PiercingProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.DamageEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.KnockbackEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.ProjectileEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.movement.LinearProjectileMovement;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.movement.ProjectileDirection;

public class StrikeThrough extends BasePlant {
    private static final double TIMER_EPSILON = 0.000001;

    private final StrikeThroughPlantType type;

    private double secondsSinceLastAttack;
    private boolean familyBoostActivated;
    private boolean familyBoostPending;
    private boolean plantFoodPending;
    private boolean plantFoodUsed;

    public StrikeThrough() {
        this(StrikeThroughPlantType.CACTUS, 1, null);
    }

    public StrikeThrough(StrikeThroughPlantType type, EntityPosition entityPosition) {
        this(type, 1, entityPosition);
    }

    public StrikeThrough(StrikeThroughPlantType type, int level,
            EntityPosition entityPosition) {
        super(requireType(type).getDisplayName(), PlantCategory.STRIKE_THROUGH,
                type.getTags(), level, type.getCost(level), type.getBaseHP(level),
                type.getDamage(level), entityPosition);
        StrikeThroughPlantType.validateLevel(level);
        this.type = type;
    }

    private static StrikeThroughPlantType requireType(StrikeThroughPlantType type) {
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
        if (type.getBehavior() == StrikeThroughBehavior.FAMILY_BOOST) {
            activateFamilyBoost();
            return;
        }
        float interval = getActionIntervalSeconds();
        secondsSinceLastAttack = Math.min(interval,
                secondsSinceLastAttack + deltaSeconds);
    }

    private void activateFamilyBoost() {
        if (!familyBoostActivated) {
            familyBoostActivated = true;
            familyBoostPending = true;
        }
    }

    public boolean isReadyToAttack() {
        float interval = getActionIntervalSeconds();
        return !isRemoved()
                && type.getBehavior() != StrikeThroughBehavior.FAMILY_BOOST
                && interval > 0.0f
                && secondsSinceLastAttack + TIMER_EPSILON >= interval;
    }

    public PiercingProjectile shoot() {
        if (!isReadyToAttack()) {
            return null;
        }
        secondsSinceLastAttack = 0.0;
        return createProjectile(getDamage(), getMaximumTargets(), false,
                getRangeTiles());
    }

    public void usePlantFood() {
        if (isRemoved() || type.getBehavior() == StrikeThroughBehavior.FAMILY_BOOST) {
            return;
        }
        plantFoodUsed = true;
        plantFoodPending = true;
    }

    public PiercingProjectile drainPlantFoodProjectile() {
        if (!plantFoodPending) {
            return null;
        }
        plantFoodPending = false;
        int damage = getDamage()
                * StrikeThroughPlantType.PLANT_FOOD_DAMAGE_MULTIPLIER;
        return createProjectile(damage, Integer.MAX_VALUE,
                type == StrikeThroughPlantType.FUME_SHROOM,
                Double.POSITIVE_INFINITY);
    }

    private PiercingProjectile createProjectile(int damage,
            int maximumTargets, boolean applyKnockback, double rangeTiles) {
        EntityPosition position = getEntityPosition();
        if (position == null) {
            return null;
        }
        List<ProjectileEffect> effects = new ArrayList<>();
        effects.add(new DamageEffect(Math.max(0, damage)));
        if (applyKnockback) {
            effects.add(new KnockbackEffect(
                    StrikeThroughPlantType.FUME_PLANT_FOOD_KNOCKBACK_TILES));
        }
        return new PiercingProjectile(getName(), position.getRow(),
                position.getColumn(), effects,
                new LinearProjectileMovement(ProjectileDirection.RIGHT,
                        StrikeThroughPlantType.PROJECTILE_SPEED_TILES_PER_SECOND),
                rangeTiles, maximumTargets);
    }

    public boolean canTarget(double zombieColumn, int zombieLane) {
        EntityPosition position = getEntityPosition();
        if (position == null || zombieLane != position.getRow()) {
            return false;
        }
        double distance = zombieColumn - position.getColumn();
        return distance > TIMER_EPSILON && distance <= getRangeTiles() + TIMER_EPSILON;
    }

    public boolean drainFamilyBoostPending() {
        boolean pending = familyBoostPending;
        familyBoostPending = false;
        return pending;
    }

    public void resetActionTimer() {
        secondsSinceLastAttack = getActionIntervalSeconds();
    }

    public StrikeThroughPlantType getType() {
        return type;
    }

    public float getActionIntervalSeconds() {
        return type.getActionIntervalSeconds(getLevel());
    }

    public float getRechargeSeconds() {
        return type.getRechargeSeconds(getLevel());
    }

    public double getRangeTiles() {
        return type.getRangeTiles(getLevel());
    }

    public int getMaximumTargets() {
        return type.getMaximumTargets(getLevel());
    }

    public float getFamilyBoostDurationSeconds() {
        return type.getFamilyBoostDurationSeconds(getLevel());
    }

    public boolean resetsFamilyCooldowns() {
        return type.resetsFamilyCooldowns(getLevel());
    }

    public boolean wasPlantFoodUsed() {
        return plantFoodUsed;
    }
}
