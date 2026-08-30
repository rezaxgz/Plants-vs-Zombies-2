package io.github.Plants_Vs_Zombies_2.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer.MultiplayerIZombieConfig;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ActionResult;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchFinishReason;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

class AuthoritativeMultiplayerSimulationTest {
    private final List<MatchmakingEvent> events = new ArrayList<>();
    private MultiplayerSessionService service;

    @AfterEach
    void closeService() {
        if (service != null) service.close();
    }

    @Test
    void fixedStepsMoveZombiesAndDoNotAdvanceMutationRevision() throws Exception {
        service = service(20, 120.0);
        start("match", "plants", "zombies");
        ActionResult placed = service.placeZombie("zombies", "match",
                "BASIC", 0, 4, 2);
        long revision = placed.getRevision();
        double start = placed.getSnapshot().getZombies().get(0).getColumnPosition();

        step(20);
        MatchStateSnapshot state = service.getState("plants", "match");
        assertEquals(20, state.getSimulationTick());
        assertEquals(revision, state.getRevision());
        assertTrue(state.getZombies().get(0).getColumnPosition() < start);
        assertEquals(1.0, state.getElapsedSeconds(), 0.0001);
    }

    @Test
    void plantTargetsLaneCreatesProjectileAndDamagesZombie() throws Exception {
        service = service(20, 120.0);
        start("match", "plants", "zombies");
        ActionResult plant = service.placePlant("plants", "match",
                "Peashooter", 0, 3, 2);
        ActionResult zombie = service.placeZombie("zombies", "match",
                "BASIC", 0, 4, plant.getRevision());
        int maximum = zombie.getSnapshot().getZombies().get(0).getMaximumHealth();

        boolean sawProjectile = false;
        boolean sawDamage = false;
        for (int i = 0; i < 80; i++) {
            service.tickOnceForTesting();
            MatchStateSnapshot state = service.getState("plants", "match");
            sawProjectile |= !state.getProjectiles().isEmpty();
            if (!state.getZombies().isEmpty()) {
                sawDamage |= state.getZombies().get(0).getHealth() < maximum;
            } else {
                sawDamage = true;
            }
        }
        assertTrue(sawProjectile);
        assertTrue(sawDamage);
    }

    @Test
    void zombieStopsAndDamagesBlockingPlant() throws Exception {
        service = service(20, 120.0);
        start("match", "plants", "zombies");
        ActionResult plant = service.placePlant("plants", "match",
                "Sunflower", 0, 3, 2);
        int plantMaximum = plant.getSnapshot().getPlants().get(0).getMaximumHealth();
        service.placeZombie("zombies", "match", "BASIC", 0, 4,
                plant.getRevision());

        step(120);
        MatchStateSnapshot state = service.getState("plants", "match");
        assertTrue(state.getPlants().isEmpty()
                || state.getPlants().get(0).getHealth() < plantMaximum);
    }

    @Test
    void allBrainsConsumedFinishesOnceForBothPlayers() throws Exception {
        service = service(20, 60.0);
        start("match", "plants", "zombies");
        long revision = 2;
        for (int row = 0; row < 5; row++) {
            ActionResult result = service.placeZombie("zombies", "match",
                    "BASIC", row, 4, revision);
            revision = result.getRevision();
        }

        for (int i = 0; i < 900 && service.activeSessionCount() > 0; i++) {
            service.tickOnceForTesting();
        }
        List<MatchmakingEvent> finished = events.stream()
                .filter(event -> event.type() == MessageType.MATCH_FINISHED)
                .toList();
        assertEquals(2, finished.size());
        MatchStateSnapshot terminal = (MatchStateSnapshot) finished.get(0).payload();
        assertEquals(MatchStatus.FINISHED, terminal.getStatus());
        assertEquals(MatchRole.ZOMBIES, terminal.getWinner());
        assertEquals(MatchFinishReason.ALL_BRAINS_EATEN,
                terminal.getFinishReason());
        assertFalse(service.hasSession("plants"));
        assertFalse(service.hasSession("zombies"));

        int count = finished.size();
        step(20);
        assertEquals(count, events.stream()
                .filter(event -> event.type() == MessageType.MATCH_FINISHED).count());
    }

    @Test
    void timerExpiryAwardsPlantsAndStopsMutation() throws Exception {
        service = service(20, 1.0);
        start("match", "plants", "zombies");
        step(20);
        List<MatchmakingEvent> finished = events.stream()
                .filter(event -> event.type() == MessageType.MATCH_FINISHED)
                .toList();
        assertEquals(2, finished.size());
        MatchStateSnapshot terminal = (MatchStateSnapshot) finished.get(0).payload();
        assertEquals(MatchRole.PLANTS, terminal.getWinner());
        assertEquals(MatchFinishReason.TIME_EXPIRED, terminal.getFinishReason());
        assertEquals(0.0, terminal.getRemainingSeconds(), 0.0001);
        assertThrows(MultiplayerSessionException.class,
                () -> service.placePlant("plants", "match",
                        "Peashooter", 0, 0, terminal.getRevision()));
    }

