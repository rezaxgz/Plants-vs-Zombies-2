package io.github.Plants_Vs_Zombies_2.model.game.defense;

/**
 * One single-use lawn mower protecting a single board row.
 *
 * <p>After triggering, the mower remains active while it sweeps from the
 * house side to the far edge of the lawn. The sweep position is measured in
 * board-column units: {@code 0} is the house edge and increasing values move
 * to the right.</p>
 */
public final class LawnMower implements java.io.Serializable {
    private static final double SWEEP_SPEED_COLUMNS_PER_SECOND = 4.55;
    private static final double EXIT_MARGIN_COLUMNS = 0.75;

    private final int row;
    private boolean used;
    private boolean active;
    private double columnPosition;

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
        active = true;
        columnPosition = 0.0;
        return true;
    }

    void advance(float deltaSeconds, int boardColumnCount) {
        if (!active) {
            return;
        }
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException(
                    "deltaSeconds must be finite and non-negative");
        }
        if (boardColumnCount <= 0) {
            throw new IllegalArgumentException(
                    "boardColumnCount must be positive");
        }

        double endColumn = boardColumnCount + EXIT_MARGIN_COLUMNS;
        columnPosition = Math.min(endColumn,
                columnPosition
                        + SWEEP_SPEED_COLUMNS_PER_SECOND * deltaSeconds);
        if (columnPosition >= endColumn) {
            active = false;
        }
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

    /** Returns whether this mower is currently travelling across its row. */
    public boolean isActive() {
        return active;
    }

    /**
     * Current sweep position in board-column units. This is zero when the
     * mower first triggers and increases as it travels to the right.
     */
    public double getColumnPosition() {
        return columnPosition;
    }
}
