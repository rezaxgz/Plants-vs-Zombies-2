package model.game.defense;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Owns one lawn mower per row and resolves zombies reaching the house.
 */
public final class LawnMowerSystem {
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

    public LawnMowerResolution resolve(Board board) {
        if (board == null) {
            throw new IllegalArgumentException(
                    "board cannot be null");
        }
        if (board.getNumberOfRows() != mowers.size()) {
            throw new IllegalArgumentException(
                    "board row count does not match the mower system");
        }

        List<Zombie> snapshot =
                new ArrayList<>(board.getZombies());
        List<Integer> reachedRows =
                findReachedRows(snapshot);
        List<Integer> failedRows = new ArrayList<>();
        List<Zombie> killed = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        for (int row : reachedRows) {
            LawnMower mower = mowers.get(row);
            if (!mower.trigger()) {
                failedRows.add(row);
                continue;
            }

            List<Zombie> rowKills =
                    killNonBossZombiesInRow(snapshot, row);
            killed.addAll(rowKills);
            messages.add(buildTriggerMessage(row, rowKills));
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

    private static List<Zombie> killNonBossZombiesInRow(
            List<Zombie> zombies, int row) {
        List<Zombie> killed = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (zombie == null
                    || zombie.isDead()
                    || zombie.isRemoved()
                    || zombie.getLane() != row
                    || zombie.getType().isBoss()) {
                continue;
            }
            zombie.kill();
            zombie.markForRemoval();
            killed.add(zombie);
        }
        return killed;
    }

    private static String buildTriggerMessage(
            int row, List<Zombie> killed) {
        StringBuilder message = new StringBuilder();
        message.append("The lawn mower in the row ")
                .append(row)
                .append(" is triggered and killed these zombies:");

        if (killed.isEmpty()) {
            message.append(System.lineSeparator())
                    .append("- none");
            return message.toString();
        }

        for (Zombie zombie : killed) {
            message.append(System.lineSeparator())
                    .append("- ")
                    .append(zombie.getName());
        }
        return message.toString();
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
