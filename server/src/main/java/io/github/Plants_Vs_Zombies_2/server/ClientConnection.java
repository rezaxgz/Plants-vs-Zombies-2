package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ClientConnection implements Runnable, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(ClientConnection.class.getName());

    private final GameServer server;
    private final Socket socket;
    private final ServerMessageHandler messageHandler;
    private final ProtocolCodec codec = new ProtocolCodec();
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final Object sendLock = new Object();
    private final String description;
    private BufferedWriter writer;

    ClientConnection(GameServer server, Socket socket, ServerMessageHandler messageHandler) {
        this.server = server;
        this.socket = socket;
        this.messageHandler = messageHandler;
        this.description = String.valueOf(socket.getRemoteSocketAddress());
    }

    @Override
    public void run() {
        LOGGER.info(() -> "Client connected: " + description);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8))) {
            writer = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
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
        try {
            ProtocolMessage request = codec.deserialize(line);
            send(messageHandler.handle(request));
        } catch (ProtocolException exception) {
            LOGGER.warning(() -> "Protocol error from " + description + ": " + exception.getMessage());
            String requestId = exception.getRequestId();
            if (requestId == null || requestId.isBlank()) {
                requestId = ProtocolMessages.newRequestId();
            }
            send(ProtocolMessages.error(requestId, exception.getErrorCode(), exception.getMessage()));
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

    @Override
    public void close() {
        if (!open.compareAndSet(true, false)) {
            return;
        }
        try {
            socket.close();
        } catch (IOException exception) {
            LOGGER.log(Level.FINE, "Socket was already closed for " + description, exception);
        } finally {
            server.connectionClosed(this);
            LOGGER.info(() -> "Client disconnected: " + description);
        }
    }
}
