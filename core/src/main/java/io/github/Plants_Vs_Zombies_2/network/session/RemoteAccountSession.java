package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingClient;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameClient;

/**
 * One application-scoped remote account session. It never blocks its caller;
 * all socket work and request completions remain asynchronous.
 */
public final class RemoteAccountSession implements AccountSession {
    public static final String HOST_PROPERTY = "pvz.client.server.host";
    public static final String PORT_PROPERTY = "pvz.client.server.port";

    private final Object lock = new Object();
    private final RemoteAccountTransport transport;
    private final MatchmakingClient matchmakingClient;
    private final MultiplayerGameClient multiplayerGameClient;
    private final List<SessionStateListener> listeners = new CopyOnWriteArrayList<>();
    private volatile ClientSessionState state = ClientSessionState.DISCONNECTED;
    private volatile AccountProfile profile;
    private volatile Throwable lastFailure;
    private CompletableFuture<Void> connectionAttempt;
    private boolean closed;

    public static RemoteAccountSession fromSystemProperties() {
        String host = System.getProperty(HOST_PROPERTY, NetworkClient.DEFAULT_HOST);
        int port = Integer.getInteger(PORT_PROPERTY, NetworkClient.DEFAULT_PORT);
        return new RemoteAccountSession(new NetworkAccountTransport(
                new NetworkClient(host, port)));
    }

    RemoteAccountSession(RemoteAccountTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.matchmakingClient = transport.getMatchmakingClient();
        this.multiplayerGameClient = transport.getMultiplayerGameClient();
        transport.setDisconnectListener(this::handleDisconnect);
    }

    @Override
    public CompletableFuture<Void> connect() {
        synchronized (lock) {
            if (closed) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Account session is closed"));
            }
            if (transport.isConnected()) {
                if (profile == null) {
                    transition(ClientSessionState.CONNECTED, null);
                }
                return CompletableFuture.completedFuture(null);
            }
            if (connectionAttempt != null && !connectionAttempt.isDone()) {
                return connectionAttempt;
            }
            transition(ClientSessionState.CONNECTING, null);
            CompletableFuture<Void> attempt = transport.connect();
            connectionAttempt = attempt;
            attempt.whenComplete((ignored, failure) -> {
                synchronized (lock) {
                    if (connectionAttempt == attempt) {
                        connectionAttempt = null;
                    }
                }
                if (failure == null) {
                    transition(profile == null
                            ? ClientSessionState.CONNECTED
                            : ClientSessionState.AUTHENTICATED, null);
                } else {
                    transition(ClientSessionState.DISCONNECTED, unwrap(failure));
                }
            });
            return attempt;
        }
    }

    @Override
    public CompletableFuture<Void> register(RegistrationDetails details) {
        Objects.requireNonNull(details, "details");
        return connect().thenCompose(ignored -> {
            transition(ClientSessionState.REGISTERING, null);
            return transport.register(details);
        }).whenComplete((ignored, failure) -> transitionAfterRequest(failure));
    }

    @Override
    public CompletableFuture<AccountProfile> login(String username, String password) {
        return connect().thenCompose(ignored -> {
            transition(ClientSessionState.AUTHENTICATING, null);
            return transport.login(username, password);
        }).whenComplete((result, failure) -> {
            if (failure == null) {
                profile = result;
                transition(ClientSessionState.AUTHENTICATED, null);
            } else {
                profile = null;
                transitionAfterRequest(failure);
            }
        });
    }

    @Override
    public CompletableFuture<AccountProfile> refreshProfile() {
        if (profile == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No authenticated remote account"));
        }
        return connect().thenCompose(ignored -> transport.getProfile())
                .whenComplete((result, failure) -> {
                    if (failure == null) {
                        profile = result;
                        transition(ClientSessionState.AUTHENTICATED, null);
                    } else {
                        transitionAfterRequest(failure);
                    }
                });
    }

    @Override
    public CompletableFuture<Void> logout() {
        AccountProfile previousProfile = profile;
        profile = null;
        if (!transport.isConnected()) {
            clearMatchmakingState();
            transition(ClientSessionState.DISCONNECTED, null);
            return CompletableFuture.completedFuture(null);
        }
        transition(ClientSessionState.LOGGING_OUT, null);
        CompletableFuture<Void> request = previousProfile == null
                ? CompletableFuture.completedFuture(null)
                : transport.logout();
        return request.whenComplete((ignored, failure) -> {
            profile = null;
            clearMatchmakingState();
            if (failure != null && transport.isConnected()) {
                // A timed-out logout has an unknown server outcome. Closing the
                // connection is the only safe way to release any online-session
                // ownership before the next login attempt.
                transport.disconnect();
            }
            transition(transport.isConnected()
                    ? ClientSessionState.CONNECTED
                    : ClientSessionState.DISCONNECTED,
                    failure == null ? null : unwrap(failure));
        });
    }

    @Override
    public ClientSessionState getState() {
        return state;
    }

    @Override
    public AccountProfile getProfile() {
        return profile;
    }

    @Override
    public Throwable getLastFailure() {
        return lastFailure;
    }

    @Override
    public MatchmakingClient getMatchmakingClient() {
        return matchmakingClient;
    }

    @Override
    public MultiplayerGameClient getMultiplayerGameClient() {
        return multiplayerGameClient;
    }

    @Override
    public void addStateListener(SessionStateListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void removeStateListener(SessionStateListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void disconnect() {
        profile = null;
        clearMatchmakingState();
        transport.disconnect();
        transition(ClientSessionState.DISCONNECTED, null);
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        profile = null;
        clearMatchmakingState();
        transport.close();
        transition(ClientSessionState.CLOSED, null);
    }

    private void handleDisconnect(Throwable failure) {
        profile = null;
        clearMatchmakingState();
        transition(closed ? ClientSessionState.CLOSED
                : ClientSessionState.DISCONNECTED, failure);
    }

    private void transitionAfterRequest(Throwable failure) {
        if (failure == null) {
            transition(profile == null ? ClientSessionState.CONNECTED
                    : ClientSessionState.AUTHENTICATED, null);
        } else {
            transition(transport.isConnected()
                    ? (profile == null ? ClientSessionState.CONNECTED
                            : ClientSessionState.AUTHENTICATED)
                    : ClientSessionState.DISCONNECTED, unwrap(failure));
        }
    }

    private void clearMatchmakingState() {
        if (matchmakingClient != null) {
            matchmakingClient.clearState();
        }
        if (multiplayerGameClient != null) {
            multiplayerGameClient.clearState();
        }
    }

    private void transition(ClientSessionState next, Throwable failure) {
        ClientSessionState previous = state;
        state = next;
        lastFailure = failure;
        if (previous == next && failure == null) {
            return;
        }
        for (SessionStateListener listener : listeners) {
            listener.onStateChanged(previous, next, failure);
        }
    }

    static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
