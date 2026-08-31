package io.github.Plants_Vs_Zombies_2.network.leaderboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.network.session.UiDispatcher;

class LeaderboardFlowControllerTest {
    @Test
    void successfulAndEmptyLoadsArePublishedOnlyThroughUiDispatcher() {
        FakeTransport transport = new FakeTransport();
        QueuedUi ui = new QueuedUi();
        List<LeaderboardFlowController.State> observed = new ArrayList<>();
        LeaderboardFlowController controller = new LeaderboardFlowController(
                transport, ui, observed::add);

        controller.load();
        assertTrue(observed.isEmpty());
        assertEquals(1, transport.calls);
        ui.runNext();
        assertTrue(observed.get(0).loading());
        transport.requests.get(0).complete(page("alice", 1));
        assertEquals(1, observed.size());
        ui.runNext();
        assertFalse(observed.get(1).loading());
        assertEquals("alice", observed.get(1).entries().get(0).getUsername());

        controller.selectSort(LeaderboardSortColumn.USERNAME,
                LeaderboardSortDirection.ASCENDING);
        ui.runNext();
        transport.requests.get(1).complete(new LeaderboardPage(
                List.of(), 0, null, 0, 100));
        ui.runNext();
        assertTrue(controller.getState().entries().isEmpty());
        assertEquals(0, controller.getState().totalPlayers());
    }

    @Test
    void duplicateIsPreventedAndNewestSortWins() {
        FakeTransport transport = new FakeTransport();
        QueuedUi ui = new QueuedUi();
        LeaderboardFlowController controller = new LeaderboardFlowController(
                transport, ui, ignored -> { });
        controller.load();
        controller.load();
        assertEquals(1, transport.calls);

        CompletableFuture<LeaderboardPage> old = transport.requests.get(0);
        controller.selectSort(LeaderboardSortColumn.USERNAME,
                LeaderboardSortDirection.ASCENDING);
        assertTrue(old.isCancelled());
        assertEquals(2, transport.calls);
        transport.requests.get(1).complete(page("newest", 1));
        ui.runAll();
        assertEquals("newest", controller.getState().entries().get(0)
                .getUsername());
        assertEquals(LeaderboardSortColumn.USERNAME,
                controller.getState().sortColumn());
    }

    @Test
    void timeoutIsRecoverableAndExplicitRetryCanSucceed() {
        FakeTransport transport = new FakeTransport();
        QueuedUi ui = new QueuedUi();
        LeaderboardFlowController controller = new LeaderboardFlowController(
                transport, ui, ignored -> { });
        controller.load();
        transport.requests.get(0).completeExceptionally(
                new TimeoutException("late"));
        ui.runAll();
        assertTrue(controller.getState().retryAvailable());
        assertTrue(controller.getState().message().contains("timed out"));

        controller.retry();
        transport.requests.get(1).complete(page("alice", 1));
        ui.runAll();
        assertFalse(controller.getState().retryAvailable());
        assertEquals("alice", controller.getState().entries().get(0)
                .getUsername());
    }

    @Test
    void unavailableServerProducesRecoverableState() {
        FakeTransport transport = new FakeTransport();
        QueuedUi ui = new QueuedUi();
        LeaderboardFlowController controller = new LeaderboardFlowController(
                transport, ui, ignored -> { });
        controller.load();
        transport.requests.get(0).completeExceptionally(
                new java.net.ConnectException("Could not connect"));
        ui.runAll();

        assertTrue(controller.getState().retryAvailable());
        assertTrue(controller.getState().message().contains("unavailable"));
    }

    @Test
    void disposalAndConnectionLossInvalidatePendingResults() {
        FakeTransport transport = new FakeTransport();
        QueuedUi ui = new QueuedUi();
        List<LeaderboardFlowController.State> observed = new ArrayList<>();
        LeaderboardFlowController controller = new LeaderboardFlowController(
                transport, ui, observed::add);
        controller.load();
        controller.connectionLost(new IllegalStateException(
                "Server disconnected"));
        ui.runAll();
        assertFalse(controller.getState().loading());
        assertTrue(controller.getState().entries().isEmpty());
        assertTrue(controller.getState().retryAvailable());

        controller.retry();
        CompletableFuture<LeaderboardPage> pending = transport.requests.get(1);
        controller.close();
        assertTrue(pending.isCancelled());
        int callbacks = observed.size();
        ui.runAll();
        assertEquals(callbacks, observed.size());
    }

    @Test
    void graphicalSourceHasNoLocalLeaderboardOrUserManagerAccess()
            throws IOException {
        Path source = Path.of("src", "main", "java", "io", "github",
                "Plants_Vs_Zombies_2", "view", "screens",
                "LeaderboardScreen.java");
        String text = Files.readString(source);
        assertFalse(text.contains("UserManager"));
        assertFalse(text.contains("LeaderBoard"));
        assertFalse(text.contains("getSortedLeaderboard"));
        assertTrue(text.contains("profile.getUsername().equals(entry.getUsername())"));
    }

    private static LeaderboardPage page(String username, int rank) {
        return new LeaderboardPage(List.of(new LeaderboardEntry(rank, username,
                1, 2, 3, 4, 5, 6)), 1, rank, 0, 100);
    }

    private static final class FakeTransport implements LeaderboardTransport {
        private final List<CompletableFuture<LeaderboardPage>> requests =
                new ArrayList<>();
        private int calls;

        @Override public CompletableFuture<LeaderboardPage> load(
                LeaderboardQuery query) {
            calls++;
            CompletableFuture<LeaderboardPage> future = new CompletableFuture<>();
            requests.add(future);
            return future;
        }
    }

    private static final class QueuedUi implements UiDispatcher {
        private final Queue<Runnable> queue = new ArrayDeque<>();

        @Override public void dispatch(Runnable runnable) { queue.add(runnable); }
        private void runNext() { queue.remove().run(); }
        private void runAll() { while (!queue.isEmpty()) runNext(); }
    }
}
