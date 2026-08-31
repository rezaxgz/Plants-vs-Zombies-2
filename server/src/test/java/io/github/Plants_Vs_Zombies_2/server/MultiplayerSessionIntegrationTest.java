package io.github.Plants_Vs_Zombies_2.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.Plants_Vs_Zombies_2.network.auth.AuthenticationClient;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.client.ConnectionStatus;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.Invitation;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingClient;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingListener;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.PlayerMatchmakingState;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ActionResult;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionEvent;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionReceipt;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionType;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameClient;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameException;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameListener;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ReadyStatus;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;
import io.github.Plants_Vs_Zombies_2.network.session.RemoteAccountSession;

class MultiplayerSessionIntegrationTest {
    private static final String PASSWORD = "GoodPass1!";

    @TempDir Path temporaryDirectory;
    private Path databasePath;
    private GameServer server;
    private final List<Player> players = new ArrayList<>();

    @BeforeEach
    void startServer() throws Exception {
        databasePath = temporaryDirectory.resolve("server-users.json");
        server = new GameServer(GameServer.DEFAULT_HOST, 0, databasePath,
                Duration.ofSeconds(5));
        server.start();
    }

    @AfterEach
    void stopServer() {
        for (Player player : players) player.close();
        players.clear();
        server.close();
    }

    @Test
    void oneReadyDoesNotStartAndBothReadyStartExactlyOnce() throws Exception {
        Matched matched = match(online("alice"), online("bob"));
        ReadyStatus first = matched.plants.game.markReady(matched.matchId)
                .get(5, TimeUnit.SECONDS);
        assertEquals(MatchStatus.READY, first.getStatus());
        assertEquals(1, first.getRevision());
        assertNull(matched.plants.started.poll(100, TimeUnit.MILLISECONDS));
        ReadyStatus repeated = matched.plants.game.markReady(matched.matchId)
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, repeated.getRevision());
        assertFailure(matched.plants.game.placePlant(matched.matchId,
                "Peashooter", 0, 0, 1), ProtocolErrorCode.MATCH_NOT_ACTIVE);

