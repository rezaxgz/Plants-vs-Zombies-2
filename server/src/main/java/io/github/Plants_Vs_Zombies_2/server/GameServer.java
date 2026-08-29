package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.model.auth.JsonUserRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GameServer implements AutoCloseable {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 54555;
    public static final String USERS_DATABASE_PROPERTY = "pvz.server.users.database";
    public static final String DEFAULT_USERS_DATABASE = "data/server-users.json";
    private static final Logger LOGGER = Logger.getLogger(GameServer.class.getName());

    private final String host;
    private final int requestedPort;
    private final AtomicBoolean running = new AtomicBoolean();
    private final Set<ClientConnection> connections = ConcurrentHashMap.newKeySet();
    private final ServerRequestHandler messageHandler;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final AtomicInteger connectionThreadNumber = new AtomicInteger();
    private final ExecutorService connectionExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(
                runnable, "pvz2-server-client-" + connectionThreadNumber.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;

    public GameServer() {
        this(DEFAULT_HOST, DEFAULT_PORT, resolveDatabasePath());
    }

    public GameServer(String host, int port) {
        this(host, port, resolveDatabasePath());
    }

    public GameServer(String host, int port, Path databasePath) {
        this(host, port, new ServerMessageHandler(
                new ServerAccountService(new JsonUserRepository(databasePath))));
    }

    GameServer(String host, int port, ServerRequestHandler messageHandler) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        this.host = host;
        this.requestedPort = port;
        this.messageHandler = Objects.requireNonNull(messageHandler, "messageHandler");
    }

    public synchronized void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Server is already running");
        }
        try {
            ServerSocket listeningSocket = new ServerSocket();
            listeningSocket.setReuseAddress(true);
            listeningSocket.bind(new InetSocketAddress(host, requestedPort));
            serverSocket = listeningSocket;
            Thread thread = new Thread(this::acceptLoop, "pvz2-server-accept");
            thread.setDaemon(true);
            acceptThread = thread;
            thread.start();
            LOGGER.info(() -> "Server listening on " + host + ":" + getPort());
        } catch (IOException | RuntimeException exception) {
            running.set(false);
            shutdownLatch.countDown();
            throw exception;
        }
    }

    public int getPort() {
        ServerSocket listeningSocket = serverSocket;
        return listeningSocket == null ? requestedPort : listeningSocket.getLocalPort();
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getConnectionCount() {
        return connections.size();
    }

    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ClientConnection connection = new ClientConnection(this, socket, messageHandler);
                connections.add(connection);
                connectionExecutor.execute(connection);
            } catch (SocketException exception) {
                if (running.get()) {
                    LOGGER.log(Level.SEVERE, "Server socket failed", exception);
                }
            } catch (IOException exception) {
                if (running.get()) {
                    LOGGER.log(Level.WARNING, "Could not accept client", exception);
                }
            }
        }
    }

    void connectionClosed(ClientConnection connection) {
        connections.remove(connection);
    }

    private static Path resolveDatabasePath() {
        String configured = System.getProperty(USERS_DATABASE_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        return Path.of(DEFAULT_USERS_DATABASE);
    }

    @Override
    public synchronized void close() {
        if (!running.getAndSet(false)) {
            shutdownLatch.countDown();
            return;
        }

        ServerSocket listeningSocket = serverSocket;
        if (listeningSocket != null) {
            try {
                listeningSocket.close();
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Could not close server socket", exception);
            }
        }
        for (ClientConnection connection : connections.toArray(ClientConnection[]::new)) {
            connection.close();
        }
        connectionExecutor.shutdown();

        Thread thread = acceptThread;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(2_000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            if (!connectionExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                connectionExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            connectionExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            shutdownLatch.countDown();
            LOGGER.info("Server stopped");
        }
    }
}
