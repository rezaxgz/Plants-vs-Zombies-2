package io.github.Plants_Vs_Zombies_2.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.Plants_Vs_Zombies_2.model.auth.JsonUserRepository;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.AuthenticationClient;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateClient;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardClient;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardException;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardPage;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardQuery;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardSortColumn;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardSortDirection;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;
import io.github.Plants_Vs_Zombies_2.network.session.RemoteGameplayUserFactory;

class LeaderboardIntegrationTest {
    private static final String PASSWORD = "GoodPass1!";
    @TempDir Path temporaryDirectory;
    private GameServer server;

    @AfterEach void closeServer() {
        if (server != null) server.close();
    }

    @Test
    void gameplayUpdatesDriveSortedCorrelatedPagesAndSurviveRestart()
            throws Exception {
        Path database = temporaryDirectory.resolve("server-users.json");
        Path local = Path.of("data", "users.json").toAbsolutePath().normalize();
        boolean localExisted = Files.exists(local);
        long localModified = localExisted
                ? Files.getLastModifiedTime(local).toMillis() : -1;
        start(database);
        try (Client alice = online("alice"); Client bob = online("bob");
                Client charlie = online("charlie")) {
            synchronize(alice, 1, 2, 2, 4, 1, 900);
            synchronize(bob, 1, 3, 1, 2, 5, 900);
            synchronize(charlie, 2, 1, 3, 1, 1, 1200);

            LeaderboardPage descending = alice.leaderboard.load(query(
                    LeaderboardSortColumn.HIGH_SCORE,
                    LeaderboardSortDirection.DESCENDING, 0, 2))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(3, descending.getTotalPlayers());
            assertEquals("charlie", descending.getEntries().get(0).getUsername());
            assertEquals("alice", descending.getEntries().get(1).getUsername());
            assertEquals(2, descending.getAuthenticatedUserRank());

            LeaderboardPage ascending = alice.leaderboard.load(query(
                    LeaderboardSortColumn.HIGH_SCORE,
                    LeaderboardSortDirection.ASCENDING, 0, 100))
                    .get(5, TimeUnit.SECONDS);
            assertEquals("alice", ascending.getEntries().get(0).getUsername());
            assertEquals("bob", ascending.getEntries().get(1).getUsername());

            LeaderboardPage lastLevel = alice.leaderboard.load(query(
                    LeaderboardSortColumn.LAST_LEVEL,
                    LeaderboardSortDirection.ASCENDING, 0, 100))
                    .get(5, TimeUnit.SECONDS);
            assertEquals("alice", lastLevel.getEntries().get(0).getUsername());
            assertEquals("charlie", lastLevel.getEntries().get(2).getUsername());

            LeaderboardPage quests = alice.leaderboard.load(query(
                    LeaderboardSortColumn.QUESTS,
                    LeaderboardSortDirection.DESCENDING, 0, 100))
                    .get(5, TimeUnit.SECONDS);
            assertEquals("bob", quests.getEntries().get(0).getUsername());
            assertEquals(7, quests.getEntries().get(0).getTotalCompletedQuests());
            assertEquals("alice", alice.leaderboard.load(query(
                    LeaderboardSortColumn.DAILY_QUESTS,
                    LeaderboardSortDirection.DESCENDING, 0, 100))
                    .get(5, TimeUnit.SECONDS).getEntries().get(0).getUsername());
            assertEquals("bob", alice.leaderboard.load(query(
                    LeaderboardSortColumn.NON_DAILY_QUESTS,
                    LeaderboardSortDirection.DESCENDING, 0, 100))
                    .get(5, TimeUnit.SECONDS).getEntries().get(0).getUsername());

            ProtocolMessage correlated = alice.network.sendRequest(
                    ProtocolMessages.withPayload(
                            MessageType.GET_LEADERBOARD_REQUEST,
                            "leaderboard-correlation", query(
                                    LeaderboardSortColumn.LAST_LEVEL,
                                    LeaderboardSortDirection.ASCENDING, 0, 100)))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(MessageType.GET_LEADERBOARD_RESPONSE,
                    correlated.getType());
            assertEquals("leaderboard-correlation", correlated.getRequestId());
        }
        server.close();
        server = null;

        start(database);
        try (Client alice = loginExisting("alice")) {
            LeaderboardPage restored = alice.leaderboard.load(query(
                    LeaderboardSortColumn.HIGH_SCORE,
                    LeaderboardSortDirection.DESCENDING, 0, 100))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(3, restored.getTotalPlayers());
            assertEquals("charlie", restored.getEntries().get(0).getUsername());
            assertEquals(4, restored.getEntries().get(1)
                    .getCompletedDailyQuests());
        }
        assertEquals(localExisted, Files.exists(local));
        if (localExisted) assertEquals(localModified,
                Files.getLastModifiedTime(local).toMillis());
    }

    @Test
    void authenticationMalformedAndFailurePathsKeepHealthyRequestsIsolated()
            throws Exception {
        start(temporaryDirectory.resolve("auth.json"));
        try (NetworkClient anonymous = connected("anonymous")) {
            assertLeaderboardFailure(new LeaderboardClient(anonymous).load(query(
                    LeaderboardSortColumn.USERNAME,
                    LeaderboardSortDirection.ASCENDING, 0, 100)),
                    ProtocolErrorCode.AUTH_REQUIRED);
            assertTrue(anonymous.isConnected());
        }
        try (Client alice = online("alice")) {
            ProtocolMessage malformed = alice.network.sendRequest(
                    ProtocolMessages.withPayload(
                            MessageType.GET_LEADERBOARD_REQUEST, "bad-sort",
                            Map.of("sortColumn", "UNKNOWN",
                                    "sortDirection", "ASCENDING",
                                    "offset", 0, "limit", 100)))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(MessageType.ERROR, malformed.getType());
            assertEquals(ProtocolErrorCode.VALIDATION_FAILED.name(),
                    malformed.getPayload().getAsJsonObject().get("code")
                            .getAsString());
            assertTrue(alice.network.isConnected());
            assertEquals(MessageType.PONG,
                    alice.network.ping().get(5, TimeUnit.SECONDS).getType());
        }
    }

