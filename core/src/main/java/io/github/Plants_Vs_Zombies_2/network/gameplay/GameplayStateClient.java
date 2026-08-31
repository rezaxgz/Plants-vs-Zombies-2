package io.github.Plants_Vs_Zombies_2.network.gameplay;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolException;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

/** Typed gameplay persistence API sharing the account session's socket. */
public final class GameplayStateClient {
    private final NetworkClient networkClient;
    private final ProtocolCodec codec = new ProtocolCodec();

    public GameplayStateClient(NetworkClient networkClient) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
    }

    public CompletableFuture<GameplayStateSnapshot> getState() {
        return exchange(ProtocolMessages.empty(MessageType.GET_GAMEPLAY_STATE_REQUEST,
                ProtocolMessages.newRequestId()),
                MessageType.GET_GAMEPLAY_STATE_RESPONSE);
    }

    public CompletableFuture<GameplayStateSnapshot> synchronize(
            long expectedRevision, GameplayState state) {
        Objects.requireNonNull(state, "state");
        return exchange(ProtocolMessages.withPayload(
                MessageType.SYNC_GAMEPLAY_STATE_REQUEST,
                ProtocolMessages.newRequestId(),
                new GameplaySyncRequest(expectedRevision, state)),
                MessageType.SYNC_GAMEPLAY_STATE_RESPONSE);
    }

    private CompletableFuture<GameplayStateSnapshot> exchange(
            ProtocolMessage request, MessageType expected) {
        return networkClient.sendRequest(request).thenApply(response -> {
            if (response.getType() == MessageType.ERROR) throw readError(response);
            if (response.getType() != expected) {
                throw new GameplaySyncException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                        "Expected " + expected + " but received " + response.getType());
            }
            try {
                GameplayStateSnapshot snapshot = codec.deserializePayload(
                        response, GameplayStateSnapshot.class);
                if (snapshot == null || snapshot.getState() == null
                        || snapshot.getRevision() < 0) {
                    throw new ProtocolException(ProtocolErrorCode.MALFORMED_PAYLOAD.name(),
                            response.getRequestId(), "Gameplay snapshot is incomplete");
                }
                return snapshot;
            } catch (ProtocolException exception) {
                throw new GameplaySyncException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                        "The server returned malformed gameplay state");
            }
        });
    }

    private static GameplaySyncException readError(ProtocolMessage response) {
        JsonElement payload = response.getPayload();
        if (!payload.isJsonObject()) return malformedError();
        JsonObject object = payload.getAsJsonObject();
        JsonElement codeValue = object.get("code");
        JsonElement messageValue = object.get("message");
        if (codeValue == null || messageValue == null
                || !codeValue.isJsonPrimitive() || !messageValue.isJsonPrimitive()
                || !codeValue.getAsJsonPrimitive().isString()
                || !messageValue.getAsJsonPrimitive().isString()) return malformedError();
        try {
            return new GameplaySyncException(
                    ProtocolErrorCode.valueOf(codeValue.getAsString()),
                    messageValue.getAsString());
        } catch (IllegalArgumentException exception) {
            return malformedError();
        }
    }

    private static GameplaySyncException malformedError() {
        return new GameplaySyncException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                "The server returned a malformed error response");
    }
}
