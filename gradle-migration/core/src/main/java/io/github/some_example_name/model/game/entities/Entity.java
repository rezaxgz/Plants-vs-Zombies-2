package io.github.some_example_name.model.game.entities;

public abstract class Entity {
    protected EntityPosition entityPosition;

    private double elapsedSeconds;
    private boolean removed;

    protected Entity() {
        this(null);
    }

    protected Entity(EntityPosition entityPosition) {
        this.entityPosition = entityPosition;
    }

    /**
     * Updates this entity using seconds. Tick conversion must happen before this
     * method is called.
     */
    public void update(float deltaSeconds) {
        validateDeltaSeconds(deltaSeconds);
        elapsedSeconds += deltaSeconds;
    }

    protected final void validateDeltaSeconds(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
    }

    public final double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public final EntityPosition getEntityPosition() {
        return entityPosition;
    }

    public final void setEntityPosition(EntityPosition entityPosition) {
        this.entityPosition = entityPosition;
    }

    public final boolean isRemoved() {
        return removed;
    }

    public final void markForRemoval() {
        removed = true;
    }
}
