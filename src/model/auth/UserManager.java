package model.auth;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import model.user.User;

public final class UserManager {
    private static final String DATABASE_PATH_PROPERTY = "pvz.users.database";
    private static final String DEFAULT_DATABASE_PATH = "data/users.json";
    private static final Path databasePath = Path.of(System.getProperty(DATABASE_PATH_PROPERTY, DEFAULT_DATABASE_PATH));
    private static final List<User> users = new ArrayList<>();

    static {
        reloadUsersFromDatabase();
    }

    private UserManager() {
    }

    public static synchronized List<User> loadAllUsers() {
        return new ArrayList<>(users);
    }

    public static synchronized void reloadUsersFromDatabase() {
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
        UserJsonDatabase.save(databasePath, users);
    }

    public static synchronized boolean usernameExists(String username) {
        return getUserByUsername(username) != null;
    }

    public static synchronized void addUserToDatabase(User user) {
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
}
