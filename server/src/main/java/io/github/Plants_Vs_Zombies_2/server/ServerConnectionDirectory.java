package io.github.Plants_Vs_Zombies_2.server;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

/** Thread-safe routing directory for active authenticated sockets. */
final class ServerConnectionDirectory {
    private final ConcurrentMap<String, ClientConnection> connections =
            new ConcurrentHashMap<>();

    synchronized boolean register(String username, ClientConnection connection,
            Runnable staleConnectionCleanup) {
        ClientConnection existing = connections.get(username);
        if (existing == connection) {
            return true;
        }
        if (existing != null && existing.isOpen()) {
            return false;
        }
        if (existing != null) {
            connections.remove(username, existing);
            staleConnectionCleanup.run();
        }
        connections.put(username, connection);
        return true;
    }

    boolean unregister(String username, ClientConnection connection) {
        return username != null && connection != null
                && connections.remove(username, connection);
    }

    boolean isOnline(String username) {
        ClientConnection connection = connections.get(username);
        return connection != null && connection.isOpen();
    }

    void publish(List<MatchmakingEvent> events) {
        for (MatchmakingEvent event : events) {
            ClientConnection connection = connections.get(event.username());
            if (connection != null && connection.isOpen()) {
                connection.sendEvent(ProtocolMessages.withPayload(
                        event.type(), ProtocolMessages.newRequestId(), event.payload()));
            }
        }
    }

    void clear() {
        connections.clear();
    }
}
