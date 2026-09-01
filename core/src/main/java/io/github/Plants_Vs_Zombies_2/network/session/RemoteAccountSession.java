package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.auth.PasswordResetChallenge;
import io.github.Plants_Vs_Zombies_2.network.auth.PasswordResetRequest;
import io.github.Plants_Vs_Zombies_2.network.auth.PersistentLoginCredentials;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingClient;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameClient;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardClient;

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
    private final LeaderboardClient leaderboardClient;
    private final RemoteSessionStore persistentSession;
    private final List<SessionStateListener> listeners = new CopyOnWriteArrayList<>();
    private volatile ClientSessionState state = ClientSessionState.DISCONNECTED;
    private volatile AccountProfile profile;
    private volatile GameplayStateSnapshot gameplayStateSnapshot;
    private volatile Throwable lastFailure;
    private long authenticationGeneration;
    private CompletableFuture<Void> connectionAttempt;
    private boolean closed;

    public static RemoteAccountSession fromSystemProperties() {
        String host = System.getProperty(HOST_PROPERTY, NetworkClient.DEFAULT_HOST);
        int port = Integer.getInteger(PORT_PROPERTY, NetworkClient.DEFAULT_PORT);
        return new RemoteAccountSession(new NetworkAccountTransport(
                new NetworkClient(host, port)));
    }

    RemoteAccountSession(RemoteAccountTransport transport) {
        this(transport, RemoteSessionStore.fromSystemProperties());
    }

    RemoteAccountSession(RemoteAccountTransport transport,
            RemoteSessionStore persistentSession) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.persistentSession = Objects.requireNonNull(
                persistentSession, "persistentSession");
        this.matchmakingClient = transport.getMatchmakingClient();
        this.multiplayerGameClient = transport.getMultiplayerGameClient();
        this.leaderboardClient = transport.getLeaderboardClient();
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
        return login(username, password, false);
    }

    @Override
    public CompletableFuture<AccountProfile> login(String username, String password,
            boolean stayLoggedIn) {
        if (!stayLoggedIn) persistentSession.clear();
        return connect().thenCompose(ignored -> {
            transition(ClientSessionState.AUTHENTICATING, null);
            return transport.login(username, password);
        }).thenCompose(authenticatedProfile -> {
            if (!stayLoggedIn) {
                return CompletableFuture.completedFuture(authenticatedProfile);
            }
            return transport.createPersistentLogin().thenApply(token -> {
                persistentSession.save(token);
                return authenticatedProfile;
            });
        }).thenCompose(authenticatedProfile -> transport.getGameplayState()
                .thenApply(gameplay -> new LoginResult(authenticatedProfile, gameplay)))
        .whenComplete((result, failure) -> {
            if (failure == null) {
                synchronized (lock) {
                    authenticationGeneration++;
                    gameplayStateSnapshot = result.gameplay();
                    profile = result.profile().withGameplayState(
                            result.gameplay().getState());
                }
                transition(ClientSessionState.AUTHENTICATED, null);
            } else {
                profile = null;
                gameplayStateSnapshot = null;
                if (transport.isConnected()) transport.disconnect();
                transitionAfterRequest(failure);
            }
        }).thenApply(result -> result.profile().withGameplayState(
                result.gameplay().getState()));
    }

    @Override
    public boolean hasPersistentLogin() {
        return persistentSession.load().isPresent();
    }

    @Override
    public CompletableFuture<AccountProfile> restorePersistentLogin() {
        PersistentLoginCredentials credentials = persistentSession.load()
                .orElse(null);
        if (credentials == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No saved remote login"));
        }
        return connect().thenCompose(ignored -> {
            transition(ClientSessionState.AUTHENTICATING, null);
            return transport.login(credentials);
        }).thenCompose(authenticatedProfile -> transport.getGameplayState()
                .thenApply(gameplay -> new LoginResult(authenticatedProfile, gameplay)))
        .whenComplete((result, failure) -> {
            if (failure == null) {
                synchronized (lock) {
                    authenticationGeneration++;
                    gameplayStateSnapshot = result.gameplay();
                    profile = result.profile().withGameplayState(
                            result.gameplay().getState());
                }
                transition(ClientSessionState.AUTHENTICATED, null);
            } else {
                persistentSession.clear();
                profile = null;
                gameplayStateSnapshot = null;
                if (transport.isConnected()) transport.disconnect();
                transitionAfterRequest(failure);
            }
        }).thenApply(result -> result.profile().withGameplayState(
                result.gameplay().getState()));
    }

    @Override
    public CompletableFuture<PasswordResetChallenge> lookupPasswordReset(
            String username, String email) {
        return connect().thenCompose(ignored ->
                transport.lookupPasswordReset(username, email));
    }

    @Override
    public CompletableFuture<Void> resetPassword(PasswordResetRequest details) {
        return connect().thenCompose(ignored -> transport.resetPassword(details))
                .thenRun(persistentSession::clear);
    }

    @Override
    public CompletableFuture<AccountProfile> refreshProfile() {
        if (profile == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No authenticated remote account"));
        }
        long generation = currentGeneration();
        return connect().thenCompose(ignored -> transport.getProfile())
                .thenCombine(transport.getGameplayState(), LoginResult::new)
                .whenComplete((result, failure) -> {
                    if (failure == null) {
                        storeGameplaySnapshot(result.gameplay(), generation);
                        if (currentGeneration() == generation && profile != null) {
                            profile = result.profile().withGameplayState(
                                    result.gameplay().getState());
                        }
                        transition(ClientSessionState.AUTHENTICATED, null);
                    } else {
                        transitionAfterRequest(failure);
                    }
                }).thenApply(result -> result.profile().withGameplayState(
                        result.gameplay().getState()));
    }

    @Override
    public GameplayStateSnapshot getGameplayStateSnapshot() {
        return gameplayStateSnapshot;
    }

    @Override
    public CompletableFuture<GameplayStateSnapshot> refreshGameplayState() {
        if (profile == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No authenticated remote account"));
        }
        long generation = currentGeneration();
        return transport.getGameplayState().thenApply(snapshot -> {
            storeGameplaySnapshot(snapshot, generation);
            return snapshot;
        });
    }

    @Override
    public CompletableFuture<GameplayStateSnapshot> synchronizeGameplayState(
            long expectedRevision, GameplayState state) {
        if (profile == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No authenticated remote account"));
        }
        long generation = currentGeneration();
        return transport.synchronizeGameplayState(expectedRevision, state)
                .thenApply(snapshot -> {
                    storeGameplaySnapshot(snapshot, generation);
                    return snapshot;
                });
    }

    @Override
    public CompletableFuture<Void> logout() {
        persistentSession.clear();
        AccountProfile previousProfile = profile;
        clearAuthenticatedState();
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
            gameplayStateSnapshot = null;
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
    public LeaderboardClient getLeaderboardClient() {
        return leaderboardClient;
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
        clearAuthenticatedState();
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
        clearAuthenticatedState();
        clearMatchmakingState();
        transport.close();
        transition(ClientSessionState.CLOSED, null);
    }

    private void handleDisconnect(Throwable failure) {
        clearAuthenticatedState();
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

    private long currentGeneration() {
        synchronized (lock) { return authenticationGeneration; }
    }

    private void storeGameplaySnapshot(GameplayStateSnapshot snapshot,
            long generation) {
        synchronized (lock) {
            if (closed || profile == null || authenticationGeneration != generation
                    || snapshot == null || snapshot.getState() == null) return;
            GameplayStateSnapshot current = gameplayStateSnapshot;
            if (current == null || snapshot.getRevision() > current.getRevision()) {
                gameplayStateSnapshot = snapshot;
                profile = profile.withGameplayState(snapshot.getState());
            }
        }
    }

    private void clearAuthenticatedState() {
        synchronized (lock) {
            authenticationGeneration++;
            profile = null;
            gameplayStateSnapshot = null;
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

    private record LoginResult(AccountProfile profile,
            GameplayStateSnapshot gameplay) { }
}
