package model.game.defense;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.game.entities.zombies.Zombie;

/**
 * Immutable outcome of checking every lawn mower after a board update.
 */
public final class LawnMowerResolution {
    private final boolean brainEaten;
    private final List<Integer> failedRows;
    private final List<Zombie> killedZombies;
    private final List<String> messages;

    LawnMowerResolution(boolean brainEaten,
            List<Integer> failedRows,
            List<Zombie> killedZombies,
            List<String> messages) {
        this.brainEaten = brainEaten;
        this.failedRows = immutableCopy(failedRows);
        this.killedZombies = immutableCopy(killedZombies);
        this.messages = immutableCopy(messages);
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return Collections.unmodifiableList(
                new ArrayList<>(values));
    }

    public boolean isBrainEaten() {
        return brainEaten;
    }

    public List<Integer> getFailedRows() {
        return failedRows;
    }

    public List<Zombie> getKilledZombies() {
        return killedZombies;
    }

    public List<String> getMessages() {
        return messages;
    }
}
