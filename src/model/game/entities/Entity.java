package model.game.entities;

public abstract class Entity {
    protected EntityPosition entityPosition;
    protected int ticksRecieved;

    public void tick() {
        ticksRecieved++;
    }
}
