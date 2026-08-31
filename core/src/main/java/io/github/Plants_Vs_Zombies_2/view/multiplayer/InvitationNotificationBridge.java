package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.Objects;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.Invitation;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.InvitationStatus;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingListener;
import io.github.Plants_Vs_Zombies_2.network.session.UiDispatcher;

/**
 * One application-scoped listener for incoming matchmaking notifications.
 * It contains no Scene2D objects; the navigator decides how to present its
 * immutable state on the currently active screen.
 */
public final class InvitationNotificationBridge implements AutoCloseable {
    public record InvitationView(String invitationId, String inviter,
            long expiresAtEpochMillis, boolean responding, String status) { }

    public interface Observer {
        void invitationChanged(InvitationView invitation);
        void matchFound(MatchAssignment assignment);
    }

    private final MatchmakingTransport transport;
    private final UiDispatcher ui;
    private final Observer observer;
    private Invitation pending;
    private boolean responding;
    private boolean closed;
    private String deliveredMatchId;
    private String responseError;

    private final MatchmakingListener listener = new MatchmakingListener() {
        @Override public void invitationReceived(Invitation invitation) {
            ui.dispatch(() -> receiveInvitation(invitation));
        }
        @Override public void invitationResult(Invitation invitation) {
            ui.dispatch(() -> receiveInvitationResult(invitation));
        }
        @Override public void matchFound(MatchAssignment assignment) {
            ui.dispatch(() -> receiveMatchFound(assignment));
        }
    };

    public InvitationNotificationBridge(MatchmakingTransport transport,
            UiDispatcher ui, Observer observer) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.ui = Objects.requireNonNull(ui, "ui");
        this.observer = Objects.requireNonNull(observer, "observer");
        transport.addListener(listener);
    }

    public InvitationView getCurrentInvitation() {
        if (pending == null) return null;
        return new InvitationView(pending.getInvitationId(),
                pending.getInviterUsername(), pending.getExpirationTimeEpochMillis(),
                responding, responding ? "Sending response..."
                        : responseError == null ? "Invitation pending" : responseError);
    }

    public void accept() { respond(true); }
    public void reject() { respond(false); }

    private void respond(boolean accept) {
        if (closed || responding || pending == null
                || pending.getStatus() != InvitationStatus.PENDING) return;
        String id = pending.getInvitationId();
        responding = true;
        responseError = null;
        publishInvitation();
        transport.respondToInvitation(id, accept).whenComplete((ignored, failure) ->
                ui.dispatch(() -> {
                    if (closed || pending == null
                            || !id.equals(pending.getInvitationId())) return;
                    responding = false;
                    if (failure != null) {
                        responseError = deepestMessage(failure);
                        publishInvitation();
                    } else if (!accept) {
                        pending = null;
                        observer.invitationChanged(null);
                    } else {
                        // MATCH_FOUND is authoritative for navigation. Remove the
                        // popup immediately so duplicate response clicks are impossible.
                        pending = null;
                        observer.invitationChanged(null);
                    }
                }));
    }

    private void receiveInvitation(Invitation invitation) {
        if (closed || invitation == null
                || invitation.getStatus() != InvitationStatus.PENDING) return;
        pending = invitation;
        responding = false;
        responseError = null;
        publishInvitation();
    }

    private void receiveInvitationResult(Invitation invitation) {
        if (closed || invitation == null || pending == null
                || !pending.getInvitationId().equals(invitation.getInvitationId())) return;
        if (invitation.getStatus() == InvitationStatus.PENDING) {
            pending = invitation;
            publishInvitation();
            return;
        }
        pending = null;
        responding = false;
        responseError = null;
        observer.invitationChanged(null);
    }

    private void receiveMatchFound(MatchAssignment assignment) {
        if (closed || assignment == null || assignment.getMatchId() == null) return;
        if (assignment.getMatchId().equals(deliveredMatchId)) return;
        deliveredMatchId = assignment.getMatchId();
        pending = null;
        responding = false;
        responseError = null;
        observer.invitationChanged(null);
        observer.matchFound(assignment);
    }

    public void clearTransientState() {
        if (closed) return;
        pending = null;
        responding = false;
        deliveredMatchId = null;
        responseError = null;
        observer.invitationChanged(null);
    }

    private void publishInvitation() {
        observer.invitationChanged(getCurrentInvitation());
    }

    private static String deepestMessage(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? "Invitation response failed." : cause.getMessage();
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        pending = null;
        transport.removeListener(listener);
    }
}
