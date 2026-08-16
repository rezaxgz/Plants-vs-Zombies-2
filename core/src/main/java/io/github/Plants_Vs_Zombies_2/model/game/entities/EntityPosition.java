package io.github.Plants_Vs_Zombies_2.model.game.entities;

import java.util.Objects;

public final class EntityPosition implements java.io.Serializable {
    private final int row;
    private final int column;

    public EntityPosition(int row, int column) {
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException("row and column must be non-negative");
        }
        this.row = row;
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof EntityPosition)) {
            return false;
        }
        EntityPosition that = (EntityPosition) object;
        return row == that.row && column == that.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + column + ")";
    }
}
