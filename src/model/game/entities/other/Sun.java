package model.game.entities.other;

import model.game.entities.Entity;

public class Sun extends Entity {
    private int lifeSpanInTicks;
    private int sunAmount;

    private boolean shouldDespawn() {
        return false;
    }

    private boolean isCloseToDespawning() {
        return false;
    }
}
