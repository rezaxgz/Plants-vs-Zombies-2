package io.github.Plants_Vs_Zombies_2.network.client;

import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkClientTest {
    @Test
    void timedOutRequestIsRemovedWithoutDisconnectingHealthyClient() throws Exception {
        try (SilentProtocolServer server = new SilentProtocolServer();
                NetworkClient client = new NetworkClient(
                        NetworkClient.DEFAULT_HOST,
                        server.getPort(),
                        "timeout-client",
                        Duration.ofMillis(100))) {
            client.connect().get(5, TimeUnit.SECONDS);
            CompletableFuture<ProtocolMessage> request = client.ping();
            assertTrue(server.awaitIgnoredRequest());

            ExecutionException exception = assertThrows(
                    ExecutionException.class, () -> request.get(2, TimeUnit.SECONDS));

            assertInstanceOf(TimeoutException.class, exception.getCause());
            assertEquals(0, client.getPendingRequestCount());
            assertTrue(client.isConnected());
        }
    }

    @Test
    void cancelledRequestIsRemovedWithoutDisconnectingHealthyClient() throws Exception {
        try (SilentProtocolServer server = new SilentProtocolServer();
                NetworkClient client = new NetworkClient(
                        NetworkClient.DEFAULT_HOST,
                        server.getPort(),
                        "cancel-client",
                        Duration.ofSeconds(5))) {
            client.connect().get(5, TimeUnit.SECONDS);
            CompletableFuture<ProtocolMessage> request = client.ping();
            assertTrue(server.awaitIgnoredRequest());

            assertTrue(request.cancel(false));

            assertEquals(0, client.getPendingRequestCount());
            assertTrue(client.isConnected());
        }
    }

    @Test
    void disconnectDuringConnectClosesObsoleteSocketWithoutRepublishingIt()
            throws Exception {
        CountDownLatch connectorEntered = new CountDownLatch(1);
        CountDownLatch allowConnectorToReturn = new CountDownLatch(1);
        AtomicReference<TrackingSocket> createdSocket = new AtomicReference<>();
        NetworkConnector delayedConnector = (host, port, timeout) -> {
            connectorEntered.countDown();
            try {
                if (!allowConnectorToReturn.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("Test connector was not released");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", exception);
            }
            TrackingSocket socket = new TrackingSocket();
            createdSocket.set(socket);
            return socket;
        };

        try (NetworkClient client = new NetworkClient(
                "test-host",
                12345,
                "race-client",
                Duration.ofSeconds(1),
                delayedConnector)) {
            CompletableFuture<ProtocolMessage> connection = client.connect();
            assertTrue(connectorEntered.await(5, TimeUnit.SECONDS));

            client.disconnect();
            allowConnectorToReturn.countDown();

            assertThrows(Exception.class, () -> connection.get(5, TimeUnit.SECONDS));
            assertEquals(ConnectionStatus.DISCONNECTED, client.getStatus());
            assertTrue(createdSocket.get().isClosedByClient());
        }
    }

    private static final class SilentProtocolServer implements AutoCloseable {
        private final ServerSocket serverSocket = new ServerSocket(0);
        private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "silent-protocol-server");
            thread.setDaemon(true);
            return thread;
        });
        private final CountDownLatch ignoredRequest = new CountDownLatch(1);
        private volatile Socket socket;

        private SilentProtocolServer() throws IOException {
            executor.execute(this::serve);
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }

        boolean awaitIgnoredRequest() throws InterruptedException {
            return ignoredRequest.await(5, TimeUnit.SECONDS);
        }

        private void serve() {
            ProtocolCodec codec = new ProtocolCodec();
            try (Socket accepted = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            accepted.getInputStream(), StandardCharsets.UTF_8));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                            accepted.getOutputStream(), StandardCharsets.UTF_8))) {
                socket = accepted;
                ProtocolMessage hello = codec.deserialize(reader.readLine());
                writer.write(codec.serialize(ProtocolMessages.serverHello(hello.getRequestId())));
                writer.newLine();
                writer.flush();

                String line;
                while ((line = reader.readLine()) != null) {
                    ProtocolMessage request = codec.deserialize(line);
                    if (request.getType() == MessageType.PING) {
                        ignoredRequest.countDown();
                    }
                }
            } catch (Exception exception) {
                if (!serverSocket.isClosed()) {
                    throw new IllegalStateException(exception);
                }
            }
        }

        @Override
        public void close() throws Exception {
            serverSocket.close();
            if (socket != null) {
                socket.close();
            }
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static final class TrackingSocket extends Socket {
        private final InputStream input = new ByteArrayInputStream(new byte[0]);
        private final OutputStream output = new ByteArrayOutputStream();
        private volatile boolean closedByClient;

        @Override
        public void setTcpNoDelay(boolean on) {
        }

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        @Override
        public synchronized void close() throws IOException {
            closedByClient = true;
            super.close();
        }

        boolean isClosedByClient() {
            return closedByClient;
        }
    }
}
