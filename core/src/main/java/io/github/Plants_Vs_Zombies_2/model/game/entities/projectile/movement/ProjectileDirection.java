package io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.movement;

public enum ProjectileDirection {
    RIGHT(0.0, 1.0),
    LEFT(0.0, -1.0),
    UP_RIGHT(-1.0, 1.0),
    DOWN_RIGHT(1.0, 1.0),
    UP_LEFT(-1.0, -1.0),
    DOWN_LEFT(1.0, -1.0);

    private final double rowComponent;
    private final double columnComponent;

    ProjectileDirection(double rowComponent, double columnComponent) {
        double length = Math.sqrt(rowComponent * rowComponent
                + columnComponent * columnComponent);
        this.rowComponent = rowComponent / length;
        this.columnComponent = columnComponent / length;
    }

    public double getRowComponent() {
        return rowComponent;
    }

    public double getColumnComponent() {
        return columnComponent;
    }
}
