package io.github.some_example_name.model.game.entities.plants.wallnut;

import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.plants.PlantCategory;

public class Wallnut extends BasePlant {
    private final WallnutPlantType type;
    private int armorHP;
    private int bonusReflectDamage;
    private int pendingSunAmount;
    private int pendingExplosionDamage;
    private boolean familyBoostPending;
    private boolean divertLanePending;
    private boolean divertAllPending;
    private boolean attractAllPending;
    private boolean plantFoodUsed;
    private boolean coreExplosionTriggered;
    private double pendingReflectedDamage;
    private boolean divertUpwardNext = true;

    public Wallnut() {
        this(WallnutPlantType.WALL_NUT, 1, null);
    }

    public Wallnut(WallnutPlantType type) {
        this(type, 1, null);
    }

    public Wallnut(WallnutPlantType type, EntityPosition position) {
        this(type, 1, position);
    }

    public Wallnut(WallnutPlantType type, int level, EntityPosition position) {
        super(requireType(type).getDisplayName(), PlantCategory.WALL_NUT, type.getTags(), level,
                type.getCost(level), type.getBaseHP(level), type.getDamage(level), position);
        WallnutPlantType.validateLevel(level);
        this.type = type;
    }

    private static WallnutPlantType requireType(WallnutPlantType type) {
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
        if (type.getBehavior() == WallnutBehavior.FAMILY_BOOST && !familyBoostPending) {
            familyBoostPending = true;
        }
    }

    @Override
    public void takeDamage(int damageAmount) {
        if (damageAmount < 0) {
            throw new IllegalArgumentException("damageAmount cannot be negative");
        }
        if (damageAmount == 0 || isDestroyed()) {
            return;
        }

        int remainingDamage = absorbArmor(damageAmount);
        if (remainingDamage > 0) {
            super.takeDamage(remainingDamage);
        }
        registerOnHitEffects();
        registerCoreExplosion();
    }

    private int absorbArmor(int damageAmount) {
        if (armorHP <= 0) {
            return damageAmount;
        }

        int armorBeforeHit = armorHP;
        int absorbedDamage = Math.min(armorHP, damageAmount);
        armorHP -= absorbedDamage;
        if (armorBeforeHit > 0 && armorHP == 0
                && type.getBehavior() == WallnutBehavior.EXPLOSIVE) {
            pendingExplosionDamage += getDamage();
        }
        return damageAmount - absorbedDamage;
    }

    private void registerOnHitEffects() {
        if (type.getBehavior() == WallnutBehavior.SUN_ON_HIT) {
            pendingSunAmount += type.getSunPerHit(getLevel());
        } else if (type.getBehavior() == WallnutBehavior.LANE_DIVERSION) {
            divertLanePending = true;
        }
    }

    private void registerCoreExplosion() {
        if (isDestroyed() && type.getBehavior() == WallnutBehavior.EXPLOSIVE
                && !coreExplosionTriggered) {
            coreExplosionTriggered = true;
            pendingExplosionDamage += getDamage();
        }
    }

    public void usePlantFood() {
        if (isRemoved() || type.getBehavior() == WallnutBehavior.FAMILY_BOOST) {
            return;
        }
        plantFoodUsed = true;
        applyPlantFoodArmor();
        applySpecialPlantFoodEffect();
    }

    private void applyPlantFoodArmor() {
        int armorAmount = type.getPlantFoodArmor();
        if (armorAmount > 0) {
            armorHP += armorAmount;
        }
    }

    private void applySpecialPlantFoodEffect() {
        switch (type.getBehavior()) {
            case REFLECTIVE:
                bonusReflectDamage += type.getPlantFoodReflectBonus();
                break;
            case LANE_DIVERSION:
                divertAllPending = true;
                break;
            case LANE_ATTRACTOR:
                restoreHealth();
                attractAllPending = true;
                break;
            default:
                break;
        }
    }

    public int calculateReflectedDamage(float deltaSeconds) {
        if (type.getBehavior() != WallnutBehavior.REFLECTIVE || deltaSeconds <= 0.0f) {
            return 0;
        }
        pendingReflectedDamage += (getDamage() + bonusReflectDamage) * deltaSeconds;
        int reflectedDamage = (int) pendingReflectedDamage;
        pendingReflectedDamage -= reflectedDamage;
        return reflectedDamage;
    }

    public int chooseAdjacentLane(int currentLane, int laneCount) {
        if (laneCount <= 1) {
            return currentLane;
        }
        if (currentLane <= 0) {
            return 1;
        }
        if (currentLane >= laneCount - 1) {
            return laneCount - 2;
        }
        int newLane = divertUpwardNext ? currentLane - 1 : currentLane + 1;
        divertUpwardNext = !divertUpwardNext;
        return newLane;
    }

    public boolean drainFamilyBoostPending() {
        boolean result = familyBoostPending;
        familyBoostPending = false;
        return result;
    }

    public boolean drainDivertLanePending() {
        boolean result = divertLanePending;
        divertLanePending = false;
        return result;
    }

    public boolean drainDivertAllPending() {
        boolean result = divertAllPending;
        divertAllPending = false;
        return result;
    }

    public boolean drainAttractAllPending() {
        boolean result = attractAllPending;
        attractAllPending = false;
        return result;
    }

    public int drainPendingSunAmount() {
        int result = pendingSunAmount;
        pendingSunAmount = 0;
        return result;
    }

    public int drainPendingExplosionDamage() {
        int result = pendingExplosionDamage;
        pendingExplosionDamage = 0;
        return result;
    }

    public boolean isCoverPlant() {
        return type.getBehavior() == WallnutBehavior.COVER;
    }

    public boolean blocksJumpingZombies() {
        return type.getBehavior() == WallnutBehavior.TALL_BLOCKER;
    }

    public boolean attractsAdjacentLanes() {
        return type.getBehavior() == WallnutBehavior.LANE_ATTRACTOR;
    }

    public int getArmorHP() {
        return armorHP;
    }

    public int getReflectDamagePerSecond() {
        return getDamage() + bonusReflectDamage;
    }

    public float getRechargeSeconds() {
        return type.getRechargeSeconds(getLevel());
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

    public WallnutPlantType getType() {
        return type;
    }
}
