package io.github.Plants_Vs_Zombies_2.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.Plants_Vs_Zombies_2.network.auth.AuthenticationClient;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.client.ConnectionStatus;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.Invitation;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.InvitationStatus;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingClient;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingException;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingListener;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.PlayerMatchmakingState;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.QueueStatus;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;
import io.github.Plants_Vs_Zombies_2.network.session.RemoteAccountSession;

class MatchmakingIntegrationTest {
    private static final String PASSWORD = "GoodPass1!";

    @TempDir
    Path temporaryDirectory;

    private Path databasePath;
    private GameServer server;
    private final List<Player> players = new ArrayList<>();

    @BeforeEach
    void startServer() throws Exception {
        databasePath = temporaryDirectory.resolve("server-users.json");
        server = new GameServer(GameServer.DEFAULT_HOST, 0, databasePath,
                Duration.ofMillis(150));
        server.start();
    }

    @AfterEach
    void stopServer() {
        for (Player player : players) player.close();
        players.clear();
        server.close();
    }

    @Test
    void invitationCanBeRejectedThenAcceptedIntoExactlyOneMatch() throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");

        Invitation rejected = alice.matchmaking.invitePlayer("bob").get(5, TimeUnit.SECONDS);
        assertEquals(rejected.getInvitationId(), take(bob.received).getInvitationId());
        bob.matchmaking.respondToInvitation(rejected.getInvitationId(), false)
                .get(5, TimeUnit.SECONDS);
        assertEquals(InvitationStatus.REJECTED, take(alice.results).getStatus());
        assertEquals(InvitationStatus.REJECTED, take(bob.results).getStatus());
        assertNull(alice.matches.poll(150, TimeUnit.MILLISECONDS));

