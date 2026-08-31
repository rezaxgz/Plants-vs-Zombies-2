package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplaySyncException;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

/**
 * UI-owned synchronization coordinator. Public state-changing methods are
 * called on the render thread; network completions re-enter through the
 * supplied UiDispatcher. No method blocks.
 */
public final class RemoteGameplaySyncService implements AutoCloseable {
    public record Status(boolean attached, boolean dirty, boolean pending,
            boolean conflict, Throwable error, long acknowledgedRevision) { }

    private final AccountSession session;
    private final UiDispatcher ui;
    private User user;
    private GameplayState acknowledged;
    private long acknowledgedRevision;
    private boolean dirty;
    private boolean pending;
    private boolean conflict;
    private Throwable error;
    private boolean closed;
    private long generation;
    private CompletableFuture<GameplayStateSnapshot> activeResult;

    public RemoteGameplaySyncService(AccountSession session, UiDispatcher ui) {
        this.session = Objects.requireNonNull(session, "session");
        this.ui = Objects.requireNonNull(ui, "ui");
    }

    public void attach(User compatibilityUser, GameplayStateSnapshot snapshot) {
        if (closed || compatibilityUser == null || snapshot == null
                || snapshot.getState() == null) {
            throw new IllegalArgumentException("user and gameplay snapshot are required");
        }
        generation++;
        user = compatibilityUser;
        acknowledged = snapshot.getState();
        acknowledgedRevision = snapshot.getRevision();
        compatibilityUser.applyGameplayState(acknowledged);
        compatibilityUser.setGameplayRevisionForStorage(acknowledgedRevision);
        dirty = false;
        pending = false;
        conflict = false;
        error = null;
        activeResult = null;
    }

    /** Called from render/update boundaries; starts I/O only when state changed. */
    public void observeAndSynchronize() {
        if (!isAttached()) return;
        GameplayState current = GameplayState.fromUser(user);
        if (!current.equals(acknowledged)) dirty = true;
        if (dirty && !pending && !conflict && error == null) startSync(current);
    }

    public void markDirty() {
        if (isAttached()) dirty = true;
    }

    public CompletableFuture<GameplayStateSnapshot> synchronize() {
        if (!isAttached()) return unavailable();
        GameplayState current = GameplayState.fromUser(user);
        if (!current.equals(acknowledged)) dirty = true;
        if (!dirty) return CompletableFuture.completedFuture(
                new GameplayStateSnapshot(acknowledgedRevision, acknowledged));
        if (pending) return activeResult;
        conflict = false;
        error = null;
        return startSync(current);
    }

    /** Explicit retry is required after timeout, server failure, or conflict. */
    public CompletableFuture<GameplayStateSnapshot> retry() {
        if (!isAttached()) return unavailable();
        conflict = false;
        error = null;
        dirty = !GameplayState.fromUser(user).equals(acknowledged);
        return synchronize();
    }

    public CompletableFuture<GameplayStateSnapshot> refresh() {
        if (!isAttached()) return unavailable();
        long operationGeneration = generation;
        GameplayState localBefore = GameplayState.fromUser(user);
        boolean hadLocalChanges = dirty || !localBefore.equals(acknowledged);
        CompletableFuture<GameplayStateSnapshot> result = new CompletableFuture<>();
        session.refreshGameplayState().whenComplete((snapshot, failure) ->
                ui.dispatch(() -> {
                    if (!isCurrent(operationGeneration)) {
                        result.completeExceptionally(new CancellationException(
                                "Gameplay session changed"));
                        return;
                    }
                    if (failure != null) {
                        error = unwrap(failure);
                        result.completeExceptionally(error);
                        return;
                    }
                    if (snapshot.getRevision() < acknowledgedRevision) {
                        result.complete(snapshot);
                        return;
                    }
                    acknowledged = snapshot.getState();
                    acknowledgedRevision = snapshot.getRevision();
                    GameplayState current = GameplayState.fromUser(user);
                    dirty = hadLocalChanges || !current.equals(localBefore);
                    if (dirty) {
                        conflict = true;
                        error = new GameplaySyncException(
                                ProtocolErrorCode.STALE_ACCOUNT_REVISION,
                                "Server gameplay changed; local unsynchronized changes were preserved");
                    } else {
                        applyAcknowledged(snapshot);
                        conflict = false;
                        error = null;
                    }
                    result.complete(snapshot);
                }));
        return result;
    }

