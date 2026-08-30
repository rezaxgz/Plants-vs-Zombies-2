package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.Objects;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.Invitation;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.InvitationStatus;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingListener;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.PlayerMatchmakingState;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.QueueStatus;
import io.github.Plants_Vs_Zombies_2.network.session.UiDispatcher;

/**
 * Render-independent state machine for the outgoing direct-invite and random
 * queue controls. Incoming invitation presentation is deliberately owned by
 * the application-scoped {@link InvitationNotificationBridge}.
 */
public final class MatchmakingFlowController implements AutoCloseable {
    private enum LastAction { NONE, INVITE, CANCEL_INVITATION, JOIN_QUEUE, LEAVE_QUEUE }

    public record State(boolean requestInFlight, boolean queued,
            String pendingInvitationId, String status, boolean error) { }

    private final MatchmakingTransport transport;
    private final UiDispatcher ui;
    private final ControllerObserver<State> observer;
    private boolean disposed;
    private boolean requestInFlight;
    private boolean queued;
    private String pendingInvitationId;
    private String status = "Choose a multiplayer matchmaking mode.";
    private boolean error;
    private LastAction lastAction = LastAction.NONE;
    private String lastInviteTarget;

    private final MatchmakingListener listener = new MatchmakingListener() {
        @Override public void invitationResult(Invitation invitation) {
            ui.dispatch(() -> handleInvitationResult(invitation));
        }
        @Override public void queueStatusChanged(QueueStatus queueStatus) {
            ui.dispatch(() -> handleQueueStatus(queueStatus));
        }
        @Override public void matchCancelled(MatchCancelled cancellation) {
            ui.dispatch(() -> {
                if (disposed) return;
                requestInFlight = false;
                queued = false;
                pendingInvitationId = null;
                status = cancellation == null ? "Match cancelled."
                        : "Match cancelled: " + cancellation.getReason();
                error = false;
                publish();
            });
        }
    };

    public MatchmakingFlowController(MatchmakingTransport transport,
            UiDispatcher ui, ControllerObserver<State> observer) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.ui = Objects.requireNonNull(ui, "ui");
        this.observer = Objects.requireNonNull(observer, "observer");
        transport.addListener(listener);
        publish();
    }

    public State getState() {
        return new State(requestInFlight, queued, pendingInvitationId, status, error);
    }

    public void invite(String username) {
        if (disposed || requestInFlight || pendingInvitationId != null) return;
        String target = username == null ? "" : username.trim();
        if (target.isEmpty()) {
            failLocal("Enter an opponent username.");
            return;
        }
        lastAction = LastAction.INVITE;
        lastInviteTarget = target;
        requestInFlight = true;
        status = "Sending invitation to " + target + "...";
        error = false;
        publish();
        transport.invitePlayer(target).whenComplete((invitation, failure) ->
                ui.dispatch(() -> {
                    if (disposed) return;
                    requestInFlight = false;
                    if (failure != null) {
                        pendingInvitationId = null;
                        status = readableFailure(failure);
                        error = true;
                    } else {
                        pendingInvitationId = invitation == null ? null
                                : invitation.getInvitationId();
                        status = invitation == null
                                ? "Invitation sent. Waiting for server update."
                                : "Invitation sent to " + target + ".";
                        error = false;
                    }
                    publish();
                }));
    }

    public void cancelInvitation() {
        if (disposed || requestInFlight || pendingInvitationId == null) return;
        String invitationId = pendingInvitationId;
        lastAction = LastAction.CANCEL_INVITATION;
        requestInFlight = true;
        status = "Cancelling invitation...";
        error = false;
        publish();
        transport.cancelInvitation(invitationId).whenComplete((ignored, failure) ->
                ui.dispatch(() -> {
                    if (disposed) return;
                    requestInFlight = false;
                    if (failure != null) {
                        status = readableFailure(failure);
                        error = true;
                    } else {
                        pendingInvitationId = null;
                        status = "Invitation cancelled.";
                        error = false;
                    }
                    publish();
                }));
    }

    public void joinQueue() {
        if (disposed || requestInFlight || queued) return;
        lastAction = LastAction.JOIN_QUEUE;
        requestInFlight = true;
        status = "Joining random queue...";
        error = false;
        publish();
        transport.joinRandomQueue().whenComplete((queueStatus, failure) ->
                ui.dispatch(() -> {
                    if (disposed) return;
                    requestInFlight = false;
                    if (failure != null) {
                        status = readableFailure(failure);
                        error = true;
                    } else {
                        applyQueueState(queueStatus);
                    }
                    publish();
                }));
    }

    public void leaveQueue() {
        if (disposed || requestInFlight || !queued) return;
        lastAction = LastAction.LEAVE_QUEUE;
        requestInFlight = true;
        status = "Leaving random queue...";
        error = false;
        publish();
        transport.leaveRandomQueue().whenComplete((ignored, failure) ->
                ui.dispatch(() -> {
                    if (disposed) return;
                    requestInFlight = false;
                    if (failure != null) {
                        status = readableFailure(failure);
                        error = true;
                    } else {
                        queued = false;
                        status = "Left random queue.";
                        error = false;
                    }
                    publish();
                }));
    }

    /** Repeats only the most recent failed network command. */
    public void retryLast() {
        if (disposed || requestInFlight || !error) return;
        switch (lastAction) {
            case INVITE -> invite(lastInviteTarget);
            case CANCEL_INVITATION -> cancelInvitation();
            case JOIN_QUEUE -> joinQueue();
            case LEAVE_QUEUE -> leaveQueue();
            case NONE -> {
                status = "Nothing to retry.";
                publish();
            }
        }
    }

    /** Presents a navigation/cancellation notice without inventing server state. */
    public void showNotice(String notice) {
        if (disposed || notice == null || notice.isBlank()) return;
        status = notice;
        error = false;
        publish();
    }

    private void handleInvitationResult(Invitation invitation) {
        if (disposed || invitation == null || pendingInvitationId == null
                || !pendingInvitationId.equals(invitation.getInvitationId())) return;
        InvitationStatus result = invitation.getStatus();
        if (result != InvitationStatus.PENDING) pendingInvitationId = null;
        requestInFlight = false;
        error = false;
        status = switch (result) {
            case ACCEPTED -> "Invitation accepted. Preparing match...";
            case REJECTED -> "Invitation rejected.";
            case EXPIRED -> "Invitation expired.";
            case CANCELLED -> "Invitation cancelled.";
            case PENDING -> "Waiting for invitation response...";
        };
        publish();
    }

    private void handleQueueStatus(QueueStatus queueStatus) {
        if (disposed || queueStatus == null) return;
        applyQueueState(queueStatus);
        requestInFlight = false;
        publish();
    }

    private void applyQueueState(QueueStatus queueStatus) {
        queued = queueStatus != null
                && queueStatus.getState() == PlayerMatchmakingState.QUEUED;
        status = queued
                ? "Waiting for a random opponent (position "
                        + Math.max(1, queueStatus.getPosition()) + ")..."
                : "Random queue is idle.";
        error = false;
    }

    private void failLocal(String message) {
        status = message;
        error = true;
        publish();
    }

    private static String readableFailure(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? "Server request failed. Retry when connected." : message;
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
