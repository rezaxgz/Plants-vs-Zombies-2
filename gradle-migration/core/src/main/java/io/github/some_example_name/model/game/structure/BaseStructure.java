package io.github.some_example_name.model.game.structure;

import io.github.some_example_name.model.game.entities.EntityPosition;

public class BaseStructure {
    private final EntityPosition position;
    private boolean removed;

    public BaseStructure() {
        this(null);
    }

    public BaseStructure(EntityPosition position) {
        this.position = position;
    }

    public EntityPosition getPosition() {
        return position;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void markForRemoval() {
        removed = true;
    }
}
