package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;

interface ServerRequestHandler {
    ProtocolMessage handle(ProtocolMessage message, ConnectionContext context);

    default void connectionClosed(ConnectionContext context) {
    }

    default void close() {
    }
}
