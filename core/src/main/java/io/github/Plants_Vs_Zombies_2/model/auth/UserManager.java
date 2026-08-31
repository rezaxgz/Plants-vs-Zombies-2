package io.github.Plants_Vs_Zombies_2.model.auth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.user.User;

public final class UserManager {
    private static final String DATABASE_PATH_PROPERTY = "pvz.users.database";
    private static final String DEFAULT_DATABASE_PATH = "data/users.json";
    private static final Path databasePath = resolveDatabasePath();
    private static final List<User> users = new ArrayList<>();
    private static boolean persistenceEnabled = true;

    static {
        reloadUsersFromDatabase();
    }

    private UserManager() {
    }

    private static Path resolveDatabasePath() {
        String configuredPath = System.getProperty(DATABASE_PATH_PROPERTY);
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Path.of(configuredPath);
        }

        Path direct = Path.of(DEFAULT_DATABASE_PATH);

        // Recent LibGDX desktop launchers commonly run with assets/ as their
        // working directory. During development, keep using the phase-one
        // project-root data/users.json instead of silently creating a second
        // user database under assets/data/.
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path folderName = workingDirectory.getFileName();
        Path projectRoot = workingDirectory.getParent();
        if (folderName != null
                && "assets".equalsIgnoreCase(folderName.toString())
                && projectRoot != null
                && (Files.exists(projectRoot.resolve("core"))
                        || Files.exists(projectRoot.resolve("lwjgl3")))) {
            return projectRoot.resolve(DEFAULT_DATABASE_PATH);
        }

        return direct;
    }

    public static Path getDatabasePath() {
        return databasePath;
    }

    /**
     * Prevents the server-authenticated graphical application from rewriting
     * the legacy local account database. Console mode keeps persistence enabled
     * by default; graphical startup opts into this process-wide remote-only mode.
     */
    public static synchronized void useRemoteOnlyMode() {
        persistenceEnabled = false;
    }

    public static synchronized boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    public static synchronized List<User> loadAllUsers() {
        return new ArrayList<>(users);
    }

    public static synchronized void reloadUsersFromDatabase() {
        requirePersistenceEnabled("reload the local user database");
        List<User> loadedUsers = UserJsonDatabase.load(databasePath);
        users.clear();
        for (User user : loadedUsers) {
            if (getUserByUsername(user.getUsername()) != null) {
                throw new IllegalStateException("duplicate username in user database: " + user.getUsername());
            }
            users.add(user);
        }
    }

    public static synchronized void saveAllUsers() {
        if (!persistenceEnabled) {
            return;
        }
        UserJsonDatabase.save(databasePath, users);
    }

    public static synchronized boolean usernameExists(String username) {
        return getUserByUsername(username) != null;
    }

    public static synchronized void addUserToDatabase(User user) {
        requirePersistenceEnabled("add a local user");
        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }
        if (usernameExists(user.getUsername())) {
            throw new IllegalArgumentException("username already exists: " + user.getUsername());
        }

        users.add(user);
        try {
            saveAllUsers();
        } catch (RuntimeException e) {
            users.remove(user);
            throw e;
        }
    }

    public static synchronized void renameUser(User user, String newUsername) {
        requirePersistenceEnabled("rename a local user");
        if (user == null || !users.contains(user)) {
            throw new IllegalArgumentException("user is not managed by the database");
        }
        User existing = getUserByUsername(newUsername);
        if (existing != null && existing != user) {
            throw new IllegalArgumentException("username already exists: " + newUsername);
        }

        String oldUsername = user.getUsername();
        boolean persistent = SessionManager.isPersistentUser(oldUsername);
        user.changeUsername(newUsername);
        try {
            saveAllUsers();
            if (persistent) {
                SessionManager.replacePersistentUsername(oldUsername, newUsername);
            }
        } catch (RuntimeException exception) {
            user.changeUsername(oldUsername);
            saveAllUsers();
            if (persistent) {
                SessionManager.persist(user);
            }
            throw exception;
        }
    }

    public static synchronized User getUserByUsername(String username) {
        if (username == null) {
            return null;
        }
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    static synchronized void restoreLocalModeForTesting() {
        persistenceEnabled = true;
    }

    private static void requirePersistenceEnabled(String operation) {
        if (!persistenceEnabled) {
            throw new IllegalStateException(
                    "Remote-only graphical mode cannot " + operation);
        }
    }
}
