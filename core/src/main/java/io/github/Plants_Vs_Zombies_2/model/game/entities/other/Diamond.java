package io.github.Plants_Vs_Zombies_2.model.game.entities.other;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;

public final class Diamond extends CollectibleDrop {
    public static final int AMOUNT = 1;

    public Diamond() {
        this(new EntityPosition(0, 0));
    }

    public Diamond(EntityPosition position) {
        super(position);
    }
}
