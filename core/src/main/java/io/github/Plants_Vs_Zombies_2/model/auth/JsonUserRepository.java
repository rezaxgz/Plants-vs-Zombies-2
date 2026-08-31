package io.github.Plants_Vs_Zombies_2.model.auth;

import io.github.Plants_Vs_Zombies_2.model.user.User;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;

public final class JsonUserRepository implements UserRepository {
    private final Path databasePath;
    private final List<User> users;

    public JsonUserRepository(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath")
                .toAbsolutePath().normalize();
        users = new ArrayList<>(UserJsonDatabase.load(this.databasePath));
        ensureUniqueUsernames();
    }

    @Override
    public synchronized Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public synchronized List<User> findAll() {
        return List.copyOf(users);
    }

    @Override
    public synchronized boolean addIfUsernameAvailable(User user) {
        Objects.requireNonNull(user, "user");
        if (findByUsername(user.getUsername()).isPresent()) {
            return false;
        }
        users.add(user);
        try {
            UserJsonDatabase.save(databasePath, users);
            return true;
        } catch (RuntimeException exception) {
            users.remove(user);
            throw exception;
        }
    }

    @Override
    public synchronized Optional<GameplayStateSnapshot> findGameplayState(String username) {
        return findByUsername(username).map(user -> new GameplayStateSnapshot(
                user.getGameplayRevision(), GameplayState.fromUser(user)));
    }

    @Override
    public synchronized GameplayStateSnapshot updateGameplayState(String username,
            long expectedRevision, GameplayState state)
            throws GameplayUpdateException {
        User user = findByUsername(username).orElseThrow(() ->
                new GameplayUpdateException(GameplayUpdateFailure.USER_NOT_FOUND,
                        "The authenticated account no longer exists"));
        if (expectedRevision != user.getGameplayRevision()) {
            throw new GameplayUpdateException(GameplayUpdateFailure.STALE_REVISION,
                    "Expected gameplay revision " + user.getGameplayRevision()
                            + " but received " + expectedRevision);
        }
        if (user.getGameplayRevision() == Long.MAX_VALUE) {
            throw new GameplayUpdateException(GameplayUpdateFailure.VALIDATION_FAILED,
                    "Gameplay revision limit reached");
        }
        GameplayState previous = GameplayState.fromUser(user);
        GameplayStateValidator.validate(state, previous);
        long previousRevision = user.getGameplayRevision();
        try {
            user.applyGameplayState(state);
            user.setGameplayRevisionForStorage(previousRevision + 1);
            UserJsonDatabase.save(databasePath, users);
            return new GameplayStateSnapshot(user.getGameplayRevision(),
                    GameplayState.fromUser(user));
        } catch (RuntimeException exception) {
            user.applyGameplayState(previous);
            user.setGameplayRevisionForStorage(previousRevision);
            throw exception;
        }
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    private void ensureUniqueUsernames() {
        for (int first = 0; first < users.size(); first++) {
            for (int second = first + 1; second < users.size(); second++) {
                if (users.get(first).getUsername().equals(users.get(second).getUsername())) {
                    throw new IllegalStateException(
                            "duplicate username in user database: "
                                    + users.get(first).getUsername());
                }
            }
        }
    }
}
