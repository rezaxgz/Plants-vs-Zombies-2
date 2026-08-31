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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NetworkClient implements AutoCloseable {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 54555;
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final Logger LOGGER = Logger.getLogger(NetworkClient.class.getName());

    private final String host;
    private final int port;
    private final String clientName;
    private final Duration requestTimeout;
    private final NetworkConnector connector;
    private final ProtocolCodec codec = new ProtocolCodec();
    private final Object sendLock = new Object();
    private final Object lifecycleLock = new Object();
    private final Map<String, CompletableFuture<ProtocolMessage>> pendingRequests =
            new ConcurrentHashMap<>();
    private final List<NetworkMessageListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicReference<ConnectionStatus> status =
            new AtomicReference<>(ConnectionStatus.DISCONNECTED);
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ScheduledExecutorService timeoutExecutor;

    private volatile Socket socket;
    private volatile BufferedWriter writer;
    private long connectionGeneration;

    public NetworkClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public NetworkClient(String host, int port) {
        this(host, port, "pvz2-client");
    }

    public NetworkClient(String host, int port, String clientName) {
        this(host, port, clientName, DEFAULT_REQUEST_TIMEOUT);
    }

    public NetworkClient(String host, int port, String clientName, Duration requestTimeout) {
        this(host, port, clientName, requestTimeout, NetworkClient::openSocket);
    }

    NetworkClient(
            String host,
            int port,
            String clientName,
            Duration requestTimeout,
            NetworkConnector connector) {
        this.host = Objects.requireNonNull(host, "host");
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        this.port = port;
        this.clientName = Objects.requireNonNull(clientName, "clientName");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        this.connector = Objects.requireNonNull(connector, "connector");
        timeoutExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "pvz2-network-client-timeouts");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<ProtocolMessage> connect() {
        final long attempt;
        synchronized (lifecycleLock) {
            if (closed.get()) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Client has been closed"));
            }
            if (status.get() != ConnectionStatus.DISCONNECTED) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Client is already connecting or connected"));
            }
            status.set(ConnectionStatus.CONNECTING);
            attempt = ++connectionGeneration;
        }

        CompletableFuture<ProtocolMessage> connection = CompletableFuture
                .runAsync(() -> openConnection(attempt))
                .thenCompose(ignored -> {
                    synchronized (lifecycleLock) {
                        if (!isCurrentAttempt(attempt, ConnectionStatus.HANDSHAKING)) {
                            return CompletableFuture.failedFuture(
                                    new CancellationException("Connection attempt is obsolete"));
                        }
                    }
                    return sendRequest(ProtocolMessages.clientHello(
                            ProtocolMessages.newRequestId(), clientName));
                })
                .thenApply(response -> {
                    synchronized (lifecycleLock) {
                        if (!isCurrentAttempt(attempt, ConnectionStatus.HANDSHAKING)) {
                            throw new CancellationException("Connection attempt is obsolete");
                        }
                        if (response.getType() != MessageType.SERVER_HELLO) {
                            throw new IllegalStateException(
                                    "Expected SERVER_HELLO but received " + response.getType());
                        }
                        status.set(ConnectionStatus.CONNECTED);
                    }
                    return response;
                });
        connection.whenComplete((response, failure) -> {
            if (failure != null) {
                disconnectAttempt(attempt, unwrap(failure));
            }
        });
        return connection;
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

        final ScheduledFuture<?> timeoutTask;
        try {
            timeoutTask = timeoutExecutor.schedule(() -> {
                if (pendingRequests.remove(message.getRequestId(), response)) {
                    response.completeExceptionally(new TimeoutException(
                            "Request timed out: " + message.getRequestId()));
                }
            }, requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RuntimeException exception) {
            pendingRequests.remove(message.getRequestId(), response);
            response.completeExceptionally(exception);
            return response;
        }

        response.whenComplete((result, failure) -> {
            pendingRequests.remove(message.getRequestId(), response);
            timeoutTask.cancel(false);
        });
        try {
            send(message);
        } catch (RuntimeException exception) {
            response.completeExceptionally(exception);
        }
        return response;
    }

    public void send(ProtocolMessage message) {
        String line = codec.serialize(message);
        synchronized (sendLock) {
            BufferedWriter currentWriter = writer;
            Socket currentSocket = socket;
            if (currentWriter == null || currentSocket == null || currentSocket.isClosed()) {
                throw new IllegalStateException("Client is not connected");
            }
            try {
                currentWriter.write(line);
                currentWriter.newLine();
                currentWriter.flush();
            } catch (IOException exception) {
                disconnectCurrent(exception);
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

    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void disconnect() {
        disconnectCurrent(null);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            disconnect();
            timeoutExecutor.shutdownNow();
        }
    }

    private void openConnection(long attempt) {
        Socket newSocket = null;
        boolean published = false;
        try {
            newSocket = connector.connect(host, port, CONNECT_TIMEOUT_MILLIS);
            newSocket.setTcpNoDelay(true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    newSocket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter newWriter = new BufferedWriter(new OutputStreamWriter(
                    newSocket.getOutputStream(), StandardCharsets.UTF_8));
            synchronized (lifecycleLock) {
                if (!isCurrentAttempt(attempt, ConnectionStatus.CONNECTING)) {
                    throw new CancellationException("Connection attempt is obsolete");
                }
                socket = newSocket;
                writer = newWriter;
                status.set(ConnectionStatus.HANDSHAKING);
                published = true;
            }
            Thread thread = new Thread(
                    () -> readLoop(reader, attempt), "pvz2-network-client-reader");
            thread.setDaemon(true);
            thread.start();
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING,
                    "Could not connect to " + host + ":" + port, exception);
            throw new IllegalStateException("Could not connect to " + host + ":" + port, exception);
        } finally {
            if (!published && newSocket != null) {
                closeSocket(newSocket);
            }
        }
    }

    private void readLoop(BufferedReader reader, long attempt) {
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
                notifyMessageListeners(message);
            }
        } catch (IOException | ProtocolException exception) {
            failure = exception;
        } finally {
            disconnectAttempt(attempt, failure);
        }
    }

    private void notifyMessageListeners(ProtocolMessage message) {
        for (NetworkMessageListener listener : listeners) {
            try {
                listener.onMessage(message);
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Network message listener failed", exception);
            }
        }
    }

    private void notifyDisconnectedListeners(Throwable cause) {
        for (NetworkMessageListener listener : listeners) {
            try {
                listener.onDisconnected(cause);
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Network disconnect listener failed", exception);
            }
        }
    }

    private void disconnectCurrent(Throwable cause) {
        final Socket socketToClose;
        final ConnectionStatus previous;
        synchronized (lifecycleLock) {
            previous = status.get();
            connectionGeneration++;
            status.set(ConnectionStatus.DISCONNECTING);
            socketToClose = socket;
            socket = null;
            writer = null;
            status.set(ConnectionStatus.DISCONNECTED);
        }
        closeSocket(socketToClose);
        failPendingRequests(cause);
        if (previous != ConnectionStatus.DISCONNECTED
                && previous != ConnectionStatus.DISCONNECTING) {
            notifyDisconnectedListeners(cause);
        }
    }

    private void disconnectAttempt(long attempt, Throwable cause) {
        final Socket socketToClose;
        final ConnectionStatus previous;
        synchronized (lifecycleLock) {
            if (connectionGeneration != attempt) {
                return;
            }
            previous = status.get();
            connectionGeneration++;
            socketToClose = socket;
            socket = null;
            writer = null;
            status.set(ConnectionStatus.DISCONNECTED);
        }
        closeSocket(socketToClose);
        failPendingRequests(cause);
        if (previous != ConnectionStatus.DISCONNECTED) {
            notifyDisconnectedListeners(cause);
        }
    }

    private void failPendingRequests(Throwable cause) {
        IllegalStateException disconnected = new IllegalStateException("Server disconnected", cause);
        pendingRequests.forEach((requestId, future) ->
                future.completeExceptionally(disconnected));
        pendingRequests.clear();
    }

    private boolean isCurrentAttempt(long attempt, ConnectionStatus expectedStatus) {
        return connectionGeneration == attempt && status.get() == expectedStatus;
    }

    private static Socket openSocket(String host, int port, int timeoutMillis)
            throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            return socket;
        } catch (IOException exception) {
            closeSocket(socket);
            throw exception;
        }
    }

    private static void closeSocket(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // Closing is best-effort; connection state is already invalidated.
        }
    }

    private static Throwable unwrap(Throwable failure) {
        return failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;
    }
}
