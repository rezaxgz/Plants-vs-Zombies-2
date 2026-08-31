package io.github.Plants_Vs_Zombies_2.network.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.greenHouse.PlantedPlant;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

class GameplayStateCodecTest {
    @Test
    void sanitizedGameplaySnapshotRoundTripsWithoutCredentials() throws Exception {
        User user = new User("alice", "GoodPass1!", "Alice",
                "alice@example.com", Gender.FEMALE);
        user.setSecurityQuestion(1, "private answer");
        user.addCoins(125);
        user.addDiamonds(7);
        user.addSprouts(3);
        user.getGameProgerss().setHighestScore(900);
        user.getQuestProgress().restoreCompletedCountsForStorage(4, 6);
        user.getGreenHouse().getBoard().getPotAt(1, 1).setPlant(
                new PlantedPlant("Peashooter", false, 1_000L,
                        8L * 60 * 60 * 1_000));
        user.addNews("Quest Completed", "A bounded account news entry.");
        GameplayState state = GameplayState.fromUser(user);
        ProtocolMessage message = ProtocolMessages.withPayload(
                MessageType.GET_GAMEPLAY_STATE_RESPONSE, "gameplay-codec",
                new GameplayStateSnapshot(4L, state));

        ProtocolCodec codec = new ProtocolCodec();
        String json = codec.serialize(message);
        ProtocolMessage decoded = codec.deserialize(json);
        GameplayStateSnapshot snapshot = codec.deserializePayload(decoded,
                GameplayStateSnapshot.class);

        assertEquals(4L, snapshot.getRevision());
        assertEquals(state, snapshot.getState());
        assertEquals(4, snapshot.getState().getCompletedDailyQuests());
        assertEquals(6, snapshot.getState().getCompletedNonDailyQuests());
        assertEquals(12, snapshot.getState().getGreenhousePots().size());
        assertEquals("Peashooter", snapshot.getState().getGreenhousePots()
                .get(0).getPlantName());
        assertEquals(19, snapshot.getState().getActiveQuests().size());
        assertEquals(1, snapshot.getState().getNews().size());
        assertFalse(json.contains("password"));
        assertFalse(json.contains("security"));
        assertFalse(json.contains("private answer"));
    }
}
