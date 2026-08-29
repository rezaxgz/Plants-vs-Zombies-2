package io.github.Plants_Vs_Zombies_2.model.auth;

import io.github.Plants_Vs_Zombies_2.model.user.User;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
