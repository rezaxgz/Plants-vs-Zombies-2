package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.Objects;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchPlayerSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameListener;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ReadyStatus;
import io.github.Plants_Vs_Zombies_2.network.session.UiDispatcher;
import io.github.Plants_Vs_Zombies_2.view.presentation.Phase3Text;

/** Render-independent PRE_GAME/readiness controller. */
public final class PregameController implements AutoCloseable {
    public record State(String matchId, String opponent, MatchRole role,
            boolean localReady, boolean opponentReady, boolean requestInFlight,
            String status, boolean cancelled) { }

    public interface Observer extends ControllerObserver<State> {
        void matchStarted(MatchStateSnapshot snapshot);
        void leaveCompleted();
    }

    private final MultiplayerTransport transport;
    private final UiDispatcher ui;
    private final MatchAssignment assignment;
    private final Observer observer;
    private boolean disposed;
    private boolean localReady;
    private boolean opponentReady;
    private boolean requestInFlight;
    private boolean startDelivered;
    private boolean leaveDelivered;
    private boolean cancelled;
    private String status = "Connected. Waiting for both players to ready.";

    private final MultiplayerGameListener listener = new MultiplayerGameListener() {
        @Override public void opponentReady(ReadyStatus readyStatus) {
            ui.dispatch(() -> applyReadyStatus(readyStatus));
        }
        @Override public void matchStarted(MatchStateSnapshot snapshot) {
            ui.dispatch(() -> deliverStart(snapshot));
        }
        @Override public void matchCancelled(MatchCancelled cancellation) {
            ui.dispatch(() -> {
                if (disposed || cancelled) return;
                cancelled = true;
                requestInFlight = false;
                status = cancellation == null
                        ? "Match cancelled. Connection lost."
                        : "Match cancelled. "
                                + Phase3Text.cancellationReason(
                                        cancellation.getReason());
                publish();
            });
        }
    };

    public PregameController(MultiplayerTransport transport,
            UiDispatcher ui, MatchAssignment assignment, Observer observer) {
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
        publish();
        refreshFromServer();
    }

    public State getState() {
        return new State(assignment.getMatchId(), assignment.getOpponentUsername(),
                assignment.getRole(), localReady, opponentReady, requestInFlight,
                status, cancelled);
    }

    public void ready() {
        if (disposed || cancelled || localReady || requestInFlight) return;
        requestInFlight = true;
        status = "Sending ready state...";
        publish();
        transport.markReady(assignment.getMatchId()).whenComplete((readyStatus, failure) ->
                ui.dispatch(() -> {
                    if (disposed) return;
                    requestInFlight = false;
                    if (failure != null) {
                        status = readableFailure(failure);
                    } else {
                        applyReadyStatus(readyStatus);
                        status = localReady
                                ? "Ready. Waiting for opponent..."
                                : "Server did not confirm ready state yet.";
                    }
                    publish();
                }));
    }

    public void leave() {
        if (disposed || requestInFlight || leaveDelivered) return;
        requestInFlight = true;
        status = "Leaving match...";
        publish();
        transport.leaveMatch(assignment.getMatchId()).whenComplete((ignored, failure) ->
                ui.dispatch(() -> {
                    if (disposed) return;
                    requestInFlight = false;
                    if (failure != null) {
                        status = readableFailure(failure);
                        publish();
                        return;
                    }
                    if (!leaveDelivered) {
                        leaveDelivered = true;
                        observer.leaveCompleted();
                    }
                }));
    }


    private void refreshFromServer() {
        transport.getState(assignment.getMatchId()).whenComplete((snapshot, failure) ->
                ui.dispatch(() -> {
                    if (disposed) return;
                    if (failure != null) {
                        status = readableFailure(failure);
                        publish();
                        return;
                    }
                    String validationError =
                            MultiplayerSnapshotValidator.snapshotError(
                                    snapshot, assignment);
                    if (validationError != null) {
                        status = validationError;
                        publish();
                        return;
                    }
                    for (MatchPlayerSnapshot player : snapshot.getPlayers()) {
                        if (player.getRole() == assignment.getRole()) {
                            localReady = player.isReady();
                        } else {
                            opponentReady = player.isReady();
                        }
                    }
                    if (snapshot.getStatus() == io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus.ACTIVE) {
                        deliverStart(snapshot);
                    } else {
                        status = localReady ? "Ready. Waiting for opponent..."
                                : "Connected. Waiting for both players to ready.";
                        publish();
                    }
                }));
    }

    private void applyReadyStatus(ReadyStatus readyStatus) {
        if (disposed || readyStatus == null
                || !assignment.getMatchId().equals(readyStatus.getMatchId())) return;
        boolean plantsReady = readyStatus.isPlantsReady();
        boolean zombiesReady = readyStatus.isZombiesReady();
        if (assignment.getRole() == MatchRole.PLANTS) {
            localReady = plantsReady;
            opponentReady = zombiesReady;
        } else {
            localReady = zombiesReady;
            opponentReady = plantsReady;
        }
        if (localReady && opponentReady) status = "Both players ready. Starting...";
        publish();
    }

    private void deliverStart(MatchStateSnapshot snapshot) {
        if (disposed || startDelivered) return;
        String validationError = MultiplayerSnapshotValidator.snapshotError(
                snapshot, assignment);
        if (validationError != null) {
            status = validationError;
            publish();
            return;
        }
        startDelivered = true;
        requestInFlight = false;
        status = "Match started.";
        publish();
        observer.matchStarted(snapshot);
    }

    private static String readableFailure(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
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
