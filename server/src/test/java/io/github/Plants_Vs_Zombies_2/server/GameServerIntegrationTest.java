package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.model.auth.JsonUserRepository;
import io.github.Plants_Vs_Zombies_2.network.client.ConnectionStatus;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameServerIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private GameServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new GameServer(
                GameServer.DEFAULT_HOST,
                0,
                temporaryDirectory.resolve("server-users.json"));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void completesHelloHandshake() throws Exception {
        try (NetworkClient client = newClient("hello-client")) {
            ProtocolMessage response = client.connect().get(5, TimeUnit.SECONDS);

            assertEquals(MessageType.SERVER_HELLO, response.getType());
            assertEquals(ConnectionStatus.CONNECTED, client.getStatus());
        }
    }

    @Test
    void repeatedHelloReturnsClearErrorWithoutBreakingConnection() throws Exception {
        try (NetworkClient client = newClient("repeat-hello-client")) {
            client.connect().get(5, TimeUnit.SECONDS);

            ProtocolMessage response = client.sendRequest(ProtocolMessages.clientHello(
                    "repeated-hello", "repeat-hello-client")).get(5, TimeUnit.SECONDS);

            assertEquals(MessageType.ERROR, response.getType());
            assertEquals(
                    ProtocolErrorCode.HELLO_ALREADY_COMPLETED.name(),
                    response.getPayload().getAsJsonObject().get("code").getAsString());
            assertEquals(MessageType.PONG, client.ping().get(5, TimeUnit.SECONDS).getType());
        }
    }

    @Test
    void malformedHelloCanBeFollowedByValidHandshakeOnSameSocket() throws Exception {
        ProtocolCodec codec = new ProtocolCodec();
        try (Socket socket = new Socket(GameServer.DEFAULT_HOST, server.getPort());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), StandardCharsets.UTF_8))) {
            writer.write(codec.serialize(ProtocolMessages.clientHello("bad-hello", "")));
            writer.newLine();
            writer.flush();
            ProtocolMessage invalid = codec.deserialize(reader.readLine());

            writer.write(codec.serialize(ProtocolMessages.clientHello("good-hello", "client")));
            writer.newLine();
            writer.flush();
            ProtocolMessage valid = codec.deserialize(reader.readLine());

            assertEquals(MessageType.ERROR, invalid.getType());
            assertEquals(
                    ProtocolErrorCode.MALFORMED_PAYLOAD.name(),
                    invalid.getPayload().getAsJsonObject().get("code").getAsString());
            assertEquals(MessageType.SERVER_HELLO, valid.getType());
            assertEquals("good-hello", valid.getRequestId());
        }
    }

    @Test
    void correlatesPingAndPong() throws Exception {
        try (NetworkClient client = newClient("ping-client")) {
            client.connect().get(5, TimeUnit.SECONDS);

            ProtocolMessage response = client.ping().get(5, TimeUnit.SECONDS);

            assertEquals(MessageType.PONG, response.getType());
            assertTrue(response.getPayload().getAsJsonObject().has("sentAtEpochMillis"));
            assertTrue(response.getPayload().getAsJsonObject().has("serverTimeEpochMillis"));
        }
    }

    @Test
    void invalidJsonReturnsErrorWithoutClosingServer() throws Exception {
        ProtocolCodec codec = new ProtocolCodec();
        try (Socket socket = new Socket(GameServer.DEFAULT_HOST, server.getPort());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), StandardCharsets.UTF_8))) {
            writer.write("{not-valid-json}");
            writer.newLine();
            writer.flush();

            ProtocolMessage response = codec.deserialize(reader.readLine());

            assertEquals(MessageType.ERROR, response.getType());
            assertEquals("INVALID_JSON", response.getPayload().getAsJsonObject().get("code").getAsString());
            assertTrue(server.isRunning());
        }
    }

    @Test
    void unsupportedMessageTypeReturnsCorrelatedError() throws Exception {
        ProtocolCodec codec = new ProtocolCodec();
        try (Socket socket = new Socket(GameServer.DEFAULT_HOST, server.getPort());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), StandardCharsets.UTF_8))) {
            writer.write("{\"type\":\"LOGIN\",\"requestId\":\"unsupported-1\","
                    + "\"protocolVersion\":1,\"payload\":{}}");
            writer.newLine();
            writer.flush();

            ProtocolMessage response = codec.deserialize(reader.readLine());

            assertEquals(MessageType.ERROR, response.getType());
            assertEquals("unsupported-1", response.getRequestId());
            assertEquals(
                    "UNSUPPORTED_MESSAGE_TYPE",
                    response.getPayload().getAsJsonObject().get("code").getAsString());
        }
    }

    @Test
    void supportsTwoSimultaneousClients() throws Exception {
        try (NetworkClient first = newClient("first"); NetworkClient second = newClient("second")) {
            CompletableFuture.allOf(first.connect(), second.connect()).get(5, TimeUnit.SECONDS);

            awaitCondition(() -> server.getConnectionCount() == 2);
            assertTrue(first.isConnected());
            assertTrue(second.isConnected());
        }
    }

    @Test
    void oneDisconnectDoesNotAffectOtherClient() throws Exception {
        try (NetworkClient first = newClient("first"); NetworkClient second = newClient("second")) {
            CompletableFuture.allOf(first.connect(), second.connect()).get(5, TimeUnit.SECONDS);

            first.disconnect();
            awaitCondition(() -> server.getConnectionCount() == 1);

            assertEquals(MessageType.PONG, second.ping().get(5, TimeUnit.SECONDS).getType());
            assertTrue(server.isRunning());
        }
    }

    @Test
    void shutsDownCleanlyAndDisconnectsClients() throws Exception {
        try (NetworkClient client = newClient("shutdown-client")) {
            client.connect().get(5, TimeUnit.SECONDS);

            server.close();

            awaitCondition(() -> client.getStatus() == ConnectionStatus.DISCONNECTED);
            assertFalse(server.isRunning());
            assertEquals(0, server.getConnectionCount());
        }
    }

    @Test
    void listenerFailureDoesNotDisconnectClientOrHideMessageFromOtherListeners()
            throws Exception {
        try (NetworkClient client = newClient("listener-client")) {
            client.connect().get(5, TimeUnit.SECONDS);
            CountDownLatch secondListenerCalled = new CountDownLatch(1);
            client.addListener(message -> {
                throw new IllegalStateException("listener failure");
            });
            client.addListener(new NetworkMessageListener() {
                @Override
                public void onMessage(ProtocolMessage message) {
                    if (message.getType() == MessageType.PONG) {
                        secondListenerCalled.countDown();
                    }
                }
            });

            assertEquals(MessageType.PONG, client.ping().get(5, TimeUnit.SECONDS).getType());
            assertTrue(secondListenerCalled.await(5, TimeUnit.SECONDS));
            assertTrue(client.isConnected());
        }
    }

    @Test
    void unexpectedHandlerFailureReturnsSafeErrorAndKeepsConnectionUsable()
            throws Exception {
        server.close();
        ServerRequestHandler delegate = new ServerMessageHandler(new ServerAccountService(
                new JsonUserRepository(temporaryDirectory.resolve("failure-users.json"))));
        AtomicBoolean failNextPing = new AtomicBoolean(true);
        ServerRequestHandler failingHandler = new ServerRequestHandler() {
            @Override
            public ProtocolMessage handle(ProtocolMessage message, ConnectionContext context) {
                if (message.getType() == MessageType.PING
                        && failNextPing.compareAndSet(true, false)) {
                    throw new IllegalStateException("sensitive internal detail");
                }
                return delegate.handle(message, context);
            }

            @Override
            public void connectionClosed(ConnectionContext context) {
                delegate.connectionClosed(context);
            }

            @Override
            public void close() {
                delegate.close();
            }
        };
        server = new GameServer(GameServer.DEFAULT_HOST, 0, failingHandler);
        server.start();

        try (NetworkClient client = newClient("failure-boundary-client")) {
            client.connect().get(5, TimeUnit.SECONDS);

            ProtocolMessage failure = client.ping().get(5, TimeUnit.SECONDS);
            String failureJson = new ProtocolCodec().serialize(failure);

            assertEquals(MessageType.ERROR, failure.getType());
            assertEquals(
                    "INTERNAL_SERVER_ERROR",
                    failure.getPayload().getAsJsonObject().get("code").getAsString());
            assertFalse(failureJson.contains("sensitive internal detail"));
            assertEquals(MessageType.PONG, client.ping().get(5, TimeUnit.SECONDS).getType());
            assertTrue(server.isRunning());
        }
    }

    private NetworkClient newClient(String name) {
        return new NetworkClient(GameServer.DEFAULT_HOST, server.getPort(), name);
    }

    private static void awaitCondition(CheckedBooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("Condition was not met before timeout");
            }
            Thread.sleep(10);
        }
    }

    @FunctionalInterface
    private interface CheckedBooleanSupplier {
        boolean getAsBoolean() throws Exception;
    }
}
