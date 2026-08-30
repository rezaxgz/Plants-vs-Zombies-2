package io.github.Plants_Vs_Zombies_2.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer.MultiplayerIZombieConfig;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ActionResult;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ReadyStatus;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

class MultiplayerSessionServiceTest {
    private final List<MatchmakingEvent> events = new ArrayList<>();
    private MultiplayerSessionService service;

    @BeforeEach
    void createService() {
        service = new MultiplayerSessionService(events::addAll,
                MultiplayerIZombieConfig.firstBiteDefaults(),
                Clock.fixed(Instant.ofEpochMilli(10_000), ZoneOffset.UTC),
                () -> 9876L);
    }

    @AfterEach
    void closeService() {
        service.close();
    }

    @Test
    void readyLifecycleStartsExactlyOnceAndRepeatedReadyIsStable() throws Exception {
        create("match-1", "alice", "bob");
        MatchStateSnapshot initial = service.getState("alice", "match-1");
        assertEquals(MatchStatus.PRE_GAME, initial.getStatus());
        assertEquals(0, initial.getRevision());

        ReadyStatus first = service.markReady("alice", "match-1");
        assertEquals(MatchStatus.READY, first.getStatus());
        assertEquals(1, first.getRevision());
        int afterFirst = events.size();
        ReadyStatus repeated = service.markReady("alice", "match-1");
        assertEquals(1, repeated.getRevision());
        assertEquals(afterFirst, events.size());

        ReadyStatus second = service.markReady("bob", "match-1");
        assertEquals(MatchStatus.ACTIVE, second.getStatus());
        assertEquals(2, second.getRevision());
        assertEquals(2, events.stream()
                .filter(event -> event.type() == MessageType.MATCH_STARTED).count());
        assertFailure(ProtocolErrorCode.MATCH_ALREADY_STARTED,
                () -> service.markReady("bob", "match-1"));
    }

    @Test
    void rolesResourcesAndRevisionAreAuthoritative() throws Exception {
        createAndStart("match-1", "alice", "bob");
        ActionResult plant = service.placePlant("alice", "match-1",
                "Peashooter", 0, 0, 2);
        assertEquals(3, plant.getRevision());
        assertEquals(400, plant.getSnapshot().getPlantResource());
        assertEquals(300, plant.getSnapshot().getZombieResource());

        assertFailure(ProtocolErrorCode.WRONG_ROLE,
                () -> service.placeZombie("alice", "match-1", "BASIC", 0, 4, 3));
        assertFailure(ProtocolErrorCode.WRONG_ROLE,
                () -> service.placePlant("bob", "match-1", "Sunflower", 0, 1, 3));
        assertEquals(3, service.getState("bob", "match-1").getRevision());

        ActionResult zombie = service.placeZombie("bob", "match-1",
                "BASIC", 0, 4, 3);
        assertEquals(4, zombie.getRevision());
        assertEquals(400, zombie.getSnapshot().getPlantResource());
        assertEquals(275, zombie.getSnapshot().getZombieResource());
        assertEquals(4, service.getState("alice", "match-1").getRevision());
    }

    @Test
    void staleAndConcurrentSameRevisionCommandsMutateAtMostOnce() throws Exception {
        createAndStart("match-1", "alice", "bob");
        CompletableFuture<ActionResult> first = CompletableFuture.supplyAsync(() ->
                uncheckedPlacePlant("alice", "match-1", "Peashooter", 0, 0, 2));
        CompletableFuture<ActionResult> second = CompletableFuture.supplyAsync(() ->
                uncheckedPlacePlant("alice", "match-1", "Sunflower", 0, 1, 2));
        CompletableFuture.allOf(first.handle((v, e) -> null),
                second.handle((v, e) -> null)).join();

        assertEquals(1, (first.isCompletedExceptionally() ? 0 : 1)
                + (second.isCompletedExceptionally() ? 0 : 1));
        MatchStateSnapshot state = service.getState("alice", "match-1");
        assertEquals(3, state.getRevision());
        assertEquals(1, state.getPlants().size());
        assertFailure(ProtocolErrorCode.STALE_MATCH_REVISION,
                () -> service.placePlant("alice", "match-1",
                        "Sunflower", 1, 0, 2));
        assertFailure(ProtocolErrorCode.STALE_MATCH_REVISION,
                () -> service.placePlant("alice", "match-1",
                        "Sunflower", 1, 0, 99));
        assertEquals(3, service.getState("alice", "match-1").getRevision());
    }

    @Test
    void removalUsesStableIdsAndNoRefund() throws Exception {
        createAndStart("match-1", "alice", "bob");
        ActionResult placed = service.placePlant("alice", "match-1",
                "Peashooter", 0, 0, 2);
        ActionResult removed = service.removePlant("alice", "match-1",
                placed.getEntityId(), 3);
        assertEquals(4, removed.getRevision());
        assertEquals(400, removed.getSnapshot().getPlantResource());
        assertTrue(removed.getSnapshot().getPlants().isEmpty());
        ActionResult second = service.placePlant("alice", "match-1",
                "Sunflower", 0, 1, 4);
        assertNotEquals(placed.getEntityId(), second.getEntityId());
        assertFailure(ProtocolErrorCode.ENTITY_NOT_FOUND,
                () -> service.removePlant("alice", "match-1",
                        placed.getEntityId(), 5));
    }

    @Test
    void sessionsAreIsolatedAndCancellationReleasesBothPlayers() throws Exception {
        create("match-1", "alice", "bob");
        create("match-2", "carol", "dave");
        assertFailure(ProtocolErrorCode.NOT_MATCH_PARTICIPANT,
                () -> service.getState("alice", "match-2"));
        service.leave("alice", "match-1");
        assertFalse(service.hasSession("alice"));
        assertFalse(service.hasSession("bob"));
        assertTrue(service.hasSession("carol"));
        assertEquals(1, service.activeSessionCount());
        assertEquals(1, events.stream()
                .filter(event -> event.type() == MessageType.MATCH_CANCELLED).count());
        service.playerDisconnected("alice");
        assertEquals(1, service.activeSessionCount());
    }

    private void create(String id, String plants, String zombies) throws Exception {
        service.createSession(id, plants, MatchRole.PLANTS,
                zombies, MatchRole.ZOMBIES, 1_000L);
    }

    private void createAndStart(String id, String plants, String zombies)
            throws Exception {
        create(id, plants, zombies);
        service.markReady(plants, id);
        service.markReady(zombies, id);
    }

    private ActionResult uncheckedPlacePlant(String username, String matchId,
            String type, int row, int column, long revision) {
        try {
            return service.placePlant(username, matchId, type, row, column, revision);
        } catch (MultiplayerSessionException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void assertFailure(ProtocolErrorCode expected,
            ThrowingAction action) {
        MultiplayerSessionException exception = assertThrows(
                MultiplayerSessionException.class, action::run);
        assertEquals(expected, exception.getErrorCode());
    }

    @FunctionalInterface
    private interface ThrowingAction { void run() throws Exception; }
}
