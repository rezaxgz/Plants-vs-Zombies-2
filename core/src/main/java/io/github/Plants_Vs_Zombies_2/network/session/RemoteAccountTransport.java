package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingClient;

interface RemoteAccountTransport extends AutoCloseable {
    CompletableFuture<Void> connect();

    boolean isConnected();

    CompletableFuture<Void> register(RegistrationDetails details);

    CompletableFuture<AccountProfile> login(String username, String password);

    CompletableFuture<AccountProfile> getProfile();

    CompletableFuture<Void> logout();

    void setDisconnectListener(Consumer<Throwable> listener);

    default MatchmakingClient getMatchmakingClient() { return null; }

    void disconnect();

    @Override
    void close();
}
