package io.github.Plants_Vs_Zombies_2.model.game.entities.other;

import io.github.Plants_Vs_Zombies_2.model.Constants;
import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;

/**
 * A temporary item dropped on the lawn that can be collected by the player.
 */
public abstract class CollectibleDrop extends Entity {
    private final float lifeSpanSeconds;
    private boolean collected;

    protected CollectibleDrop(EntityPosition position) {
        this(position, Constants.DEFAULT_DROP_LIFESPAN_SECONDS);
    }

    protected CollectibleDrop(EntityPosition position, float lifeSpanSeconds) {
        super(position);
        if (position == null) {
            throw new IllegalArgumentException("position cannot be null");
        }
        if (!Float.isFinite(lifeSpanSeconds) || lifeSpanSeconds <= 0.0f) {
            throw new IllegalArgumentException("lifeSpanSeconds must be finite and positive");
        }
        this.lifeSpanSeconds = lifeSpanSeconds;
    }

    @Override
    public void update(float deltaSeconds) {
        if (isRemoved()) {
            return;
        }
        super.update(deltaSeconds);
        if (getElapsedSeconds() >= lifeSpanSeconds) {
            markForRemoval();
        }
    }

    public boolean collect() {
        if (collected || isRemoved()) {
            return false;
        }
        collected = true;
        markForRemoval();
        return true;
    }

    public float getLifeSpanSeconds() {
        return lifeSpanSeconds;
    }

    public boolean isCollected() {
        return collected;
    }
}
