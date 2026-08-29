package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

public final class ServerMessageHandler implements ServerRequestHandler {
    private final ServerAccountService accountService;

    ServerMessageHandler(ServerAccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public ProtocolMessage handle(ProtocolMessage message, ConnectionContext context) {
        if (message.getProtocolVersion() != ProtocolMessages.CURRENT_VERSION) {
            return error(
                    message,
                    ProtocolErrorCode.UNSUPPORTED_PROTOCOL_VERSION,
                    "Supported protocol version is " + ProtocolMessages.CURRENT_VERSION);
        }

        try {
            if (message.getType() == MessageType.CLIENT_HELLO) {
                return handleHello(message, context);
            }
            if (!context.isHandshakeCompleted()) {
                return error(
                        message,
                        ProtocolErrorCode.HANDSHAKE_REQUIRED,
                        "Complete CLIENT_HELLO before sending requests");
            }
            return handleAfterHandshake(message, context);
        } catch (AccountServiceException exception) {
            return error(message, exception.getErrorCode(), exception.getMessage());
        }
    }

    @Override
    public void connectionClosed(ConnectionContext context) {
        accountService.connectionClosed(context);
    }

    private ProtocolMessage handleHello(ProtocolMessage message, ConnectionContext context)
            throws AccountServiceException {
        if (context.isHandshakeCompleted()) {
            return error(
                    message,
                    ProtocolErrorCode.HELLO_ALREADY_COMPLETED,
                    "The hello handshake has already completed");
        }
        String clientName = PayloadReader.from(message).requiredString("clientName");
        if (clientName.isBlank()) {
            return error(
                    message,
                    ProtocolErrorCode.MALFORMED_PAYLOAD,
                    "clientName must not be blank");
        }
        if (!context.completeHandshake()) {
            return error(
                    message,
                    ProtocolErrorCode.HELLO_ALREADY_COMPLETED,
                    "The hello handshake has already completed");
        }
        return ProtocolMessages.serverHello(message.getRequestId());
    }

    private ProtocolMessage handleAfterHandshake(
            ProtocolMessage message, ConnectionContext context)
            throws AccountServiceException {
        return switch (message.getType()) {
            case PING -> ProtocolMessages.pong(message.getRequestId(), message.getPayload());
            case REGISTER_REQUEST -> register(message);
            case LOGIN_REQUEST -> login(message, context);
            case LOGOUT_REQUEST -> logout(message, context);
            case GET_PROFILE_REQUEST -> getProfile(message, context);
            default -> error(
                    message,
                    ProtocolErrorCode.UNSUPPORTED_MESSAGE_TYPE,
                    "The server does not accept " + message.getType() + " messages from clients");
        };
    }

    private ProtocolMessage register(ProtocolMessage message) throws AccountServiceException {
        accountService.register(PayloadReader.from(message).registration());
        return ProtocolMessages.empty(MessageType.REGISTER_RESPONSE, message.getRequestId());
    }

    private ProtocolMessage login(ProtocolMessage message, ConnectionContext context)
            throws AccountServiceException {
        AccountProfile profile = accountService.login(
                context, PayloadReader.from(message).login());
        return ProtocolMessages.withPayload(
                MessageType.LOGIN_RESPONSE, message.getRequestId(), profile);
    }

    private ProtocolMessage logout(ProtocolMessage message, ConnectionContext context)
            throws AccountServiceException {
        PayloadReader.from(message);
        accountService.logout(context);
        return ProtocolMessages.empty(MessageType.LOGOUT_RESPONSE, message.getRequestId());
    }

    private ProtocolMessage getProfile(ProtocolMessage message, ConnectionContext context)
            throws AccountServiceException {
        PayloadReader.from(message);
        return ProtocolMessages.withPayload(
                MessageType.GET_PROFILE_RESPONSE,
                message.getRequestId(),
                accountService.getProfile(context));
    }

    private static ProtocolMessage error(
            ProtocolMessage request, ProtocolErrorCode code, String message) {
        return ProtocolMessages.error(request.getRequestId(), code, message);
    }
}
