package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkMessageListener;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolException;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

/** Typed headless multiplayer API that reuses an existing NetworkClient connection. */
public final class MultiplayerGameClient implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(MultiplayerGameClient.class.getName());

    private final NetworkClient networkClient;
    private final ProtocolCodec codec = new ProtocolCodec();
    private final List<MultiplayerGameListener> listeners = new CopyOnWriteArrayList<>();
    private final NetworkMessageListener networkListener = new NetworkMessageListener() {
        @Override public void onMessage(ProtocolMessage message) { receivePush(message); }
        @Override public void onDisconnected(Throwable cause) { clearState(); }
    };
    private volatile MatchStateSnapshot currentSnapshot;
    private volatile String terminalMatchId;
    private volatile long newestSimulationTick = -1L;
    private volatile long newestRevision = -1L;

    public MultiplayerGameClient(NetworkClient networkClient) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        networkClient.addListener(networkListener);
    }

    public CompletableFuture<ReadyStatus> markReady(String matchId) {
        return exchange(ProtocolMessages.withPayload(MessageType.MATCH_READY_REQUEST,
                ProtocolMessages.newRequestId(), new MatchRequest(matchId)),
                MessageType.MATCH_READY_RESPONSE)
                .thenApply(message -> read(message, ReadyStatus.class));
    }

    public CompletableFuture<MatchStateSnapshot> getState(String matchId) {
        return exchange(ProtocolMessages.withPayload(MessageType.GET_MATCH_STATE_REQUEST,
                ProtocolMessages.newRequestId(), new MatchRequest(matchId)),
                MessageType.GET_MATCH_STATE_RESPONSE)
                .thenApply(message -> read(message, MatchStateSnapshot.class))
                .thenApply(this::remember);
    }

    public CompletableFuture<ActionResult> placePlant(String matchId,
            String plantType, int row, int column, long expectedRevision) {
        return mutation(MessageType.PLACE_MATCH_PLANT_REQUEST,
                MessageType.PLACE_MATCH_PLANT_RESPONSE,
                new PlacementRequest(matchId, plantType, row, column, expectedRevision));
    }

    public CompletableFuture<ActionResult> removePlant(String matchId,
            String entityId, long expectedRevision) {
        return mutation(MessageType.REMOVE_MATCH_PLANT_REQUEST,
                MessageType.REMOVE_MATCH_PLANT_RESPONSE,
                new RemovalRequest(matchId, entityId, expectedRevision));
    }

    public CompletableFuture<ActionResult> placeZombie(String matchId,
            String zombieType, int row, int column, long expectedRevision) {
        return mutation(MessageType.PLACE_MATCH_ZOMBIE_REQUEST,
                MessageType.PLACE_MATCH_ZOMBIE_RESPONSE,
                new PlacementRequest(matchId, zombieType, row, column, expectedRevision));
    }

    public CompletableFuture<Void> leaveMatch(String matchId) {
        return exchange(ProtocolMessages.withPayload(MessageType.LEAVE_MATCH_REQUEST,
                ProtocolMessages.newRequestId(), new MatchRequest(matchId)),
                MessageType.LEAVE_MATCH_RESPONSE).thenApply(ignored -> {
                    clearState();
                    return null;
                });
    }

    private CompletableFuture<ActionResult> mutation(MessageType requestType,
            MessageType responseType, Object payload) {
        return exchange(ProtocolMessages.withPayload(requestType,
                ProtocolMessages.newRequestId(), payload), responseType)
                .thenApply(message -> read(message, ActionResult.class))
                .thenApply(result -> {
                    remember(result.getSnapshot());
                    return result;
                });
    }

    public void addListener(MultiplayerGameListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(MultiplayerGameListener listener) {
        listeners.remove(listener);
    }

    public MatchStateSnapshot getCurrentSnapshot() { return currentSnapshot; }

    public synchronized void clearState() {
        currentSnapshot = null;
        terminalMatchId = null;
        newestSimulationTick = -1L;
        newestRevision = -1L;
    }

    @Override
    public void close() {
        networkClient.removeListener(networkListener);
        listeners.clear();
        clearState();
    }

    private void receivePush(ProtocolMessage message) {
        try {
            switch (message.getType()) {
                case MATCH_PLAYER_READY -> {
                    ReadyStatus status = read(message, ReadyStatus.class);
                    notifyListeners(listener -> listener.opponentReady(status));
                }
                case MATCH_STARTED -> {
                    MatchStateSnapshot incoming = read(message, MatchStateSnapshot.class);
                    synchronized (this) {
                        terminalMatchId = null;
                        newestSimulationTick = -1L;
                        newestRevision = -1L;
                    }
                    MatchStateSnapshot snapshot = remember(incoming);
                    notifyListeners(listener -> listener.matchStarted(snapshot));
                }
                case MATCH_STATE_UPDATED -> {
                    MatchStateSnapshot incoming = read(message, MatchStateSnapshot.class);
                    if (rememberIfNewer(incoming)) {
                        notifyListeners(listener -> listener.matchStateUpdated(incoming));
                    }
                }
                case MATCH_FINISHED -> {
                    MatchStateSnapshot incoming = read(message, MatchStateSnapshot.class);
                    if (rememberTerminal(incoming)) {
                        notifyListeners(listener -> listener.matchFinished(incoming));
                    }
                    clearFinishedSnapshot(incoming.getMatchId());
                }
                case MATCH_CANCELLED -> {
                    MatchCancelled cancellation = read(message, MatchCancelled.class);
                    clearState();
                    notifyListeners(listener -> listener.matchCancelled(cancellation));
                }
                default -> { }
            }
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Ignored malformed multiplayer event", exception);
        }
    }

    private synchronized MatchStateSnapshot remember(MatchStateSnapshot snapshot) {
        rememberIfNewer(snapshot);
        return currentSnapshot == null ? snapshot : currentSnapshot;
    }

    private synchronized boolean rememberIfNewer(MatchStateSnapshot snapshot) {
        if (snapshot == null) return false;
        if (terminalMatchId != null
                && terminalMatchId.equals(snapshot.getMatchId())) {
            return false;
        }
        MatchStateSnapshot current = currentSnapshot;
        if (current != null && current.getMatchId().equals(snapshot.getMatchId())) {
            if (snapshot.getSimulationTick() < newestSimulationTick
                    || snapshot.getSimulationTick() == newestSimulationTick
                    && snapshot.getRevision() < newestRevision) {
                return false;
            }
        }
        currentSnapshot = snapshot;
        newestSimulationTick = snapshot.getSimulationTick();
        newestRevision = snapshot.getRevision();
        return true;
    }

    private synchronized boolean rememberTerminal(MatchStateSnapshot snapshot) {
        if (snapshot == null || snapshot.getMatchId().equals(terminalMatchId)) {
            return false;
        }
        terminalMatchId = snapshot.getMatchId();
        currentSnapshot = snapshot;
        newestSimulationTick = snapshot.getSimulationTick();
        newestRevision = snapshot.getRevision();
        return true;
    }

    private synchronized void clearFinishedSnapshot(String matchId) {
        if (currentSnapshot != null
                && currentSnapshot.getMatchId().equals(matchId)) {
            currentSnapshot = null;
        }
    }

    private CompletableFuture<ProtocolMessage> exchange(
            ProtocolMessage request, MessageType expected) {
        return networkClient.sendRequest(request).thenApply(response -> {
            if (response.getType() == MessageType.ERROR) throw readError(response);
            if (response.getType() != expected) {
                throw new MultiplayerGameException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                        "Expected " + expected + " but received " + response.getType());
            }
            return response;
        });
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
            throw new MultiplayerGameException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                    "The server returned malformed multiplayer data");
        }
    }

    private MultiplayerGameException readError(ProtocolMessage response) {
        JsonElement payload = response.getPayload();
        if (!payload.isJsonObject()) return unexpectedError();
        JsonObject object = payload.getAsJsonObject();
        String code = string(object, "code");
        String message = string(object, "message");
        try {
            return code == null || message == null ? unexpectedError()
                    : new MultiplayerGameException(ProtocolErrorCode.valueOf(code), message);
        } catch (IllegalArgumentException exception) {
            return unexpectedError();
        }
    }

    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString() ? value.getAsString() : null;
    }

    private static MultiplayerGameException unexpectedError() {
        return new MultiplayerGameException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                "The server returned a malformed error response");
    }

    private void notifyListeners(ListenerAction action) {
        for (MultiplayerGameListener listener : listeners) {
            try {
                action.call(listener);
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Multiplayer listener failed", exception);
            }
        }
    }

    @FunctionalInterface
    private interface ListenerAction { void call(MultiplayerGameListener listener); }
    private record MatchRequest(String matchId) { }
    private record PlacementRequest(String matchId, String entityType,
            int row, int column, long expectedRevision) { }
    private record RemovalRequest(String matchId, String entityId,
            long expectedRevision) { }
}
