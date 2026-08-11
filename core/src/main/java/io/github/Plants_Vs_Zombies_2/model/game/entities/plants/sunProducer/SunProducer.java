package io.github.Plants_Vs_Zombies_2.model.game.entities.plants.sunProducer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.Sun;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantCategory;

public class SunProducer extends BasePlant {
    private static final double TIMER_EPSILON = 0.000001;

    private final SunProducerPlantType type;
    private final List<Sun> pendingSuns = new ArrayList<>();
    private final List<Sun> activeProducedSuns = new ArrayList<>();
    private final Random random;

    private double secondsSinceLastProduction;
    private boolean activated;
    private boolean fullyGrown;
    private boolean familyBoostPending;
    private boolean plantFoodUsed;

    public SunProducer() {
        this(SunProducerPlantType.SUNFLOWER, 1, null);
    }

    public SunProducer(SunProducerPlantType type) {
        this(type, 1, null);
    }

    public SunProducer(SunProducerPlantType type, EntityPosition entityPosition) {
        this(type, 1, entityPosition);
    }

    public SunProducer(SunProducerPlantType type, int level, EntityPosition entityPosition) {
        this(type, level, entityPosition, new Random());
    }

    public SunProducer(SunProducerPlantType type, int level,
            EntityPosition entityPosition, Random random) {
        super(requireType(type).getDisplayName(), PlantCategory.SUN_PRODUCER,
                type.getTags(), level, type.getCost(level), type.getBaseHP(level),
                type.getDamage(), entityPosition);
        SunProducerPlantType.validateLevel(level);
        if (random == null) {
            throw new IllegalArgumentException("random cannot be null");
        }
        this.type = type;
        this.random = random;
    }

    private static SunProducerPlantType requireType(SunProducerPlantType type) {
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

        switch (type.getBehavior()) {
            case PERIODIC:
                updatePeriodicProduction(deltaSeconds);
                break;
            case INSTANT:
                activateInstantProducer();
                break;
            case FAMILY_BOOST:
                activateFamilyBoost();
                break;
            default:
                throw new IllegalStateException("Unknown sun producer behavior: " + type.getBehavior());
        }
    }

    private void updatePeriodicProduction(float deltaSeconds) {
        updateNaturalGrowthState();
        if (isWaitingForProducedSun()) {
            return;
        }

        float intervalSeconds = type.getActionIntervalSeconds(getLevel());
        if (intervalSeconds <= 0.0f) {
            return;
        }

        secondsSinceLastProduction += deltaSeconds;
        if (secondsSinceLastProduction + TIMER_EPSILON >= intervalSeconds) {
            secondsSinceLastProduction = 0.0;
            int amount = fullyGrown
                    ? type.getFinalSunAmount(getLevel())
                    : type.getSunAmountAt(getElapsedSeconds(), getLevel());
            produceSun(amount, true);
        }
    }

    private void updateNaturalGrowthState() {
        if (type == SunProducerPlantType.SUN_SHROOM
                && type.getSunAmountAt(getElapsedSeconds(), getLevel()) == type.getFinalSunAmount(getLevel())) {
            fullyGrown = true;
        }
    }

    private void activateInstantProducer() {
        if (activated) {
            return;
        }
        activated = true;
        produceSun(type.getSunAmountAt(getElapsedSeconds(), getLevel()), false);
        markForRemoval();
    }

    private void activateFamilyBoost() {
        if (!activated) {
            activated = true;
            familyBoostPending = true;
        }
    }

    private void produceSun(int amount, boolean allowDoubleSun) {
        int producedAmount = amount;
        if (allowDoubleSun && type.hasDoubleSunChance(getLevel())
                && random.nextDouble() < SunProducerPlantType.DOUBLE_SUN_CHANCE) {
            producedAmount *= 2;
        }
        if (producedAmount <= 0) {
            return;
        }

        Sun sun = Sun.createPlantSun(producedAmount, getEntityPosition());
        activeProducedSuns.add(sun);
        pendingSuns.add(sun);
    }

    public void usePlantFood() {
        if (isRemoved() || type.getPlantFoodSunAmount() <= 0) {
            return;
        }
        plantFoodUsed = true;
        if (type == SunProducerPlantType.SUN_SHROOM) {
            fullyGrown = true;
        }
        produceSun(type.getPlantFoodSunAmount(), false);
    }

    public List<Sun> drainProducedSuns() {
        if (pendingSuns.isEmpty()) {
            return Collections.emptyList();
        }
        List<Sun> produced = new ArrayList<>(pendingSuns);
        pendingSuns.clear();
        return produced;
    }

    public void resetActionTimer() {
        secondsSinceLastProduction = type.getActionIntervalSeconds(getLevel());
    }

    public boolean drainFamilyBoostPending() {
        boolean result = familyBoostPending;
        familyBoostPending = false;
        return result;
    }

    public SunProducerPlantType getType() {
        return type;
    }

    public double getSecondsSinceLastProduction() {
        return secondsSinceLastProduction;
    }

    public boolean isWaitingForProducedSun() {
        activeProducedSuns.removeIf(Sun::isRemoved);
        return !activeProducedSuns.isEmpty();
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

    public boolean isFullyGrown() {
        return fullyGrown;
    }
}
