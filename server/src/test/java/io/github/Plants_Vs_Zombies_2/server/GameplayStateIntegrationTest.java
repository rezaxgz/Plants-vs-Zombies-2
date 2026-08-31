package io.github.Plants_Vs_Zombies_2.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.AuthenticationClient;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateClient;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplaySyncException;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;
import io.github.Plants_Vs_Zombies_2.network.session.RemoteGameplayUserFactory;

class GameplayStateIntegrationTest {
    private static final String PASSWORD = "GoodPass1!";
    @TempDir Path temporaryDirectory;
    private GameServer server;

    @AfterEach void closeServer() {
        if (server != null) server.close();
    }

    @Test
    void authenticationMalformedValidationStaleAndIdentityAreEnforced()
            throws Exception {
        Path database = temporaryDirectory.resolve("server-users.json");
        start(database);
        try (Client alice = online("alice"); Client bob = online("bob");
                NetworkClient anonymousNetwork = connected("anonymous")) {
            GameplayStateClient anonymous = new GameplayStateClient(anonymousNetwork);
            assertFailure(anonymous.getState(), ProtocolErrorCode.AUTH_REQUIRED);

            GameplayStateSnapshot initial = alice.gameplay.getState()
                    .get(5, TimeUnit.SECONDS);
            User compatibility = RemoteGameplayUserFactory.create(
                    alice.profile, initial);
            compatibility.addCoins(200);
            GameplayState acceptedState = GameplayState.fromUser(compatibility);

            Map<String, Object> injected = new LinkedHashMap<>();
            injected.put("username", "bob");
            injected.put("expectedRevision", 0L);
            injected.put("state", acceptedState);
            ProtocolMessage response = alice.network.sendRequest(
                    ProtocolMessages.withPayload(MessageType.SYNC_GAMEPLAY_STATE_REQUEST,
                            "identity-injection", injected)).get(5, TimeUnit.SECONDS);
            assertEquals(MessageType.SYNC_GAMEPLAY_STATE_RESPONSE, response.getType());
            assertEquals("identity-injection", response.getRequestId());
            assertEquals(200, alice.gameplay.getState().get(5, TimeUnit.SECONDS)
                    .getState().getCoins());
            assertEquals(0, bob.gameplay.getState().get(5, TimeUnit.SECONDS)
                    .getState().getCoins(), "extra username data must not select an account");

            assertFailure(alice.gameplay.synchronize(0, acceptedState),
                    ProtocolErrorCode.STALE_ACCOUNT_REVISION);
            User invalid = RemoteGameplayUserFactory.create(alice.profile,
                    alice.gameplay.getState().get(5, TimeUnit.SECONDS));
            invalid.deductCoins(500);
            assertFailure(alice.gameplay.synchronize(1,
                    GameplayState.fromUser(invalid)), ProtocolErrorCode.VALIDATION_FAILED);
            assertEquals(1L, alice.gameplay.getState().get(5, TimeUnit.SECONDS)
                    .getRevision());

            ProtocolMessage malformed = alice.network.sendRequest(
                    ProtocolMessages.withPayload(MessageType.SYNC_GAMEPLAY_STATE_REQUEST,
                            "malformed-gameplay", Map.of("expectedRevision", 1L)))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(MessageType.ERROR, malformed.getType());
            assertEquals("malformed-gameplay", malformed.getRequestId());
            assertEquals(ProtocolErrorCode.MALFORMED_PAYLOAD.name(),
                    malformed.getPayload().getAsJsonObject().get("code").getAsString());
            assertTrue(alice.network.isConnected());
        }
    }

    @Test
    void synchronizedGameplaySurvivesRestartAndResponsesStaySanitized()
            throws Exception {
        Path database = temporaryDirectory.resolve("server-users.json");
        Path localDatabase = Path.of("data", "users.json").toAbsolutePath().normalize();
        boolean localExisted = Files.exists(localDatabase);
        long localModified = localExisted
                ? Files.getLastModifiedTime(localDatabase).toMillis() : -1L;
        start(database);
        GameplayState accepted;
        try (Client first = online("alice")) {
            GameplayStateSnapshot initial = first.gameplay.getState()
                    .get(5, TimeUnit.SECONDS);
            User compatibility = RemoteGameplayUserFactory.create(first.profile, initial);
            compatibility.addCoins(321);
            compatibility.addDiamonds(17);
            compatibility.setSprouts(8);
            compatibility.setPlantFoodCount(4);
            compatibility.setPotCount(3);
            compatibility.getGameProgerss().recordCompletedLevel(1, 2);
            compatibility.getGameProgerss().setHighestScore(7654);
            compatibility.getGameProgerss().recordGameStarted();
            compatibility.getGameProgerss().addCompletedMinigame();
            accepted = GameplayState.fromUser(compatibility);
            GameplayStateSnapshot saved = first.gameplay.synchronize(0, accepted)
                    .get(5, TimeUnit.SECONDS);
            assertEquals(1L, saved.getRevision());

            ProtocolMessage raw = first.network.sendRequest(ProtocolMessages.empty(
                    MessageType.GET_GAMEPLAY_STATE_REQUEST, "sanitized"))
                    .get(5, TimeUnit.SECONDS);
            String json = new ProtocolCodec().serialize(raw).toLowerCase();
            assertFalse(json.contains("password"));
            assertFalse(json.contains("securityquestion"));
            assertFalse(json.contains("answerhash"));
        }
        server.close();
        server = null;

        start(database);
        try (Client second = loginExisting("alice")) {
            GameplayStateSnapshot restored = second.gameplay.getState()
                    .get(5, TimeUnit.SECONDS);
            assertEquals(1L, restored.getRevision());
            assertEquals(accepted, restored.getState());
            assertEquals(321, second.profile.getCoins());
        }
        assertEquals(localExisted, Files.exists(localDatabase));
        if (localExisted) {
            assertEquals(localModified,
                    Files.getLastModifiedTime(localDatabase).toMillis());
        }
    }

    private void start(Path database) throws Exception {
        server = new GameServer(GameServer.DEFAULT_HOST, 0, database,
                Duration.ofSeconds(5));
        server.start();
    }

    private Client online(String username) throws Exception {
        Client client = new Client(connected(username + "-client"));
        client.authentication.register(details(username)).get(5, TimeUnit.SECONDS);
        client.profile = client.authentication.login(username, PASSWORD)
                .get(5, TimeUnit.SECONDS);
        return client;
    }

    private Client loginExisting(String username) throws Exception {
        Client client = new Client(connected(username + "-second-client"));
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

    private static RegistrationDetails details(String username) {
        return new RegistrationDetails(username, PASSWORD, PASSWORD, "Player",
                username + "@example.com", "Male", 1, "answer", "answer");
    }

    private static void assertFailure(java.util.concurrent.CompletableFuture<?> future,
            ProtocolErrorCode expected) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Expected " + expected);
        } catch (ExecutionException exception) {
            assertTrue(exception.getCause() instanceof GameplaySyncException);
            assertEquals(expected,
                    ((GameplaySyncException) exception.getCause()).getErrorCode());
        }
    }

    private static final class Client implements AutoCloseable {
        private final NetworkClient network;
        private final AuthenticationClient authentication;
        private final GameplayStateClient gameplay;
        private AccountProfile profile;

        private Client(NetworkClient network) {
            this.network = network;
            authentication = new AuthenticationClient(network);
            gameplay = new GameplayStateClient(network);
        }

        @Override public void close() { network.close(); }
    }
}
