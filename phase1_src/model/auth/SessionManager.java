package model.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.user.User;

/**
 * Stores the username selected by the --stay-logged-in login option.
 */
public final class SessionManager {
    private static final String SESSION_PATH_PROPERTY = "pvz.session.database";
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "\\{\\s*\\\"username\\\"\\s*:\\s*\\\"(?<username>[-A-Za-z0-9]+)\\\"\\s*}");

    private SessionManager() {
    }

    public static synchronized User restorePersistentUser() {
        String username = readPersistentUsername();
        if (username == null) {
            return null;
        }
        User user = UserManager.getUserByUsername(username);
        if (user == null) {
            clearPersistentSession();
        }
        return user;
    }

    public static synchronized void persist(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }
        writeSession(user.getUsername());
    }

    public static synchronized void clearPersistentSession() {
        try {
            Files.deleteIfExists(getSessionPath());
        } catch (IOException exception) {
            throw new IllegalStateException("could not clear persistent session", exception);
        }
    }

    public static synchronized boolean isPersistentUser(String username) {
        return username != null && username.equals(readPersistentUsername());
    }

    public static synchronized void replacePersistentUsername(
            String oldUsername, String newUsername) {
        if (oldUsername == null || newUsername == null
                || !isPersistentUser(oldUsername)) {
            return;
        }
        writeSession(newUsername);
    }

    private static String readPersistentUsername() {
        Path sessionPath = getSessionPath();
        if (!Files.exists(sessionPath)) {
            return null;
        }
        try {
            String json = Files.readString(sessionPath, StandardCharsets.UTF_8);
            Matcher matcher = USERNAME_PATTERN.matcher(json.trim());
            if (!matcher.matches()) {
                clearPersistentSession();
                return null;
            }
            return matcher.group("username");
        } catch (IOException exception) {
            throw new IllegalStateException("could not read persistent session", exception);
        }
    }

    private static void writeSession(String username) {
        Path destination = getSessionPath();
        Path parent = destination.getParent();
        Path temporaryFile = null;
        try {
            if (parent != null) {
                Files.createDirectories(parent);
                temporaryFile = Files.createTempFile(parent, "session-", ".json.tmp");
            } else {
                temporaryFile = Files.createTempFile("session-", ".json.tmp");
            }
            String json = "{\n  \"username\": \"" + username + "\"\n}\n";
            Files.writeString(temporaryFile, json, StandardCharsets.UTF_8);
            moveIntoPlace(temporaryFile, destination);
            temporaryFile = null;
        } catch (IOException exception) {
            throw new IllegalStateException("could not save persistent session", exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static Path getSessionPath() {
        String configuredPath = System.getProperty(SESSION_PATH_PROPERTY);
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Path.of(configuredPath).toAbsolutePath().normalize();
        }
        Path usersPath = UserManager.getDatabasePath().toAbsolutePath().normalize();
        Path parent = usersPath.getParent();
        return parent == null
                ? Path.of("session.json").toAbsolutePath().normalize()
                : parent.resolve("session.json");
    }

    private static void moveIntoPlace(Path source, Path destination)
            throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
