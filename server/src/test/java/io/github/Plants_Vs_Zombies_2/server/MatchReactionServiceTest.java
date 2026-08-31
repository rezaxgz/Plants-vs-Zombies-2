package io.github.Plants_Vs_Zombies_2.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer.MultiplayerIZombieConfig;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionEvent;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionReceipt;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionType;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

class MatchReactionServiceTest {
    private final BlockingQueue<MatchmakingEvent> events = new LinkedBlockingQueue<>();
    private MutableClock clock;
    private MultiplayerSessionService service;

    @BeforeEach
    void createService() {
        clock = new MutableClock(10_000L);
        service = new MultiplayerSessionService(events::addAll,
                MultiplayerIZombieConfig.firstBiteDefaults(), clock, () -> 42L,
                20, 120.0, 1_000L);
    }

    @AfterEach
    void closeService() {
        if (service != null) service.close();
    }

    @Test
    void acceptedReactionIsAuthenticatedDeliveredToBothAndDoesNotMutateGameState()
            throws Exception {
        start("m1", "alice", "bob");
        MatchStateSnapshot before = service.getState("alice", "m1");

        MatchReactionReceipt receipt = service.sendReaction("alice", "m1",
                MatchReactionType.GOOD_LUCK);
        MatchmakingEvent first = takeReaction();
        MatchmakingEvent second = takeReaction();
        MatchReactionEvent event = (MatchReactionEvent) first.payload();

        assertEquals(Set.of("alice", "bob"), Set.of(first.username(), second.username()));
        assertSame(first.payload(), second.payload());
        assertEquals("alice", event.getSenderUsername());
        assertEquals(MatchReactionType.GOOD_LUCK, event.getReactionType());
        assertEquals(receipt.getSequence(), event.getSequence());
        assertEquals(receipt.getServerTimestampMillis(), event.getServerTimestampMillis());
        MatchStateSnapshot after = service.getState("bob", "m1");
        assertEquals(before.getRevision(), after.getRevision());
        assertEquals(before.getSimulationTick(), after.getSimulationTick());
    }

    @Test
    void activeMembershipAndLifecycleAreRequired() throws Exception {
        create("m1", "alice", "bob");
        assertFailure(ProtocolErrorCode.MATCH_NOT_ACTIVE,
                () -> service.sendReaction("alice", "m1", MatchReactionType.SMILE));
        assertFailure(ProtocolErrorCode.NOT_MATCH_PARTICIPANT,
                () -> service.sendReaction("carol", "m1", MatchReactionType.SMILE));
        assertFailure(ProtocolErrorCode.MATCH_NOT_FOUND,
                () -> service.sendReaction("alice", "missing", MatchReactionType.SMILE));

        service.markReady("alice", "m1");
        service.markReady("bob", "m1");
        service.leave("alice", "m1");
        assertFailure(ProtocolErrorCode.MATCH_NOT_FOUND,
                () -> service.sendReaction("bob", "m1", MatchReactionType.SMILE));

        replaceService(1, 0.5, 1_000L);
        start("finished", "dave", "erin");
        service.tickOnceForTesting();
        assertFailure(ProtocolErrorCode.MATCH_NOT_FOUND,
                () -> service.sendReaction("dave", "finished", MatchReactionType.SMILE));
    }

    @Test
    void cooldownIsPerPlayerAndRejectedReactionDoesNotAdvanceSequence()
            throws Exception {
        start("m1", "alice", "bob");
        MatchReactionReceipt first = service.sendReaction("alice", "m1",
                MatchReactionType.NICE_MOVE);
        drainReactionPair();
        assertFailure(ProtocolErrorCode.REACTION_RATE_LIMITED,
                () -> service.sendReaction("alice", "m1", MatchReactionType.ANGRY));

        MatchReactionReceipt opponent = service.sendReaction("bob", "m1",
                MatchReactionType.LAUGH);
        drainReactionPair();
        clock.advanceMillis(1_000L);
        MatchReactionReceipt retried = service.sendReaction("alice", "m1",
                MatchReactionType.ANGRY);
        drainReactionPair();

        assertEquals(1L, first.getSequence());
        assertEquals(2L, opponent.getSequence());
        assertEquals(3L, retried.getSequence());
    }