    public CompletableFuture<GameplayStateSnapshot> flush() {
        return synchronize();
    }

    public Status getStatus() {
        return new Status(isAttached(), dirty, pending, conflict,
                error, acknowledgedRevision);
    }

    public void detach() {
        generation++;
        user = null;
        acknowledged = null;
        acknowledgedRevision = 0;
        dirty = false;
        pending = false;
        conflict = false;
        error = null;
        activeResult = null;
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        detach();
    }

    private CompletableFuture<GameplayStateSnapshot> startSync(GameplayState sent) {
        long operationGeneration = generation;
        long expectedRevision = acknowledgedRevision;
        dirty = false;
        pending = true;
        CompletableFuture<GameplayStateSnapshot> result = new CompletableFuture<>();
        activeResult = result;
        session.synchronizeGameplayState(expectedRevision, sent)
                .whenComplete((snapshot, failure) -> ui.dispatch(() ->
                        completeSync(operationGeneration, sent, snapshot,
                                failure, result)));
        return result;
    }

    private void completeSync(long operationGeneration, GameplayState sent,
            GameplayStateSnapshot snapshot, Throwable failure,
            CompletableFuture<GameplayStateSnapshot> result) {
        if (!isCurrent(operationGeneration)) {
            result.completeExceptionally(new CancellationException(
                    "Gameplay session changed"));
            return;
        }
        pending = false;
        activeResult = null;
        if (failure != null) {
            Throwable cause = unwrap(failure);
            dirty = true;
            if (cause instanceof GameplaySyncException syncFailure
                    && syncFailure.getErrorCode()
                            == ProtocolErrorCode.STALE_ACCOUNT_REVISION) {
                refreshAfterConflict(operationGeneration, cause, result);
                return;
            }
            error = cause;
            result.completeExceptionally(cause);
            return;
        }

        if (snapshot == null || snapshot.getState() == null
                || snapshot.getRevision() <= acknowledgedRevision) {
            dirty = true;
            error = new GameplaySyncException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                    "Gameplay acknowledgement did not advance the revision");
            result.completeExceptionally(error);
            return;
        }

        acknowledged = snapshot.getState();
        acknowledgedRevision = snapshot.getRevision();
        GameplayState current = GameplayState.fromUser(user);
        if (current.equals(sent)) {
            applyAcknowledged(snapshot);
            dirty = false;
        } else {
            // A newer local mutation happened while this request was in flight.
            dirty = true;
        }
        conflict = false;
        error = null;
        result.complete(snapshot);
        if (dirty) startSync(GameplayState.fromUser(user));
    }

    private void refreshAfterConflict(long operationGeneration, Throwable stale,
            CompletableFuture<GameplayStateSnapshot> result) {
        session.refreshGameplayState().whenComplete((latest, refreshFailure) ->
                ui.dispatch(() -> {
                    if (!isCurrent(operationGeneration)) {
                        result.completeExceptionally(new CancellationException(
                                "Gameplay session changed"));
                        return;
                    }
                    if (refreshFailure == null) {
                        if (latest.getRevision() >= acknowledgedRevision) {
                            acknowledged = latest.getState();
                            acknowledgedRevision = latest.getRevision();
                        }
                    }
                    dirty = true;
                    conflict = true;
                    error = refreshFailure == null ? stale : unwrap(refreshFailure);
                    result.completeExceptionally(stale);
                }));
    }

    private void applyAcknowledged(GameplayStateSnapshot snapshot) {
        user.applyGameplayState(snapshot.getState());
        user.setGameplayRevisionForStorage(snapshot.getRevision());
        acknowledged = snapshot.getState();
        acknowledgedRevision = snapshot.getRevision();
    }

    private boolean isAttached() {
        return !closed && user != null && acknowledged != null;
    }

    private boolean isCurrent(long operationGeneration) {
        return isAttached() && generation == operationGeneration;
    }

    private static CompletableFuture<GameplayStateSnapshot> unavailable() {
        return CompletableFuture.failedFuture(
                new IllegalStateException("No remote gameplay user is attached"));
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) current = current.getCause();
        return current;
    }
}
