package io.github.Plants_Vs_Zombies_2.server;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer.MultiplayerIZombieConfig;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer.MultiplayerIZombieGame;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer.MultiplayerRuleException;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer.PlacedMatchEntity;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ActionResult;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchEntitySnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchPlayerSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ReadyStatus;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

/** Canonical owner of Stage 5 match lifecycle and authoritative game state. */
final class MultiplayerSessionService implements AutoCloseable {
    private final Object registryLock = new Object();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, String> matchByPlayer = new HashMap<>();
    private final Consumer<List<MatchmakingEvent>> publisher;
    private final MultiplayerIZombieConfig config;
    private final Clock clock;
    private final LongSupplier seedSupplier;
    private boolean closed;

    MultiplayerSessionService(Consumer<List<MatchmakingEvent>> publisher) {
        SecureRandom seeds = new SecureRandom();
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.config = MultiplayerIZombieConfig.firstBiteDefaults();
        this.clock = Clock.systemUTC();
        this.seedSupplier = seeds::nextLong;
    }

    MultiplayerSessionService(Consumer<List<MatchmakingEvent>> publisher,
            MultiplayerIZombieConfig config, Clock clock, LongSupplier seedSupplier) {
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.config = Objects.requireNonNull(config, "config");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.seedSupplier = Objects.requireNonNull(seedSupplier, "seedSupplier");
    }

    void createSession(String matchId, String firstUsername, MatchRole firstRole,
            String secondUsername, MatchRole secondRole, long createdAt)
            throws MultiplayerSessionException {
        synchronized (registryLock) {
            ensureOpen();
            if (matchId == null || sessions.containsKey(matchId)
                    || matchByPlayer.containsKey(firstUsername)
                    || matchByPlayer.containsKey(secondUsername)) {
                throw failure(ProtocolErrorCode.PLAYER_BUSY,
                        "A player already owns a multiplayer session");
            }
            if (firstRole == null || secondRole == null || firstRole == secondRole) {
                throw failure(ProtocolErrorCode.INTERNAL_SERVER_ERROR,
                        "The match roles are invalid");
            }
            String plants = firstRole == MatchRole.PLANTS
                    ? firstUsername : secondUsername;
            String zombies = firstRole == MatchRole.ZOMBIES
                    ? firstUsername : secondUsername;
            Session session = new Session(matchId, plants, zombies, createdAt,
                    new MultiplayerIZombieGame(config, seedSupplier.getAsLong()));
            sessions.put(matchId, session);
            matchByPlayer.put(firstUsername, matchId);
            matchByPlayer.put(secondUsername, matchId);
        }
    }

    boolean hasSession(String username) {
        synchronized (registryLock) {
            return matchByPlayer.containsKey(username);
        }
    }

    ReadyStatus markReady(String username, String matchId)
            throws MultiplayerSessionException {
        SessionOutcome<ReadyStatus> outcome = requireSession(matchId).markReady(username);
        publisher.accept(outcome.events());
        return outcome.value();
    }

    MatchStateSnapshot getState(String username, String matchId)
            throws MultiplayerSessionException {
        return requireSession(matchId).getState(username);
    }

    ActionResult placePlant(String username, String matchId, String plantType,
            int row, int column, long expectedRevision)
            throws MultiplayerSessionException {
        return requireSession(matchId).placePlant(username, plantType,
                row, column, expectedRevision);
    }

    ActionResult removePlant(String username, String matchId, String entityId,
            long expectedRevision) throws MultiplayerSessionException {
        return requireSession(matchId).removePlant(username, entityId, expectedRevision);
    }

    ActionResult placeZombie(String username, String matchId, String zombieType,
            int row, int column, long expectedRevision)
            throws MultiplayerSessionException {
        return requireSession(matchId).placeZombie(username, zombieType,
                row, column, expectedRevision);
    }

    void leave(String username, String matchId) throws MultiplayerSessionException {
        List<MatchmakingEvent> events;
        synchronized (registryLock) {
            Session session = sessions.get(matchId);
            if (session == null) throw notFound();
            session.requireParticipant(username);
            events = cancelLocked(session, username, "Opponent left the match");
        }
        publisher.accept(events);
    }

    void playerDisconnected(String username) {
        if (username == null) return;
        List<MatchmakingEvent> events = List.of();
        synchronized (registryLock) {
            String matchId = matchByPlayer.get(username);
            Session session = matchId == null ? null : sessions.get(matchId);
            if (session != null) {
                events = cancelLocked(session, username,
                        "Opponent disconnected before the match finished");
            }
        }
        publisher.accept(events);
    }

