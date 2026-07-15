package model.game.structure;

import model.game.entities.EntityPosition;

public class Grave extends BaseStructure {
    public static final int DEFAULT_HIT_POINTS = 700;

    private int hitPoints;

    public Grave() {
        this(null);
    }

    public Grave(EntityPosition position) {
        super(position);
        hitPoints = DEFAULT_HIT_POINTS;
    }

    public int getHitPoints() {
        return hitPoints;
    }

    public void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        hitPoints = Math.max(0, hitPoints - damage);
        if (hitPoints == 0) {
            markForRemoval();
        }
    }
}
