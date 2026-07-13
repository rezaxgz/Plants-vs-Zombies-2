package model.game.entities.other;

import model.Constants;
import model.game.entities.Entity;
import model.game.entities.EntityPosition;

public class Sun extends Entity {
    private final float lifeSpanSeconds;
    private final int sunAmount;
    private final SunType type;
    private float remainingFallSeconds;
    private double elapsedGroundSeconds;
    private boolean collected;

    public Sun(int sunAmount, EntityPosition entityPosition) {
        this(sunAmount, entityPosition, Constants.DEFAULT_SUN_LIFESPAN_SECONDS);
    }

    public Sun(int sunAmount, EntityPosition entityPosition, float lifeSpanSeconds) {
        this(sunAmount, entityPosition, lifeSpanSeconds, null, 0.0f);
    }

    private Sun(int sunAmount, EntityPosition entityPosition, float lifeSpanSeconds,
            SunType type, float fallSeconds) {
        super(entityPosition);
        if (sunAmount <= 0) {
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
    }

    public static Sun createSkySun(SunType type, EntityPosition position) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        return new Sun(type.getAmount(), position, Constants.DEFAULT_SUN_LIFESPAN_SECONDS,
                type, Constants.SKY_SUN_FALL_SECONDS);
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
            }
        }

        elapsedGroundSeconds += groundTimeThisUpdate;
        if (shouldDespawn()) {
            markForRemoval();
        }
    }

    public int collect() {
        if (collected || isRemoved() || isDropping()) {
            return 0;
        }
        collected = true;
        markForRemoval();
        return sunAmount;
    }

    public boolean shouldDespawn() {
        return !isDropping() && elapsedGroundSeconds >= lifeSpanSeconds;
    }

    public boolean isCloseToDespawning() {
        double remainingSeconds = lifeSpanSeconds - elapsedGroundSeconds;
        return !isDropping() && !shouldDespawn()
                && remainingSeconds <= Constants.SUN_DESPAWN_WARNING_SECONDS;
    }

    public boolean isDropping() {
        return remainingFallSeconds > 0.0f;
    }

    public boolean isCollectable() {
        return !isDropping() && !collected && !isRemoved();
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

    public SunType getType() {
        return type;
    }
}