    int activeSessionCount() {
        synchronized (registryLock) { return sessions.size(); }
    }

    private List<MatchmakingEvent> cancelLocked(Session session,
            String departingUsername, String reason) {
        Cancellation cancellation = session.cancel(departingUsername);
        sessions.remove(session.matchId, session);
        matchByPlayer.remove(session.plantsUsername, session.matchId);
        matchByPlayer.remove(session.zombiesUsername, session.matchId);
        if (cancellation.remainingUsername() == null) return List.of();
        return List.of(new MatchmakingEvent(cancellation.remainingUsername(),
                MessageType.MATCH_CANCELLED,
                new MatchCancelled(session.matchId, departingUsername, reason)));
    }

    private Session requireSession(String matchId) throws MultiplayerSessionException {
        synchronized (registryLock) {
            Session session = sessions.get(matchId);
            if (session == null) throw notFound();
            return session;
        }
    }

    private void ensureOpen() throws MultiplayerSessionException {
        if (closed) {
            throw failure(ProtocolErrorCode.INTERNAL_SERVER_ERROR,
                    "Multiplayer sessions are unavailable");
        }
    }

    @Override
    public void close() {
        synchronized (registryLock) {
            if (closed) return;
            closed = true;
            for (Session session : sessions.values()) session.close();
            sessions.clear();
            matchByPlayer.clear();
        }
    }

    private static MultiplayerSessionException notFound() {
        return failure(ProtocolErrorCode.MATCH_NOT_FOUND,
                "The match does not exist");
    }

    private static MultiplayerSessionException failure(
            ProtocolErrorCode code, String message) {
        return new MultiplayerSessionException(code, message);
    }

    private final class Session {
        private final String matchId;
        private final String plantsUsername;
        private final String zombiesUsername;
        private final long createdAt;
        private final MultiplayerIZombieGame game;
        private boolean plantsReady;
        private boolean zombiesReady;
        private MatchStatus status = MatchStatus.PRE_GAME;
        private long revision;
        private MatchStateSnapshot snapshot;

        private Session(String matchId, String plantsUsername,
                String zombiesUsername, long createdAt,
                MultiplayerIZombieGame game) {
            this.matchId = matchId;
            this.plantsUsername = plantsUsername;
            this.zombiesUsername = zombiesUsername;
            this.createdAt = createdAt;
            this.game = game;
            refreshSnapshot();
        }

        synchronized SessionOutcome<ReadyStatus> markReady(String username)
                throws MultiplayerSessionException {
            MatchRole role = requireParticipant(username);
            if (status == MatchStatus.ACTIVE) {
                throw failure(ProtocolErrorCode.MATCH_ALREADY_STARTED,
                        "The match has already started");
            }
            if (status == MatchStatus.CANCELLED || status == MatchStatus.FINISHED) {
                throw failure(ProtocolErrorCode.MATCH_NOT_ACTIVE,
                        "The match is no longer waiting for players");
            }
            boolean alreadyReady = role == MatchRole.PLANTS
                    ? plantsReady : zombiesReady;
            if (alreadyReady) return new SessionOutcome<>(readyStatus(), List.of());

            if (role == MatchRole.PLANTS) plantsReady = true;
            else zombiesReady = true;
            revision++;
            status = plantsReady && zombiesReady
                    ? MatchStatus.ACTIVE : MatchStatus.READY;
            refreshSnapshot();

            String opponent = role == MatchRole.PLANTS
                    ? zombiesUsername : plantsUsername;
            ReadyStatus ready = readyStatus();
            List<MatchmakingEvent> events = new ArrayList<>();
            events.add(new MatchmakingEvent(opponent,
                    MessageType.MATCH_PLAYER_READY, ready));
            if (status == MatchStatus.ACTIVE) {
                events.add(new MatchmakingEvent(plantsUsername,
                        MessageType.MATCH_STARTED, snapshot));
                events.add(new MatchmakingEvent(zombiesUsername,
                        MessageType.MATCH_STARTED, snapshot));
            }
            return new SessionOutcome<>(ready, List.copyOf(events));
        }

        synchronized MatchStateSnapshot getState(String username)
                throws MultiplayerSessionException {
            requireParticipant(username);
            return snapshot;
        }

