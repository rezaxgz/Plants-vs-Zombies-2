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
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;

final class NetworkAccountTransport implements RemoteAccountTransport {
    private final NetworkClient networkClient;
    private final AuthenticationClient authenticationClient;
    private final MatchmakingClient matchmakingClient;
    private volatile Consumer<Throwable> disconnectListener = ignored -> { };

    NetworkAccountTransport(NetworkClient networkClient) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        authenticationClient = new AuthenticationClient(networkClient);
        matchmakingClient = new MatchmakingClient(networkClient);
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
    public void disconnect() {
        networkClient.disconnect();
    }

    @Override
    public void close() {
        matchmakingClient.close();
        networkClient.close();
    }
}
