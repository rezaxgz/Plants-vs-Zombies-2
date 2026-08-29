package io.github.Plants_Vs_Zombies_2.network.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.UUID;

public final class ProtocolMessages {
    public static final int CURRENT_VERSION = 1;

    private ProtocolMessages() {
    }

    public static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    public static ProtocolMessage clientHello(String requestId, String clientName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("clientName", clientName);
        return new ProtocolMessage(MessageType.CLIENT_HELLO, requestId, CURRENT_VERSION, payload);
    }

    public static ProtocolMessage serverHello(String requestId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("serverName", "Plants-vs-Zombies-2 Server");
        payload.addProperty("protocolVersion", CURRENT_VERSION);
        return new ProtocolMessage(MessageType.SERVER_HELLO, requestId, CURRENT_VERSION, payload);
    }

    public static ProtocolMessage ping(String requestId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("sentAtEpochMillis", Instant.now().toEpochMilli());
        return new ProtocolMessage(MessageType.PING, requestId, CURRENT_VERSION, payload);
    }

    public static ProtocolMessage pong(String requestId, JsonElement pingPayload) {
        JsonObject payload = new JsonObject();
        if (pingPayload != null && pingPayload.isJsonObject()
                && pingPayload.getAsJsonObject().has("sentAtEpochMillis")) {
            payload.add("sentAtEpochMillis", pingPayload.getAsJsonObject().get("sentAtEpochMillis"));
        }
        payload.addProperty("serverTimeEpochMillis", Instant.now().toEpochMilli());
        return new ProtocolMessage(MessageType.PONG, requestId, CURRENT_VERSION, payload);
    }

    public static ProtocolMessage error(String requestId, String code, String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("code", code);
        payload.addProperty("message", message);
        return new ProtocolMessage(MessageType.ERROR, requestId, CURRENT_VERSION, payload);
    }
}
