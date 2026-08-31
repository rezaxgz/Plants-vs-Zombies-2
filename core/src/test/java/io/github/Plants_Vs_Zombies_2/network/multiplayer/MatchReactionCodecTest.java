package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

class MatchReactionCodecTest {
    private final ProtocolCodec codec = new ProtocolCodec();

    @Test
    void catalogHasExactlyThreeStableTextAndThreeStableEmojiIdentifiers() {
        assertEquals(List.of("GOOD_LUCK", "NICE_MOVE", "WELL_PLAYED",
                        "SMILE", "LAUGH", "ANGRY"),
                Arrays.stream(MatchReactionType.values()).map(Enum::name).toList());
        assertEquals(3, Arrays.stream(MatchReactionType.values())
                .filter(type -> type.getKind() == MatchReactionKind.TEXT).count());
        assertEquals(3, Arrays.stream(MatchReactionType.values())
                .filter(type -> type.getKind() == MatchReactionKind.EMOJI).count());
        assertEquals("Smile", MatchReactionType.SMILE.getDisplayText());
        assertEquals("Laugh", MatchReactionType.LAUGH.getDisplayText());
        assertEquals("Angry", MatchReactionType.ANGRY.getDisplayText());
    }

    @Test
    void requestRoundTripsWithOnlyMatchAndStableIdentifier() throws Exception {
        ProtocolMessage original = ProtocolMessages.withPayload(
                MessageType.SEND_MATCH_REACTION_REQUEST, "reaction-request",
                new MatchReaction("match-1", MatchReactionType.WELL_PLAYED));
        ProtocolMessage decoded = codec.deserialize(codec.serialize(original));
        MatchReaction reaction = codec.deserializePayload(decoded, MatchReaction.class);

        assertEquals("match-1", reaction.getMatchId());
        assertEquals(MatchReactionType.WELL_PLAYED, reaction.getReactionType());
        JsonObject payload = decoded.getPayload().getAsJsonObject();
        assertEquals(2, payload.size());
        assertEquals("WELL_PLAYED", payload.get("reactionType").getAsString());
        for (String forbidden : List.of("text", "message", "url", "markup",
                "senderUsername", "sequence", "serverTimestampMillis")) {
            assertFalse(payload.has(forbidden));
        }
    }

    @Test
    void serverEventAndReceiptRoundTripWithoutArbitraryText() throws Exception {
        MatchReactionEvent event = new MatchReactionEvent("match-1", "alice",
                MatchReactionType.LAUGH, MatchReactionKind.EMOJI, 7L, 12_345L);
        ProtocolMessage decoded = codec.deserialize(codec.serialize(
                ProtocolMessages.withPayload(MessageType.MATCH_REACTION_RECEIVED,
                        "event-1", event)));
        MatchReactionEvent roundTrip = codec.deserializePayload(decoded,
                MatchReactionEvent.class);
        assertEquals("alice", roundTrip.getSenderUsername());
        assertEquals(MatchReactionType.LAUGH, roundTrip.getReactionType());
        assertEquals(MatchReactionKind.EMOJI, roundTrip.getReactionKind());
        assertEquals(7L, roundTrip.getSequence());
        assertFalse(decoded.getPayload().getAsJsonObject().has("text"));

        MatchReactionReceipt receipt = new MatchReactionReceipt("match-1",
                MatchReactionType.LAUGH, 7L, 12_345L);
        ProtocolMessage receiptMessage = codec.deserialize(codec.serialize(
                ProtocolMessages.withPayload(MessageType.SEND_MATCH_REACTION_RESPONSE,
                        "receipt-1", receipt)));
        MatchReactionReceipt decodedReceipt = codec.deserializePayload(receiptMessage,
                MatchReactionReceipt.class);
        assertEquals(7L, decodedReceipt.getSequence());
        assertEquals(MatchReactionType.LAUGH, decodedReceipt.getReactionType());
    }

    @Test
    void unknownIdentifierCannotBecomeAValidTypedReaction() throws Exception {
        ProtocolMessage message = codec.deserialize("{\"type\":\"MATCH_REACTION_RECEIVED\","
                + "\"requestId\":\"bad\",\"protocolVersion\":1,\"payload\":{"
                + "\"matchId\":\"m\",\"senderUsername\":\"alice\","
                + "\"reactionType\":\"CUSTOM_TEXT\",\"reactionKind\":\"TEXT\","
                + "\"sequence\":1,\"serverTimestampMillis\":1}}" );
        MatchReactionEvent decoded = codec.deserializePayload(message,
                MatchReactionEvent.class);
        assertNull(decoded.getReactionType());
    }
}
