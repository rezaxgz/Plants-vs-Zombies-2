package io.github.some_example_name.model.game.entities.plants.explosive;

import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.plants.PlantCategory;

public class Explosive extends BasePlant {
    private static final double TIMER_EPSILON = 0.000001;
    private static final int PLANT_FOOD_CLONE_COUNT = 2;
    private static final int PLANT_FOOD_SQUASH_TARGETS = 2;
    private static final int PLANT_FOOD_KELP_TARGETS = 3;

    private final ExplosivePlantType type;
    private double actionTimerSeconds;
    private boolean armed;
    private boolean activationPending;
    private boolean familyBoostPending;
    private boolean cloneMinesPending;
    private boolean globalFreezePending;
    private boolean plantFoodSquashPending;
    private boolean plantFoodKelpPending;
    private boolean plantFoodUsed;
    private int remainingActivations;

    public Explosive() {
        this(ExplosivePlantType.CHERRY_BOMB, 1, null);
    }

    public Explosive(ExplosivePlantType type, EntityPosition position) {
        this(type, 1, position);
    }

    public Explosive(ExplosivePlantType type, int level, EntityPosition position) {
        this(type, level, position, false);
    }

    private Explosive(ExplosivePlantType type, int level,
            EntityPosition position, boolean startArmed) {
        super(requireType(type).getDisplayName(), PlantCategory.EXPLOSIVE,
                type.getTags(), level, type.getCost(level), type.getBaseHP(level),
                type.getDamage(level), position);
        ExplosivePlantType.validateLevel(level);
        this.type = type;
        this.remainingActivations = type.getMaximumActivations(level);
        this.armed = startArmed || isInitiallyArmedBehavior(type.getBehavior());
    }

    private static ExplosivePlantType requireType(ExplosivePlantType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        return type;
    }

    private static boolean isInitiallyArmedBehavior(ExplosiveBehavior behavior) {
        return behavior == ExplosiveBehavior.SQUASH
                || behavior == ExplosiveBehavior.WATER_TRAP
                || behavior == ExplosiveBehavior.FREEZE_TRAP;
    }

    public static Explosive createArmedClone(ExplosivePlantType type,
            int level, EntityPosition position) {
        if (type != ExplosivePlantType.POTATO_MINE
                && type != ExplosivePlantType.PRIMAL_POTATO_MINE) {
            throw new IllegalArgumentException("only potato mines can be cloned");
        }
        return new Explosive(type, level, position, true);
    }

    @Override
    public void update(float deltaSeconds) {
        if (isRemoved()) {
            return;
        }
        super.update(deltaSeconds);
        updateTimedBehavior(deltaSeconds);
    }

    private void updateTimedBehavior(float deltaSeconds) {
        switch (type.getBehavior()) {
            case CONTACT_MINE:
            case AREA_CONTACT_MINE:
                updateArming(deltaSeconds);
                break;
            case CONSUME_GRAVE:
                updateGraveEating(deltaSeconds);
                break;
            case FAMILY_BOOST:
                familyBoostPending = true;
                break;
            case INSTANT_AREA:
            case GRAPESHOT:
            case LANE_FIRE:
            case WHOLE_BOARD:
            case WHOLE_BOARD_FREEZE:
            case MELT_ICE:
                activationPending = true;
                break;
            default:
                break;
        }
    }

    private void updateArming(float deltaSeconds) {
        if (armed) {
            return;
        }
        actionTimerSeconds += deltaSeconds;
        if (actionTimerSeconds + TIMER_EPSILON >= type.getArmTimeSeconds(getLevel())) {
            armed = true;
        }
    }

    private void updateGraveEating(float deltaSeconds) {
        actionTimerSeconds += deltaSeconds;
        if (actionTimerSeconds + TIMER_EPSILON >= type.getEatTimeSeconds(getLevel())) {
            activationPending = true;
        }
    }

