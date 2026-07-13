package model.game.entities.other;

import model.Constants;
import model.game.entities.Entity;
import model.game.entities.EntityPosition;

public class Sun extends Entity {
    private final float lifeSpanSeconds;
    private final int sunAmount;
    private boolean collected;

    public Sun(int sunAmount, EntityPosition entityPosition) {
        this(sunAmount, entityPosition, Constants.DEFAULT_SUN_LIFESPAN_SECONDS);
    }

    public Sun(int sunAmount, EntityPosition entityPosition, float lifeSpanSeconds) {
        super(entityPosition);
        if (sunAmount <= 0) {
            throw new IllegalArgumentException("sunAmount must be positive");
        }
        if (!Float.isFinite(lifeSpanSeconds) || lifeSpanSeconds <= 0.0f) {
            throw new IllegalArgumentException("lifeSpanSeconds must be finite and positive");
        }
        this.sunAmount = sunAmount;
        this.lifeSpanSeconds = lifeSpanSeconds;
    }

    @Override
    public void update(float deltaSeconds) {
        if (isRemoved()) {
            return;
        }
        super.update(deltaSeconds);
        if (shouldDespawn()) {
            markForRemoval();
        }
    }

    public int collect() {
        if (collected || isRemoved()) {
            return 0;
        }
        collected = true;
        markForRemoval();
        return sunAmount;
    }

    public boolean shouldDespawn() {
        return getElapsedSeconds() >= lifeSpanSeconds;
    }

    public boolean isCloseToDespawning() {
        double remainingSeconds = lifeSpanSeconds - getElapsedSeconds();
        return !shouldDespawn() && remainingSeconds <= Constants.SUN_DESPAWN_WARNING_SECONDS;
    }

    public int getSunAmount() {
        return sunAmount;
    }

    public float getLifeSpanSeconds() {
        return lifeSpanSeconds;
    }

    public boolean isCollected() {
        return collected;
    }
}
