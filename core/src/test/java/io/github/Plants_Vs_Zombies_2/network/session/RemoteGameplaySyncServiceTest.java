package io.github.Plants_Vs_Zombies_2.network.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplaySyncException;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

class RemoteGameplaySyncServiceTest {
    @Test
    void successfulSyncIsSerializedAndCoalescesNewerLocalChanges() {
        FakeSession session = new FakeSession();
        AtomicInteger dispatches = new AtomicInteger();
        RemoteGameplaySyncService service = new RemoteGameplaySyncService(
                session, runnable -> { dispatches.incrementAndGet(); runnable.run(); });
        User user = user();
        GameplayStateSnapshot initial = snapshot(0, user);
        service.attach(user, initial);

        user.addCoins(10);
        CompletableFuture<GameplayStateSnapshot> first = service.synchronize();
        assertSame(first, service.synchronize());
        assertEquals(1, session.syncCalls);
        user.addCoins(20);
        service.observeAndSynchronize();
        GameplayState firstSent = session.sentStates.removeFirst();
        session.syncFutures.removeFirst().complete(
                new GameplayStateSnapshot(1, firstSent));

        assertEquals(30, user.getCoins(), "older acknowledgement must not overwrite local state");
        assertEquals(2, session.syncCalls, "newer changes are serialized after the first request");
        GameplayState secondSent = session.sentStates.removeFirst();
        session.syncFutures.removeFirst().complete(
                new GameplayStateSnapshot(2, secondSent));
        assertEquals(2L, service.getStatus().acknowledgedRevision());
        assertFalse(service.getStatus().dirty());
        assertFalse(service.getStatus().pending());
        assertTrue(dispatches.get() >= 2);
    }

    @Test
    void timeoutRetainsDirtyStateAndRequiresExplicitRetry() {
        FakeSession session = new FakeSession();
        RemoteGameplaySyncService service = new RemoteGameplaySyncService(
                session, UiDispatcher.direct());
        User user = user();
        service.attach(user, snapshot(0, user));
        user.addCoins(5);
        service.synchronize();
        session.syncFutures.removeFirst().completeExceptionally(
                new java.util.concurrent.TimeoutException("timed out"));

        assertTrue(service.getStatus().dirty());
        assertTrue(service.getStatus().error() instanceof java.util.concurrent.TimeoutException);
        service.observeAndSynchronize();
        assertEquals(1, session.syncCalls, "failure must not create an automatic retry loop");
        service.retry();
        assertEquals(2, session.syncCalls);
    }

    @Test
    void staleRevisionRefreshesButPreservesLocalDirtyState() {
        FakeSession session = new FakeSession();
        RemoteGameplaySyncService service = new RemoteGameplaySyncService(
                session, UiDispatcher.direct());
        User user = user();
        service.attach(user, snapshot(0, user));
        user.addCoins(100);
        service.synchronize();
        session.syncFutures.removeFirst().completeExceptionally(
                new GameplaySyncException(ProtocolErrorCode.STALE_ACCOUNT_REVISION,
                        "stale"));
        User serverUser = user();
        serverUser.addCoins(50);
        session.refreshFuture.complete(snapshot(5, serverUser));

        assertEquals(100, user.getCoins());
        assertTrue(service.getStatus().dirty());
        assertTrue(service.getStatus().conflict());
        assertEquals(5L, service.getStatus().acknowledgedRevision());
        service.retry();
        assertEquals(5L, session.expectedRevisions.removeLast());
    }

    @Test
    void cleanRefreshHydratesAndDetachIgnoresLateCompletion() {
        FakeSession session = new FakeSession();
        RemoteGameplaySyncService service = new RemoteGameplaySyncService(
                session, UiDispatcher.direct());
        User user = user();
        service.attach(user, snapshot(0, user));
        User serverUser = user();
        serverUser.addCoins(25);
        CompletableFuture<GameplayStateSnapshot> refresh = service.refresh();
        session.refreshFuture.complete(snapshot(3, serverUser));
        refresh.join();
        assertEquals(25, user.getCoins());

        user.addCoins(1);
        CompletableFuture<GameplayStateSnapshot> pending = service.synchronize();
        GameplayState sent = session.sentStates.removeFirst();
        CompletableFuture<GameplayStateSnapshot> network = session.syncFutures.removeFirst();
        service.detach();
        network.complete(new GameplayStateSnapshot(4, sent));
        assertTrue(pending.isCompletedExceptionally());
        assertFalse(service.getStatus().attached());
    }

    private static User user() {
        return new User("alice", "GoodPass1!", "Alice",
                "alice@example.com", Gender.FEMALE);
    }

    private static GameplayStateSnapshot snapshot(long revision, User user) {
        return new GameplayStateSnapshot(revision, GameplayState.fromUser(user));
    }

    private static final class FakeSession implements AccountSession {
        private final Deque<CompletableFuture<GameplayStateSnapshot>> syncFutures =
                new ArrayDeque<>();
        private final Deque<GameplayState> sentStates = new ArrayDeque<>();
        private final Deque<Long> expectedRevisions = new ArrayDeque<>();
        private CompletableFuture<GameplayStateSnapshot> refreshFuture =
                new CompletableFuture<>();
        private int syncCalls;

        @Override public CompletableFuture<GameplayStateSnapshot> synchronizeGameplayState(
                long expectedRevision, GameplayState state) {
            syncCalls++;
            expectedRevisions.add(expectedRevision);
            sentStates.add(state);
            CompletableFuture<GameplayStateSnapshot> future = new CompletableFuture<>();
            syncFutures.add(future);
            return future;
        }

        @Override public CompletableFuture<GameplayStateSnapshot> refreshGameplayState() {
            return refreshFuture;
        }

        @Override public CompletableFuture<Void> connect() { return CompletableFuture.completedFuture(null); }
        @Override public CompletableFuture<Void> register(RegistrationDetails details) { return CompletableFuture.completedFuture(null); }
        @Override public CompletableFuture<AccountProfile> login(String username, String password) { return CompletableFuture.failedFuture(new UnsupportedOperationException()); }
        @Override public CompletableFuture<AccountProfile> refreshProfile() { return CompletableFuture.failedFuture(new UnsupportedOperationException()); }
        @Override public CompletableFuture<Void> logout() { return CompletableFuture.completedFuture(null); }
        @Override public ClientSessionState getState() { return ClientSessionState.AUTHENTICATED; }
        @Override public AccountProfile getProfile() { return null; }
        @Override public Throwable getLastFailure() { return null; }
        @Override public void disconnect() { }
        @Override public void close() { }
    }
}
