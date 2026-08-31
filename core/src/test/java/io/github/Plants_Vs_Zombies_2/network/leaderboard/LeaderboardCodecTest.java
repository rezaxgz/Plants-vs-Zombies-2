package io.github.Plants_Vs_Zombies_2.network.leaderboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

class LeaderboardCodecTest {
    @Test
    void pageRoundTripsWithDerivedTotalsAndNoPrivateProfileFields()
            throws Exception {
        LeaderboardEntry entry = new LeaderboardEntry(2, "alice",
                3, 4, 5, 6, 7, 800);
        LeaderboardPage page = new LeaderboardPage(List.of(entry), 120,
                2, 0, 100);
        ProtocolCodec codec = new ProtocolCodec();
        String json = codec.serialize(ProtocolMessages.withPayload(
                MessageType.GET_LEADERBOARD_RESPONSE, "leaderboard", page));
        ProtocolMessage decoded = codec.deserialize(json);
        LeaderboardPage restored = codec.deserializePayload(decoded,
                LeaderboardPage.class);

        assertEquals(13, restored.getEntries().get(0)
                .getTotalCompletedQuests());
        assertEquals(120, restored.getTotalPlayers());
        String lower = json.toLowerCase();
        assertFalse(lower.contains("password"));
        assertFalse(lower.contains("email"));
        assertFalse(lower.contains("security"));
        assertFalse(lower.contains("connection"));
        assertFalse(lower.contains("revision"));
    }
}
