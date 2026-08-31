package io.github.Plants_Vs_Zombies_2.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonObject;

import io.github.Plants_Vs_Zombies_2.model.auth.JsonUserRepository;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReaction;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionType;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

class MatchReactionProtocolValidationTest {
    @TempDir Path temporaryDirectory;

    @Test
    void reactionHandlerRequiresHandshakeAuthenticationAndCurrentOwnership()
            throws Exception {
        ServerMessageHandler handler = new ServerMessageHandler(new ServerAccountService(
                new JsonUserRepository(temporaryDirectory.resolve("users.json"))));
        try {
            ProtocolMessage request = ProtocolMessages.withPayload(
                    MessageType.SEND_MATCH_REACTION_REQUEST, "reaction-gates",
                    new MatchReaction("m1", MatchReactionType.SMILE));
            ConnectionContext context = new ConnectionContext(null);
            assertCode(handler.handle(request, context),
                    ProtocolErrorCode.HANDSHAKE_REQUIRED);
            context.completeHandshake();
            assertCode(handler.handle(request, context), ProtocolErrorCode.AUTH_REQUIRED);
            context.authenticate("alice");
            assertCode(handler.handle(request, context), ProtocolErrorCode.AUTH_REQUIRED);
        } finally {
            handler.close();
        }
    }

    @Test
    void malformedUnknownAndSenderControlledFieldsAreRejected() throws Exception {
        JsonObject unknown = new JsonObject();
        unknown.addProperty("matchId", "m1");
        unknown.addProperty("reactionType", "CUSTOM_REACTION");
        AccountServiceException unknownFailure = assertThrows(
                AccountServiceException.class,
                () -> PayloadReader.from(message(unknown)).reactionType());
        assertEquals(ProtocolErrorCode.VALIDATION_FAILED,
                unknownFailure.getErrorCode());

        JsonObject malformed = new JsonObject();
        malformed.addProperty("matchId", "m1");
        malformed.addProperty("reactionType", 42);
        AccountServiceException malformedFailure = assertThrows(
                AccountServiceException.class,
                () -> PayloadReader.from(message(malformed)).reactionType());
        assertEquals(ProtocolErrorCode.MALFORMED_PAYLOAD,
                malformedFailure.getErrorCode());

        JsonObject forged = new JsonObject();
        forged.addProperty("matchId", "m1");
        forged.addProperty("reactionType", "SMILE");
        forged.addProperty("senderUsername", "mallory");
        AccountServiceException forgedFailure = assertThrows(
                AccountServiceException.class, () -> PayloadReader.from(message(forged))
                        .requireOnlyFields("matchId", "reactionType"));
        assertEquals(ProtocolErrorCode.MALFORMED_PAYLOAD,
                forgedFailure.getErrorCode());

        JsonObject oversized = new JsonObject();
        oversized.addProperty("matchId", "m1");
        oversized.addProperty("reactionType", "X".repeat(65));
        AccountServiceException oversizedFailure = assertThrows(
                AccountServiceException.class,
                () -> PayloadReader.from(message(oversized)).reactionType());
        assertEquals(ProtocolErrorCode.MALFORMED_PAYLOAD,
                oversizedFailure.getErrorCode());
    }

    private static ProtocolMessage message(JsonObject payload) {
        return new ProtocolMessage(MessageType.SEND_MATCH_REACTION_REQUEST,
                "payload", ProtocolMessages.CURRENT_VERSION, payload);
    }

    private static void assertCode(ProtocolMessage response,
            ProtocolErrorCode expected) {
        assertEquals(MessageType.ERROR, response.getType());
        assertEquals(expected.name(), response.getPayload().getAsJsonObject()
                .get("code").getAsString());
    }
}
