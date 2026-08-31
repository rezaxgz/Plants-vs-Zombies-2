package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

class MatchReactionClientTimeoutTest {
    @Test
    void typedReactionTimeoutAndCancellationCleanCorrelatedPendingRequests()
            throws Exception {
        try (SilentReactionServer server = new SilentReactionServer();
                NetworkClient network = new NetworkClient(NetworkClient.DEFAULT_HOST,
                        server.port(), "reaction-timeout", Duration.ofMillis(150));
                MultiplayerGameClient client = new MultiplayerGameClient(network)) {
            network.connect().get(5, TimeUnit.SECONDS);
            CompletableFuture<MatchReactionReceipt> timedOut = client.sendReaction(
                    "m1", MatchReactionType.SMILE);
            assertEquals(MessageType.SEND_MATCH_REACTION_REQUEST,
                    server.takeRequest());
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> timedOut.get(2, TimeUnit.SECONDS));
            assertInstanceOf(TimeoutException.class, failure.getCause());
            assertEquals(0, network.getPendingRequestCount());
            assertTrue(network.isConnected());

            CompletableFuture<MatchReactionReceipt> cancelled = client.sendReaction(
                    "m1", MatchReactionType.LAUGH);
            assertEquals(MessageType.SEND_MATCH_REACTION_REQUEST,
                    server.takeRequest());
            assertTrue(cancelled.cancel(false));
            assertEquals(0, network.getPendingRequestCount());
            assertTrue(network.isConnected());
        }
    }

    private static final class SilentReactionServer implements AutoCloseable {
        private final ServerSocket server = new ServerSocket(0);
        private final BlockingQueue<MessageType> requests = new LinkedBlockingQueue<>();
        private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "silent-reaction-server");
            thread.setDaemon(true);
            return thread;
        });
        private volatile Socket client;

        private SilentReactionServer() throws Exception { executor.execute(this::serve); }
        int port() { return server.getLocalPort(); }
        MessageType takeRequest() throws Exception {
            return requests.poll(5, TimeUnit.SECONDS);
        }

        private void serve() {
            ProtocolCodec codec = new ProtocolCodec();
            try (Socket accepted = server.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            accepted.getInputStream(), StandardCharsets.UTF_8));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                            accepted.getOutputStream(), StandardCharsets.UTF_8))) {
                client = accepted;
                ProtocolMessage hello = codec.deserialize(reader.readLine());
                writer.write(codec.serialize(ProtocolMessages.serverHello(
                        hello.getRequestId())));
                writer.newLine();
                writer.flush();
                String line;
                while ((line = reader.readLine()) != null) {
                    requests.add(codec.deserialize(line).getType());
                }
            } catch (Exception exception) {
                if (!server.isClosed()) throw new IllegalStateException(exception);
            }
        }

        @Override public void close() throws Exception {
            server.close();
            if (client != null) client.close();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
