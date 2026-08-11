package model.game.defense;

/**
 * One single-use lawn mower protecting a single board row.
 */
public final class LawnMower {
    private final int row;
    private boolean used;

    public LawnMower(int row) {
        if (row < 0) {
            throw new IllegalArgumentException(
                    "row cannot be negative");
        }
        this.row = row;
    }

    boolean trigger() {
        if (used) {
            return false;
        }
        used = true;
        return true;
    }

    public int getRow() {
        return row;
    }

    public boolean isUsed() {
        return used;
    }

    public boolean isAvailable() {
        return !used;
    }
}