    public boolean canTriggerOnContact() {
        return armed && !activationPending && remainingActivations > 0
                && isContactBehavior(type.getBehavior());
    }

    private static boolean isContactBehavior(ExplosiveBehavior behavior) {
        return behavior == ExplosiveBehavior.CONTACT_MINE
                || behavior == ExplosiveBehavior.AREA_CONTACT_MINE
                || behavior == ExplosiveBehavior.SQUASH
                || behavior == ExplosiveBehavior.WATER_TRAP
                || behavior == ExplosiveBehavior.FREEZE_TRAP;
    }

    public void trigger() {
        if (canTriggerOnContact()) {
            activationPending = true;
        }
    }

    public boolean drainActivationPending() {
        boolean result = activationPending;
        activationPending = false;
        return result;
    }

    public void finishActivation() {
        if (type.getBehavior() == ExplosiveBehavior.SQUASH && remainingActivations > 1) {
            remainingActivations--;
            armed = true;
            return;
        }
        remainingActivations = Math.max(0, remainingActivations - 1);
        markForRemoval();
    }

    public void usePlantFood() {
        if (isRemoved() || type.getBehavior() == ExplosiveBehavior.FAMILY_BOOST) {
            return;
        }
        plantFoodUsed = true;
        switch (type.getBehavior()) {
            case CONTACT_MINE:
            case AREA_CONTACT_MINE:
                armed = true;
                cloneMinesPending = true;
                break;
            case SQUASH:
                plantFoodSquashPending = true;
                break;
            case WATER_TRAP:
                plantFoodKelpPending = true;
                break;
            case FREEZE_TRAP:
                globalFreezePending = true;
                break;
            default:
                break;
        }
    }

    public boolean drainFamilyBoostPending() {
        boolean result = familyBoostPending;
        familyBoostPending = false;
        return result;
    }

    public int drainCloneMineCount() {
        if (!cloneMinesPending) {
            return 0;
        }
        cloneMinesPending = false;
        return PLANT_FOOD_CLONE_COUNT;
    }

    public boolean drainGlobalFreezePending() {
        boolean result = globalFreezePending;
        globalFreezePending = false;
        return result;
    }

    public int drainPlantFoodSquashTargetCount() {
        if (!plantFoodSquashPending) {
            return 0;
        }
        plantFoodSquashPending = false;
        return PLANT_FOOD_SQUASH_TARGETS;
    }

    public int drainPlantFoodKelpTargetCount() {
        if (!plantFoodKelpPending) {
            return 0;
        }
        plantFoodKelpPending = false;
        return PLANT_FOOD_KELP_TARGETS + type.getTargetCount(getLevel()) - 1;
    }

    public void resetActionTimer() {
        actionTimerSeconds = 0.0;
        if (type.getBehavior() == ExplosiveBehavior.CONTACT_MINE
                || type.getBehavior() == ExplosiveBehavior.AREA_CONTACT_MINE) {
            armed = true;
        }
    }

    public ExplosivePlantType getType() {
        return type;
    }

    public boolean isArmed() {
        return armed;
    }

    public int getRemainingActivations() {
        return remainingActivations;
    }

    public float getRechargeSeconds() {
        return type.getRechargeSeconds(getLevel());
    }

    public double getArmTimeSeconds() {
        return type.getArmTimeSeconds(getLevel());
    }

    public int getGrapeBounceCount() {
        return type.getGrapeBounceCount(getLevel());
    }

    public int getTargetCount() {
        return type.getTargetCount(getLevel());
    }

    public double getFreezeDurationSeconds() {
        return type.getFreezeDurationSeconds(getLevel());
    }

    public int getMeltRadius() {
        return type.getMeltRadius(getLevel());
    }

    public double getEatTimeSeconds() {
        return type.getEatTimeSeconds(getLevel());
    }

    public boolean explodesOnFinish() {
        return type.explodesOnFinish(getLevel());
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
