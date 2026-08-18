package io.github.Plants_Vs_Zombies_2.model.game.defense;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.Constants;
import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.view.game.LawnMowerView;

/**
 * Owns one lawn mower per row and resolves zombies reaching the house.
 */
public final class LawnMowerSystem implements java.io.Serializable {
    /**
     * A small lead lets the mower's front edge hit a zombie while its visual
     * centre is still slightly to the left of that zombie.
     */
    private static final double COLLISION_LEAD_COLUMNS = 0.55;

    private final List<LawnMower> mowers;

    public LawnMowerSystem(int rowCount) {
        if (rowCount <= 0) {
            throw new IllegalArgumentException(
                    "rowCount must be positive");
        }
        mowers = new ArrayList<>();
        for (int row = 0; row < rowCount; row++) {
            mowers.add(new LawnMower(row));
        }
    }

    /**
     * Compatibility entry point for callers that still advance the game one
     * fixed terminal tick at a time.
     */
    public LawnMowerResolution resolve(Board board) {
        return resolve(board, Constants.ONE_TICK_IN_SECONDS);
    }

    /**
     * Advances any active mower and kills zombies only when the mower reaches
     * them. Triggering a mower no longer clears the entire row instantly.
     */
    public LawnMowerResolution resolve(Board board, float deltaSeconds) {
        if (board == null) {
            throw new IllegalArgumentException(
                    "board cannot be null");
        }
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException(
                    "deltaSeconds must be finite and non-negative");
        }
        if (board.getNumberOfRows() != mowers.size()) {
            throw new IllegalArgumentException(
                    "board row count does not match the mower system");
        }

        List<Zombie> snapshot = new ArrayList<>(board.getZombies());
        List<Integer> reachedRows = findReachedRows(snapshot);
        List<Integer> triggeredRows = new ArrayList<>();
        List<Integer> failedRows = new ArrayList<>();
        List<Zombie> killed = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // Reaching the house starts the mower. A row only fails after its
        // mower has already completed its one allowed sweep.
        for (int row : reachedRows) {
            LawnMower mower = mowers.get(row);
            if (mower.isAvailable()) {
                if (mower.trigger()) {
                    triggeredRows.add(row);
                }
            } else if (!mower.isActive()) {
                failedRows.add(row);
            }
        }

        // Every active mower advances a small amount this update. Zombies are
        // removed only once its moving front reaches their current position.
        for (LawnMower mower : mowers) {
            if (!mower.isActive()) {
                continue;
            }
            mower.advance(deltaSeconds, board.getNumberOfColumns());
            killed.addAll(killReachedZombies(snapshot, mower));
        }

        for (int row : triggeredRows) {
            messages.add(LawnMowerView.buildTriggerMessage(
                    row, killedInRow(killed, row)));
        }

        return new LawnMowerResolution(
                !failedRows.isEmpty(),
                failedRows,
                killed,
                messages);
    }

    private static List<Integer> findReachedRows(
            List<Zombie> zombies) {
        List<Integer> reachedRows = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (!isHouseThreat(zombie)
                    || reachedRows.contains(zombie.getLane())) {
                continue;
            }
            reachedRows.add(zombie.getLane());
        }
        Collections.sort(reachedRows);
        return reachedRows;
    }

    private static boolean isHouseThreat(Zombie zombie) {
        return zombie != null
                && !zombie.isDead()
                && !zombie.isRemoved()
                && !zombie.isHypnotized()
                && !zombie.getType().isBoss()
                && zombie.hasReachedHouse();
    }

    private static List<Zombie> killReachedZombies(
            List<Zombie> zombies, LawnMower mower) {
        List<Zombie> killed = new ArrayList<>();
        double collisionColumn = mower.getColumnPosition()
                + COLLISION_LEAD_COLUMNS;
        for (Zombie zombie : zombies) {
            if (zombie == null
                    || zombie.isDead()
                    || zombie.isRemoved()
                    || zombie.getLane() != mower.getRow()
                    || zombie.getType().isBoss()
                    || zombie.getColumnPosition() > collisionColumn) {
                continue;
            }
            zombie.clearDamageSourcePlant();
            zombie.kill();
            zombie.markForRemoval();
            killed.add(zombie);
        }
        return killed;
    }

    private static List<Zombie> killedInRow(
            List<Zombie> zombies, int row) {
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (zombie != null && zombie.getLane() == row) {
                result.add(zombie);
            }
        }
        return result;
    }

    public List<LawnMower> getMowers() {
        return Collections.unmodifiableList(
                new ArrayList<>(mowers));
    }

    public LawnMower getMowerAtRow(int row) {
        if (row < 0 || row >= mowers.size()) {
            throw new IllegalArgumentException(
                    "row is outside the mower system");
        }
        return mowers.get(row);
    }
}
