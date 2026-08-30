package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

public final class ServerMessageHandler implements ServerRequestHandler {
    private final ServerAccountService accountService;
    private final ServerConnectionDirectory connectionDirectory;
    private final MatchmakingService matchmakingService;

    ServerMessageHandler(ServerAccountService accountService) {
        this(accountService, new ServerConnectionDirectory(), null);
    }

    ServerMessageHandler(ServerAccountService accountService,
            ServerConnectionDirectory connectionDirectory,
            MatchmakingService matchmakingService) {
        this.accountService = accountService;
        this.connectionDirectory = connectionDirectory;
        this.matchmakingService = matchmakingService == null
                ? new MatchmakingService(accountService::usernameExists,
                        connectionDirectory::isOnline, connectionDirectory::publish,
                        MatchmakingService.DEFAULT_INVITATION_DURATION)
                : matchmakingService;
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
        } catch (MatchmakingServiceException exception) {
            return error(message, exception.getErrorCode(), exception.getMessage());
        }
    }

    @Override
    public void connectionClosed(ConnectionContext context) {
        String username = accountService.connectionClosed(context);
        if (username != null && connectionDirectory.unregister(
                username, context.getConnection())) {
            matchmakingService.playerDisconnected(username);
        }
    }

    @Override
    public void close() {
        matchmakingService.close();
        connectionDirectory.clear();
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
            throws AccountServiceException, MatchmakingServiceException {
        return switch (message.getType()) {
            case PING -> ProtocolMessages.pong(message.getRequestId(), message.getPayload());
            case REGISTER_REQUEST -> register(message);
            case LOGIN_REQUEST -> login(message, context);
            case LOGOUT_REQUEST -> logout(message, context);
            case GET_PROFILE_REQUEST -> getProfile(message, context);
            case SEND_INVITATION_REQUEST -> sendInvitation(message, context);
            case RESPOND_INVITATION_REQUEST -> respondInvitation(message, context);
            case CANCEL_INVITATION_REQUEST -> cancelInvitation(message, context);
            case JOIN_RANDOM_QUEUE_REQUEST -> joinRandomQueue(message, context);
            case LEAVE_RANDOM_QUEUE_REQUEST -> leaveRandomQueue(message, context);
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
        if (!connectionDirectory.register(profile.getUsername(),
                context.getConnection(), () -> matchmakingService.playerDisconnected(
                        profile.getUsername()))) {
            context.clearAuthentication();
            throw new AccountServiceException(
                    ProtocolErrorCode.USER_ALREADY_ONLINE,
                    "This account is already online");
        }
        return ProtocolMessages.withPayload(
                MessageType.LOGIN_RESPONSE, message.getRequestId(), profile);
    }

    private ProtocolMessage logout(ProtocolMessage message, ConnectionContext context)
            throws AccountServiceException {
        PayloadReader.from(message);
        String username = accountService.logout(context);
        if (connectionDirectory.unregister(username, context.getConnection())) {
            matchmakingService.playerDisconnected(username);
        }
        return ProtocolMessages.empty(MessageType.LOGOUT_RESPONSE, message.getRequestId());
    }

    private ProtocolMessage sendInvitation(
            ProtocolMessage message, ConnectionContext context)
            throws AccountServiceException, MatchmakingServiceException {
        String username = requireAuthentication(context);
        String recipient = PayloadReader.from(message).requiredString("username");
        return ProtocolMessages.withPayload(MessageType.SEND_INVITATION_RESPONSE,
                message.getRequestId(), matchmakingService.invite(username, recipient));
    }

    private ProtocolMessage respondInvitation(
            ProtocolMessage message, ConnectionContext context)
            throws AccountServiceException, MatchmakingServiceException {
        String username = requireAuthentication(context);
        PayloadReader payload = PayloadReader.from(message);
        matchmakingService.respond(username,
                payload.requiredString("invitationId"),
                payload.requiredBoolean("accept"));
        return ProtocolMessages.empty(MessageType.RESPOND_INVITATION_RESPONSE,
                message.getRequestId());
    }

    private ProtocolMessage cancelInvitation(
            ProtocolMessage message, ConnectionContext context)
            throws AccountServiceException, MatchmakingServiceException {
        String username = requireAuthentication(context);
        matchmakingService.cancel(username,
                PayloadReader.from(message).requiredString("invitationId"));
        return ProtocolMessages.empty(MessageType.CANCEL_INVITATION_RESPONSE,
                message.getRequestId());
    }

    private ProtocolMessage joinRandomQueue(
            ProtocolMessage message, ConnectionContext context)
            throws AccountServiceException, MatchmakingServiceException {
        String username = requireAuthentication(context);
        PayloadReader.from(message);
        return ProtocolMessages.withPayload(MessageType.JOIN_RANDOM_QUEUE_RESPONSE,
                message.getRequestId(), matchmakingService.joinQueue(username));
    }

    private ProtocolMessage leaveRandomQueue(
            ProtocolMessage message, ConnectionContext context)
            throws AccountServiceException, MatchmakingServiceException {
        String username = requireAuthentication(context);
        PayloadReader.from(message);
        matchmakingService.leaveQueue(username);
        return ProtocolMessages.empty(MessageType.LEAVE_RANDOM_QUEUE_RESPONSE,
                message.getRequestId());
    }

    private static String requireAuthentication(ConnectionContext context)
            throws AccountServiceException {
        String username = context.getAuthenticatedUsername();
        if (username == null) {
            throw new AccountServiceException(ProtocolErrorCode.AUTH_REQUIRED,
                    "Authentication is required");
        }
        return username;
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
