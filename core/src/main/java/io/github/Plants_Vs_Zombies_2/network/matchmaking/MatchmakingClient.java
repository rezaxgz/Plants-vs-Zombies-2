package io.github.Plants_Vs_Zombies_2.network.matchmaking;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkMessageListener;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolException;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

/** Typed asynchronous matchmaking API using the existing NetworkClient socket. */
public final class MatchmakingClient implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(MatchmakingClient.class.getName());

    private final NetworkClient networkClient;
    private final ProtocolCodec codec = new ProtocolCodec();
    private final List<MatchmakingListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, Invitation> invitations = new ConcurrentHashMap<>();
    private final NetworkMessageListener networkListener = new NetworkMessageListener() {
        @Override
        public void onMessage(ProtocolMessage message) {
            receivePush(message);
        }

        @Override
        public void onDisconnected(Throwable cause) {
            clearState();
        }
    };
    private volatile QueueStatus queueStatus = availableStatus();
    private volatile MatchAssignment currentMatch;

    public MatchmakingClient(NetworkClient networkClient) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        networkClient.addListener(networkListener);
    }

    public CompletableFuture<Invitation> invitePlayer(String username) {
        return exchange(
                ProtocolMessages.withPayload(MessageType.SEND_INVITATION_REQUEST,
                        ProtocolMessages.newRequestId(), new InviteRequest(username)),
                MessageType.SEND_INVITATION_RESPONSE)
                .thenApply(message -> read(message, Invitation.class))
                .thenApply(invitation -> {
                    invitations.put(invitation.getInvitationId(), invitation);
                    return invitation;
                });
    }

    public CompletableFuture<Void> respondToInvitation(
            String invitationId, boolean accept) {
        return exchange(
                ProtocolMessages.withPayload(MessageType.RESPOND_INVITATION_REQUEST,
                        ProtocolMessages.newRequestId(),
                        new RespondRequest(invitationId, accept)),
                MessageType.RESPOND_INVITATION_RESPONSE).thenApply(ignored -> null);
    }

    public CompletableFuture<Void> cancelInvitation(String invitationId) {
        return exchange(
                ProtocolMessages.withPayload(MessageType.CANCEL_INVITATION_REQUEST,
                        ProtocolMessages.newRequestId(), new IdRequest(invitationId)),
                MessageType.CANCEL_INVITATION_RESPONSE).thenApply(ignored -> null);
    }

    public CompletableFuture<QueueStatus> joinRandomQueue() {
        return exchange(
                ProtocolMessages.empty(MessageType.JOIN_RANDOM_QUEUE_REQUEST,
                        ProtocolMessages.newRequestId()),
                MessageType.JOIN_RANDOM_QUEUE_RESPONSE)
                .thenApply(message -> read(message, QueueStatus.class))
                .thenApply(status -> {
                    queueStatus = status;
                    return status;
                });
    }

    public CompletableFuture<Void> leaveRandomQueue() {
        return exchange(
                ProtocolMessages.empty(MessageType.LEAVE_RANDOM_QUEUE_REQUEST,
                        ProtocolMessages.newRequestId()),
                MessageType.LEAVE_RANDOM_QUEUE_RESPONSE).thenApply(ignored -> {
                    queueStatus = availableStatus();
                    return null;
                });
    }

    public void addListener(MatchmakingListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(MatchmakingListener listener) {
        listeners.remove(listener);
    }

    public QueueStatus getQueueStatus() { return queueStatus; }
    public MatchAssignment getCurrentMatch() { return currentMatch; }
    public List<Invitation> getInvitations() { return List.copyOf(invitations.values()); }

    /** Clears transient snapshots on logout/disconnect without removing listeners. */
    public void clearState() {
        invitations.clear();
        queueStatus = availableStatus();
        currentMatch = null;
    }

    @Override
    public void close() {
        networkClient.removeListener(networkListener);
        listeners.clear();
        clearState();
    }

    private CompletableFuture<ProtocolMessage> exchange(
            ProtocolMessage request, MessageType expected) {
        return networkClient.sendRequest(request).thenApply(response -> {
            if (response.getType() == MessageType.ERROR) {
                throw readError(response);
            }
            if (response.getType() != expected) {
                throw new MatchmakingException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                        "Expected " + expected + " but received " + response.getType());
            }
            return response;
        });
    }

    private void receivePush(ProtocolMessage message) {
        try {
            switch (message.getType()) {
                case INVITATION_RECEIVED -> {
                    Invitation invitation = read(message, Invitation.class);
                    invitations.put(invitation.getInvitationId(), invitation);
                    notifyListeners(listener -> listener.invitationReceived(invitation));
                }
                case INVITATION_RESULT -> {
                    Invitation invitation = read(message, Invitation.class);
                    invitations.put(invitation.getInvitationId(), invitation);
                    notifyListeners(listener -> listener.invitationResult(invitation));
                }
                case QUEUE_STATUS_CHANGED -> {
                    QueueStatus status = read(message, QueueStatus.class);
                    queueStatus = status;
                    notifyListeners(listener -> listener.queueStatusChanged(status));
                }
                case MATCH_FOUND -> {
                    MatchAssignment assignment = read(message, MatchAssignment.class);
                    currentMatch = assignment;
                    queueStatus = new QueueStatus(PlayerMatchmakingState.MATCHED,
                            assignment.getCreationTimeEpochMillis(), 0);
                    notifyListeners(listener -> listener.matchFound(assignment));
                }
                case MATCH_CANCELLED -> {
                    MatchCancelled cancellation = read(message, MatchCancelled.class);
                    currentMatch = null;
                    queueStatus = availableStatus();
                    notifyListeners(listener -> listener.matchCancelled(cancellation));
                }
                default -> { }
            }
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Ignored malformed matchmaking event", exception);
        }
    }

    private <T> T read(ProtocolMessage message, Class<T> type) {
        try {
            T value = codec.deserializePayload(message, type);
            if (value == null) {
                throw new ProtocolException(ProtocolErrorCode.MALFORMED_PAYLOAD.name(),
                        message.getRequestId(), "Payload is empty");
            }
            return value;
        } catch (ProtocolException exception) {
            throw new MatchmakingException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                    "The server returned malformed matchmaking data");
        }
    }

    private MatchmakingException readError(ProtocolMessage response) {
        JsonElement payload = response.getPayload();
        if (!payload.isJsonObject()) {
            return unexpectedError();
        }
        JsonObject object = payload.getAsJsonObject();
        String code = string(object, "code");
        String message = string(object, "message");
        try {
            return code == null || message == null
                    ? unexpectedError()
                    : new MatchmakingException(ProtocolErrorCode.valueOf(code), message);
        } catch (IllegalArgumentException exception) {
            return unexpectedError();
        }
    }

    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString() ? value.getAsString() : null;
    }

    private static MatchmakingException unexpectedError() {
        return new MatchmakingException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                "The server returned a malformed error response");
    }

    private void notifyListeners(ListenerAction action) {
        for (MatchmakingListener listener : listeners) {
            try {
                action.call(listener);
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Matchmaking listener failed", exception);
            }
        }
    }

    private static QueueStatus availableStatus() {
        return new QueueStatus(PlayerMatchmakingState.AVAILABLE,
                System.currentTimeMillis(), 0);
    }

    @FunctionalInterface
    private interface ListenerAction { void call(MatchmakingListener listener); }
    private record InviteRequest(String username) { }
    private record RespondRequest(String invitationId, boolean accept) { }
    private record IdRequest(String invitationId) { }
}
