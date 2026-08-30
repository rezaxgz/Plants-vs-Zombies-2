package io.github.Plants_Vs_Zombies_2.server;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.Invitation;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.InvitationStatus;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.PlayerMatchmakingState;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.QueueStatus;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

/** Single-lock transient invitation, queue, and pre-game match state machine. */
final class MatchmakingService implements AutoCloseable {
    static final Duration DEFAULT_INVITATION_DURATION = Duration.ofSeconds(30);

    private final Object lock = new Object();
    private final Predicate<String> accountExists;
    private final Predicate<String> online;
    private final Consumer<List<MatchmakingEvent>> publisher;
    private final Duration invitationDuration;
    private final Clock clock;
    private final ScheduledExecutorService scheduler;
    private final Map<String, PlayerMatchmakingState> playerStates = new HashMap<>();
    private final Map<String, Invitation> invitations = new HashMap<>();
    private final Map<String, String> activeInvitationByPlayer = new HashMap<>();
    private final Map<String, ScheduledFuture<?>> expirationTasks = new HashMap<>();
    private final ArrayDeque<QueueEntry> queue = new ArrayDeque<>();
    private final Map<String, PreGameMatch> matches = new HashMap<>();
    private final Map<String, String> matchByPlayer = new HashMap<>();
    private boolean firstPlayerGetsPlants = true;
    private boolean closed;

