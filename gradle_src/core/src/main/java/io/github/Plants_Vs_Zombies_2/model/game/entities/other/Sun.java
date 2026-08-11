package io.github.Plants_Vs_Zombies_2.model.game.entities.other;

import io.github.Plants_Vs_Zombies_2.model.Constants;
import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;

public class Sun extends Entity {
    private final float lifeSpanSeconds;
    private int sunAmount;
    private SunType type;
    private final boolean persistent;
    private float remainingFallSeconds;
    private double elapsedGroundSeconds;
    private boolean collected;
    private boolean landedEventPending;

    public Sun(int sunAmount, EntityPosition entityPosition) {
        this(sunAmount, entityPosition, Constants.DEFAULT_SUN_LIFESPAN_SECONDS);
    }

    public Sun(int sunAmount, EntityPosition entityPosition, float lifeSpanSeconds) {
        this(sunAmount, entityPosition, lifeSpanSeconds, null, 0.0f, false);
    }

    private Sun(int sunAmount, EntityPosition entityPosition, float lifeSpanSeconds,
            SunType type, float fallSeconds, boolean persistent) {
        super(entityPosition);
        if (sunAmount < 0 || type != SunType.RADIOACTIVE && sunAmount == 0) {
            throw new IllegalArgumentException("sunAmount must be positive");
        }
        if (!Float.isFinite(lifeSpanSeconds) || lifeSpanSeconds <= 0.0f) {
            throw new IllegalArgumentException("lifeSpanSeconds must be finite and positive");
        }
        if (!Float.isFinite(fallSeconds) || fallSeconds < 0.0f) {
            throw new IllegalArgumentException("fallSeconds must be finite and non-negative");
        }
        this.sunAmount = sunAmount;
        this.lifeSpanSeconds = lifeSpanSeconds;
        this.type = type;
        this.remainingFallSeconds = fallSeconds;
        this.persistent = persistent;
    }

    public static Sun createSkySun(SunType type, EntityPosition position) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        return new Sun(type.getAmount(), position, Constants.DEFAULT_SUN_LIFESPAN_SECONDS,
                type, Constants.SKY_SUN_FALL_SECONDS, false);
    }

    public static Sun createPlantSun(int sunAmount, EntityPosition position) {
        return new Sun(sunAmount, position, Constants.DEFAULT_SUN_LIFESPAN_SECONDS,
                null, 0.0f, true);
    }

    @Override
    public void update(float deltaSeconds) {
        if (isRemoved()) {
            return;
        }
        validateDeltaSeconds(deltaSeconds);
        super.update(deltaSeconds);

        float groundTimeThisUpdate = deltaSeconds;
        if (remainingFallSeconds > 0.0f) {
            if (deltaSeconds < remainingFallSeconds) {
                remainingFallSeconds -= deltaSeconds;
                groundTimeThisUpdate = 0.0f;
            } else {
                groundTimeThisUpdate = deltaSeconds - remainingFallSeconds;
                remainingFallSeconds = 0.0f;
                land();
            }
        }

        elapsedGroundSeconds += groundTimeThisUpdate;
        if (shouldDespawn()) {
            markForRemoval();
        }
    }

    private void land() {
        landedEventPending = true;
        if (type == SunType.RADIOACTIVE) {
            type = SunType.NORMAL;
            sunAmount = SunType.NORMAL.getAmount();
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

    public boolean collectRadioactiveWhileDropping() {
        if (collected || isRemoved() || !isRadioactive() || !isDropping()) {
            return false;
        }
        collected = true;
        markForRemoval();
        return true;
    }

    public boolean consumeLandedEvent() {
        if (!landedEventPending) {
            return false;
        }
        landedEventPending = false;
        return true;
    }

    public boolean shouldDespawn() {
        return !persistent && !isDropping() && elapsedGroundSeconds >= lifeSpanSeconds;
    }

    public boolean isCloseToDespawning() {
        double remainingSeconds = lifeSpanSeconds - elapsedGroundSeconds;
        return !persistent && !isDropping() && !shouldDespawn()
                && remainingSeconds <= Constants.SUN_DESPAWN_WARNING_SECONDS;
    }

    public boolean isDropping() {
        return remainingFallSeconds > 0.0f;
    }

    public boolean isCollectable() {
        return !collected && !isRemoved();
    }

    public boolean isRadioactive() {
        return type == SunType.RADIOACTIVE;
    }

    /**
     * Ra may steal a sun only after it has actually rested on the lawn.
     */
    public boolean isStealableFromGround() {
        return isCollectable() && !isDropping() && elapsedGroundSeconds > 0.0;
    }

    public double getElapsedGroundSeconds() {
        return Math.max(0.0, elapsedGroundSeconds);
    }

    public int getSunAmount() {
        return sunAmount;
    }

    public float getLifeSpanSeconds() {
        return lifeSpanSeconds;
    }

    public float getRemainingFallSeconds() {
        return Math.max(0.0f, remainingFallSeconds);
    }

    public boolean isCollected() {
        return collected;
    }

    public SunType getType() {
        return type;
    }

    public boolean isPersistent() {
        return persistent;
    }
}