    @Test
    void concurrentOrderingMatchIsolationAndCleanupAreDeterministic() throws Exception {
        replaceService(20, 120.0, 0L);
        start("m1", "alice", "bob");
        start("m2", "carol", "dave");

        List<CompletableFuture<MatchReactionReceipt>> requests = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            String username = index % 2 == 0 ? "alice" : "bob";
            MatchReactionType type = MatchReactionType.values()[
                    index % MatchReactionType.values().length];
            requests.add(CompletableFuture.supplyAsync(() ->
                    sendUnchecked(username, "m1", type)));
        }
        CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).join();
        Set<Long> sequences = new HashSet<>();
        requests.forEach(request -> sequences.add(request.join().getSequence()));
        assertEquals(12, sequences.size());
        assertEquals(Set.copyOf(java.util.stream.LongStream.rangeClosed(1, 12)
                .boxed().toList()), sequences);

        for (long sequence = 1; sequence <= 12; sequence++) {
            MatchmakingEvent first = takeReaction();
            MatchmakingEvent second = takeReaction();
            MatchReactionEvent reaction = (MatchReactionEvent) first.payload();
            assertEquals(sequence, reaction.getSequence());
            assertEquals(Set.of("alice", "bob"),
                    Set.of(first.username(), second.username()));
        }

        MatchReactionReceipt other = service.sendReaction("carol", "m2",
                MatchReactionType.WELL_PLAYED);
        assertEquals(1L, other.getSequence());
        MatchmakingEvent otherFirst = takeReaction();
        MatchmakingEvent otherSecond = takeReaction();
        assertEquals(Set.of("carol", "dave"),
                Set.of(otherFirst.username(), otherSecond.username()));

        service.leave("alice", "m1");
        create("m1", "alice", "bob");
        service.markReady("alice", "m1");
        service.markReady("bob", "m1");
        events.clear();
        MatchReactionReceipt recreated = service.sendReaction("alice", "m1",
                MatchReactionType.SMILE);
        assertEquals(1L, recreated.getSequence());
        drainReactionPair();
    }

    private void replaceService(int tickRate, double duration, long cooldown) {
        service.close();
        events.clear();
        service = new MultiplayerSessionService(events::addAll,
                MultiplayerIZombieConfig.firstBiteDefaults(), clock, () -> 42L,
                tickRate, duration, cooldown);
    }

    private void create(String id, String plants, String zombies) throws Exception {
        service.createSession(id, plants, MatchRole.PLANTS,
                zombies, MatchRole.ZOMBIES, clock.millis());
    }

    private void start(String id, String plants, String zombies) throws Exception {
        create(id, plants, zombies);
        service.markReady(plants, id);
        service.markReady(zombies, id);
        events.clear();
    }

    private MatchReactionReceipt sendUnchecked(String username, String matchId,
            MatchReactionType type) {
        try {
            return service.sendReaction(username, matchId, type);
        } catch (MultiplayerSessionException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private MatchmakingEvent takeReaction() throws Exception {
        MatchmakingEvent event = events.poll(5, TimeUnit.SECONDS);
        assertTrue(event != null, "Expected reaction event");
        assertEquals(MessageType.MATCH_REACTION_RECEIVED, event.type());
        return event;
    }

    private void drainReactionPair() throws Exception {
        takeReaction();
        takeReaction();
    }

    private static void assertFailure(ProtocolErrorCode code, ThrowingAction action) {
        MultiplayerSessionException exception = assertThrows(
                MultiplayerSessionException.class, action::run);
        assertEquals(code, exception.getErrorCode());
    }

    @FunctionalInterface
    private interface ThrowingAction { void run() throws Exception; }

    private static final class MutableClock extends Clock {
        private final AtomicLong millis;

        private MutableClock(long millis) { this.millis = new AtomicLong(millis); }
        void advanceMillis(long amount) { millis.addAndGet(amount); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis()); }
        @Override public long millis() { return millis.get(); }
    }
}
