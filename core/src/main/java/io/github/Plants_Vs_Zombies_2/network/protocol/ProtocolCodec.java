package io.github.Plants_Vs_Zombies_2.network.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public final class ProtocolCodec {
    private final Gson gson;

    public ProtocolCodec() {
        gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
    }

    public String serialize(ProtocolMessage message) {
        return gson.toJson(message);
    }

    public <T> T deserializePayload(ProtocolMessage message, Class<T> payloadType)
            throws ProtocolException {
        try {
            return gson.fromJson(message.getPayload(), payloadType);
        } catch (JsonParseException | IllegalStateException exception) {
            throw new ProtocolException(
                    ProtocolErrorCode.MALFORMED_PAYLOAD.name(),
                    message.getRequestId(),
                    "The response payload is malformed",
                    exception);
        }
    }

    public ProtocolMessage deserialize(String json) throws ProtocolException {
        final JsonObject envelope;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                throw new ProtocolException("INVALID_JSON", null, "The JSON message must be an object");
            }
            envelope = root.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException exception) {
            throw new ProtocolException("INVALID_JSON", null, "The message is not valid JSON", exception);
        }

        String requestId = readOptionalString(envelope, "requestId");
        String typeName = readRequiredString(envelope, "type", requestId);
        MessageType type;
        try {
            type = MessageType.valueOf(typeName);
        } catch (IllegalArgumentException exception) {
            throw new ProtocolException(
                    "UNSUPPORTED_MESSAGE_TYPE",
                    requestId,
                    "Unsupported message type: " + typeName,
                    exception);
        }

        if (requestId == null || requestId.isBlank()) {
            throw new ProtocolException("MALFORMED_MESSAGE", requestId, "requestId must not be blank");
        }

        int protocolVersion = readRequiredInteger(envelope, "protocolVersion", requestId);
        JsonElement payload = envelope.has("payload") ? envelope.get("payload") : null;
        return new ProtocolMessage(type, requestId, protocolVersion, payload);
    }

    private String readRequiredString(JsonObject envelope, String field, String requestId)
            throws ProtocolException {
        String value = readOptionalString(envelope, field);
        if (value == null || value.isBlank()) {
            throw new ProtocolException("MALFORMED_MESSAGE", requestId, field + " must be a string");
        }
        return value;
    }

    private String readOptionalString(JsonObject envelope, String field) {
        JsonElement value = envelope.get(field);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            return null;
        }
        return value.getAsString();
    }

    private int readRequiredInteger(JsonObject envelope, String field, String requestId)
            throws ProtocolException {
        JsonElement value = envelope.get(field);
        try {
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                throw new NumberFormatException();
            }
            double numericValue = value.getAsDouble();
            int integerValue = value.getAsInt();
            if (!Double.isFinite(numericValue) || numericValue != integerValue) {
                throw new NumberFormatException();
            }
            return integerValue;
        } catch (NumberFormatException exception) {
            throw new ProtocolException(
                    "MALFORMED_MESSAGE", requestId, field + " must be an integer", exception);
        }
    }
}
