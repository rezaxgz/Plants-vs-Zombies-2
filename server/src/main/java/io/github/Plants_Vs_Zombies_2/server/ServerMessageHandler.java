package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

public final class ServerMessageHandler {
    public ProtocolMessage handle(ProtocolMessage message) {
        if (message.getProtocolVersion() != ProtocolMessages.CURRENT_VERSION) {
            return ProtocolMessages.error(
                    message.getRequestId(),
                    "UNSUPPORTED_PROTOCOL_VERSION",
                    "Supported protocol version is " + ProtocolMessages.CURRENT_VERSION);
        }

        if (message.getType() == MessageType.CLIENT_HELLO) {
            return ProtocolMessages.serverHello(message.getRequestId());
        }
        if (message.getType() == MessageType.PING) {
            return ProtocolMessages.pong(message.getRequestId(), message.getPayload());
        }
        return ProtocolMessages.error(
                message.getRequestId(),
                "UNSUPPORTED_MESSAGE_TYPE",
                "The server does not accept " + message.getType() + " messages from clients");
    }
}
