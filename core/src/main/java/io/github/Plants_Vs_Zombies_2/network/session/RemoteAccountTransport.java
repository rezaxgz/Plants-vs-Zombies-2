package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.auth.PasswordResetChallenge;
import io.github.Plants_Vs_Zombies_2.network.auth.PasswordResetRequest;
import io.github.Plants_Vs_Zombies_2.network.auth.PersistentLoginCredentials;
import io.github.Plants_Vs_Zombies_2.network.auth.PersistentLoginToken;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingClient;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameClient;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardClient;

interface RemoteAccountTransport extends AutoCloseable {
    CompletableFuture<Void> connect();

    boolean isConnected();

    CompletableFuture<Void> register(RegistrationDetails details);

    CompletableFuture<AccountProfile> login(String username, String password);

    default CompletableFuture<PersistentLoginToken> createPersistentLogin() {
        return CompletableFuture.failedFuture(
                new IllegalStateException("Persistent login is unavailable"));
    }

    default CompletableFuture<AccountProfile> login(
            PersistentLoginCredentials credentials) {
        return CompletableFuture.failedFuture(
                new IllegalStateException("Persistent login is unavailable"));
    }

    default CompletableFuture<PasswordResetChallenge> lookupPasswordReset(
            String username, String email) {
        return CompletableFuture.failedFuture(
                new IllegalStateException("Password recovery is unavailable"));
    }

    default CompletableFuture<Void> resetPassword(PasswordResetRequest details) {
        return CompletableFuture.failedFuture(
                new IllegalStateException("Password recovery is unavailable"));
    }

    CompletableFuture<AccountProfile> getProfile();

    CompletableFuture<GameplayStateSnapshot> getGameplayState();

    CompletableFuture<GameplayStateSnapshot> synchronizeGameplayState(
            long expectedRevision, GameplayState state);

    CompletableFuture<Void> logout();

    void setDisconnectListener(Consumer<Throwable> listener);

    default MatchmakingClient getMatchmakingClient() { return null; }

    default MultiplayerGameClient getMultiplayerGameClient() { return null; }

    default LeaderboardClient getLeaderboardClient() { return null; }

    void disconnect();

    @Override
    void close();
}
