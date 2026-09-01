package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ActionResult;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionEvent;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionReceipt;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionType;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameException;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameListener;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.session.UiDispatcher;
import io.github.Plants_Vs_Zombies_2.view.presentation.Phase3Text;

/**
 * Graphical live-match state holder. It never simulates gameplay: movement,
 * combat, resources, time, brains and the winner all come from server snapshots.
 */
public final class LiveMatchController implements AutoCloseable {
    static final int MAX_RECENT_REACTIONS = 5;
    public enum TerminalKind { NONE, VICTORY, CANCELLATION }

    public record State(MatchStateSnapshot snapshot, MatchRole localRole,
            String opponent, boolean commandInFlight, String status,
            TerminalKind terminalKind, String cancellationReason,
            boolean reactionInFlight, List<MatchReactionEvent> recentReactions,
            String reactionStatus) {
        public State {
            recentReactions = recentReactions == null
                    ? List.of() : List.copyOf(recentReactions);
        }
    }

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
    private boolean reactionInFlight;
    private final List<MatchReactionEvent> recentReactions = new ArrayList<>();
    private long newestReactionSequence;
    private String reactionStatus = "Choose a predefined reaction.";

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
        @Override public void reactionReceived(MatchReactionEvent reaction) {
            ui.dispatch(() -> acceptReaction(reaction));
        }
        @Override public void connectionLost(Throwable cause) {
            ui.dispatch(() -> cancel(null));
        }
    };

    public LiveMatchController(MultiplayerTransport transport,
            UiDispatcher ui, MatchAssignment assignment,
            MatchStateSnapshot initialSnapshot, ControllerObserver<State> observer) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.ui = Objects.requireNonNull(ui, "ui");
        this.assignment = Objects.requireNonNull(assignment, "assignment");
        String assignmentError = MultiplayerSnapshotValidator.assignmentError(
                assignment);
        if (assignmentError != null) {
            throw new IllegalArgumentException(assignmentError);
        }
        this.observer = Objects.requireNonNull(observer, "observer");
        transport.addListener(listener);
        if (initialSnapshot != null) acceptSnapshot(initialSnapshot);
        publish();
        refresh();
    }

    public State getState() {
        return new State(snapshot, assignment.getRole(), assignment.getOpponentUsername(),
                commandInFlight, status, terminalKind, cancellationReason,
                reactionInFlight, recentReactions, reactionStatus);
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

    public void sendReaction(MatchReactionType reactionType) {
        if (disposed || reactionInFlight
                || terminalKind != TerminalKind.NONE || reactionType == null) return;
        reactionInFlight = true;
        reactionStatus = "Sending " + reactionType.getDisplayText() + "...";
        publish();
        CompletableFuture<MatchReactionReceipt> request;
        try {
            request = transport.sendReaction(assignment.getMatchId(), reactionType);
        } catch (RuntimeException failure) {
            reactionInFlight = false;
            reactionStatus = readableReactionFailure(failure);
            publish();
            return;
        }
        request.whenComplete((receipt, failure) -> ui.dispatch(() -> {
            if (disposed) return;
            reactionInFlight = false;
            reactionStatus = failure == null
                    ? receipt != null
                            && newestReactionSequence >= receipt.getSequence()
                                    ? "Reaction delivered."
                                    : "Reaction confirmed; waiting for the server event."
                    : readableReactionFailure(failure);
            publish();
        }));
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
                        clearReactions();
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
        if (disposed) return;
        String validationError = MultiplayerSnapshotValidator.snapshotError(
                incoming, assignment);
        if (validationError != null) {
            status = validationError + " Keeping the last valid state.";
            publish();
            return;
        }
        long tick = incoming.getSimulationTick();
        long revision = incoming.getRevision();
        if (snapshot != null && (tick < newestTick
                || (tick == newestTick && revision < newestRevision))) return;
        boolean advancedTick = tick > newestTick;
        snapshot = incoming;
        newestTick = Math.max(newestTick, tick);
        newestRevision = advancedTick ? revision : Math.max(newestRevision, revision);
        status = "Board synchronized with the server.";
        publish();
    }

    private void acceptReaction(MatchReactionEvent reaction) {
        if (disposed || terminalKind != TerminalKind.NONE || reaction == null
                || !assignment.getMatchId().equals(reaction.getMatchId())
                || !Phase3Text.hasText(reaction.getSenderUsername())
                || reaction.getReactionType() == null
                || reaction.getSequence() <= 0L
                || reaction.getSequence() <= newestReactionSequence) return;
        newestReactionSequence = reaction.getSequence();
        recentReactions.add(reaction);
        while (recentReactions.size() > MAX_RECENT_REACTIONS) {
            recentReactions.remove(0);
        }
        reactionStatus = "Reaction received from the server.";
        publish();
    }

    private void finish(MatchStateSnapshot finalSnapshot) {
        if (disposed || terminalDelivered) return;
        String validationError = MultiplayerSnapshotValidator.snapshotError(
                finalSnapshot, assignment);
        if (validationError != null) {
            status = validationError + " Waiting for a valid final result.";
            publish();
            return;
        }
        // A terminal event is authoritative even if it races the last periodic update.
        snapshot = finalSnapshot;
        newestTick = Math.max(newestTick, finalSnapshot.getSimulationTick());
        newestRevision = Math.max(newestRevision, finalSnapshot.getRevision());
        terminalDelivered = true;
        terminalKind = TerminalKind.VICTORY;
        clearReactions();
        status = "Match finished. " + Phase3Text.finishReason(
                finalSnapshot.getFinishReason());
        publish();
    }

    private void cancel(MatchCancelled cancellation) {
        if (disposed || terminalDelivered) return;
        if (cancellation != null && cancellation.getMatchId() != null
                && !assignment.getMatchId().equals(cancellation.getMatchId())) return;
        terminalDelivered = true;
        terminalKind = TerminalKind.CANCELLATION;
        cancellationReason = cancellation == null ? "Connection lost."
                : Phase3Text.cancellationReason(cancellation.getReason());
        status = "Match cancelled. " + cancellationReason;
        clearReactions();
        publish();
    }

    private void clearReactions() {
        reactionInFlight = false;
        recentReactions.clear();
        newestReactionSequence = 0L;
        reactionStatus = "Reaction state cleared.";
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

    private static String readableReactionFailure(Throwable failure) {
        Throwable cause = unwrap(failure);
        if (cause instanceof MultiplayerGameException gameFailure) {
            return switch (gameFailure.getErrorCode()) {
                case REACTION_RATE_LIMITED ->
                        "Reaction cooldown active. Try again shortly.";
                case MATCH_NOT_FOUND, MATCH_NOT_ACTIVE,
                        NOT_MATCH_PARTICIPANT ->
                        "This match no longer accepts reactions.";
                case AUTH_REQUIRED ->
                        "Authentication was lost; reactions are unavailable.";
                case INTERNAL_SERVER_ERROR ->
                        "The server could not send the reaction. Try again.";
                default -> "Reaction rejected: " + gameFailure.getMessage();
            };
        }
        if (cause instanceof TimeoutException) {
            return "Reaction timed out; it was not retried automatically.";
        }
        String message = cause.getMessage();
        if (message != null && (message.toLowerCase().contains("disconnect")
                || message.toLowerCase().contains("not connected"))) {
            return "Connection lost; the reaction was not sent.";
        }
        return "Reaction failed. Try again later.";
    }

    private void publish() {
        if (!disposed) observer.changed(getState());
    }

    @Override public void close() {
        if (disposed) return;
        disposed = true;
        transport.removeListener(listener);
        clearReactions();
    }
}