    @Test
    void simulationTicksDoNotInvalidateMutationRevisionButStaleCommandsDo()
            throws Exception {
        service = service(20, 120.0);
        start("match", "plants", "zombies");
        step(40);
        MatchStateSnapshot state = service.getState("plants", "match");
        assertEquals(2, state.getRevision());
        assertEquals(40, state.getSimulationTick());
        ActionResult accepted = service.placePlant("plants", "match",
                "Peashooter", 0, 0, 2);
        assertEquals(3, accepted.getRevision());
        MultiplayerSessionException stale = assertThrows(
                MultiplayerSessionException.class,
                () -> service.placePlant("plants", "match",
                        "Sunflower", 1, 0, 2));
        assertEquals(ProtocolErrorCode.STALE_MATCH_REVISION,
                stale.getErrorCode());
    }

    @Test
    void entityAndProjectileIdsAreStableAndNeverReused() throws Exception {
        service = service(20, 120.0);
        start("match", "plants", "zombies");
        ActionResult first = service.placePlant("plants", "match",
                "Peashooter", 0, 3, 2);
        ActionResult removed = service.removePlant("plants", "match",
                first.getEntityId(), first.getRevision());
        ActionResult second = service.placePlant("plants", "match",
                "Peashooter", 0, 3, removed.getRevision());
        assertNotEquals(first.getEntityId(), second.getEntityId());
        service.placeZombie("zombies", "match", "BASIC", 0, 4,
                second.getRevision());

        String firstProjectile = null;
        String secondProjectile = null;
        for (int i = 0; i < 160 && secondProjectile == null; i++) {
            service.tickOnceForTesting();
            MatchStateSnapshot state = service.getState("plants", "match");
            if (!state.getProjectiles().isEmpty()) {
                String id = state.getProjectiles().get(0).getProjectileId();
                if (firstProjectile == null) firstProjectile = id;
                else if (!firstProjectile.equals(id)) secondProjectile = id;
            }
        }
        assertNotNull(firstProjectile);
        assertNotNull(secondProjectile);
        assertNotEquals(firstProjectile, secondProjectile);
    }

    @Test
    void serverShutdownUsesCancellationReasonInsteadOfDisconnectVictory()
            throws Exception {
        service = service(20, 120.0);
        start("match", "plants", "zombies");
        service.close();

        List<MatchmakingEvent> cancellations = events.stream()
                .filter(event -> event.type() == MessageType.MATCH_CANCELLED)
                .toList();
        assertEquals(2, cancellations.size());
        for (MatchmakingEvent event : cancellations) {
            io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled cancellation =
                    (io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled)
                            event.payload();
            assertEquals(MatchFinishReason.SERVER_SHUTDOWN.name(),
                    cancellation.getReason());
        }
        assertFalse(service.hasSession("plants"));
        assertFalse(service.hasSession("zombies"));
    }

    @Test
    void twoActiveMatchesRemainIsolatedAndCancellationReleasesOwnership()
            throws Exception {
        service = service(20, 120.0);
        start("one", "a", "b");
        start("two", "c", "d");
        service.placeZombie("b", "one", "BASIC", 0, 4, 2);
        step(10);
        assertEquals(1, service.getState("a", "one").getZombies().size());
        assertTrue(service.getState("c", "two").getZombies().isEmpty());
        assertEquals(10, service.getState("c", "two").getSimulationTick());

        service.playerDisconnected("a");
        assertFalse(service.hasSession("a"));
        assertFalse(service.hasSession("b"));
        assertTrue(service.hasSession("c"));
        assertEquals(MatchFinishReason.PLAYER_DISCONNECTED.name(),
                events.stream()
                        .filter(event -> event.type() == MessageType.MATCH_CANCELLED)
                        .map(event -> (io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled)
                                event.payload())
                        .findFirst().orElseThrow().getReason());
    }

    private MultiplayerSessionService service(int tickRate, double duration) {
        return new MultiplayerSessionService(events::addAll,
                MultiplayerIZombieConfig.firstBiteDefaults(),
                Clock.fixed(Instant.ofEpochMilli(10_000), ZoneOffset.UTC),
                () -> 1234L, tickRate, duration);
    }

    private void start(String matchId, String plants, String zombies)
            throws Exception {
        service.createSession(matchId, plants, MatchRole.PLANTS,
                zombies, MatchRole.ZOMBIES, 1_000L);
        service.markReady(plants, matchId);
        service.markReady(zombies, matchId);
    }

    private void step(int ticks) {
        for (int i = 0; i < ticks; i++) service.tickOnceForTesting();
    }
}
