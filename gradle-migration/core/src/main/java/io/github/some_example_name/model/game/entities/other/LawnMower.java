package io.github.some_example_name.model.game.entities.other;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.entities.Entity;

public class LawnMower extends Entity {
    private boolean hasActivated;

    public int getRow() {
        return getEntityPosition() == null ? -1 : getEntityPosition().getRow();
    }

    public void execute(Board board) {
        // Kill all zombies in this lawn mower's row when zombie behavior exists.
        hasActivated = true;
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        // Position movement will also use deltaSeconds when implemented.
    }

    public boolean hasActivated() {
        return hasActivated;
    }
}
