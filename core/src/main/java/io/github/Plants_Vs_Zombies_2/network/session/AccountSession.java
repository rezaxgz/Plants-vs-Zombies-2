package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.concurrent.CompletableFuture;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;

/** Testable boundary between graphical authentication flows and the network. */
public interface AccountSession extends AutoCloseable {
    CompletableFuture<Void> connect();

    CompletableFuture<Void> register(RegistrationDetails details);

    CompletableFuture<AccountProfile> login(String username, String password);

    CompletableFuture<AccountProfile> refreshProfile();

    CompletableFuture<Void> logout();

    ClientSessionState getState();

    AccountProfile getProfile();

    Throwable getLastFailure();

    default void addStateListener(SessionStateListener listener) {
    }

    default void removeStateListener(SessionStateListener listener) {
    }

    void disconnect();

    @Override
    void close();
}
