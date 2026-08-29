package io.github.Plants_Vs_Zombies_2.server;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ConnectionContext {
    private final String connectionId = UUID.randomUUID().toString();
    private final Instant connectedAt = Instant.now();
    private final AtomicBoolean handshakeCompleted = new AtomicBoolean();
    private final AtomicReference<String> authenticatedUsername = new AtomicReference<>();

    public String getConnectionId() {
        return connectionId;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public boolean isHandshakeCompleted() {
        return handshakeCompleted.get();
    }

    public String getAuthenticatedUsername() {
        return authenticatedUsername.get();
    }

    boolean completeHandshake() {
        return handshakeCompleted.compareAndSet(false, true);
    }

    boolean authenticate(String username) {
        return authenticatedUsername.compareAndSet(null, username);
    }

    String clearAuthentication() {
        return authenticatedUsername.getAndSet(null);
    }
}
