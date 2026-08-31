package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingClient;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameClient;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;

interface RemoteAccountTransport extends AutoCloseable {
    CompletableFuture<Void> connect();

    boolean isConnected();

    CompletableFuture<Void> register(RegistrationDetails details);

    CompletableFuture<AccountProfile> login(String username, String password);

    CompletableFuture<AccountProfile> getProfile();

    CompletableFuture<GameplayStateSnapshot> getGameplayState();

    CompletableFuture<GameplayStateSnapshot> synchronizeGameplayState(
            long expectedRevision, GameplayState state);

    CompletableFuture<Void> logout();

    void setDisconnectListener(Consumer<Throwable> listener);

    default MatchmakingClient getMatchmakingClient() { return null; }

    default MultiplayerGameClient getMultiplayerGameClient() { return null; }

    void disconnect();

    @Override
    void close();
}
