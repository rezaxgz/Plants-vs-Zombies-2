package io.github.Plants_Vs_Zombies_2.network.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolCodecTest {
    private final ProtocolCodec codec = new ProtocolCodec();

    @Test
    void serializesAndDeserializesEveryEnvelopeField() throws ProtocolException {
        ProtocolMessage original = ProtocolMessages.clientHello("request-123", "test-client");

        String json = codec.serialize(original);
        ProtocolMessage restored = codec.deserialize(json);

        assertEquals(MessageType.CLIENT_HELLO, restored.getType());
        assertEquals("request-123", restored.getRequestId());
        assertEquals(ProtocolMessages.CURRENT_VERSION, restored.getProtocolVersion());
        assertEquals("test-client", restored.getPayload().getAsJsonObject().get("clientName").getAsString());
        assertTrue(json.contains("\"payload\""));
    }

    @Test
    void rejectsUnknownMessageTypes() {
        String json = "{\"type\":\"LOGIN\",\"requestId\":\"r1\","
                + "\"protocolVersion\":1,\"payload\":{}}";

        ProtocolException exception = assertThrows(ProtocolException.class, () -> codec.deserialize(json));

        assertEquals("UNSUPPORTED_MESSAGE_TYPE", exception.getErrorCode());
        assertEquals("r1", exception.getRequestId());
    }
}
