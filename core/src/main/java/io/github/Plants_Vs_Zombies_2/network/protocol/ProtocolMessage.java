package io.github.Plants_Vs_Zombies_2.network.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import java.util.Objects;

public final class ProtocolMessage {
    private final MessageType type;
    private final String requestId;
    private final int protocolVersion;
    private final JsonElement payload;

    public ProtocolMessage(MessageType type, String requestId, int protocolVersion, JsonElement payload) {
        this.type = Objects.requireNonNull(type, "type");
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        this.requestId = requestId;
        this.protocolVersion = protocolVersion;
        this.payload = payload == null ? JsonNull.INSTANCE : payload.deepCopy();
    }

    public MessageType getType() {
        return type;
    }

    public String getRequestId() {
        return requestId;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public JsonElement getPayload() {
        return payload.deepCopy();
    }
}
