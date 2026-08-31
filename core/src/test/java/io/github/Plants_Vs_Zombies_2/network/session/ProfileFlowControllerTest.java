package io.github.Plants_Vs_Zombies_2.network.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;

class ProfileFlowControllerTest {
    private static final AccountProfile INITIAL = profile("alice", 10);
    private static final AccountProfile UPDATED = profile("alice", 25);

    @Test
    void refreshUsesDispatcherAndPublishesLatestServerProfile() {
        FakeSession session = new FakeSession(INITIAL);
        AtomicInteger dispatches = new AtomicInteger();
        AtomicReference<ProfileFlowController.State> observed =
                new AtomicReference<>();
        UiDispatcher ui = runnable -> {
            dispatches.incrementAndGet();
            runnable.run();
        };
        try (ProfileFlowController controller = new ProfileFlowController(
                session, ui, observed::set)) {
            controller.refresh();
            assertTrue(controller.getState().loading());
            session.refresh.complete(UPDATED);

            assertFalse(controller.getState().loading());
            assertEquals(25, controller.getState().profile().getCoins());
            assertEquals(25, observed.get().profile().getCoins());
            assertTrue(dispatches.get() >= 2);
        }
    }

    @Test
    void failedRefreshKeepsLastProfileAndCanRetry() {
        FakeSession session = new FakeSession(INITIAL);
        try (ProfileFlowController controller = new ProfileFlowController(
                session, UiDispatcher.direct(), ignored -> { })) {
            controller.refresh();
            session.refresh.completeExceptionally(
                    new IllegalStateException("server unavailable"));

            assertEquals(INITIAL, controller.getState().profile());
            assertTrue(controller.getState().retryAvailable());
            assertTrue(controller.getState().message().contains("server unavailable"));
        }
    }

    @Test
    void closeIgnoresLateRefreshCompletion() {
        FakeSession session = new FakeSession(INITIAL);
        AtomicInteger callbacks = new AtomicInteger();
        ProfileFlowController controller = new ProfileFlowController(
                session, UiDispatcher.direct(), ignored -> callbacks.incrementAndGet());
        controller.refresh();
        int beforeClose = callbacks.get();
        controller.close();
        session.refresh.complete(UPDATED);
        assertEquals(beforeClose, callbacks.get());
    }

    private static AccountProfile profile(String username, int coins) {
        return new AccountProfile(username, "Alice", "alice@example.com",
                "FEMALE", coins, 2, 3, 4, 5, 1, 2, 3, 100, 7);
    }

    private static final class FakeSession implements AccountSession {
        private final AccountProfile profile;
        private final CompletableFuture<AccountProfile> refresh =
                new CompletableFuture<>();

        private FakeSession(AccountProfile profile) {
            this.profile = profile;
        }

        @Override public CompletableFuture<Void> connect() {
            return CompletableFuture.completedFuture(null);
        }
        @Override public CompletableFuture<Void> register(RegistrationDetails details) {
            return CompletableFuture.completedFuture(null);
        }
        @Override public CompletableFuture<AccountProfile> login(
                String username, String password) {
            return CompletableFuture.completedFuture(profile);
        }
        @Override public CompletableFuture<AccountProfile> refreshProfile() {
            return refresh;
        }
        @Override public CompletableFuture<Void> logout() {
            return CompletableFuture.completedFuture(null);
        }
        @Override public ClientSessionState getState() {
            return ClientSessionState.AUTHENTICATED;
        }
        @Override public AccountProfile getProfile() { return profile; }
        @Override public Throwable getLastFailure() { return null; }
        @Override public void disconnect() { }
        @Override public void close() { }
    }
}
