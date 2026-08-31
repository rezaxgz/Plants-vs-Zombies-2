package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.AuthenticationClient;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkMessageListener;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingClient;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameClient;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateClient;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;

final class NetworkAccountTransport implements RemoteAccountTransport {
    private final NetworkClient networkClient;
    private final AuthenticationClient authenticationClient;
    private final MatchmakingClient matchmakingClient;
    private final MultiplayerGameClient multiplayerGameClient;
    private final GameplayStateClient gameplayStateClient;
    private volatile Consumer<Throwable> disconnectListener = ignored -> { };

    NetworkAccountTransport(NetworkClient networkClient) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        authenticationClient = new AuthenticationClient(networkClient);
        matchmakingClient = new MatchmakingClient(networkClient);
        multiplayerGameClient = new MultiplayerGameClient(networkClient);
        gameplayStateClient = new GameplayStateClient(networkClient);
        networkClient.addListener(new NetworkMessageListener() {
            @Override
            public void onMessage(ProtocolMessage message) {
                // Request futures are consumed by AuthenticationClient.
            }

            @Override
            public void onDisconnected(Throwable cause) {
                disconnectListener.accept(cause);
            }
        });
    }

    @Override
    public CompletableFuture<Void> connect() {
        return networkClient.connect().thenApply(ignored -> null);
    }

    @Override
    public boolean isConnected() {
        return networkClient.isConnected();
    }

    @Override
    public CompletableFuture<Void> register(RegistrationDetails details) {
        return authenticationClient.register(details);
    }

    @Override
    public CompletableFuture<AccountProfile> login(String username, String password) {
        return authenticationClient.login(username, password);
    }

    @Override
    public CompletableFuture<AccountProfile> getProfile() {
        return authenticationClient.getProfile();
    }

    @Override
    public CompletableFuture<GameplayStateSnapshot> getGameplayState() {
        return gameplayStateClient.getState();
    }

    @Override
    public CompletableFuture<GameplayStateSnapshot> synchronizeGameplayState(
            long expectedRevision, GameplayState state) {
        return gameplayStateClient.synchronize(expectedRevision, state);
    }

    @Override
    public CompletableFuture<Void> logout() {
        return authenticationClient.logout();
    }

    @Override
    public void setDisconnectListener(Consumer<Throwable> listener) {
        disconnectListener = Objects.requireNonNull(listener, "listener");
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
    public void disconnect() {
        networkClient.disconnect();
    }

    @Override
    public void close() {
        multiplayerGameClient.close();
        matchmakingClient.close();
        networkClient.close();
    }
}
