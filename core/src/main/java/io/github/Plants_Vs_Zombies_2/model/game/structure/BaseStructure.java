package io.github.Plants_Vs_Zombies_2.model.game.structure;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;

public class BaseStructure implements java.io.Serializable {
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
