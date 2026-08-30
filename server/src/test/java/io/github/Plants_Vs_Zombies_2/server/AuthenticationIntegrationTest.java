package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.AuthenticationClient;
import io.github.Plants_Vs_Zombies_2.network.auth.AuthenticationException;
import io.github.Plants_Vs_Zombies_2.network.auth.LoginCredentials;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkMessageListener;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationIntegrationTest {
    private static final String PASSWORD = "GoodPass1!";

    @TempDir
    Path temporaryDirectory;

    private Path databasePath;
    private GameServer server;

    @BeforeEach
    void startServer() throws Exception {
        databasePath = temporaryDirectory.resolve("server-users.json");
        server = new GameServer(GameServer.DEFAULT_HOST, 0, databasePath);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void registersValidAccountWithoutAuthenticatingConnection() throws Exception {
        try (NetworkClient client = connectedClient("register-client")) {
            AuthenticationClient authentication = new AuthenticationClient(client);

            authentication.register(validDetails()).get(5, TimeUnit.SECONDS);

            assertEquals(
                    ProtocolErrorCode.AUTH_REQUIRED,
                    authenticationFailure(authentication.getProfile()).getErrorCode());
            String persisted = Files.readString(databasePath, StandardCharsets.UTF_8);
            assertTrue(persisted.contains("\"username\": \"valid-user\""));
            assertFalse(persisted.contains(PASSWORD));
            assertFalse(persisted.contains("favorite-answer"));
        }
    }

    @ParameterizedTest(name = "rejects invalid {0}")
    @MethodSource("invalidRegistrationFields")
    void rejectsInvalidRegistrationFields(String field, RegistrationDetails details)
            throws Exception {
        try (NetworkClient client = connectedClient("validation-client")) {
            AuthenticationException failure = authenticationFailure(
                    new AuthenticationClient(client).register(details));

            assertEquals(ProtocolErrorCode.VALIDATION_FAILED, failure.getErrorCode());
        }
    }

    @Test
    void rejectsPasswordConfirmationMismatch() throws Exception {
        RegistrationDetails details = details(
                "valid-user", PASSWORD, "Different1!", "Player", "player@example.com",
                "female", 1, "favorite-answer", "favorite-answer");
        try (NetworkClient client = connectedClient("password-confirm-client")) {
            assertEquals(
                    ProtocolErrorCode.VALIDATION_FAILED,
                    authenticationFailure(new AuthenticationClient(client).register(details))
                            .getErrorCode());
        }
    }

    @Test
    void rejectsSecurityAnswerConfirmationMismatch() throws Exception {
        RegistrationDetails details = details(
                "valid-user", PASSWORD, PASSWORD, "Player", "player@example.com",
                "female", 1, "favorite-answer", "different-answer");
        try (NetworkClient client = connectedClient("answer-confirm-client")) {
            assertEquals(
                    ProtocolErrorCode.VALIDATION_FAILED,
                    authenticationFailure(new AuthenticationClient(client).register(details))
                            .getErrorCode());
        }
    }

    @Test
    void rejectsDuplicateUsername() throws Exception {
        try (NetworkClient client = connectedClient("duplicate-client")) {
            AuthenticationClient authentication = new AuthenticationClient(client);
            authentication.register(validDetails()).get(5, TimeUnit.SECONDS);

            AuthenticationException failure = authenticationFailure(
                    authentication.register(validDetails()));

            assertEquals(ProtocolErrorCode.USERNAME_EXISTS, failure.getErrorCode());
        }
    }

    @Test
    void simultaneousRegistrationAllowsExactlyOneWinner() throws Exception {
        try (NetworkClient firstClient = connectedClient("register-race-one");
                NetworkClient secondClient = connectedClient("register-race-two")) {
            AuthenticationClient first = new AuthenticationClient(firstClient);
            AuthenticationClient second = new AuthenticationClient(secondClient);

            CompletableFuture<Throwable> firstResult = first.register(validDetails())
                    .handle((ignored, failure) -> unwrap(failure));
            CompletableFuture<Throwable> secondResult = second.register(validDetails())
                    .handle((ignored, failure) -> unwrap(failure));
            Throwable firstFailure = firstResult.get(5, TimeUnit.SECONDS);
            Throwable secondFailure = secondResult.get(5, TimeUnit.SECONDS);

            assertEquals(1, (firstFailure == null ? 1 : 0) + (secondFailure == null ? 1 : 0));
            Throwable loser = firstFailure == null ? secondFailure : firstFailure;
            assertInstanceOf(AuthenticationException.class, loser);
            assertEquals(
                    ProtocolErrorCode.USERNAME_EXISTS,
                    ((AuthenticationException) loser).getErrorCode());
        }
    }

    @Test
    void logsInWithCorrectCredentialsAndReturnsProfile() throws Exception {
        try (NetworkClient client = connectedClient("login-client")) {
            AuthenticationClient authentication = new AuthenticationClient(client);
            authentication.register(validDetails()).get(5, TimeUnit.SECONDS);

            AccountProfile profile = authentication.login("valid-user", PASSWORD)
                    .get(5, TimeUnit.SECONDS);

            assertEquals("valid-user", profile.getUsername());
            assertEquals("Player", profile.getNickname());
            assertEquals("player@example.com", profile.getEmail());
            assertEquals("FEMALE", profile.getGender());
        }
    }

    @Test
    void rejectsSecondLoginOnAuthenticatedConnection() throws Exception {
        try (NetworkClient client = connectedClient("already-authenticated-client")) {
            AuthenticationClient authentication = new AuthenticationClient(client);
            authentication.register(validDetails()).get(5, TimeUnit.SECONDS);
            authentication.login("valid-user", PASSWORD).get(5, TimeUnit.SECONDS);

            AuthenticationException failure = authenticationFailure(
                    authentication.login("valid-user", PASSWORD));

            assertEquals(ProtocolErrorCode.ALREADY_AUTHENTICATED, failure.getErrorCode());
        }
    }

    @Test
    void wrongPasswordAndUnknownUsernameUseSameError() throws Exception {
        try (NetworkClient client = connectedClient("credential-client")) {
            AuthenticationClient authentication = new AuthenticationClient(client);
            authentication.register(validDetails()).get(5, TimeUnit.SECONDS);
            AuthenticationException wrongPassword = authenticationFailure(
                    authentication.login("valid-user", "WrongPass1!"));
            AuthenticationException unknownUser = authenticationFailure(
                    authentication.login("missing-user", PASSWORD));

            assertEquals(ProtocolErrorCode.INVALID_CREDENTIALS, wrongPassword.getErrorCode());
            assertEquals(ProtocolErrorCode.INVALID_CREDENTIALS, unknownUser.getErrorCode());
            assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
        }
    }

    @Test
    void loginBeforeHelloReturnsHandshakeRequired() throws Exception {
        ProtocolMessage request = ProtocolMessages.withPayload(
                MessageType.LOGIN_REQUEST,
                "login-before-hello",
                new LoginCredentials("valid-user", PASSWORD));

        ProtocolMessage response = sendRaw(request);

        assertError(response, ProtocolErrorCode.HANDSHAKE_REQUIRED);
        assertEquals("login-before-hello", response.getRequestId());
    }

    @Test
    void profileWithoutLoginRequiresAuthentication() throws Exception {
        try (NetworkClient client = connectedClient("anonymous-profile-client")) {
            AuthenticationException failure = authenticationFailure(
                    new AuthenticationClient(client).getProfile());

            assertEquals(ProtocolErrorCode.AUTH_REQUIRED, failure.getErrorCode());
        }
    }

    @Test
    void returnsTypedSanitizedProfileWithoutCredentialMaterial() throws Exception {
        try (NetworkClient client = connectedClient("profile-client")) {
            AuthenticationClient authentication = new AuthenticationClient(client);
            authentication.register(validDetails()).get(5, TimeUnit.SECONDS);
            authentication.login("valid-user", PASSWORD).get(5, TimeUnit.SECONDS);
            CountDownLatch profileReceived = new CountDownLatch(1);
            AtomicReference<ProtocolMessage> rawProfile = new AtomicReference<>();
            client.addListener(new NetworkMessageListener() {
                @Override
                public void onMessage(ProtocolMessage message) {
                    if (message.getType() == MessageType.GET_PROFILE_RESPONSE) {
                        rawProfile.set(message);
                        profileReceived.countDown();
                    }
                }
            });

            AccountProfile profile = authentication.getProfile().get(5, TimeUnit.SECONDS);
            assertTrue(profileReceived.await(5, TimeUnit.SECONDS));
            String json = new ProtocolCodec().serialize(rawProfile.get()).toLowerCase();

            assertEquals("valid-user", profile.getUsername());
            assertEquals(0, profile.getCoins());
            assertEquals(0, profile.getGamesPlayed());
            assertFalse(json.contains("password"));
            assertFalse(json.contains("securityquestion"));
            assertFalse(json.contains("answerhash"));
            assertFalse(json.contains("favorite-answer"));
        }
    }

    @Test
    void logoutKeepsConnectionAvailableForAnotherLogin() throws Exception {
        try (NetworkClient client = connectedClient("logout-client")) {
            AuthenticationClient authentication = new AuthenticationClient(client);
            authentication.register(validDetails()).get(5, TimeUnit.SECONDS);
            authentication.login("valid-user", PASSWORD).get(5, TimeUnit.SECONDS);

            authentication.logout().get(5, TimeUnit.SECONDS);
            assertEquals(
                    ProtocolErrorCode.AUTH_REQUIRED,
                    authenticationFailure(authentication.getProfile()).getErrorCode());
            assertNotNull(authentication.login("valid-user", PASSWORD).get(5, TimeUnit.SECONDS));
            assertTrue(client.isConnected());
        }
    }

    @Test
    void disconnectAutomaticallyReleasesOnlineSession() throws Exception {
        NetworkClient firstClient = connectedClient("online-owner");
        try {
            AuthenticationClient first = new AuthenticationClient(firstClient);
            first.register(validDetails()).get(5, TimeUnit.SECONDS);
            first.login("valid-user", PASSWORD).get(5, TimeUnit.SECONDS);
            firstClient.disconnect();
            awaitCondition(() -> server.getConnectionCount() == 0);

            try (NetworkClient secondClient = connectedClient("online-successor")) {
                AccountProfile profile = new AuthenticationClient(secondClient)
                        .login("valid-user", PASSWORD).get(5, TimeUnit.SECONDS);
                assertEquals("valid-user", profile.getUsername());
            }
        } finally {
            firstClient.close();
        }
    }

    @Test
    void rejectsSimultaneousLoginForSameAccount() throws Exception {
        try (NetworkClient firstClient = connectedClient("online-first");
                NetworkClient secondClient = connectedClient("online-second")) {
            AuthenticationClient first = new AuthenticationClient(firstClient);
            AuthenticationClient second = new AuthenticationClient(secondClient);
            first.register(validDetails()).get(5, TimeUnit.SECONDS);
            first.login("valid-user", PASSWORD).get(5, TimeUnit.SECONDS);

            AuthenticationException failure = authenticationFailure(
                    second.login("valid-user", PASSWORD));

            assertEquals(ProtocolErrorCode.USER_ALREADY_ONLINE, failure.getErrorCode());
            assertTrue(secondClient.isConnected());
        }
    }

    @Test
    void accountSurvivesCompleteServerRestart() throws Exception {
        try (NetworkClient client = connectedClient("persistence-register")) {
            new AuthenticationClient(client).register(validDetails()).get(5, TimeUnit.SECONDS);
        }
        server.close();
        server = new GameServer(GameServer.DEFAULT_HOST, 0, databasePath);
        server.start();

        try (NetworkClient client = connectedClient("persistence-login")) {
            AccountProfile profile = new AuthenticationClient(client)
                    .login("valid-user", PASSWORD).get(5, TimeUnit.SECONDS);

            assertEquals("valid-user", profile.getUsername());
        }
    }

    @Test
    void malformedAuthenticationPayloadReturnsSafeErrorWithoutClosingConnection()
            throws Exception {
        try (NetworkClient client = connectedClient("malformed-client")) {
            ProtocolMessage malformed = ProtocolMessages.empty(
                    MessageType.REGISTER_REQUEST, "malformed-register");

            ProtocolMessage response = client.sendRequest(malformed).get(5, TimeUnit.SECONDS);

            assertError(response, ProtocolErrorCode.MALFORMED_PAYLOAD);
            assertTrue(client.isConnected());
            assertEquals(MessageType.PONG, client.ping().get(5, TimeUnit.SECONDS).getType());
        }
    }

    private NetworkClient connectedClient(String name) throws Exception {
        NetworkClient client = new NetworkClient(GameServer.DEFAULT_HOST, server.getPort(), name);
        try {
            client.connect().get(5, TimeUnit.SECONDS);
            return client;
        } catch (Exception exception) {
            client.close();
            throw exception;
        }
    }

    private ProtocolMessage sendRaw(ProtocolMessage request) throws Exception {
        ProtocolCodec codec = new ProtocolCodec();
        try (Socket socket = new Socket(GameServer.DEFAULT_HOST, server.getPort());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), StandardCharsets.UTF_8))) {
            writer.write(codec.serialize(request));
            writer.newLine();
            writer.flush();
            return codec.deserialize(reader.readLine());
        }
    }

    private static AuthenticationException authenticationFailure(
            CompletableFuture<?> operation) throws Exception {
        try {
            operation.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Expected authentication operation to fail");
        } catch (ExecutionException exception) {
            Throwable cause = unwrap(exception.getCause());
            assertInstanceOf(AuthenticationException.class, cause);
            return (AuthenticationException) cause;
        }
    }

    private static void assertError(ProtocolMessage response, ProtocolErrorCode code) {
        assertEquals(MessageType.ERROR, response.getType());
        assertEquals(
                code.name(),
                response.getPayload().getAsJsonObject().get("code").getAsString());
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static void awaitCondition(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("Condition was not met before timeout");
            }
            Thread.onSpinWait();
        }
    }

    private static Stream<Arguments> invalidRegistrationFields() {
        return Stream.of(
                Arguments.of("username", details(
                        "bad name", PASSWORD, PASSWORD, "Player", "player@example.com",
                        "female", 1, "favorite-answer", "favorite-answer")),
                Arguments.of("password", details(
                        "valid-user", "weak", "weak", "Player", "player@example.com",
                        "female", 1, "favorite-answer", "favorite-answer")),
                Arguments.of("nickname", details(
                        "valid-user", PASSWORD, PASSWORD, "ab", "player@example.com",
                        "female", 1, "favorite-answer", "favorite-answer")),
                Arguments.of("email", details(
                        "valid-user", PASSWORD, PASSWORD, "Player", "invalid-email",
                        "female", 1, "favorite-answer", "favorite-answer")),
                Arguments.of("gender", details(
                        "valid-user", PASSWORD, PASSWORD, "Player", "player@example.com",
                        "other", 1, "favorite-answer", "favorite-answer")),
                Arguments.of("security question", details(
                        "valid-user", PASSWORD, PASSWORD, "Player", "player@example.com",
                        "female", 99, "favorite-answer", "favorite-answer")));
    }

    private static RegistrationDetails validDetails() {
        return details(
                "valid-user", PASSWORD, PASSWORD, "Player", "player@example.com",
                "female", 1, "favorite-answer", "favorite-answer");
    }

    private static RegistrationDetails details(
            String username,
            String password,
            String passwordConfirmation,
            String nickname,
            String email,
            String gender,
            int securityQuestionNumber,
            String securityAnswer,
            String securityAnswerConfirmation) {
        return new RegistrationDetails(
                username,
                password,
                passwordConfirmation,
                nickname,
                email,
                gender,
                securityQuestionNumber,
                securityAnswer,
                securityAnswerConfirmation);
    }
}
