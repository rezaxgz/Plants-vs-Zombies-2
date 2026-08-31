package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

class MultiplayerGameClientStage6Test {
    @Test
    void ignoresOutOfOrderUpdatesAndIsolatesListenerFailures() throws Exception {
        try (NetworkClient network = new NetworkClient("127.0.0.1", 0);
                MultiplayerGameClient client = new MultiplayerGameClient(network)) {
            AtomicInteger healthyCalls = new AtomicInteger();
            MultiplayerGameListener throwing = new MultiplayerGameListener() {
                @Override public void matchStateUpdated(MatchStateSnapshot snapshot) {
                    throw new IllegalStateException("listener failure");
                }
            };
            MultiplayerGameListener healthy = new MultiplayerGameListener() {
                @Override public void matchStateUpdated(MatchStateSnapshot snapshot) {
                    healthyCalls.incrementAndGet();
                }
            };
            client.addListener(throwing);
            client.addListener(healthy);

            push(client, MessageType.MATCH_STATE_UPDATED, snapshot(5, 2, MatchStatus.ACTIVE));
            push(client, MessageType.MATCH_STATE_UPDATED, snapshot(4, 99, MatchStatus.ACTIVE));
            assertEquals(5, client.getCurrentSnapshot().getSimulationTick());
            assertEquals(1, healthyCalls.get());

            client.removeListener(healthy);
            push(client, MessageType.MATCH_STATE_UPDATED, snapshot(6, 2, MatchStatus.ACTIVE));
            assertEquals(1, healthyCalls.get());
        }
    }

    @Test
    void terminalEventIsDeliveredOnceAndClearsTransientSnapshot() throws Exception {
        try (NetworkClient network = new NetworkClient("127.0.0.1", 0);
                MultiplayerGameClient client = new MultiplayerGameClient(network)) {
            AtomicInteger finished = new AtomicInteger();
            client.addListener(new MultiplayerGameListener() {
                @Override public void matchFinished(MatchStateSnapshot snapshot) {
                    finished.incrementAndGet();
                }
            });
            MatchStateSnapshot terminal = snapshot(20, 3, MatchStatus.FINISHED);
            push(client, MessageType.MATCH_FINISHED, terminal);
            push(client, MessageType.MATCH_FINISHED, terminal);
            push(client, MessageType.MATCH_STATE_UPDATED,
                    snapshot(19, 2, MatchStatus.ACTIVE));
            assertEquals(1, finished.get());
            assertNull(client.getCurrentSnapshot());
        }
    }

    @Test
    void reactionPushIsTypedAndMalformedDataAndListenerFailuresAreIsolated()
            throws Exception {
        try (NetworkClient network = new NetworkClient("127.0.0.1", 0);
                MultiplayerGameClient client = new MultiplayerGameClient(network)) {
            AtomicInteger healthyCalls = new AtomicInteger();
            client.addListener(new MultiplayerGameListener() {
                @Override public void reactionReceived(MatchReactionEvent reaction) {
                    throw new IllegalStateException("reaction listener failure");
                }
            });
            client.addListener(new MultiplayerGameListener() {
                @Override public void reactionReceived(MatchReactionEvent reaction) {
                    assertEquals(MatchReactionType.SMILE, reaction.getReactionType());
                    healthyCalls.incrementAndGet();
                }
            });

            push(client, MessageType.MATCH_REACTION_RECEIVED,
                    new MatchReactionEvent("match", "alice", MatchReactionType.SMILE,
                            MatchReactionKind.EMOJI, 1L, 1_000L));
            push(client, MessageType.MATCH_REACTION_RECEIVED,
                    new MatchReactionEvent("match", "alice", MatchReactionType.SMILE,
                            MatchReactionKind.TEXT, 2L, 1_001L));

            assertEquals(1, healthyCalls.get());
        }
    }

    private static void push(MultiplayerGameClient client, MessageType type,
            MatchStateSnapshot snapshot) throws Exception {
        push(client, type, (Object) snapshot);
    }

    private static void push(MultiplayerGameClient client, MessageType type,
            Object payload) throws Exception {
        Method method = MultiplayerGameClient.class.getDeclaredMethod(
                "receivePush",
                io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage.class);
        method.setAccessible(true);
        method.invoke(client, ProtocolMessages.withPayload(type,
                ProtocolMessages.newRequestId(), payload));
    }

    private static MatchStateSnapshot snapshot(long tick, long revision,
            MatchStatus status) {
        MatchRole winner = status == MatchStatus.FINISHED
                ? MatchRole.PLANTS : null;
        MatchFinishReason reason = status == MatchStatus.FINISHED
                ? MatchFinishReason.TIME_EXPIRED : null;
        return new MatchStateSnapshot("match", status, tick, revision, 1_000L,
                tick / 20.0, 120.0 - tick / 20.0, "FIRST_BITE", 1L,
                5, 9, 3,
                List.of(new MatchPlayerSnapshot("p", MatchRole.PLANTS, true),
                        new MatchPlayerSnapshot("z", MatchRole.ZOMBIES, true)),
                500, 300, List.of(), List.of(), List.of(),
                List.of(true, true, true, true, true), winner, reason);
    }
}
