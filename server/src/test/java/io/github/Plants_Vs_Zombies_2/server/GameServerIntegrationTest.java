package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.network.client.ConnectionStatus;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameServerIntegrationTest {
    private GameServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new GameServer(GameServer.DEFAULT_HOST, 0);
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