        synchronized ActionResult placePlant(String username, String plantType,
                int row, int column, long expectedRevision)
                throws MultiplayerSessionException {
            requireRole(username, MatchRole.PLANTS);
            requireActiveRevision(expectedRevision);
            try {
                String id = game.placePlant(plantType, row, column);
                return accepted(id);
            } catch (MultiplayerRuleException exception) {
                throw ruleFailure(exception);
            }
        }

        synchronized ActionResult removePlant(String username, String entityId,
                long expectedRevision) throws MultiplayerSessionException {
            requireRole(username, MatchRole.PLANTS);
            requireActiveRevision(expectedRevision);
            try {
                return accepted(game.removePlant(entityId));
            } catch (MultiplayerRuleException exception) {
                throw ruleFailure(exception);
            }
        }

        synchronized ActionResult placeZombie(String username, String zombieType,
                int row, int column, long expectedRevision)
                throws MultiplayerSessionException {
            requireRole(username, MatchRole.ZOMBIES);
            requireActiveRevision(expectedRevision);
            try {
                String id = game.placeZombie(zombieType, row, column);
                return accepted(id);
            } catch (MultiplayerRuleException exception) {
                throw ruleFailure(exception);
            }
        }

        private ActionResult accepted(String entityId) {
            revision++;
            refreshSnapshot();
            return new ActionResult(matchId, revision, entityId, snapshot);
        }

        private void requireActiveRevision(long expectedRevision)
                throws MultiplayerSessionException {
            if (status != MatchStatus.ACTIVE) {
                throw failure(ProtocolErrorCode.MATCH_NOT_ACTIVE,
                        "Both players must be ready before commands are accepted");
            }
            if (expectedRevision != revision) {
                throw failure(ProtocolErrorCode.STALE_MATCH_REVISION,
                        "Expected revision " + revision + " but received "
                                + expectedRevision);
            }
        }

        private MatchRole requireParticipant(String username)
                throws MultiplayerSessionException {
            if (plantsUsername.equals(username)) return MatchRole.PLANTS;
            if (zombiesUsername.equals(username)) return MatchRole.ZOMBIES;
            throw failure(ProtocolErrorCode.NOT_MATCH_PARTICIPANT,
                    "The authenticated player is not in this match");
        }

        private void requireRole(String username, MatchRole expected)
                throws MultiplayerSessionException {
            MatchRole actual = requireParticipant(username);
            if (actual != expected) {
                throw failure(ProtocolErrorCode.WRONG_ROLE,
                        "This command belongs to the " + expected + " role");
            }
        }

        synchronized Cancellation cancel(String departingUsername) {
            if (status != MatchStatus.CANCELLED) {
                status = MatchStatus.CANCELLED;
                revision++;
                refreshSnapshot();
            }
            String remaining = plantsUsername.equals(departingUsername)
                    ? zombiesUsername : plantsUsername;
            return new Cancellation(remaining);
        }

        synchronized void close() {
            status = MatchStatus.CANCELLED;
        }

        private ReadyStatus readyStatus() {
            return new ReadyStatus(matchId, status, plantsReady,
                    zombiesReady, revision);
        }

        private void refreshSnapshot() {
            snapshot = new MatchStateSnapshot(matchId, status, revision,
                    clock.millis(), game.getConfig().getLevel().name(),
                    game.getSeed(), game.getConfig().getBoardRows(),
                    game.getConfig().getBoardColumns(),
                    game.getConfig().getRedLineColumn(),
                    List.of(
                            new MatchPlayerSnapshot(plantsUsername,
                                    MatchRole.PLANTS, plantsReady),
                            new MatchPlayerSnapshot(zombiesUsername,
                                    MatchRole.ZOMBIES, zombiesReady)),
                    game.getPlantResource(), game.getZombieResource(),
                    entitySnapshots(game.getPlants()),
                    entitySnapshots(game.getZombies()),
                    game.getBrainsAvailable());
        }

        @SuppressWarnings("unused")
        long getCreatedAt() { return createdAt; }
    }

    private static List<MatchEntitySnapshot> entitySnapshots(
            List<PlacedMatchEntity> entities) {
        return entities.stream().map(entity -> new MatchEntitySnapshot(
                entity.getEntityId(), entity.getEntityType(), entity.getOwnerRole(),
                entity.getRow(), entity.getColumn())).toList();
    }

    private static MultiplayerSessionException ruleFailure(
            MultiplayerRuleException exception) {
        return failure(ProtocolErrorCode.valueOf(exception.getError().name()),
                exception.getMessage());
    }

    private record SessionOutcome<T>(T value, List<MatchmakingEvent> events) { }
    private record Cancellation(String remainingUsername) { }
}
