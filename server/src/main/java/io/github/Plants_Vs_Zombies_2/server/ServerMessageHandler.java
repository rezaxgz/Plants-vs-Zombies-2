package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

public final class ServerMessageHandler implements ServerRequestHandler {
    private final ServerAccountService accountService;
    private final ServerConnectionDirectory connectionDirectory;
    private final MatchmakingService matchmakingService;
    private final MultiplayerSessionService multiplayerSessionService;

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
        this.multiplayerSessionService = this.matchmakingService.getSessionService();
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
        } catch (MultiplayerSessionException exception) {
            return error(message, exception.getErrorCode(), exception.getMessage());
        } catch (RuntimeException exception) {
            return error(message, ProtocolErrorCode.INTERNAL_SERVER_ERROR,
                    "The server could not process this request");
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
            throws AccountServiceException, MatchmakingServiceException,
            MultiplayerSessionException {
        return switch (message.getType()) {
            case PING -> ProtocolMessages.pong(message.getRequestId(), message.getPayload());
            case REGISTER_REQUEST -> register(message);
            case LOGIN_REQUEST -> login(message, context);
            case LOGOUT_REQUEST -> logout(message, context);
            case GET_PROFILE_REQUEST -> getProfile(message, context);
            case GET_GAMEPLAY_STATE_REQUEST -> getGameplayState(message, context);
            case SYNC_GAMEPLAY_STATE_REQUEST -> synchronizeGameplayState(message, context);
            case GET_LEADERBOARD_REQUEST -> getLeaderboard(message, context);
            case SEND_INVITATION_REQUEST -> sendInvitation(message, context);
            case RESPOND_INVITATION_REQUEST -> respondInvitation(message, context);
            case CANCEL_INVITATION_REQUEST -> cancelInvitation(message, context);
            case JOIN_RANDOM_QUEUE_REQUEST -> joinRandomQueue(message, context);
            case LEAVE_RANDOM_QUEUE_REQUEST -> leaveRandomQueue(message, context);
            case MATCH_READY_REQUEST -> matchReady(message, context);
            case LEAVE_MATCH_REQUEST -> leaveMatch(message, context);
            case PLACE_MATCH_PLANT_REQUEST -> placeMatchPlant(message, context);
            case REMOVE_MATCH_PLANT_REQUEST -> removeMatchPlant(message, context);
            case PLACE_MATCH_ZOMBIE_REQUEST -> placeMatchZombie(message, context);
            case GET_MATCH_STATE_REQUEST -> getMatchState(message, context);
            case SEND_MATCH_REACTION_REQUEST -> sendMatchReaction(message, context);
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

    private ProtocolMessage matchReady(ProtocolMessage message,
            ConnectionContext context)
            throws AccountServiceException, MultiplayerSessionException {
        String username = requireAuthentication(context);
        String matchId = PayloadReader.from(message).requiredString("matchId");
        return ProtocolMessages.withPayload(MessageType.MATCH_READY_RESPONSE,
                message.getRequestId(),
                multiplayerSessionService.markReady(username, matchId));
    }

    private ProtocolMessage leaveMatch(ProtocolMessage message,
            ConnectionContext context)
            throws AccountServiceException, MultiplayerSessionException {
        String username = requireAuthentication(context);
        String matchId = PayloadReader.from(message).requiredString("matchId");
        multiplayerSessionService.leave(username, matchId);
        return ProtocolMessages.empty(MessageType.LEAVE_MATCH_RESPONSE,
                message.getRequestId());
    }

    private ProtocolMessage placeMatchPlant(ProtocolMessage message,
            ConnectionContext context)
            throws AccountServiceException, MultiplayerSessionException {
        String username = requireAuthentication(context);
        PayloadReader payload = PayloadReader.from(message);
        return ProtocolMessages.withPayload(MessageType.PLACE_MATCH_PLANT_RESPONSE,
                message.getRequestId(), multiplayerSessionService.placePlant(
                        username, payload.requiredString("matchId"),
                        payload.requiredString("entityType"),
                        payload.requiredInteger("row"),
                        payload.requiredInteger("column"),
                        payload.requiredLong("expectedRevision")));
    }

    private ProtocolMessage removeMatchPlant(ProtocolMessage message,
            ConnectionContext context)
            throws AccountServiceException, MultiplayerSessionException {
        String username = requireAuthentication(context);
        PayloadReader payload = PayloadReader.from(message);
        return ProtocolMessages.withPayload(MessageType.REMOVE_MATCH_PLANT_RESPONSE,
                message.getRequestId(), multiplayerSessionService.removePlant(
                        username, payload.requiredString("matchId"),
                        payload.requiredString("entityId"),
                        payload.requiredLong("expectedRevision")));
    }

    private ProtocolMessage placeMatchZombie(ProtocolMessage message,
            ConnectionContext context)
            throws AccountServiceException, MultiplayerSessionException {
        String username = requireAuthentication(context);
        PayloadReader payload = PayloadReader.from(message);
        return ProtocolMessages.withPayload(MessageType.PLACE_MATCH_ZOMBIE_RESPONSE,
                message.getRequestId(), multiplayerSessionService.placeZombie(
                        username, payload.requiredString("matchId"),
                        payload.requiredString("entityType"),
                        payload.requiredInteger("row"),
                        payload.requiredInteger("column"),
                        payload.requiredLong("expectedRevision")));
    }

    private ProtocolMessage getMatchState(ProtocolMessage message,
            ConnectionContext context)
            throws AccountServiceException, MultiplayerSessionException {
        String username = requireAuthentication(context);
        String matchId = PayloadReader.from(message).requiredString("matchId");
        return ProtocolMessages.withPayload(MessageType.GET_MATCH_STATE_RESPONSE,
                message.getRequestId(),
                multiplayerSessionService.getState(username, matchId));
    }

    private ProtocolMessage sendMatchReaction(ProtocolMessage message,
            ConnectionContext context)
            throws AccountServiceException, MultiplayerSessionException {
        String username = requireAuthentication(context);
        if (!connectionDirectory.isCurrent(username, context.getConnection())) {
            throw new AccountServiceException(ProtocolErrorCode.AUTH_REQUIRED,
                    "This authenticated connection is no longer current");
        }
        PayloadReader payload = PayloadReader.from(message);
        payload.requireOnlyFields("matchId", "reactionType");
        return ProtocolMessages.withPayload(
                MessageType.SEND_MATCH_REACTION_RESPONSE,
                message.getRequestId(), multiplayerSessionService.sendReaction(
                        username, payload.requiredBoundedString("matchId", 128),
                        payload.reactionType()));
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

    private ProtocolMessage getGameplayState(ProtocolMessage message,
            ConnectionContext context) throws AccountServiceException {
        PayloadReader.from(message);
        return ProtocolMessages.withPayload(MessageType.GET_GAMEPLAY_STATE_RESPONSE,
                message.getRequestId(), accountService.getGameplayState(context));
    }

    private ProtocolMessage synchronizeGameplayState(ProtocolMessage message,
            ConnectionContext context) throws AccountServiceException {
        String username = requireAuthentication(context);
        if (!connectionDirectory.isCurrent(username, context.getConnection())) {
            throw new AccountServiceException(ProtocolErrorCode.AUTH_REQUIRED,
                    "This authenticated connection is no longer current");
        }
        PayloadReader payload = PayloadReader.from(message);
        return ProtocolMessages.withPayload(MessageType.SYNC_GAMEPLAY_STATE_RESPONSE,
                message.getRequestId(), accountService.synchronizeGameplayState(
                        context, payload.requiredLong("expectedRevision"),
                        payload.requiredObject("state", GameplayState.class)));
    }

    private ProtocolMessage getLeaderboard(ProtocolMessage message,
            ConnectionContext context) throws AccountServiceException {
        String username = requireAuthentication(context);
        if (!connectionDirectory.isCurrent(username, context.getConnection())) {
            throw new AccountServiceException(ProtocolErrorCode.AUTH_REQUIRED,
                    "This authenticated connection is no longer current");
        }
        return ProtocolMessages.withPayload(MessageType.GET_LEADERBOARD_RESPONSE,
                message.getRequestId(), accountService.getLeaderboard(
                        username, PayloadReader.from(message).leaderboardQuery()));
    }

    private static ProtocolMessage error(
            ProtocolMessage request, ProtocolErrorCode code, String message) {
        return ProtocolMessages.error(request.getRequestId(), code, message);
    }
}
