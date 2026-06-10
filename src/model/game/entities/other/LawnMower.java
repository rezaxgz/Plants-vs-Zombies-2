package model.game.entities.other;

import model.game.Board;
import model.game.entities.Entity;

public class LawnMower extends Entity {
    private boolean hasActivated;

    public int getRow() {
        return 0;
    }

    public void execute(Board board) {
        // kill all zombies
    }

    @Override
    public void tick() {
        // update position
        super.tick();
    }
}