    MatchmakingService(Predicate<String> accountExists, Predicate<String> online,
            Consumer<List<MatchmakingEvent>> publisher, Duration invitationDuration) {
        this(accountExists, online, publisher, invitationDuration,
                Clock.systemUTC(), Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "pvz2-invitation-expiration");
                    thread.setDaemon(true);
                    return thread;
                }));
    }

    MatchmakingService(Predicate<String> accountExists, Predicate<String> online,
            Consumer<List<MatchmakingEvent>> publisher, Duration invitationDuration,
            Clock clock, ScheduledExecutorService scheduler) {
        if (invitationDuration == null || invitationDuration.isNegative()
                || invitationDuration.isZero() || invitationDuration.toMillis() <= 0) {
            throw new IllegalArgumentException("invitationDuration must be at least 1 millisecond");
        }
        this.accountExists = java.util.Objects.requireNonNull(accountExists, "accountExists");
        this.online = java.util.Objects.requireNonNull(online, "online");
        this.publisher = java.util.Objects.requireNonNull(publisher, "publisher");
        this.invitationDuration = invitationDuration;
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        this.scheduler = java.util.Objects.requireNonNull(scheduler, "scheduler");
    }

    Invitation invite(String inviter, String recipient) throws MatchmakingServiceException {
        if (recipient == null || recipient.isBlank() || !accountExists.test(recipient)) {
            throw failure(ProtocolErrorCode.USER_NOT_FOUND, "The requested user does not exist");
        }
        if (inviter.equals(recipient)) {
            throw failure(ProtocolErrorCode.CANNOT_INVITE_SELF, "A player cannot invite themselves");
        }
        if (!online.test(recipient)) {
            throw failure(ProtocolErrorCode.USER_OFFLINE, "The requested user is offline");
        }

        Invitation invitation;
        List<MatchmakingEvent> events;
        synchronized (lock) {
            ensureOpen();
            if (!online.test(recipient)) {
                throw failure(ProtocolErrorCode.USER_OFFLINE, "The requested user is offline");
            }
            String inviterInvitation = activeInvitationByPlayer.get(inviter);
            String recipientInvitation = activeInvitationByPlayer.get(recipient);
            if (inviterInvitation != null && inviterInvitation.equals(recipientInvitation)) {
                throw failure(ProtocolErrorCode.DUPLICATE_INVITATION,
                        "An invitation between these players is already pending");
            }
            requireAvailable(inviter, "Inviter is busy");
            requireAvailable(recipient, "Recipient is busy");
            long createdAt = clock.millis();
            String id = UUID.randomUUID().toString();
            invitation = new Invitation(id, inviter, recipient, createdAt,
                    createdAt + invitationDuration.toMillis(), InvitationStatus.PENDING);
            invitations.put(id, invitation);
            activeInvitationByPlayer.put(inviter, id);
            activeInvitationByPlayer.put(recipient, id);
            playerStates.put(inviter, PlayerMatchmakingState.INVITATION_PENDING);
            playerStates.put(recipient, PlayerMatchmakingState.INVITATION_PENDING);
            expirationTasks.put(id, scheduler.schedule(() -> expireInvitation(id),
                    invitationDuration.toMillis(), TimeUnit.MILLISECONDS));
            events = List.of(new MatchmakingEvent(recipient,
                    MessageType.INVITATION_RECEIVED, invitation));
        }
        publisher.accept(events);
        return invitation;
    }

    void respond(String username, String invitationId, boolean accept)
            throws MatchmakingServiceException {
        List<MatchmakingEvent> events;
        synchronized (lock) {
            ensureOpen();
            Invitation current = requireInvitation(invitationId);
            if (!current.getRecipientUsername().equals(username)) {
                throw failure(ProtocolErrorCode.INVITATION_NOT_RECIPIENT,
                        "Only the invitation recipient can respond");
            }
            requirePending(current);
            if (clock.millis() >= current.getExpirationTimeEpochMillis()) {
                events = resolveInvitation(current, InvitationStatus.EXPIRED);
            } else if (accept) {
                events = acceptInvitation(current);
            } else {
                events = resolveInvitation(current, InvitationStatus.REJECTED);
            }
        }
        publisher.accept(events);
        if (!events.isEmpty()
                && ((Invitation) events.get(0).payload()).getStatus() == InvitationStatus.EXPIRED) {
            throw failure(ProtocolErrorCode.INVITATION_EXPIRED, "The invitation has expired");
        }
    }

    void cancel(String username, String invitationId) throws MatchmakingServiceException {
        List<MatchmakingEvent> events;
        synchronized (lock) {
            ensureOpen();
            Invitation current = requireInvitation(invitationId);
            if (!current.getInviterUsername().equals(username)) {
                throw failure(ProtocolErrorCode.INVITATION_NOT_RECIPIENT,
                        "Only the inviter can cancel this invitation");
            }
            requirePending(current);
            events = resolveInvitation(current, InvitationStatus.CANCELLED);
        }
        publisher.accept(events);
    }

    QueueStatus joinQueue(String username) throws MatchmakingServiceException {
        List<MatchmakingEvent> events = new ArrayList<>();
        QueueStatus response;
        synchronized (lock) {
            ensureOpen();
            PlayerMatchmakingState state = stateOf(username);
            if (state == PlayerMatchmakingState.QUEUED) {
                throw failure(ProtocolErrorCode.ALREADY_QUEUED,
                        "The player is already in the random queue");
            }
            if (state != PlayerMatchmakingState.AVAILABLE) {
                throw failure(ProtocolErrorCode.PLAYER_BUSY, "The player is busy");
            }
            QueueEntry opponent = pollValidOpponent(username);
            long now = clock.millis();
            if (opponent == null) {
                queue.addLast(new QueueEntry(username, now));
                playerStates.put(username, PlayerMatchmakingState.QUEUED);
                response = new QueueStatus(PlayerMatchmakingState.QUEUED, now, queue.size());
                events.add(new MatchmakingEvent(username,
                        MessageType.QUEUE_STATUS_CHANGED, response));
            } else {
                MatchEvents match = createMatch(opponent.username(), username, now);
                response = new QueueStatus(PlayerMatchmakingState.MATCHED, now, 0);
                events.add(new MatchmakingEvent(opponent.username(),
                        MessageType.QUEUE_STATUS_CHANGED, response));
                events.add(new MatchmakingEvent(username,
                        MessageType.QUEUE_STATUS_CHANGED, response));
                events.addAll(match.events());
            }
        }
        publisher.accept(events);
        return response;
    }

    void leaveQueue(String username) throws MatchmakingServiceException {
        List<MatchmakingEvent> events;
        synchronized (lock) {
            ensureOpen();
            if (stateOf(username) != PlayerMatchmakingState.QUEUED) {
                throw failure(ProtocolErrorCode.NOT_QUEUED,
                        "The player is not in the random queue");
            }
            queue.removeIf(entry -> entry.username().equals(username));
            playerStates.remove(username);
            QueueStatus status = new QueueStatus(PlayerMatchmakingState.AVAILABLE,
                    clock.millis(), 0);
            events = List.of(new MatchmakingEvent(username,
                    MessageType.QUEUE_STATUS_CHANGED, status));
        }
        publisher.accept(events);
    }

    void playerDisconnected(String username) {
        if (username == null) {
            return;
        }
        List<MatchmakingEvent> events = new ArrayList<>();
        synchronized (lock) {
            if (closed) {
                return;
            }
            PlayerMatchmakingState state = stateOf(username);
            if (state == PlayerMatchmakingState.INVITATION_PENDING) {
                Invitation invitation = invitations.get(activeInvitationByPlayer.get(username));
                if (invitation != null && invitation.getStatus() == InvitationStatus.PENDING) {
                    events.addAll(resolveInvitation(invitation, InvitationStatus.CANCELLED));
                    events.removeIf(event -> event.username().equals(username));
                }
            } else if (state == PlayerMatchmakingState.QUEUED) {
                queue.removeIf(entry -> entry.username().equals(username));
                playerStates.remove(username);
            } else if (state == PlayerMatchmakingState.MATCHED) {
                String matchId = matchByPlayer.remove(username);
                PreGameMatch match = matches.remove(matchId);
                playerStates.remove(username);
                if (match != null) {
                    String remaining = match.first().equals(username)
                            ? match.second() : match.first();
                    matchByPlayer.remove(remaining, matchId);
                    playerStates.remove(remaining);
                    events.add(new MatchmakingEvent(remaining, MessageType.MATCH_CANCELLED,
                            new MatchCancelled(matchId, username,
                                    "Opponent disconnected before gameplay started")));
                }
            } else {
                playerStates.remove(username);
            }
        }
        publisher.accept(events);
    }

    PlayerMatchmakingState stateOfPlayer(String username) {
        synchronized (lock) {
            return stateOf(username);
        }
    }

    int activeMatchCount() {
        synchronized (lock) { return matches.size(); }
    }

    int queuedPlayerCount() {
        synchronized (lock) { return queue.size(); }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) return;
            closed = true;
            for (ScheduledFuture<?> task : expirationTasks.values()) task.cancel(false);
            expirationTasks.clear();
            invitations.clear();
            activeInvitationByPlayer.clear();
            queue.clear();
            matches.clear();
            matchByPlayer.clear();
            playerStates.clear();
        }
        scheduler.shutdownNow();
    }

    void expireInvitation(String invitationId) {
        List<MatchmakingEvent> events = List.of();
        synchronized (lock) {
            Invitation invitation = invitations.get(invitationId);
            if (!closed && invitation != null
                    && invitation.getStatus() == InvitationStatus.PENDING) {
                long remaining = invitation.getExpirationTimeEpochMillis() - clock.millis();
                if (remaining <= 0) {
                    events = resolveInvitation(invitation, InvitationStatus.EXPIRED);
                } else {
                    expirationTasks.put(invitationId, scheduler.schedule(
                            () -> expireInvitation(invitationId), remaining,
                            TimeUnit.MILLISECONDS));
                }
            }
        }
        publisher.accept(events);
    }

    private List<MatchmakingEvent> acceptInvitation(Invitation current) {
        cancelExpiration(current.getInvitationId());
        Invitation accepted = copyWithStatus(current, InvitationStatus.ACCEPTED);
        invitations.put(current.getInvitationId(), accepted);
        clearInvitationPlayers(current);
        MatchEvents match = createMatch(current.getInviterUsername(),
                current.getRecipientUsername(), clock.millis());
        List<MatchmakingEvent> events = new ArrayList<>();
        events.add(new MatchmakingEvent(current.getInviterUsername(),
                MessageType.INVITATION_RESULT, accepted));
        events.add(new MatchmakingEvent(current.getRecipientUsername(),
                MessageType.INVITATION_RESULT, accepted));
        events.addAll(match.events());
        return events;
    }

    private List<MatchmakingEvent> resolveInvitation(
            Invitation current, InvitationStatus status) {
        cancelExpiration(current.getInvitationId());
        Invitation resolved = copyWithStatus(current, status);
        invitations.put(current.getInvitationId(), resolved);
        clearInvitationPlayers(current);
        return List.of(
                new MatchmakingEvent(current.getInviterUsername(),
                        MessageType.INVITATION_RESULT, resolved),
                new MatchmakingEvent(current.getRecipientUsername(),
                        MessageType.INVITATION_RESULT, resolved));
    }

    private void clearInvitationPlayers(Invitation invitation) {
        activeInvitationByPlayer.remove(invitation.getInviterUsername(),
                invitation.getInvitationId());
        activeInvitationByPlayer.remove(invitation.getRecipientUsername(),
                invitation.getInvitationId());
        playerStates.remove(invitation.getInviterUsername());
        playerStates.remove(invitation.getRecipientUsername());
    }

    private MatchEvents createMatch(String first, String second, long createdAt) {
        String matchId = UUID.randomUUID().toString();
        MatchRole firstRole = firstPlayerGetsPlants ? MatchRole.PLANTS : MatchRole.ZOMBIES;
        firstPlayerGetsPlants = !firstPlayerGetsPlants;
        MatchRole secondRole = firstRole == MatchRole.PLANTS
                ? MatchRole.ZOMBIES : MatchRole.PLANTS;
        PreGameMatch match = new PreGameMatch(matchId, first, second,
                firstRole, secondRole, createdAt);
        matches.put(matchId, match);
        matchByPlayer.put(first, matchId);
        matchByPlayer.put(second, matchId);
        playerStates.put(first, PlayerMatchmakingState.MATCHED);
        playerStates.put(second, PlayerMatchmakingState.MATCHED);
        return new MatchEvents(List.of(
                new MatchmakingEvent(first, MessageType.MATCH_FOUND,
                        new MatchAssignment(matchId, first, second, firstRole, createdAt,
                                MatchStatus.PRE_GAME)),
                new MatchmakingEvent(second, MessageType.MATCH_FOUND,
                        new MatchAssignment(matchId, second, first, secondRole, createdAt,
                                MatchStatus.PRE_GAME))));
    }

    private QueueEntry pollValidOpponent(String joiningUsername) {
        while (!queue.isEmpty()) {
            QueueEntry candidate = queue.removeFirst();
            if (!candidate.username().equals(joiningUsername)
                    && stateOf(candidate.username()) == PlayerMatchmakingState.QUEUED
                    && online.test(candidate.username())) {
                return candidate;
            }
            playerStates.remove(candidate.username());
        }
        return null;
    }

    private Invitation requireInvitation(String id) throws MatchmakingServiceException {
        Invitation invitation = invitations.get(id);
        if (invitation == null) {
            throw failure(ProtocolErrorCode.INVITATION_NOT_FOUND,
                    "The invitation does not exist");
        }
        return invitation;
    }

    private void requirePending(Invitation invitation) throws MatchmakingServiceException {
        if (invitation.getStatus() == InvitationStatus.EXPIRED) {
            throw failure(ProtocolErrorCode.INVITATION_EXPIRED,
                    "The invitation has expired");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw failure(ProtocolErrorCode.INVITATION_ALREADY_RESOLVED,
                    "The invitation has already been resolved");
        }
    }

    private void requireAvailable(String username, String message)
            throws MatchmakingServiceException {
        if (stateOf(username) != PlayerMatchmakingState.AVAILABLE) {
            throw failure(ProtocolErrorCode.PLAYER_BUSY, message);
        }
    }

    private PlayerMatchmakingState stateOf(String username) {
        return playerStates.getOrDefault(username, PlayerMatchmakingState.AVAILABLE);
    }

    private void cancelExpiration(String invitationId) {
        ScheduledFuture<?> task = expirationTasks.remove(invitationId);
        if (task != null) task.cancel(false);
    }

    private void ensureOpen() throws MatchmakingServiceException {
        if (closed) {
            throw failure(ProtocolErrorCode.INTERNAL_SERVER_ERROR,
                    "Matchmaking is unavailable");
        }
    }

    private static Invitation copyWithStatus(Invitation invitation, InvitationStatus status) {
        return new Invitation(invitation.getInvitationId(),
                invitation.getInviterUsername(), invitation.getRecipientUsername(),
                invitation.getCreationTimeEpochMillis(),
                invitation.getExpirationTimeEpochMillis(), status);
    }

    private static MatchmakingServiceException failure(
            ProtocolErrorCode code, String message) {
        return new MatchmakingServiceException(code, message);
    }

    private record QueueEntry(String username, long joinedAt) { }
    private record MatchEvents(List<MatchmakingEvent> events) { }
    private record PreGameMatch(String id, String first, String second,
            MatchRole firstRole, MatchRole secondRole, long createdAt) { }
}
