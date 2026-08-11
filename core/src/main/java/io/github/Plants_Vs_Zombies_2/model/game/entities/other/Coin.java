package io.github.Plants_Vs_Zombies_2.model.game.entities.other;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;

public final class Coin extends CollectibleDrop {
    public static final int AMOUNT = 50;

    public Coin() {
        this(new EntityPosition(0, 0));
    }

    public Coin(EntityPosition position) {
        super(position);
    }
}
