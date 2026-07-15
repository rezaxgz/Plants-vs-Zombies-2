package model.game.entities.plants.homing;

import java.util.Collections;

import model.game.entities.EntityPosition;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;
import model.game.entities.projectile.HomingProjectile;
import model.game.entities.projectile.effect.DamageEffect;
import model.game.entities.projectile.effect.HypnotizeEffect;
import model.game.entities.projectile.effect.ProjectileEffect;
import model.game.entities.zombies.Zombie;

public class Homing extends BasePlant {
    private static final double TIMER_EPSILON = 0.000001;

    private final HomingPlantType type;

    private double secondsSinceLastAttack;
    private boolean familyBoostActivated;
    private boolean familyBoostPending;
    private boolean plantFoodPending;
    private boolean plantFoodUsed;

    public Homing() {
        this(HomingPlantType.CAT_TAIL, 1, null);
    }

    public Homing(HomingPlantType type, EntityPosition entityPosition) {
        this(type, 1, entityPosition);
    }

    public Homing(HomingPlantType type, int level, EntityPosition entityPosition) {
        super(requireType(type).getDisplayName(), PlantCategory.HOMING,
                type.getTags(), level, type.getCost(level), type.getBaseHP(level),
                type.getDamage(level), entityPosition);
        HomingPlantType.validateLevel(level);
        this.type = type;
    }

    private static HomingPlantType requireType(HomingPlantType type) {
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
        if (type.getBehavior() == HomingBehavior.FAMILY_BOOST) {
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
                && type.getBehavior() != HomingBehavior.FAMILY_BOOST
                && interval > 0.0f
                && secondsSinceLastAttack + TIMER_EPSILON >= interval;
    }

    public HomingProjectile shoot(Zombie target) {
        if (!isReadyToAttack() || !usesProjectile() || !isTargetAvailable(target)) {
            return null;
        }
        secondsSinceLastAttack = 0.0;
        return createProjectile(target, false);
    }

    public boolean consumeDirectAttackReadiness(Zombie target) {
        if (!isReadyToAttack() || usesProjectile() || !isTargetAvailable(target)) {
            return false;
        }
        secondsSinceLastAttack = 0.0;
        return true;
    }

    private boolean usesProjectile() {
        return type.getBehavior() == HomingBehavior.HYPNOTIZE
                || type.getBehavior() == HomingBehavior.GUIDED_PROJECTILE;
    }

    private static boolean isTargetAvailable(Zombie target) {
        return target != null && !target.isDead() && !target.isRemoved()
                && !target.isHypnotized();
    }

    public HomingProjectile createPlantFoodProjectile(Zombie target) {
        if (!isTargetAvailable(target) || !usesProjectile()) {
            return null;
        }
        return createProjectile(target, true);
    }

    private HomingProjectile createProjectile(Zombie target, boolean plantFood) {
        EntityPosition position = getEntityPosition();
        if (position == null) {
            return null;
        }
        ProjectileEffect effect;
        if (type.getBehavior() == HomingBehavior.HYPNOTIZE) {
            effect = new HypnotizeEffect();
        } else {
            effect = new DamageEffect(getDamage());
        }
        return new HomingProjectile(getName(), position.getRow(),
                position.getColumn(), target, Collections.singletonList(effect),
                HomingPlantType.PROJECTILE_SPEED_TILES_PER_SECOND,
                HomingPlantType.PROJECTILE_MAX_LIFETIME_SECONDS, plantFood);
    }

    public void usePlantFood() {
        if (isRemoved() || type.getBehavior() == HomingBehavior.FAMILY_BOOST) {
            return;
        }
        plantFoodUsed = true;
        plantFoodPending = true;
    }

    public boolean drainPlantFoodPending() {
        boolean pending = plantFoodPending;
        plantFoodPending = false;
        return pending;
    }

    public boolean drainFamilyBoostPending() {
        boolean pending = familyBoostPending;
        familyBoostPending = false;
        return pending;
    }

    public void resetActionTimer() {
        secondsSinceLastAttack = getActionIntervalSeconds();
    }

    public HomingPlantType getType() {
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

    public int getPlantFoodTargetCount() {
        return type.getPlantFoodTargetCount();
    }

    public float getFamilyBoostDurationSeconds() {
        return type.getFamilyBoostDurationSeconds(getLevel());
    }

    public boolean hasTargetPriorityUp() {
        return type.hasTargetPriorityUp(getLevel());
    }

    public boolean resetsFamilyCooldowns() {
        return type.resetsFamilyCooldowns(getLevel());
    }

    public boolean wasPlantFoodUsed() {
        return plantFoodUsed;
    }
}
