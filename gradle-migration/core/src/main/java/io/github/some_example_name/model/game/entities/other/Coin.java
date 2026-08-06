package io.github.some_example_name.model.game.entities.other;

import io.github.some_example_name.model.game.entities.EntityPosition;

public final class Coin extends CollectibleDrop {
    public static final int AMOUNT = 50;

    public Coin() {
        this(new EntityPosition(0, 0));
    }

    public Coin(EntityPosition position) {
        super(position);
    }
}
