package io.github.Plants_Vs_Zombies_2.network.leaderboard;

import java.net.ConnectException;
import java.net.SocketException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.session.UiDispatcher;

/**
 * Render-thread-safe orchestration for leaderboard loading, sorting and retry.
 * Every observer callback is routed through the supplied UI dispatcher.
 */
public final class LeaderboardFlowController implements AutoCloseable {
    public static final int GRAPHICAL_PAGE_LIMIT = 100;

    public record State(LeaderboardSortColumn sortColumn,
            LeaderboardSortDirection sortDirection, boolean loading,
            List<LeaderboardEntry> entries, int totalPlayers,
            Integer authenticatedUserRank, String message,
            boolean retryAvailable) {
        public State {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }
    }

    private final Object lock = new Object();
    private final LeaderboardTransport transport;
    private final UiDispatcher ui;
    private final Consumer<State> observer;
    private State state = new State(LeaderboardSortColumn.HIGH_SCORE,
            LeaderboardSortDirection.DESCENDING, false, List.of(), 0,
            null, null, false);
    private LeaderboardQuery activeQuery;
    private CompletableFuture<LeaderboardPage> activeRequest;
    private long generation;
    private boolean closed;

    public LeaderboardFlowController(LeaderboardTransport transport,
            UiDispatcher ui, Consumer<State> observer) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.ui = Objects.requireNonNull(ui, "ui");
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    public State getState() {
        synchronized (lock) { return state; }
    }

    public void load() {
        start(new LeaderboardQuery(getState().sortColumn(),
                getState().sortDirection(), 0, GRAPHICAL_PAGE_LIMIT));
    }

    public void selectSort(LeaderboardSortColumn column,
            LeaderboardSortDirection direction) {
        start(new LeaderboardQuery(
                Objects.requireNonNull(column, "column"),
                Objects.requireNonNull(direction, "direction"),
                0, GRAPHICAL_PAGE_LIMIT));
    }

    public void retry() {
        State current = getState();
        start(new LeaderboardQuery(current.sortColumn(),
                current.sortDirection(), 0, GRAPHICAL_PAGE_LIMIT));
    }

    /** Called by the owning screen when authentication or connectivity is lost. */
    public void connectionLost(Throwable failure) {
        ui.dispatch(() -> {
            State published;
            synchronized (lock) {
                if (closed) return;
                generation++;
                cancelActive();
                state = new State(state.sortColumn(), state.sortDirection(),
                        false, List.of(), 0, null,
                        failure == null ? "The connection was lost. Please log in again."
                                : messageFor(failure), true);
                published = state;
            }
            observer.accept(published);
        });
    }

    private void start(LeaderboardQuery query) {
        State loadingState;
        long requestGeneration;
        CompletableFuture<LeaderboardPage> request;
        synchronized (lock) {
            if (closed) return;
            if (activeRequest != null && !activeRequest.isDone()
                    && query.equals(activeQuery)) return;
            generation++;
            requestGeneration = generation;
            cancelActive();
            activeQuery = query;
            state = new State(query.getSortColumn(), query.getSortDirection(),
                    true, state.entries(), state.totalPlayers(),
                    state.authenticatedUserRank(), null, false);
            loadingState = state;
            try {
                request = transport.load(query);
            } catch (RuntimeException exception) {
                request = CompletableFuture.failedFuture(exception);
            }
            activeRequest = request;
        }
        dispatch(loadingState);
        request.whenComplete((page, failure) -> ui.dispatch(() ->
                complete(requestGeneration, query, page, failure)));
    }

    private void complete(long requestGeneration, LeaderboardQuery query,
            LeaderboardPage page, Throwable failure) {
        State published;
        synchronized (lock) {
            if (closed || generation != requestGeneration
                    || !query.equals(activeQuery)) return;
            activeRequest = null;
            activeQuery = null;
            if (failure == null && page != null) {
                state = new State(query.getSortColumn(), query.getSortDirection(),
                        false, page.getEntries(), page.getTotalPlayers(),
                        page.getAuthenticatedUserRank(), null, false);
            } else {
                state = new State(query.getSortColumn(), query.getSortDirection(),
                        false, state.entries(), state.totalPlayers(),
                        state.authenticatedUserRank(), messageFor(failure), true);
            }
            published = state;
        }
        observer.accept(published);
    }

    private void dispatch(State published) {
        ui.dispatch(() -> {
            synchronized (lock) {
                if (closed || state != published) return;
            }
            observer.accept(published);
        });
    }

    private void cancelActive() {
        if (activeRequest != null && !activeRequest.isDone()) {
            activeRequest.cancel(false);
        }
        activeRequest = null;
        activeQuery = null;
    }

    private static String messageFor(Throwable failure) {
        Throwable root = unwrap(failure);
        if (root instanceof LeaderboardException leaderboard) {
            ProtocolErrorCode code = leaderboard.getErrorCode();
            return switch (code) {
                case AUTH_REQUIRED ->
                        "Authentication is required. Please log in again.";
                case MALFORMED_PAYLOAD, VALIDATION_FAILED ->
                        "The server rejected the leaderboard request.";
                case INTERNAL_SERVER_ERROR ->
                        "The server could not load the leaderboard. Please retry.";
                default -> "Unexpected server response. Please retry.";
            };
        }
        if (find(root, TimeoutException.class) != null) {
            return "The leaderboard request timed out. Please retry.";
        }
        if (find(root, ConnectException.class) != null
                || contains(root, "could not connect")) {
            return "The leaderboard server is unavailable. Please retry.";
        }
        if (find(root, SocketException.class) != null
                || contains(root, "disconnected")
                || contains(root, "not connected")) {
            return "The connection was lost. Please retry.";
        }
        if (root instanceof CancellationException) {
            return "The leaderboard request was cancelled.";
        }
        return "The leaderboard could not be loaded. Please retry.";
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure == null
                ? new IllegalStateException("Missing leaderboard response") : failure;
        while ((current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) current = current.getCause();
        return current;
    }

    private static boolean contains(Throwable failure, String text) {
        for (Throwable current = failure; current != null;
                current = current.getCause()) {
            if (current.getMessage() != null
                    && current.getMessage().toLowerCase().contains(text)) return true;
        }
        return false;
    }

    private static <T extends Throwable> T find(Throwable failure, Class<T> type) {
        for (Throwable current = failure; current != null;
                current = current.getCause()) {
            if (type.isInstance(current)) return type.cast(current);
        }
        return null;
    }

    @Override public void close() {
        synchronized (lock) {
            if (closed) return;
            closed = true;
            generation++;
            cancelActive();
        }
    }
}
