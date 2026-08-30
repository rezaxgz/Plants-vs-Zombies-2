package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ActionResult;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameException;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameListener;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.session.UiDispatcher;

/**
 * Graphical live-match state holder. It never simulates gameplay: movement,
 * combat, resources, time, brains and the winner all come from server snapshots.
 */
public final class LiveMatchController implements AutoCloseable {
    public enum TerminalKind { NONE, VICTORY, CANCELLATION }

    public record State(MatchStateSnapshot snapshot, MatchRole localRole,
            String opponent, boolean commandInFlight, String status,
            TerminalKind terminalKind, String cancellationReason) { }

    private final MultiplayerTransport transport;
    private final UiDispatcher ui;
    private final MatchAssignment assignment;
    private final ControllerObserver<State> observer;
    private boolean disposed;
    private boolean commandInFlight;
    private boolean terminalDelivered;
    private MatchStateSnapshot snapshot;
    private long newestTick = -1L;
    private long newestRevision = -1L;
    private String status = "Waiting for authoritative server state...";
    private TerminalKind terminalKind = TerminalKind.NONE;
    private String cancellationReason;

    private final MultiplayerGameListener listener = new MultiplayerGameListener() {
        @Override public void matchStarted(MatchStateSnapshot incoming) {
            ui.dispatch(() -> acceptSnapshot(incoming));
        }
        @Override public void matchStateUpdated(MatchStateSnapshot incoming) {
            ui.dispatch(() -> acceptSnapshot(incoming));
        }
        @Override public void matchFinished(MatchStateSnapshot incoming) {
            ui.dispatch(() -> finish(incoming));
        }
        @Override public void matchCancelled(MatchCancelled cancellation) {
            ui.dispatch(() -> cancel(cancellation));
        }
    };

