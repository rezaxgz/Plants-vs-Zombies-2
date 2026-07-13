package model.game.entities.plants.sunProducer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.game.entities.EntityPosition;
import model.game.entities.other.Sun;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;

public class SunProducer extends BasePlant {
    private static final double TIMER_EPSILON = 0.000001;

    private final SunProducerPlantType type;
    private final List<Sun> pendingSuns = new ArrayList<>();

    private double secondsSinceLastProduction;
    private boolean activated;

    public SunProducer() {
        this(SunProducerPlantType.SUNFLOWER, null);
    }

    public SunProducer(SunProducerPlantType type) {
        this(type, null);
    }

    public SunProducer(SunProducerPlantType type, EntityPosition entityPosition) {
        super(requireType(type).getDisplayName(), PlantCategory.SUN_PRODUCER,
                type.getTags(), 1, type.getCost(), type.getBaseHP(), type.getDamage(), entityPosition);
        this.type = type;
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
                activateFamilyBoostPlaceholder();
                break;
            default:
                throw new IllegalStateException("Unknown sun producer behavior: " + type.getBehavior());
        }
    }

    private void updatePeriodicProduction(float deltaSeconds) {
        float intervalSeconds = type.getActionIntervalSeconds();
        if (intervalSeconds <= 0.0f) {
            return;
        }

        secondsSinceLastProduction += deltaSeconds;
        while (secondsSinceLastProduction + TIMER_EPSILON >= intervalSeconds) {
            secondsSinceLastProduction -= intervalSeconds;
            double productionAgeSeconds = getElapsedSeconds() - secondsSinceLastProduction;
            produceSun(type.getSunAmountAt(productionAgeSeconds));
        }
    }

    private void activateInstantProducer() {
        if (activated) {
            return;
        }
        activated = true;
        produceSun(type.getSunAmountAt(getElapsedSeconds()));
        markForRemoval();
    }

    private void activateFamilyBoostPlaceholder() {
        if (activated) {
            return;
        }
        activated = true;
        // The plant-food/family system does not exist yet. The mint is still
        // consumed as an instant-use plant so it cannot remain on the board.
        markForRemoval();
    }

    private void produceSun(int amount) {
        if (amount > 0) {
            pendingSuns.add(new Sun(amount, getEntityPosition()));
        }
    }

    public List<Sun> drainProducedSuns() {
        if (pendingSuns.isEmpty()) {
            return Collections.emptyList();
        }
        List<Sun> produced = new ArrayList<>(pendingSuns);
        pendingSuns.clear();
        return produced;
    }

    public SunProducerPlantType getType() {
        return type;
    }

    public double getSecondsSinceLastProduction() {
        return secondsSinceLastProduction;
    }
}
