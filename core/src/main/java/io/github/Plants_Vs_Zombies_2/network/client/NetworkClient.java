package io.github.Plants_Vs_Zombies_2.network.client;

import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolException;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public final class NetworkClient implements AutoCloseable {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 54555;
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;

    private final String host;
    private final int port;
    private final String clientName;
    private final ProtocolCodec codec = new ProtocolCodec();
    private final Object sendLock = new Object();
    private final Map<String, CompletableFuture<ProtocolMessage>> pendingRequests =
            new ConcurrentHashMap<>();
    private final List<NetworkMessageListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicReference<ConnectionStatus> status =
            new AtomicReference<>(ConnectionStatus.DISCONNECTED);

    private volatile Socket socket;
    private volatile BufferedWriter writer;
    private volatile Thread readerThread;

    public NetworkClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public NetworkClient(String host, int port) {
        this(host, port, "pvz2-client");
    }

    public NetworkClient(String host, int port, String clientName) {
        this.host = Objects.requireNonNull(host, "host");
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        this.port = port;
        this.clientName = Objects.requireNonNull(clientName, "clientName");
    }

    public CompletableFuture<ProtocolMessage> connect() {
        if (!status.compareAndSet(ConnectionStatus.DISCONNECTED, ConnectionStatus.CONNECTING)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Client is already connecting or connected"));
        }

        return CompletableFuture.runAsync(this::openConnection)
                .thenCompose(ignored -> {
                    status.set(ConnectionStatus.HANDSHAKING);
                    return sendRequest(ProtocolMessages.clientHello(
                            ProtocolMessages.newRequestId(), clientName));
                })
                .thenApply(response -> {
                    if (response.getType() != MessageType.SERVER_HELLO) {
                        throw new IllegalStateException(
                                "Expected SERVER_HELLO but received " + response.getType());
                    }
                    status.set(ConnectionStatus.CONNECTED);
                    return response;
                })
                .whenComplete((response, failure) -> {
                    if (failure != null) {
                        handleDisconnection(failure);
                    }
                });
    }

    public CompletableFuture<ProtocolMessage> ping() {
        if (status.get() != ConnectionStatus.CONNECTED) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client is not connected"));
        }
        return sendRequest(ProtocolMessages.ping(ProtocolMessages.newRequestId()));
    }

    public CompletableFuture<ProtocolMessage> sendRequest(ProtocolMessage message) {
        CompletableFuture<ProtocolMessage> response = new CompletableFuture<>();
        CompletableFuture<ProtocolMessage> existing =
                pendingRequests.putIfAbsent(message.getRequestId(), response);
        if (existing != null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Duplicate requestId: " + message.getRequestId()));
        }
        try {
            send(message);
        } catch (RuntimeException exception) {
            pendingRequests.remove(message.getRequestId(), response);
            response.completeExceptionally(exception);
        }
        return response;
    }

    public void send(ProtocolMessage message) {
        String line = codec.serialize(message);
        synchronized (sendLock) {
            BufferedWriter currentWriter = writer;
            if (currentWriter == null || socket == null || socket.isClosed()) {
                throw new IllegalStateException("Client is not connected");
            }
            try {
                currentWriter.write(line);
                currentWriter.newLine();
                currentWriter.flush();
            } catch (IOException exception) {
                handleDisconnection(exception);
                throw new IllegalStateException("Could not send network message", exception);
            }
        }
    }

    public void addListener(NetworkMessageListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(NetworkMessageListener listener) {
        listeners.remove(listener);
    }

    public ConnectionStatus getStatus() {
        return status.get();
    }

    public boolean isConnected() {
        return status.get() == ConnectionStatus.CONNECTED;
    }

    public void disconnect() {
        ConnectionStatus previous = status.getAndSet(ConnectionStatus.DISCONNECTING);
        if (previous == ConnectionStatus.DISCONNECTED || previous == ConnectionStatus.DISCONNECTING) {
            status.set(ConnectionStatus.DISCONNECTED);
            return;
        }
        handleDisconnection(null);
    }

    @Override
    public void close() {
        disconnect();
    }

    private void openConnection() {
        try {
            Socket newSocket = new Socket();
            newSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
            newSocket.setTcpNoDelay(true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    newSocket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter newWriter = new BufferedWriter(new OutputStreamWriter(
                    newSocket.getOutputStream(), StandardCharsets.UTF_8));
            socket = newSocket;
            writer = newWriter;
            Thread thread = new Thread(() -> readLoop(reader), "pvz2-network-client-reader");
            thread.setDaemon(true);
            readerThread = thread;
            thread.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not connect to " + host + ":" + port, exception);
        }
    }

    private void readLoop(BufferedReader reader) {
        Throwable failure = null;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                ProtocolMessage message = codec.deserialize(line);
                CompletableFuture<ProtocolMessage> request =
                        pendingRequests.remove(message.getRequestId());
                if (request != null) {
                    request.complete(message);
                }
                for (NetworkMessageListener listener : listeners) {
                    listener.onMessage(message);
                }
            }
        } catch (IOException | ProtocolException exception) {
            if (status.get() != ConnectionStatus.DISCONNECTING) {
                failure = exception;
            }
        } finally {
            handleDisconnection(failure);
        }
    }

    private void handleDisconnection(Throwable cause) {
        ConnectionStatus previous = status.getAndSet(ConnectionStatus.DISCONNECTED);
        Socket currentSocket = socket;
        socket = null;
        writer = null;
        if (currentSocket != null) {
            try {
                currentSocket.close();
            } catch (IOException ignored) {
                // The connection is already unusable; pending requests are failed below.
            }
        }

        IllegalStateException disconnected = new IllegalStateException("Server disconnected", cause);
        pendingRequests.forEach((requestId, future) -> future.completeExceptionally(disconnected));
        pendingRequests.clear();

        if (previous != ConnectionStatus.DISCONNECTED) {
            for (NetworkMessageListener listener : listeners) {
                listener.onDisconnected(cause);
            }
        }
    }
}