    public LiveMatchController(MultiplayerTransport transport,
            UiDispatcher ui, MatchAssignment assignment,
            MatchStateSnapshot initialSnapshot, ControllerObserver<State> observer) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.ui = Objects.requireNonNull(ui, "ui");
        this.assignment = Objects.requireNonNull(assignment, "assignment");
        this.observer = Objects.requireNonNull(observer, "observer");
        transport.addListener(listener);
        if (initialSnapshot != null) acceptSnapshot(initialSnapshot);
        publish();
        refresh();
    }

    public State getState() {
        return new State(snapshot, assignment.getRole(), assignment.getOpponentUsername(),
                commandInFlight, status, terminalKind, cancellationReason);
    }

    public void refresh() {
        if (disposed || terminalKind != TerminalKind.NONE) return;
        transport.getState(assignment.getMatchId()).whenComplete((incoming, failure) ->
                ui.dispatch(() -> {
                    if (disposed) return;
                    if (failure != null) {
                        status = readableFailure(failure);
                        publish();
                    } else {
                        acceptSnapshot(incoming);
                    }
                }));
    }

    public void placePlant(String plantType, int row, int column) {
        if (assignment.getRole() != MatchRole.PLANTS) {
            rejectLocal("Only the PLANTS player can place plants.");
            return;
        }
        mutate(() -> transport.placePlant(assignment.getMatchId(), plantType,
                row, column, currentRevision()));
    }

    public void placeZombie(String zombieType, int row, int column) {
        if (assignment.getRole() != MatchRole.ZOMBIES) {
            rejectLocal("Only the ZOMBIES player can release zombies.");
            return;
        }
        mutate(() -> transport.placeZombie(assignment.getMatchId(), zombieType,
                row, column, currentRevision()));
    }

    public void removePlant(String entityId) {
        if (assignment.getRole() != MatchRole.PLANTS) {
            rejectLocal("Only the PLANTS player can remove plants.");
            return;
        }
        mutate(() -> transport.removePlant(assignment.getMatchId(), entityId,
                currentRevision()));
    }

    public void leave() {
        if (disposed || commandInFlight || terminalKind != TerminalKind.NONE) return;
        commandInFlight = true;
        status = "Leaving match...";
        publish();
        transport.leaveMatch(assignment.getMatchId()).whenComplete((ignored, failure) ->
                ui.dispatch(() -> {
                    if (disposed) return;
                    commandInFlight = false;
                    if (failure != null) {
                        status = readableFailure(failure);
                        publish();
                    } else {
                        terminalKind = TerminalKind.CANCELLATION;
                        cancellationReason = "You left the match.";
                        terminalDelivered = true;
                        publish();
                    }
                }));
    }

    private long currentRevision() {
        return snapshot == null ? -1L : snapshot.getRevision();
    }

    private void mutate(Supplier<CompletableFuture<ActionResult>> request) {
        if (disposed || commandInFlight || terminalKind != TerminalKind.NONE) return;
        if (snapshot == null) {
            rejectLocal("No authoritative snapshot yet. Retry after state refresh.");
            return;
        }
        commandInFlight = true;
        status = "Waiting for server command result...";
        publish();
        CompletableFuture<ActionResult> future;
        try {
            future = request.get();
        } catch (RuntimeException failure) {
            commandInFlight = false;
            handleMutationFailure(failure);
            return;
        }
        future.whenComplete((result, failure) -> ui.dispatch(() -> {
            if (disposed) return;
            commandInFlight = false;
            if (failure != null) {
                handleMutationFailure(failure);
                return;
            }
            if (result != null) acceptSnapshot(result.getSnapshot());
            status = "Server accepted command.";
            publish();
        }));
    }

    private void handleMutationFailure(Throwable failure) {
        Throwable cause = unwrap(failure);
        if (cause instanceof MultiplayerGameException gameFailure
                && gameFailure.getErrorCode() == ProtocolErrorCode.STALE_MATCH_REVISION) {
            status = "State changed on the server. Refreshing authoritative snapshot...";
            publish();
            refresh();
            return;
        }
        status = readableFailure(cause);
        publish();
    }

    private void rejectLocal(String message) {
        status = message;
        publish();
    }

    private void acceptSnapshot(MatchStateSnapshot incoming) {
        if (disposed || incoming == null
                || !assignment.getMatchId().equals(incoming.getMatchId())) return;
        long tick = incoming.getSimulationTick();
        long revision = incoming.getRevision();
        if (snapshot != null && (tick < newestTick
                || (tick == newestTick && revision < newestRevision))) return;
        boolean advancedTick = tick > newestTick;
        snapshot = incoming;
        newestTick = Math.max(newestTick, tick);
        newestRevision = advancedTick ? revision : Math.max(newestRevision, revision);
        status = "Authoritative state tick " + tick + ", revision " + revision;
        publish();
    }

    private void finish(MatchStateSnapshot finalSnapshot) {
        if (disposed || terminalDelivered || finalSnapshot == null
                || !assignment.getMatchId().equals(finalSnapshot.getMatchId())) return;
        // A terminal event is authoritative even if it races the last periodic update.
        snapshot = finalSnapshot;
        newestTick = Math.max(newestTick, finalSnapshot.getSimulationTick());
        newestRevision = Math.max(newestRevision, finalSnapshot.getRevision());
        terminalDelivered = true;
        terminalKind = TerminalKind.VICTORY;
        status = "Match finished: " + finalSnapshot.getFinishReason();
        publish();
    }

    private void cancel(MatchCancelled cancellation) {
        if (disposed || terminalDelivered) return;
        if (cancellation != null && cancellation.getMatchId() != null
                && !assignment.getMatchId().equals(cancellation.getMatchId())) return;
        terminalDelivered = true;
        terminalKind = TerminalKind.CANCELLATION;
        cancellationReason = cancellation == null ? "Connection lost."
                : cancellation.getReason();
        status = "Match cancelled: " + cancellationReason;
        publish();
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause;
    }

    private static String readableFailure(Throwable failure) {
        Throwable cause = unwrap(failure);
        return cause.getMessage() == null ? "Multiplayer request failed." : cause.getMessage();
    }

    private void publish() {
        if (!disposed) observer.changed(getState());
    }

    @Override public void close() {
        if (disposed) return;
        disposed = true;
        transport.removeListener(listener);
    }
}
