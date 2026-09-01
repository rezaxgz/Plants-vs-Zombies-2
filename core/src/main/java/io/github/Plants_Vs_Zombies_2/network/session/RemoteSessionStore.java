package io.github.Plants_Vs_Zombies_2.network.session;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import io.github.Plants_Vs_Zombies_2.network.auth.PersistentLoginCredentials;
import io.github.Plants_Vs_Zombies_2.network.auth.PersistentLoginToken;

/** Client-only storage for an opaque server-issued login token. */
final class RemoteSessionStore {
    static final String PATH_PROPERTY = "pvz.remote.session.path";
    private static final Gson GSON = new Gson();

    private final Path path;

    static RemoteSessionStore fromSystemProperties() {
        String configured = System.getProperty(PATH_PROPERTY);
        Path path = configured == null || configured.isBlank()
                ? Path.of("data", "remote-session.json")
                : Path.of(configured);
        return new RemoteSessionStore(path);
    }

    RemoteSessionStore(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    synchronized Optional<PersistentLoginCredentials> load() {
        if (!Files.exists(path)) return Optional.empty();
        try {
            StoredSession stored = GSON.fromJson(
                    Files.readString(path, StandardCharsets.UTF_8),
                    StoredSession.class);
            if (stored == null || stored.username == null || stored.token == null
                    || stored.username.isBlank() || stored.token.isBlank()) {
                clear();
                return Optional.empty();
            }
            return Optional.of(new PersistentLoginCredentials(
                    stored.username, stored.token));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "could not read the saved remote login", exception);
        } catch (JsonParseException | IllegalStateException exception) {
            clear();
            return Optional.empty();
        }
    }

    synchronized void save(PersistentLoginToken token) {
        if (token == null || token.getUsername() == null || token.getToken() == null
                || token.getUsername().isBlank() || token.getToken().isBlank()) {
            throw new IllegalArgumentException("persistent login token is incomplete");
        }
        Path parent = path.getParent();
        Path temporary = null;
        try {
            if (parent != null) Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, "remote-session-", ".tmp");
            Files.writeString(temporary,
                    GSON.toJson(new StoredSession(
                            token.getUsername(), token.getToken())) + "\n",
                    StandardCharsets.UTF_8);
            restrictToOwner(temporary);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
            restrictToOwner(path);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "could not save the remote login", exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    synchronized void clear() {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "could not clear the saved remote login", exception);
        }
    }

    private static void restrictToOwner(Path file) {
        try {
            Files.setPosixFilePermissions(file, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows and some filesystems do not expose POSIX permissions.
        }
    }

    private static final class StoredSession {
        private final String username;
        private final String token;

        private StoredSession(String username, String token) {
            this.username = username;
            this.token = token;
        }
    }
}