        ReadyStatus second = matched.zombies.game.markReady(matched.matchId)
                .get(5, TimeUnit.SECONDS);
        assertEquals(MatchStatus.ACTIVE, second.getStatus());
        MatchStateSnapshot plantsStart = take(matched.plants.started);
        MatchStateSnapshot zombiesStart = take(matched.zombies.started);
        assertEquivalent(plantsStart, zombiesStart);
        assertEquals(2, plantsStart.getRevision());
        assertNull(matched.plants.started.poll(100, TimeUnit.MILLISECONDS));
        assertNull(matched.zombies.started.poll(100, TimeUnit.MILLISECONDS));
        assertFailure(matched.zombies.game.markReady(matched.matchId),
                ProtocolErrorCode.MATCH_ALREADY_STARTED);
    }

    @Test
    void simultaneousReadyRequestsProduceOneStartPerParticipant() throws Exception {
        Matched matched = match(online("alice"), online("bob"));
        CompletableFuture<ReadyStatus> first = matched.plants.game.markReady(matched.matchId);
        CompletableFuture<ReadyStatus> second = matched.zombies.game.markReady(matched.matchId);
        CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);

        assertEquals(MatchStatus.ACTIVE,
                take(matched.plants.started).getStatus());
        assertEquals(MatchStatus.ACTIVE,
                take(matched.zombies.started).getStatus());
        assertNull(matched.plants.started.poll(100, TimeUnit.MILLISECONDS));
        assertNull(matched.zombies.started.poll(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void authenticationMatchIdentityAndParticipationAreServerDerived() throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");
        Player carol = online("carol");
        Matched matched = match(alice, bob);
        assertFailure(carol.game.markReady(matched.matchId),
                ProtocolErrorCode.NOT_MATCH_PARTICIPANT);
        assertFailure(alice.game.getState("missing-match"),
                ProtocolErrorCode.MATCH_NOT_FOUND);
        try (NetworkClient raw = connectedClient("anonymous");
                MultiplayerGameClient anonymous = new MultiplayerGameClient(raw)) {
            assertFailure(anonymous.getState(matched.matchId),
                    ProtocolErrorCode.AUTH_REQUIRED);
            assertTrue(raw.isConnected());
        }
    }

    @Test
    void roleGatedCommandsMutateIndependentBalancesAndSharedSnapshot()
            throws Exception {
        Matched matched = start(match(online("alice"), online("bob")));
        ActionResult plant = matched.plants.game.placePlant(matched.matchId,
                "Peashooter", 0, 0, 2).get(5, TimeUnit.SECONDS);
        assertEquals(3, plant.getRevision());
        assertEquals(400, plant.getSnapshot().getPlantResource());
        assertEquals(300, plant.getSnapshot().getZombieResource());
        assertFailure(matched.plants.game.placeZombie(matched.matchId,
                "BASIC", 0, 4, 3), ProtocolErrorCode.WRONG_ROLE);
        assertFailure(matched.zombies.game.placePlant(matched.matchId,
                "Sunflower", 0, 1, 3), ProtocolErrorCode.WRONG_ROLE);
        assertFailure(matched.zombies.game.removePlant(matched.matchId,
                plant.getEntityId(), 3), ProtocolErrorCode.WRONG_ROLE);

        ActionResult zombie = matched.zombies.game.placeZombie(matched.matchId,
                "BASIC", 0, 4, 3).get(5, TimeUnit.SECONDS);
        assertEquals(4, zombie.getRevision());
        assertEquals(400, zombie.getSnapshot().getPlantResource());
        assertEquals(275, zombie.getSnapshot().getZombieResource());
        MatchStateSnapshot plantsState = matched.plants.game.getState(matched.matchId)
                .get(5, TimeUnit.SECONDS);
        MatchStateSnapshot zombiesState = matched.zombies.game.getState(matched.matchId)
                .get(5, TimeUnit.SECONDS);
        assertEquivalent(plantsState, zombiesState);
        assertEquals(4, plantsState.getRevision());
    }

    @Test
    void invalidPlacementUnknownTypesOccupancyAndResourcesDoNotMutate()
            throws Exception {
        Matched matched = start(match(online("alice"), online("bob")));
        assertFailure(matched.plants.game.placePlant(matched.matchId,
                "Peashooter", 0, 4, 2), ProtocolErrorCode.INVALID_POSITION);
        assertFailure(matched.plants.game.placePlant(matched.matchId,
                "Peashooter", -1, 0, 2), ProtocolErrorCode.INVALID_POSITION);
        assertFailure(matched.plants.game.placePlant(matched.matchId,
                "NoSuchPlant", 0, 0, 2), ProtocolErrorCode.UNKNOWN_PLANT);
        assertFailure(matched.zombies.game.placeZombie(matched.matchId,
                "BASIC", 0, 3, 2), ProtocolErrorCode.INVALID_POSITION);
        assertFailure(matched.zombies.game.placeZombie(matched.matchId,
                "NoSuchZombie", 0, 4, 2), ProtocolErrorCode.UNKNOWN_ZOMBIE);
        assertEquals(2, matched.plants.game.getState(matched.matchId)
                .get(5, TimeUnit.SECONDS).getRevision());

        ActionResult plant = matched.plants.game.placePlant(matched.matchId,
                "Peashooter", 0, 0, 2).get(5, TimeUnit.SECONDS);
        assertFailure(matched.plants.game.placePlant(matched.matchId,
                "Sunflower", 0, 0, 3), ProtocolErrorCode.POSITION_OCCUPIED);
        long revision = plant.getRevision();
        for (int column = 4; column <= 7; column++) {
            ActionResult result = matched.zombies.game.placeZombie(matched.matchId,
                    "BUCKETHEAD", 0, column, revision).get(5, TimeUnit.SECONDS);
            revision = result.getRevision();
        }
        assertFailure(matched.zombies.game.placeZombie(matched.matchId,
                "BASIC", 0, 8, revision), ProtocolErrorCode.INSUFFICIENT_RESOURCE);
        MatchStateSnapshot state = matched.plants.game.getState(matched.matchId)
                .get(5, TimeUnit.SECONDS);
        assertEquals(revision, state.getRevision());
        assertEquals(400, state.getPlantResource());
        assertEquals(0, state.getZombieResource());
    }

    @Test
    void revisionsAreAtomicAndRemovedEntityIdsAreNotReused() throws Exception {
        Matched matched = start(match(online("alice"), online("bob")));
        CompletableFuture<ActionResult> first = matched.plants.game.placePlant(
                matched.matchId, "Peashooter", 0, 0, 2);
        CompletableFuture<ActionResult> second = matched.plants.game.placePlant(
                matched.matchId, "Sunflower", 0, 0, 2);
        CompletableFuture.allOf(first.handle((v, e) -> null),
                second.handle((v, e) -> null)).get(5, TimeUnit.SECONDS);
        assertEquals(1, (first.isCompletedExceptionally() ? 0 : 1)
                + (second.isCompletedExceptionally() ? 0 : 1));
        MatchStateSnapshot state = matched.plants.game.getState(matched.matchId)
                .get(5, TimeUnit.SECONDS);
        assertEquals(3, state.getRevision());
        assertEquals(1, state.getPlants().size());
        String firstId = state.getPlants().get(0).getEntityId();
        MatchStateSnapshot repeatedState = matched.zombies.game.getState(matched.matchId)
                .get(5, TimeUnit.SECONDS);
        assertEquals(firstId, repeatedState.getPlants().get(0).getEntityId());
        assertFailure(matched.plants.game.placePlant(matched.matchId,
                "Sunflower", 1, 0, 2), ProtocolErrorCode.STALE_MATCH_REVISION);
        assertFailure(matched.plants.game.placePlant(matched.matchId,
                "Sunflower", 1, 0, 99), ProtocolErrorCode.STALE_MATCH_REVISION);

        ActionResult removed = matched.plants.game.removePlant(matched.matchId,
                firstId, 3).get(5, TimeUnit.SECONDS);
        assertEquals(state.getPlantResource(), removed.getSnapshot().getPlantResource(),
                "plant removal must not refund resource");
        ActionResult replacement = matched.plants.game.placePlant(matched.matchId,
                "Sunflower", 1, 0, 4).get(5, TimeUnit.SECONDS);
        assertNotEquals(firstId, replacement.getEntityId());
        assertFailure(matched.plants.game.removePlant(matched.matchId,
                firstId, 5), ProtocolErrorCode.ENTITY_NOT_FOUND);
        ActionResult zombie = matched.zombies.game.placeZombie(matched.matchId,
                "BASIC", 0, 4, 5).get(5, TimeUnit.SECONDS);
        assertFailure(matched.plants.game.removePlant(matched.matchId,
                zombie.getEntityId(), 6), ProtocolErrorCode.NOT_ENTITY_OWNER);
    }

    @Test
    void leaveAndDisconnectCancelSessionAndReleaseRemainingPlayer() throws Exception {
        Matched beforeStart = match(online("alice"), online("bob"));
        beforeStart.plants.game.leaveMatch(beforeStart.matchId).get(5, TimeUnit.SECONDS);
        assertEquals(beforeStart.matchId,
                take(beforeStart.zombies.cancellations).getMatchId());
        assertEquals(PlayerMatchmakingState.QUEUED,
                beforeStart.zombies.matchmaking.joinRandomQueue()
                        .get(5, TimeUnit.SECONDS).getState());
        beforeStart.zombies.matchmaking.leaveRandomQueue().get(5, TimeUnit.SECONDS);

        Matched disconnectedBeforeStart = match(online("carol"), online("dave"));
        disconnectedBeforeStart.plants.close();
        assertEquals(disconnectedBeforeStart.matchId,
                take(disconnectedBeforeStart.zombies.cancellations).getMatchId());
        assertEquals(PlayerMatchmakingState.QUEUED,
                disconnectedBeforeStart.zombies.matchmaking.joinRandomQueue()
                        .get(5, TimeUnit.SECONDS).getState());
        disconnectedBeforeStart.zombies.matchmaking.leaveRandomQueue()
                .get(5, TimeUnit.SECONDS);

        Player erin = online("erin");
        Matched active = start(match(beforeStart.zombies, erin));
        active.plants.close();
        assertEquals(active.matchId,
                take(active.zombies.cancellations).getMatchId());
        assertNull(active.zombies.cancellations.poll(100, TimeUnit.MILLISECONDS));
        assertEquals(PlayerMatchmakingState.QUEUED,
                active.zombies.matchmaking.joinRandomQueue()
                        .get(5, TimeUnit.SECONDS).getState());
    }

    @Test
    void simultaneousMatchesAreIsolatedAndCannotBeCrossControlled() throws Exception {
        Matched first = start(match(online("alice"), online("bob")));
        Matched second = start(match(online("carol"), online("dave")));
        assertFailure(first.plants.game.getState(second.matchId),
                ProtocolErrorCode.NOT_MATCH_PARTICIPANT);
        first.plants.game.placePlant(first.matchId, "Peashooter", 0, 0, 2)
                .get(5, TimeUnit.SECONDS);
        assertEquals(3, first.plants.game.getState(first.matchId)
                .get(5, TimeUnit.SECONDS).getRevision());
        assertEquals(2, second.plants.game.getState(second.matchId)
                .get(5, TimeUnit.SECONDS).getRevision());
        assertTrue(second.plants.game.getState(second.matchId)
                .get(5, TimeUnit.SECONDS).getPlants().isEmpty());
    }

    @Test
    void predefinedReactionsAreCorrelatedOrderedScopedAndRateLimited()
            throws Exception {
        Player alice = online("reaction-alice");
        Player bob = online("reaction-bob");
        Player third = online("reaction-third");
        Matched matched = start(match(alice, bob));
        List<MatchReactionType> receivedTypes = new ArrayList<>();

        MatchReactionReceipt first = matched.plants.game.sendReaction(matched.matchId,
                MatchReactionType.GOOD_LUCK).get(5, TimeUnit.SECONDS);
        assertEquals(1L, first.getSequence());
        assertReactionPair(matched, first, matched.plants.username, receivedTypes);
        assertFailure(matched.plants.game.sendReaction(matched.matchId,
                MatchReactionType.NICE_MOVE), ProtocolErrorCode.REACTION_RATE_LIMITED);

        MatchReactionReceipt second = matched.zombies.game.sendReaction(matched.matchId,
                MatchReactionType.NICE_MOVE).get(5, TimeUnit.SECONDS);
        assertEquals(2L, second.getSequence());
        assertReactionPair(matched, second, matched.zombies.username, receivedTypes);

        MatchStateSnapshot state = matched.plants.game.getState(matched.matchId)
                .get(5, TimeUnit.SECONDS);
        ActionResult gameplay = matched.plants.game.placePlant(matched.matchId,
                "Peashooter", 0, 0, state.getRevision()).get(5, TimeUnit.SECONDS);
        assertEquals(state.getRevision() + 1, gameplay.getRevision());

        Thread.sleep(1_050L);
        MatchReactionReceipt thirdReceipt = matched.plants.game.sendReaction(
                matched.matchId, MatchReactionType.WELL_PLAYED)
                .get(5, TimeUnit.SECONDS);
        assertReactionPair(matched, thirdReceipt, matched.plants.username,
                receivedTypes);
        MatchReactionReceipt fourth = matched.zombies.game.sendReaction(matched.matchId,
                MatchReactionType.SMILE).get(5, TimeUnit.SECONDS);
        assertReactionPair(matched, fourth, matched.zombies.username, receivedTypes);

        Thread.sleep(1_050L);
        MatchReactionReceipt fifth = matched.plants.game.sendReaction(matched.matchId,
                MatchReactionType.LAUGH).get(5, TimeUnit.SECONDS);
        assertReactionPair(matched, fifth, matched.plants.username, receivedTypes);
        MatchReactionReceipt sixth = matched.zombies.game.sendReaction(matched.matchId,
                MatchReactionType.ANGRY).get(5, TimeUnit.SECONDS);
        assertReactionPair(matched, sixth, matched.zombies.username, receivedTypes);

        assertEquals(List.of(MatchReactionType.GOOD_LUCK,
                MatchReactionType.NICE_MOVE, MatchReactionType.WELL_PLAYED,
                MatchReactionType.SMILE, MatchReactionType.LAUGH,
                MatchReactionType.ANGRY), receivedTypes);
        assertNull(third.reactions.poll(250, TimeUnit.MILLISECONDS));
        assertEquals(0, matched.plants.client.getPendingRequestCount());
        assertEquals(0, matched.zombies.client.getPendingRequestCount());

        matched.plants.game.leaveMatch(matched.matchId).get(5, TimeUnit.SECONDS);
        take(matched.zombies.cancellations);
        assertFailure(matched.zombies.game.sendReaction(matched.matchId,
                MatchReactionType.SMILE), ProtocolErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    void malformedUnknownAndForgedReactionPayloadsDoNotDisconnectClient()
            throws Exception {
        Matched matched = start(match(online("payload-alice"),
                online("payload-bob")));
        ProtocolMessage missing = matched.plants.client.sendRequest(
                ProtocolMessages.withPayload(MessageType.SEND_MATCH_REACTION_REQUEST,
                        "reaction-missing", java.util.Map.of("matchId", matched.matchId)))
                .get(5, TimeUnit.SECONDS);
        assertError(missing, ProtocolErrorCode.MALFORMED_PAYLOAD);

        ProtocolMessage unknown = matched.plants.client.sendRequest(
                ProtocolMessages.withPayload(MessageType.SEND_MATCH_REACTION_REQUEST,
                        "reaction-unknown", java.util.Map.of(
                                "matchId", matched.matchId,
                                "reactionType", "CUSTOM")))
                .get(5, TimeUnit.SECONDS);
        assertError(unknown, ProtocolErrorCode.VALIDATION_FAILED);

        ProtocolMessage forged = matched.plants.client.sendRequest(
                ProtocolMessages.withPayload(MessageType.SEND_MATCH_REACTION_REQUEST,
                        "reaction-forged", java.util.Map.of(
                                "matchId", matched.matchId,
                                "reactionType", "SMILE",
                                "senderUsername", "mallory")))
                .get(5, TimeUnit.SECONDS);
        assertError(forged, ProtocolErrorCode.MALFORMED_PAYLOAD);
        assertTrue(matched.plants.client.isConnected());

        MatchReactionReceipt valid = matched.plants.game.sendReaction(matched.matchId,
                MatchReactionType.SMILE).get(5, TimeUnit.SECONDS);
        assertReactionPair(matched, valid, matched.plants.username,
                new ArrayList<>());

        try (NetworkClient raw = connectedClient("reaction-anonymous");
                MultiplayerGameClient anonymous = new MultiplayerGameClient(raw)) {
            assertFailure(anonymous.sendReaction(matched.matchId,
                    MatchReactionType.SMILE), ProtocolErrorCode.AUTH_REQUIRED);
            assertTrue(raw.isConnected());
        }
    }

    @Test
    void malformedPayloadListenerFailurePendingRequestsAndShutdownAreSafe()
            throws Exception {
        Matched matched = match(online("alice"), online("bob"));
        BlockingQueue<Boolean> laterListenerCalls = new LinkedBlockingQueue<>();
        BlockingQueue<Boolean> removedListenerCalls = new LinkedBlockingQueue<>();
        MultiplayerGameListener removed = new MultiplayerGameListener() {
            @Override public void opponentReady(ReadyStatus status) {
                removedListenerCalls.add(Boolean.TRUE);
            }
        };
        matched.zombies.game.addListener(removed);
        matched.zombies.game.removeListener(removed);
        matched.zombies.game.addListener(new MultiplayerGameListener() {
            @Override public void opponentReady(ReadyStatus status) {
                throw new IllegalStateException("listener detail");
            }
        });
        matched.zombies.game.addListener(new MultiplayerGameListener() {
            @Override public void opponentReady(ReadyStatus status) {
                laterListenerCalls.add(Boolean.TRUE);
            }
        });
        matched.plants.game.markReady(matched.matchId).get(5, TimeUnit.SECONDS);
        take(matched.zombies.readyEvents);
        assertTrue(take(laterListenerCalls));
        assertNull(removedListenerCalls.poll(100, TimeUnit.MILLISECONDS));

        ProtocolMessage malformed = matched.plants.client.sendRequest(
                ProtocolMessages.empty(MessageType.PLACE_MATCH_PLANT_REQUEST,
                        "bad-game-command")).get(5, TimeUnit.SECONDS);
        assertEquals(MessageType.ERROR, malformed.getType());
        assertEquals("bad-game-command", malformed.getRequestId());
        assertEquals(ProtocolErrorCode.MALFORMED_PAYLOAD.name(),
                malformed.getPayload().getAsJsonObject().get("code").getAsString());
        assertFalse(malformed.getPayload().toString().contains("Exception"));
        assertTrue(matched.plants.client.isConnected());
        assertEquals(0, matched.plants.client.getPendingRequestCount());
        assertEquals(0, matched.zombies.client.getPendingRequestCount());

        server.close();
        awaitDisconnected(matched.plants.client);
        awaitDisconnected(matched.zombies.client);
        server = new GameServer(GameServer.DEFAULT_HOST, 0, databasePath,
                Duration.ofSeconds(5));
        server.start();
        Player aliceAgain = loginExisting("alice");
        assertFailure(aliceAgain.game.getState(matched.matchId),
                ProtocolErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    void remoteSessionSharesOneSocketAndClearsBothTypedClientsOnLogout()
            throws Exception {
        Player bob = online("bob");
        String oldHost = System.getProperty(RemoteAccountSession.HOST_PROPERTY);
        String oldPort = System.getProperty(RemoteAccountSession.PORT_PROPERTY);
        System.setProperty(RemoteAccountSession.HOST_PROPERTY, GameServer.DEFAULT_HOST);
        System.setProperty(RemoteAccountSession.PORT_PROPERTY,
                Integer.toString(server.getPort()));
        try (RemoteAccountSession alice = RemoteAccountSession.fromSystemProperties()) {
            alice.register(details("alice")).get(5, TimeUnit.SECONDS);
            alice.login("alice", PASSWORD).get(5, TimeUnit.SECONDS);
            MatchmakingClient matchmaking = alice.getMatchmakingClient();
            MultiplayerGameClient game = alice.getMultiplayerGameClient();
            assertNotNull(matchmaking);
            assertNotNull(game);
            assertEquals(2, server.getConnectionCount());
            BlockingQueue<MatchAssignment> assignments = new LinkedBlockingQueue<>();
            matchmaking.addListener(new MatchmakingListener() {
                @Override public void matchFound(MatchAssignment value) {
                    assignments.add(value);
                }
            });

            Invitation invitation = matchmaking.invitePlayer("bob")
                    .get(5, TimeUnit.SECONDS);
            take(bob.invitations);
            bob.matchmaking.respondToInvitation(invitation.getInvitationId(), true)
                    .get(5, TimeUnit.SECONDS);
            MatchAssignment aliceAssignment = take(assignments);
            take(bob.assignments);
            game.markReady(aliceAssignment.getMatchId()).get(5, TimeUnit.SECONDS);
            bob.game.markReady(aliceAssignment.getMatchId()).get(5, TimeUnit.SECONDS);
            take(bob.started);
            game.getState(aliceAssignment.getMatchId()).get(5, TimeUnit.SECONDS);
            assertNotNull(game.getCurrentSnapshot());

            alice.logout().get(5, TimeUnit.SECONDS);
            assertNull(game.getCurrentSnapshot());
            assertNull(matchmaking.getCurrentMatch());
            assertEquals(aliceAssignment.getMatchId(),
                    take(bob.cancellations).getMatchId());
        } finally {
            restoreProperty(RemoteAccountSession.HOST_PROPERTY, oldHost);
            restoreProperty(RemoteAccountSession.PORT_PROPERTY, oldPort);
        }
    }

    private Matched match(Player first, Player second) throws Exception {
        Invitation invitation = first.matchmaking.invitePlayer(second.username)
                .get(5, TimeUnit.SECONDS);
        take(second.invitations);
        second.matchmaking.respondToInvitation(invitation.getInvitationId(), true)
                .get(5, TimeUnit.SECONDS);
        MatchAssignment firstAssignment = take(first.assignments);
        MatchAssignment secondAssignment = take(second.assignments);
        assertEquals(firstAssignment.getMatchId(), secondAssignment.getMatchId());
        Player plants = firstAssignment.getRole() == MatchRole.PLANTS ? first : second;
        Player zombies = plants == first ? second : first;
        return new Matched(firstAssignment.getMatchId(), plants, zombies);
    }

    private Matched start(Matched matched) throws Exception {
        matched.plants.game.markReady(matched.matchId).get(5, TimeUnit.SECONDS);
        matched.zombies.game.markReady(matched.matchId).get(5, TimeUnit.SECONDS);
        take(matched.plants.started);
        take(matched.zombies.started);
        return matched;
    }

    private Player online(String username) throws Exception {
        Player player = new Player(username, connectedClient(username + "-client"));
        players.add(player);
        player.authentication.register(details(username)).get(5, TimeUnit.SECONDS);
        player.authentication.login(username, PASSWORD).get(5, TimeUnit.SECONDS);
        return player;
    }

    private Player loginExisting(String username) throws Exception {
        Player player = new Player(username, connectedClient(username + "-again"));
        players.add(player);
        player.authentication.login(username, PASSWORD).get(5, TimeUnit.SECONDS);
        return player;
    }

    private NetworkClient connectedClient(String name) throws Exception {
        NetworkClient client = new NetworkClient(
                GameServer.DEFAULT_HOST, server.getPort(), name);
        client.connect().get(5, TimeUnit.SECONDS);
        return client;
    }

    private static RegistrationDetails details(String username) {
        return new RegistrationDetails(username, PASSWORD, PASSWORD, "Player",
                username + "@example.com", "Male", 1, "answer", "answer");
    }

    private static void assertFailure(CompletableFuture<?> future,
            ProtocolErrorCode expected) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Expected " + expected);
        } catch (ExecutionException exception) {
            assertTrue(exception.getCause() instanceof MultiplayerGameException);
            assertEquals(expected,
                    ((MultiplayerGameException) exception.getCause()).getErrorCode());
        }
    }

    private static <T> T take(BlockingQueue<T> queue) throws Exception {
        T value = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(value, "Expected asynchronous event");
        return value;
    }

    private static void assertEquivalent(MatchStateSnapshot first,
            MatchStateSnapshot second) {
        assertEquals(first.getMatchId(), second.getMatchId());
        assertEquals(first.getStatus(), second.getStatus());
        assertEquals(first.getRevision(), second.getRevision());
        assertEquals(first.getServerTimestampEpochMillis(),
                second.getServerTimestampEpochMillis());
        assertEquals(first.getLevel(), second.getLevel());
        assertEquals(first.getSeed(), second.getSeed());
        assertEquals(first.getPlantResource(), second.getPlantResource());
        assertEquals(first.getZombieResource(), second.getZombieResource());
        assertEquals(first.getPlants().size(), second.getPlants().size());
        assertEquals(first.getZombies().size(), second.getZombies().size());
        assertEquals(first.getBrainsAvailable(), second.getBrainsAvailable());
        for (int i = 0; i < first.getPlants().size(); i++) {
            assertEquals(first.getPlants().get(i).getEntityId(),
                    second.getPlants().get(i).getEntityId());
        }
        for (int i = 0; i < first.getZombies().size(); i++) {
            assertEquals(first.getZombies().get(i).getEntityId(),
                    second.getZombies().get(i).getEntityId());
        }
    }

    private static void assertReactionPair(Matched matched,
            MatchReactionReceipt receipt, String expectedSender,
            List<MatchReactionType> receivedTypes) throws Exception {
        MatchReactionEvent plantsEvent = take(matched.plants.reactions);
        MatchReactionEvent zombiesEvent = take(matched.zombies.reactions);
        assertEquals(receipt.getMatchId(), plantsEvent.getMatchId());
        assertEquals(receipt.getReactionType(), plantsEvent.getReactionType());
        assertEquals(receipt.getSequence(), plantsEvent.getSequence());
        assertEquals(receipt.getServerTimestampMillis(),
                plantsEvent.getServerTimestampMillis());
        assertEquals(expectedSender, plantsEvent.getSenderUsername());
        assertEquals(plantsEvent.getMatchId(), zombiesEvent.getMatchId());
        assertEquals(plantsEvent.getSenderUsername(), zombiesEvent.getSenderUsername());
        assertEquals(plantsEvent.getReactionType(), zombiesEvent.getReactionType());
        assertEquals(plantsEvent.getReactionKind(), zombiesEvent.getReactionKind());
        assertEquals(plantsEvent.getSequence(), zombiesEvent.getSequence());
        assertEquals(plantsEvent.getServerTimestampMillis(),
                zombiesEvent.getServerTimestampMillis());
        receivedTypes.add(plantsEvent.getReactionType());
        assertNull(matched.plants.reactions.poll(50, TimeUnit.MILLISECONDS));
        assertNull(matched.zombies.reactions.poll(50, TimeUnit.MILLISECONDS));
    }

    private static void assertError(ProtocolMessage response,
            ProtocolErrorCode code) {
        assertEquals(MessageType.ERROR, response.getType());
        assertEquals(code.name(), response.getPayload().getAsJsonObject()
                .get("code").getAsString());
    }

    private static void awaitDisconnected(NetworkClient client) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (client.getStatus() != ConnectionStatus.DISCONNECTED) {
            if (System.nanoTime() >= deadline) throw new AssertionError("Client stayed connected");
            Thread.sleep(10);
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }

    private record Matched(String matchId, Player plants, Player zombies) { }

    private static final class Player implements AutoCloseable {
        final String username;
        final NetworkClient client;
        final AuthenticationClient authentication;
        final MatchmakingClient matchmaking;
        final MultiplayerGameClient game;
        final BlockingQueue<Invitation> invitations = new LinkedBlockingQueue<>();
        final BlockingQueue<MatchAssignment> assignments = new LinkedBlockingQueue<>();
        final BlockingQueue<ReadyStatus> readyEvents = new LinkedBlockingQueue<>();
        final BlockingQueue<MatchStateSnapshot> started = new LinkedBlockingQueue<>();
        final BlockingQueue<MatchCancelled> cancellations = new LinkedBlockingQueue<>();
        final BlockingQueue<MatchReactionEvent> reactions = new LinkedBlockingQueue<>();
        private boolean closed;

        Player(String username, NetworkClient client) {
            this.username = username;
            this.client = client;
            this.authentication = new AuthenticationClient(client);
            this.matchmaking = new MatchmakingClient(client);
            this.game = new MultiplayerGameClient(client);
            matchmaking.addListener(new MatchmakingListener() {
                @Override public void invitationReceived(Invitation value) {
                    invitations.add(value);
                }
                @Override public void matchFound(MatchAssignment value) {
                    assignments.add(value);
                }
            });
            game.addListener(new MultiplayerGameListener() {
                @Override public void opponentReady(ReadyStatus value) {
                    readyEvents.add(value);
                }
                @Override public void matchStarted(MatchStateSnapshot value) {
                    started.add(value);
                }
                @Override public void matchCancelled(MatchCancelled value) {
                    cancellations.add(value);
                }
                @Override public void reactionReceived(MatchReactionEvent value) {
                    reactions.add(value);
                }
            });
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            game.close();
            matchmaking.close();
            client.close();
        }
    }
}
