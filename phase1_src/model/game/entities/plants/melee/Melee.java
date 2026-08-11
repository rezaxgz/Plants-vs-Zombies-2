package model.game.entities.plants.melee;

import model.game.entities.EntityPosition;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;

public class Melee extends BasePlant {
    private static final double TIMER_EPSILON = 0.000001;
    private static final double SECOND_GROWTH_TIME_SECONDS = 24.0;
    private static final double THIRD_GROWTH_TIME_SECONDS = 72.0;
    private static final double FOURTH_GROWTH_TIME_SECONDS = 120.0;

    private final MeleePlantType type;
    private double attackTimerSeconds;
    private double digestRemainingSeconds;
    private boolean familyBoostPending;
    private boolean familyBoostActivated;
    private boolean plantFoodPending;
    private boolean plantFoodUsed;

    public Melee() {
        this(MeleePlantType.BONK_CHOY, 1, null);
    }

    public Melee(MeleePlantType type, EntityPosition position) {
        this(type, 1, position);
    }

    public Melee(MeleePlantType type, int level, EntityPosition position) {
        super(requireType(type).getDisplayName(), PlantCategory.MELEE,
                type.getTags(), level, type.getCost(level), type.getBaseHP(level),
                type.getDamage(level), position);
        MeleePlantType.validateLevel(level);
        this.type = type;
    }

    private static MeleePlantType requireType(MeleePlantType type) {
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
        if (type.getBehavior() == MeleeBehavior.FAMILY_BOOST) {
            activateFamilyBoost();
            return;
        }
        updateDigestion(deltaSeconds);
        if (type.getBehavior() != MeleeBehavior.CHOMPER) {
            attackTimerSeconds += deltaSeconds;
        }
    }

    private void activateFamilyBoost() {
        if (!familyBoostActivated) {
            familyBoostActivated = true;
            familyBoostPending = true;
        }
    }

    private void updateDigestion(float deltaSeconds) {
        if (digestRemainingSeconds > 0.0) {
            digestRemainingSeconds = Math.max(0.0, digestRemainingSeconds - deltaSeconds);
        }
    }

    public boolean isReadyToAttack() {
        if (type.getBehavior() == MeleeBehavior.CHOMPER
                || type.getBehavior() == MeleeBehavior.FAMILY_BOOST) {
            return false;
        }
        double interval = getActionIntervalSeconds();
        return interval > 0.0 && attackTimerSeconds + TIMER_EPSILON >= interval;
    }

    public void consumeAttack() {
        if (!isReadyToAttack()) {
            return;
        }
        attackTimerSeconds = Math.max(0.0,
                attackTimerSeconds - getActionIntervalSeconds());
    }

    public void retainSingleReadyAttack() {
        attackTimerSeconds = Math.min(attackTimerSeconds, getActionIntervalSeconds());
    }

    public boolean isReadyToBite() {
        return type.getBehavior() == MeleeBehavior.CHOMPER
                && digestRemainingSeconds <= TIMER_EPSILON;
    }

    public void startDigesting() {
        if (type.getBehavior() == MeleeBehavior.CHOMPER) {
            digestRemainingSeconds = getDigestTimeSeconds();
        }
    }

    public void usePlantFood() {
        if (isRemoved() || type.getBehavior() == MeleeBehavior.FAMILY_BOOST) {
            return;
        }
        plantFoodUsed = true;
        plantFoodPending = true;
    }

    public boolean drainPlantFoodPending() {
        boolean result = plantFoodPending;
        plantFoodPending = false;
        return result;
    }

    public boolean drainFamilyBoostPending() {
        boolean result = familyBoostPending;
        familyBoostPending = false;
        return result;
    }

    public void resetActionTimer() {
        digestRemainingSeconds = 0.0;
        attackTimerSeconds = getActionIntervalSeconds();
    }

    public int getGrowthStage() {
        int maximumStage = getMaximumGrowthStage();
        if (maximumStage >= 4 && getElapsedSeconds() + TIMER_EPSILON >= FOURTH_GROWTH_TIME_SECONDS) {
            return 4;
        }
        if (maximumStage >= 3 && getElapsedSeconds() + TIMER_EPSILON >= THIRD_GROWTH_TIME_SECONDS) {
            return 3;
        }
        if (maximumStage >= 2 && getElapsedSeconds() + TIMER_EPSILON >= SECOND_GROWTH_TIME_SECONDS) {
            return 2;
        }
        return 1;
    }

    public int getCurrentDamage() {
        if (type.getBehavior() == MeleeBehavior.GROWING_AREA) {
            return type.getGrowthStageDamage(getLevel(), getGrowthStage());
        }
        return getDamage();
    }

    public double getCurrentAttackRangeTiles() {
        if (type.getBehavior() == MeleeBehavior.GROWING_AREA) {
            return getAttackRangeTiles() + getGrowthStage() - 1;
        }
        return getAttackRangeTiles();
    }

    public MeleePlantType getType() {
        return type;
    }

    public float getActionIntervalSeconds() {
        return type.getActionIntervalSeconds(getLevel());
    }

    public float getRechargeSeconds() {
        return type.getRechargeSeconds(getLevel());
    }

    public double getAttackRangeTiles() {
        return type.getAttackRangeTiles(getLevel());
    }

    public double getDigestTimeSeconds() {
        return type.getDigestTimeSeconds(getLevel());
    }

    public double getDigestRemainingSeconds() {
        return Math.max(0.0, digestRemainingSeconds);
    }

    public boolean isDigesting() {
        return digestRemainingSeconds > TIMER_EPSILON;
    }

    public int getMaximumGrowthStage() {
        return type.getMaximumGrowthStage(getLevel());
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