        Invitation accepted = alice.matchmaking.invitePlayer("bob").get(5, TimeUnit.SECONDS);
        take(bob.received);
        bob.matchmaking.respondToInvitation(accepted.getInvitationId(), true)
                .get(5, TimeUnit.SECONDS);
        assertEquals(InvitationStatus.ACCEPTED, take(alice.results).getStatus());
        assertEquals(InvitationStatus.ACCEPTED, take(bob.results).getStatus());
        assertOppositeAssignments(take(alice.matches), take(bob.matches));
        assertNull(alice.matches.poll(150, TimeUnit.MILLISECONDS));
        assertNull(bob.matches.poll(150, TimeUnit.MILLISECONDS));
    }

    @Test
    void directInvitationReturnsStableIdentityAndAuthenticationErrors() throws Exception {
        Player alice = online("alice");
        registerOffline("offline-user");
        try (NetworkClient raw = connectedClient("anonymous");
                MatchmakingClient anonymous = new MatchmakingClient(raw)) {
            assertFailure(alice.matchmaking.invitePlayer("missing"),
                    ProtocolErrorCode.USER_NOT_FOUND);
            assertFailure(alice.matchmaking.invitePlayer("offline-user"),
                    ProtocolErrorCode.USER_OFFLINE);
            assertFailure(alice.matchmaking.invitePlayer("alice"),
                    ProtocolErrorCode.CANNOT_INVITE_SELF);
            assertFailure(anonymous.joinRandomQueue(), ProtocolErrorCode.AUTH_REQUIRED);
            assertTrue(raw.isConnected());
        }
    }

    @Test
    void duplicateBusyAndWrongRecipientRequestsAreRejected() throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");
        Player carol = online("carol");
        Invitation invitation = alice.matchmaking.invitePlayer("bob")
                .get(5, TimeUnit.SECONDS);
        take(bob.received);

        assertFailure(alice.matchmaking.invitePlayer("bob"),
                ProtocolErrorCode.DUPLICATE_INVITATION);
        assertFailure(alice.matchmaking.invitePlayer("carol"),
                ProtocolErrorCode.PLAYER_BUSY);
        assertFailure(carol.matchmaking.invitePlayer("bob"),
                ProtocolErrorCode.PLAYER_BUSY);
        assertFailure(carol.matchmaking.respondToInvitation(
                invitation.getInvitationId(), true),
                ProtocolErrorCode.INVITATION_NOT_RECIPIENT);
        alice.matchmaking.cancelInvitation(invitation.getInvitationId())
                .get(5, TimeUnit.SECONDS);
        assertEquals(InvitationStatus.CANCELLED, take(alice.results).getStatus());
        assertEquals(InvitationStatus.CANCELLED, take(bob.results).getStatus());
        assertFailure(alice.matchmaking.cancelInvitation(invitation.getInvitationId()),
                ProtocolErrorCode.INVITATION_ALREADY_RESOLVED);
    }

    @Test
    void invitationExpiresAndCannotBeAcceptedLater() throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");
        Invitation invitation = alice.matchmaking.invitePlayer("bob")
                .get(5, TimeUnit.SECONDS);
        take(bob.received);

        assertEquals(InvitationStatus.EXPIRED, take(alice.results).getStatus());
        assertEquals(InvitationStatus.EXPIRED, take(bob.results).getStatus());
        assertFailure(bob.matchmaking.respondToInvitation(
                invitation.getInvitationId(), true),
                ProtocolErrorCode.INVITATION_EXPIRED);
        assertEquals(PlayerMatchmakingState.QUEUED,
                alice.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS).getState());
    }

    @Test
    void eitherInvitationParticipantDisconnectingCancelsAndReleasesOther() throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");
        alice.matchmaking.invitePlayer("bob").get(5, TimeUnit.SECONDS);
        take(bob.received);
        alice.close();
        assertEquals(InvitationStatus.CANCELLED, take(bob.results).getStatus());
        bob.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        bob.matchmaking.leaveRandomQueue().get(5, TimeUnit.SECONDS);

        Player aliceAgain = loginExisting("alice");
        aliceAgain.matchmaking.invitePlayer("bob").get(5, TimeUnit.SECONDS);
        take(bob.received);
        bob.close();
        assertEquals(InvitationStatus.CANCELLED, take(aliceAgain.results).getStatus());
        assertEquals(PlayerMatchmakingState.QUEUED,
                aliceAgain.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS).getState());
    }

    @Test
    void simultaneousDuplicateResponsesCreateAtMostOneResultAndMatch() throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");
        Invitation invitation = alice.matchmaking.invitePlayer("bob")
                .get(5, TimeUnit.SECONDS);
        take(bob.received);

        CompletableFuture<Void> first = bob.matchmaking.respondToInvitation(
                invitation.getInvitationId(), true);
        CompletableFuture<Void> second = bob.matchmaking.respondToInvitation(
                invitation.getInvitationId(), true);
        CompletableFuture.allOf(first.handle((v, e) -> null), second.handle((v, e) -> null))
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, (first.isCompletedExceptionally() ? 0 : 1)
                + (second.isCompletedExceptionally() ? 0 : 1));
        assertOppositeAssignments(take(alice.matches), take(bob.matches));
        assertNull(alice.matches.poll(150, TimeUnit.MILLISECONDS));
        assertEquals(0, alice.client.getPendingRequestCount());
        assertEquals(0, bob.client.getPendingRequestCount());
    }

    @Test
    void randomQueueWaitsRejectsDuplicateAndMatchesFifoWithOppositeRoles()
            throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");
        QueueStatus waiting = alice.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        assertEquals(PlayerMatchmakingState.QUEUED, waiting.getState());
        assertEquals(1, waiting.getPosition());
        assertFailure(alice.matchmaking.joinRandomQueue(), ProtocolErrorCode.ALREADY_QUEUED);
        alice.matchmaking.leaveRandomQueue().get(5, TimeUnit.SECONDS);
        assertFailure(alice.matchmaking.leaveRandomQueue(), ProtocolErrorCode.NOT_QUEUED);

        alice.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        QueueStatus matched = bob.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        assertEquals(PlayerMatchmakingState.MATCHED, matched.getState());
        MatchAssignment aliceMatch = take(alice.matches);
        MatchAssignment bobMatch = take(bob.matches);
        assertEquals("bob", aliceMatch.getOpponentUsername());
        assertOppositeAssignments(aliceMatch, bobMatch);
    }

    @Test
    void queuedAndMatchedPlayersCannotEnterConflictingStates() throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");
        alice.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        assertFailure(alice.matchmaking.invitePlayer("bob"), ProtocolErrorCode.PLAYER_BUSY);
        assertFailure(bob.matchmaking.invitePlayer("alice"), ProtocolErrorCode.PLAYER_BUSY);
        bob.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        take(alice.matches);
        take(bob.matches);
        assertFailure(alice.matchmaking.joinRandomQueue(), ProtocolErrorCode.PLAYER_BUSY);
        assertFailure(bob.matchmaking.invitePlayer("alice"), ProtocolErrorCode.PLAYER_BUSY);
    }

    @Test
    void disconnectRemovesQueueEntryAndCancelsPregameMatch() throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");
        Player carol = online("carol");
        alice.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        alice.close();

        assertEquals(PlayerMatchmakingState.QUEUED,
                bob.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS).getState());
        carol.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        take(bob.matches);
        take(carol.matches);
        bob.close();
        MatchCancelled cancellation = take(carol.cancellations);
        assertEquals("bob", cancellation.getOpponentUsername());
        assertEquals(PlayerMatchmakingState.QUEUED,
                carol.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS).getState());
    }

    @Test
    void concurrentQueueJoinsNeverAssignAPlayerTwice() throws Exception {
        List<Player> group = List.of(
                online("alice"), online("bob"), online("carol"), online("dave"));
        CompletableFuture<?>[] joins = group.stream()
                .map(player -> player.matchmaking.joinRandomQueue())
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(joins).get(5, TimeUnit.SECONDS);

        Map<String, List<MatchAssignment>> assignmentsByMatch = new HashMap<>();
        for (Player player : group) {
            MatchAssignment assignment = take(player.matches);
            assignmentsByMatch.computeIfAbsent(assignment.getMatchId(), ignored ->
                    new ArrayList<>()).add(assignment);
            assertNull(player.matches.poll(100, TimeUnit.MILLISECONDS));
        }
        assertEquals(2, assignmentsByMatch.size());
        for (List<MatchAssignment> assignments : assignmentsByMatch.values()) {
            assertEquals(2, assignments.size());
            assertOppositeAssignments(assignments.get(0), assignments.get(1));
        }
    }

    @Test
    void roleAssignmentAlternatesAcrossSequentialMatches() throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");
        Player carol = online("carol");
        Player dave = online("dave");

        alice.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        bob.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        MatchAssignment firstWaitingPlayer = take(alice.matches);
        take(bob.matches);

        carol.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        dave.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        MatchAssignment secondWaitingPlayer = take(carol.matches);
        take(dave.matches);

        assertNotEquals(firstWaitingPlayer.getRole(), secondWaitingPlayer.getRole());
    }

    @Test
    void eventsArePrivateTypedAndListenerFailuresDoNotBreakProcessing()
            throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");
        Player carol = online("carol");
        BlockingQueue<String> callbackThreads = new LinkedBlockingQueue<>();
        AtomicInteger removedListenerCalls = new AtomicInteger();
        MatchmakingListener removedListener = new MatchmakingListener() {
            @Override
            public void invitationReceived(Invitation invitation) {
                removedListenerCalls.incrementAndGet();
            }
        };
        bob.matchmaking.addListener(removedListener);
        bob.matchmaking.removeListener(removedListener);
        bob.matchmaking.addListener(new MatchmakingListener() {
            @Override
            public void invitationReceived(Invitation invitation) {
                throw new IllegalStateException("test listener failure");
            }
        });
        bob.matchmaking.addListener(new MatchmakingListener() {
            @Override
            public void invitationReceived(Invitation invitation) {
                callbackThreads.add(Thread.currentThread().getName());
            }
        });

        alice.matchmaking.invitePlayer("bob").get(5, TimeUnit.SECONDS);
        take(bob.received);
        assertTrue(take(callbackThreads).startsWith("pvz2-network-client-reader"));
        assertNull(carol.received.poll(200, TimeUnit.MILLISECONDS));
        assertEquals(0, removedListenerCalls.get());
        assertTrue(bob.client.isConnected());
        assertEquals(MessageType.PONG, bob.client.ping().get(5, TimeUnit.SECONDS).getType());
    }

    @Test
    void malformedMatchmakingRequestIsSafeAndServerShutdownClearsTransientState()
            throws Exception {
        Player alice = online("alice");
        Player bob = online("bob");
        ProtocolMessage malformed = alice.client.sendRequest(ProtocolMessages.empty(
                MessageType.SEND_INVITATION_REQUEST, "malformed-invitation"))
                .get(5, TimeUnit.SECONDS);
        String json = new ProtocolCodec().serialize(malformed);
        assertEquals(MessageType.ERROR, malformed.getType());
        assertEquals(ProtocolErrorCode.MALFORMED_PAYLOAD.name(),
                malformed.getPayload().getAsJsonObject().get("code").getAsString());
        assertFalse(json.contains("Exception"));
        assertTrue(alice.client.isConnected());

        alice.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS);
        server.close();
        awaitDisconnected(alice.client);
        awaitDisconnected(bob.client);
        assertTrue(Files.readString(databasePath, StandardCharsets.UTF_8).contains("alice"));

        server = new GameServer(GameServer.DEFAULT_HOST, 0, databasePath,
                Duration.ofMillis(150));
        server.start();
        Player aliceAgain = loginExisting("alice");
        assertEquals(PlayerMatchmakingState.QUEUED,
                aliceAgain.matchmaking.joinRandomQueue().get(5, TimeUnit.SECONDS).getState());
    }

    @Test
    void remoteAccountSessionExposesSameSocketClientAndClearsItOnLogout()
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
            MatchmakingClient client = alice.getMatchmakingClient();
            assertNotNull(client);
            BlockingQueue<MatchAssignment> assignments = new LinkedBlockingQueue<>();
            client.addListener(new MatchmakingListener() {
                @Override public void matchFound(MatchAssignment value) {
                    assignments.add(value);
                }
            });

            Invitation invitation = client.invitePlayer("bob").get(5, TimeUnit.SECONDS);
            take(bob.received);
            bob.matchmaking.respondToInvitation(invitation.getInvitationId(), true)
                    .get(5, TimeUnit.SECONDS);
            assertNotNull(take(assignments));
            take(bob.matches);
            assertNotNull(client.getCurrentMatch());

            alice.logout().get(5, TimeUnit.SECONDS);
            assertNull(client.getCurrentMatch());
            assertTrue(client.getInvitations().isEmpty());
            assertNotNull(take(bob.cancellations));
        } finally {
            restoreProperty(RemoteAccountSession.HOST_PROPERTY, oldHost);
            restoreProperty(RemoteAccountSession.PORT_PROPERTY, oldPort);
        }
    }

    @Test
    void staleConnectionCannotRemoveNewerDirectoryOwnership() throws Exception {
        ServerConnectionDirectory directory = new ServerConnectionDirectory();
        AtomicInteger staleCleanupCalls = new AtomicInteger();
        ServerRequestHandler unusedHandler = (message, context) -> message;
        Socket staleSocket = new Socket();
        Socket currentSocket = new Socket();
        ClientConnection stale = new ClientConnection(server, staleSocket, unusedHandler);
        ClientConnection current = new ClientConnection(server, currentSocket, unusedHandler);
        try {
            assertTrue(directory.register("alice", stale,
                    staleCleanupCalls::incrementAndGet));
            staleSocket.close();
            assertTrue(directory.register("alice", current,
                    staleCleanupCalls::incrementAndGet));

            assertEquals(1, staleCleanupCalls.get());
            assertFalse(directory.unregister("alice", stale));
            assertTrue(directory.isOnline("alice"));
            assertTrue(directory.unregister("alice", current));
        } finally {
            stale.close();
            current.close();
        }
    }

    private Player online(String username) throws Exception {
        Player player = new Player(connectedClient(username + "-client"));
        players.add(player);
        player.authentication.register(details(username)).get(5, TimeUnit.SECONDS);
        player.authentication.login(username, PASSWORD).get(5, TimeUnit.SECONDS);
        return player;
    }

    private Player loginExisting(String username) throws Exception {
        Player player = new Player(connectedClient(username + "-again"));
        players.add(player);
        player.authentication.login(username, PASSWORD).get(5, TimeUnit.SECONDS);
        return player;
    }

    private void registerOffline(String username) throws Exception {
        try (NetworkClient client = connectedClient(username + "-register")) {
            new AuthenticationClient(client).register(details(username))
                    .get(5, TimeUnit.SECONDS);
        }
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

    private static void assertFailure(
            CompletableFuture<?> future, ProtocolErrorCode expected) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Expected " + expected);
        } catch (ExecutionException exception) {
            assertTrue(exception.getCause() instanceof MatchmakingException);
            assertEquals(expected,
                    ((MatchmakingException) exception.getCause()).getErrorCode());
        }
    }

    private static <T> T take(BlockingQueue<T> queue) throws Exception {
        T value = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(value, "Expected asynchronous event");
        return value;
    }

    private static void assertOppositeAssignments(
            MatchAssignment first, MatchAssignment second) {
        assertEquals(first.getMatchId(), second.getMatchId());
        assertEquals(first.getLocalUsername(), second.getOpponentUsername());
        assertEquals(second.getLocalUsername(), first.getOpponentUsername());
        assertEquals(first.getCreationTimeEpochMillis(), second.getCreationTimeEpochMillis());
        assertEquals(MatchStatus.PRE_GAME, first.getStatus());
        assertEquals(MatchStatus.PRE_GAME, second.getStatus());
        assertNotEquals(first.getRole(), second.getRole());
        assertTrue((first.getRole() == MatchRole.PLANTS
                && second.getRole() == MatchRole.ZOMBIES)
                || (first.getRole() == MatchRole.ZOMBIES
                && second.getRole() == MatchRole.PLANTS));
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

    private static final class Player implements AutoCloseable {
        final NetworkClient client;
        final AuthenticationClient authentication;
        final MatchmakingClient matchmaking;
        final BlockingQueue<Invitation> received = new LinkedBlockingQueue<>();
        final BlockingQueue<Invitation> results = new LinkedBlockingQueue<>();
        final BlockingQueue<MatchAssignment> matches = new LinkedBlockingQueue<>();
        final BlockingQueue<MatchCancelled> cancellations = new LinkedBlockingQueue<>();
        final BlockingQueue<QueueStatus> queueStatuses = new LinkedBlockingQueue<>();
        private boolean closed;

        Player(NetworkClient client) {
            this.client = client;
            authentication = new AuthenticationClient(client);
            matchmaking = new MatchmakingClient(client);
            matchmaking.addListener(new MatchmakingListener() {
                @Override public void invitationReceived(Invitation value) { received.add(value); }
                @Override public void invitationResult(Invitation value) { results.add(value); }
                @Override public void matchFound(MatchAssignment value) { matches.add(value); }
                @Override public void matchCancelled(MatchCancelled value) { cancellations.add(value); }
                @Override public void queueStatusChanged(QueueStatus value) { queueStatuses.add(value); }
            });
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            matchmaking.close();
            client.close();
        }
    }
}
