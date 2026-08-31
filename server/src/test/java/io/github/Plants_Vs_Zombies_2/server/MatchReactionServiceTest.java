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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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

    @Test
    void acceptedReactionIsRegisteredBeforeLeaveCanCancelTheMatch() throws Exception {
        RegistrationBarrierExecutor executor = replaceServiceWithBarrier(
                20, 120.0, 0L);
        start("leave-race", "alice", "bob");

        CompletableFuture<MatchReactionReceipt> send = CompletableFuture.supplyAsync(
                () -> sendUnchecked("alice", "leave-race",
                        MatchReactionType.GOOD_LUCK));
        executor.awaitRegistration();
        CompletableFuture<Void> leave = CompletableFuture.runAsync(() -> {
            try {
                service.leave("bob", "leave-race");
            } catch (MultiplayerSessionException exception) {
                throw new IllegalStateException(exception);
            }
        });

        executor.allowRegistration();
        MatchReactionReceipt receipt = send.get(5, TimeUnit.SECONDS);
        leave.get(5, TimeUnit.SECONDS);
        assertRegisteredPair(receipt);
    }

    @Test
    void acceptedReactionIsRegisteredBeforeDisconnectCleanup() throws Exception {
        RegistrationBarrierExecutor executor = replaceServiceWithBarrier(
                20, 120.0, 0L);
        start("disconnect-race", "alice", "bob");

        CompletableFuture<MatchReactionReceipt> send = CompletableFuture.supplyAsync(
                () -> sendUnchecked("bob", "disconnect-race",
                        MatchReactionType.SMILE));
        executor.awaitRegistration();
        CompletableFuture<Void> disconnect = CompletableFuture.runAsync(
                () -> service.playerDisconnected("alice"));

        executor.allowRegistration();
        MatchReactionReceipt receipt = send.get(5, TimeUnit.SECONDS);
        disconnect.get(5, TimeUnit.SECONDS);
        assertRegisteredPair(receipt);
    }

    @Test
    void acceptedReactionIsRegisteredBeforeNormalFinish() throws Exception {
        RegistrationBarrierExecutor executor = replaceServiceWithBarrier(
                20, 0.05, 0L);
        start("finish-race", "alice", "bob");

        CompletableFuture<MatchReactionReceipt> send = CompletableFuture.supplyAsync(
                () -> sendUnchecked("alice", "finish-race",
                        MatchReactionType.WELL_PLAYED));
        executor.awaitRegistration();
        CompletableFuture<Void> finish = CompletableFuture.runAsync(
                service::tickOnceForTesting);

        executor.allowRegistration();
        MatchReactionReceipt receipt = send.get(5, TimeUnit.SECONDS);
        finish.get(5, TimeUnit.SECONDS);
        assertRegisteredPair(receipt);
    }

    @Test
    void shutdownDrainsEveryAcceptedReactionRegistration() throws Exception {
        RegistrationBarrierExecutor executor = replaceServiceWithBarrier(
                20, 120.0, 0L);
        start("shutdown-race", "alice", "bob");

        CompletableFuture<MatchReactionReceipt> send = CompletableFuture.supplyAsync(
                () -> sendUnchecked("bob", "shutdown-race",
                        MatchReactionType.LAUGH));
        executor.awaitRegistration();
        CompletableFuture<Void> shutdown = CompletableFuture.runAsync(service::close);

        executor.allowRegistration();
        MatchReactionReceipt receipt = send.get(5, TimeUnit.SECONDS);
        shutdown.get(5, TimeUnit.SECONDS);
        assertRegisteredPair(receipt);
        service = null;
    }

    @Test
    void publicationCallbackRunsAfterTheSessionLockIsReleased() throws Exception {
        service.close();
        events.clear();
        AtomicReference<MultiplayerSessionService> serviceReference =
                new AtomicReference<>();
        AtomicReference<Throwable> callbackFailure = new AtomicReference<>();
        AtomicBoolean stateWasReadable = new AtomicBoolean();
        service = new MultiplayerSessionService(published -> {
            if (published.stream().anyMatch(event ->
                    event.type() == MessageType.MATCH_REACTION_RECEIVED)) {
                try {
                    serviceReference.get().getState("alice", "outside-lock");
                    stateWasReadable.set(true);
                } catch (Throwable failure) {
                    callbackFailure.set(failure);
                }
            }
            events.addAll(published);
        }, MultiplayerIZombieConfig.firstBiteDefaults(), clock, () -> 42L,
                20, 120.0, 0L);
        serviceReference.set(service);
        start("outside-lock", "alice", "bob");

        service.sendReaction("alice", "outside-lock", MatchReactionType.SMILE);
        drainReactionPair();
        assertEquals(null, callbackFailure.get());
        assertTrue(stateWasReadable.get(),
                "The routing callback must execute after releasing the session monitor");
    }

    private void replaceService(int tickRate, double duration, long cooldown) {
        service.close();
        events.clear();
        service = new MultiplayerSessionService(events::addAll,
                MultiplayerIZombieConfig.firstBiteDefaults(), clock, () -> 42L,
                tickRate, duration, cooldown);
    }

    private RegistrationBarrierExecutor replaceServiceWithBarrier(int tickRate,
            double duration, long cooldown) {
        service.close();
        events.clear();
        RegistrationBarrierExecutor executor = new RegistrationBarrierExecutor();
        service = new MultiplayerSessionService(events::addAll,
                MultiplayerIZombieConfig.firstBiteDefaults(), clock, () -> 42L,
                tickRate, duration, cooldown, executor);
        return executor;
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

    private void assertRegisteredPair(MatchReactionReceipt receipt) throws Exception {
        List<MatchReactionEvent> matching = new ArrayList<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (matching.size() < 2 && System.nanoTime() < deadline) {
            MatchmakingEvent event = events.poll(100, TimeUnit.MILLISECONDS);
            if (event != null && event.type() == MessageType.MATCH_REACTION_RECEIVED) {
                MatchReactionEvent reaction = (MatchReactionEvent) event.payload();
                if (reaction.getSequence() == receipt.getSequence()) {
                    matching.add(reaction);
                }
            }
        }
        assertEquals(2, matching.size(),
                "A successful receipt must have one registered push per participant");
        assertTrue(matching.stream().allMatch(event ->
                event.getReactionType() == receipt.getReactionType()));
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

    private static final class RegistrationBarrierExecutor
            extends ThreadPoolExecutor {
        private final CountDownLatch registrationEntered = new CountDownLatch(1);
        private final CountDownLatch registrationAllowed = new CountDownLatch(1);

        private RegistrationBarrierExecutor() {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        }

        @Override
        public void execute(Runnable command) {
            registrationEntered.countDown();
            try {
                assertTrue(registrationAllowed.await(5, TimeUnit.SECONDS),
                        "Timed out waiting to release reaction registration");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            super.execute(command);
        }

        private void awaitRegistration() throws InterruptedException {
            assertTrue(registrationEntered.await(5, TimeUnit.SECONDS),
                    "Reaction never reached atomic publication registration");
        }

        private void allowRegistration() {
            registrationAllowed.countDown();
        }
    }
}
