package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolException;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ClientConnection implements Runnable, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(ClientConnection.class.getName());

    private final GameServer server;
    private final Socket socket;
    private final ServerRequestHandler messageHandler;
    private final ConnectionContext context;
    private final ProtocolCodec codec = new ProtocolCodec();
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final Object sendLock = new Object();
    private static final int EVENT_QUEUE_CAPACITY = 8;
    private final String description;
    private final BlockingQueue<ProtocolMessage> eventQueue =
            new ArrayBlockingQueue<>(EVENT_QUEUE_CAPACITY);
    private BufferedWriter writer;
    private Thread eventWriterThread;

    ClientConnection(GameServer server, Socket socket, ServerRequestHandler messageHandler) {
        this.server = server;
        this.socket = socket;
        this.messageHandler = messageHandler;
        this.context = new ConnectionContext(this);
        this.description = String.valueOf(socket.getRemoteSocketAddress());
    }

    @Override
    public void run() {
        LOGGER.info(() -> "Client connected: " + description);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8))) {
            writer = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
            eventWriterThread = new Thread(this::eventWriterLoop,
                    "pvz2-server-events-" + Integer.toHexString(hashCode()));
            eventWriterThread.setDaemon(true);
            eventWriterThread.start();
            String line;
            while (open.get() && (line = reader.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException exception) {
            if (open.get()) {
                LOGGER.log(Level.WARNING, "Connection error for " + description, exception);
            }
        } finally {
            close();
        }
    }

    private void handleLine(String line) {
        final ProtocolMessage request;
        try {
            request = codec.deserialize(line);
        } catch (ProtocolException exception) {
            LOGGER.warning(() -> "Protocol error from " + description + ": " + exception.getMessage());
            String requestId = exception.getRequestId();
            if (requestId == null || requestId.isBlank()) {
                requestId = ProtocolMessages.newRequestId();
            }
            send(ProtocolMessages.error(requestId, exception.getErrorCode(), exception.getMessage()));
            return;
        }

        try {
            ProtocolMessage response = messageHandler.handle(request, context);
            if (response == null) {
                throw new IllegalStateException("Request handler returned no response");
            }
            send(response);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE,
                    "Unexpected request failure for " + description
                            + " (requestId=" + request.getRequestId() + ")",
                    exception);
            send(ProtocolMessages.error(
                    request.getRequestId(),
                    ProtocolErrorCode.INTERNAL_SERVER_ERROR,
                    "The server could not process this request"));
        }
    }

    private void send(ProtocolMessage message) {
        synchronized (sendLock) {
            if (!open.get() || writer == null) {
                return;
            }
            try {
                writer.write(codec.serialize(message));
                writer.newLine();
                writer.flush();
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Could not respond to " + description, exception);
                close();
            }
        }
    }

    void sendEvent(ProtocolMessage message) {
        if (message == null || !open.get()) return;
        boolean terminal = message.getType() == io.github.Plants_Vs_Zombies_2.network.protocol.MessageType.MATCH_FINISHED
                || message.getType() == io.github.Plants_Vs_Zombies_2.network.protocol.MessageType.MATCH_CANCELLED;
        if (terminal) {
            eventQueue.removeIf(event -> event.getType()
                    == io.github.Plants_Vs_Zombies_2.network.protocol.MessageType.MATCH_STATE_UPDATED);
        }
        if (eventQueue.offer(message)) return;

        // Coalesce obsolete periodic state before dropping lifecycle events.
        eventQueue.removeIf(event -> event.getType()
                == io.github.Plants_Vs_Zombies_2.network.protocol.MessageType.MATCH_STATE_UPDATED);
        if (!eventQueue.offer(message) && terminal) {
            // Terminal state outranks older unsolicited events. The queue is
            // intentionally bounded so a slow client cannot grow memory usage.
            eventQueue.poll();
            eventQueue.offer(message);
        }
    }

    private void eventWriterLoop() {
        while (open.get()) {
            try {
                ProtocolMessage event = eventQueue.take();
                send(event);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    boolean isOpen() {
        return open.get() && !socket.isClosed();
    }

    @Override
    public void close() {
        if (!open.compareAndSet(true, false)) {
            return;
        }
        Thread eventWriter = eventWriterThread;
        if (eventWriter != null) eventWriter.interrupt();
        eventQueue.clear();
        try {
            socket.close();
        } catch (IOException exception) {
            LOGGER.log(Level.FINE, "Socket was already closed for " + description, exception);
        } finally {
            // During an intentional server shutdown, GameServer flips its
            // running flag before closing sockets. Do not report those socket
            // closures as PLAYER_DISCONNECTED; the service-wide close that
            // follows owns the canonical SERVER_SHUTDOWN cancellation reason.
            if (server.isRunning()) {
                try {
                    messageHandler.connectionClosed(context);
                } catch (RuntimeException exception) {
                    LOGGER.log(Level.SEVERE,
                            "Could not release connection state for " + description,
                            exception);
                }
            }
            server.connectionClosed(this);
            LOGGER.info(() -> "Client disconnected: " + description);
        }
    }
}