    @Test
    void handshakeAndCurrentConnectionAreRequiredByTheHandler() throws Exception {
        JsonUserRepository repository = new JsonUserRepository(
                temporaryDirectory.resolve("handler.json"));
        ServerMessageHandler handler = new ServerMessageHandler(
                new ServerAccountService(repository));
        ProtocolMessage request = ProtocolMessages.withPayload(
                MessageType.GET_LEADERBOARD_REQUEST, "leaderboard-handler",
                query(LeaderboardSortColumn.USERNAME,
                        LeaderboardSortDirection.ASCENDING, 0, 100));
        ConnectionContext context = new ConnectionContext(null);
        ProtocolMessage beforeHello = handler.handle(request, context);
        assertCode(beforeHello, ProtocolErrorCode.HANDSHAKE_REQUIRED);
        context.completeHandshake();
        ProtocolMessage anonymous = handler.handle(request, context);
        assertCode(anonymous, ProtocolErrorCode.AUTH_REQUIRED);
        context.authenticate("alice");
        ProtocolMessage nonCurrent = handler.handle(request, context);
        assertCode(nonCurrent, ProtocolErrorCode.AUTH_REQUIRED);
        handler.close();
    }

    @Test
    void stoppedServerFailsRecoverablyAndReleasesItsPort() throws Exception {
        start(temporaryDirectory.resolve("stopped.json"));
        int port = server.getPort();
        try (Client alice = online("alice")) {
            server.close();
            server = null;
            try {
                alice.leaderboard.load(query(LeaderboardSortColumn.USERNAME,
                        LeaderboardSortDirection.ASCENDING, 0, 100))
                        .get(5, TimeUnit.SECONDS);
                throw new AssertionError("Expected stopped-server failure");
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof IllegalStateException);
            }
        }
        try (ServerSocket rebound = new ServerSocket()) {
            rebound.setReuseAddress(true);
            rebound.bind(new InetSocketAddress(GameServer.DEFAULT_HOST, port));
            assertTrue(rebound.isBound());
        }
    }

    private void start(Path database) throws Exception {
        server = new GameServer(GameServer.DEFAULT_HOST, 0, database,
                Duration.ofSeconds(5));
        server.start();
    }

    private Client online(String username) throws Exception {
        Client client = new Client(connected(username));
        client.authentication.register(details(username)).get(5, TimeUnit.SECONDS);
        client.profile = client.authentication.login(username, PASSWORD)
                .get(5, TimeUnit.SECONDS);
        return client;
    }

    private Client loginExisting(String username) throws Exception {
        Client client = new Client(connected(username + "-restart"));
        client.profile = client.authentication.login(username, PASSWORD)
                .get(5, TimeUnit.SECONDS);
        return client;
    }

    private NetworkClient connected(String name) throws Exception {
        NetworkClient client = new NetworkClient(GameServer.DEFAULT_HOST,
                server.getPort(), name);
        client.connect().get(5, TimeUnit.SECONDS);
        return client;
    }

    private static void synchronize(Client client, int chapter, int level,
            int minigames, int daily, int nonDaily, int score) throws Exception {
        GameplayStateSnapshot initial = client.gameplay.getState()
                .get(5, TimeUnit.SECONDS);
        User user = RemoteGameplayUserFactory.create(client.profile, initial);
        user.getGameProgerss().setLastCompletedChapter(chapter);
        user.getGameProgerss().setLastCompletedLevel(level);
        for (int index = 0; index < minigames; index++) {
            user.getGameProgerss().addCompletedMinigame();
        }
        user.getGameProgerss().setHighestScore(score);
        user.getQuestProgress().restoreCompletedCountsForStorage(daily, nonDaily);
        client.gameplay.synchronize(initial.getRevision(),
                GameplayState.fromUser(user)).get(5, TimeUnit.SECONDS);
    }

    private static RegistrationDetails details(String username) {
        return new RegistrationDetails(username, PASSWORD, PASSWORD, "Player",
                username + "@example.com", "Male", 1,
                "answer", "answer");
    }

    private static LeaderboardQuery query(LeaderboardSortColumn column,
            LeaderboardSortDirection direction, int offset, int limit) {
        return new LeaderboardQuery(column, direction, offset, limit);
    }

    private static void assertLeaderboardFailure(
            java.util.concurrent.CompletableFuture<?> future,
            ProtocolErrorCode expected) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Expected " + expected);
        } catch (ExecutionException exception) {
            assertTrue(exception.getCause() instanceof LeaderboardException);
            assertEquals(expected, ((LeaderboardException) exception.getCause())
                    .getErrorCode());
        }
    }

    private static void assertCode(ProtocolMessage message,
            ProtocolErrorCode expected) {
        assertEquals(MessageType.ERROR, message.getType());
        assertEquals(expected.name(), message.getPayload().getAsJsonObject()
                .get("code").getAsString());
    }

    private static final class Client implements AutoCloseable {
        private final NetworkClient network;
        private final AuthenticationClient authentication;
        private final GameplayStateClient gameplay;
        private final LeaderboardClient leaderboard;
        private AccountProfile profile;

        private Client(NetworkClient network) {
            this.network = network;
            authentication = new AuthenticationClient(network);
            gameplay = new GameplayStateClient(network);
            leaderboard = new LeaderboardClient(network);
        }

        @Override public void close() { network.close(); }
    }
}
