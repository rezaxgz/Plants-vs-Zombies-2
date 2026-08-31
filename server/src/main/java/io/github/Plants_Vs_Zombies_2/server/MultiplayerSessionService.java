package io.github.Plants_Vs_Zombies_2.server;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer.MultiplayerIZombieConfig;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer.MultiplayerIZombieGame;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer.MultiplayerRuleException;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ActionResult;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchFinishReason;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchPlayerSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionEvent;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionReceipt;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionType;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ReadyStatus;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

/** Canonical owner of authoritative multiplayer I, Zombie sessions. */
final class MultiplayerSessionService implements AutoCloseable {
    static final String TICK_RATE_PROPERTY = "pvz.server.multiplayer.tick.rate";
    static final String MATCH_DURATION_PROPERTY =
            "pvz.server.multiplayer.match.duration.seconds";
    static final String REACTION_COOLDOWN_PROPERTY =
            "pvz.server.multiplayer.reaction.cooldown.millis";
    static final int DEFAULT_TICK_RATE = 20;
    static final double DEFAULT_MATCH_DURATION_SECONDS = 120.0;
    static final long DEFAULT_REACTION_COOLDOWN_MILLIS = 1_000L;
    private static final int SNAPSHOT_BROADCASTS_PER_SECOND = 5;
    private static final Logger LOGGER = Logger.getLogger(
            MultiplayerSessionService.class.getName());

    private final Object registryLock = new Object();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, String> matchByPlayer = new HashMap<>();
    private final Consumer<List<MatchmakingEvent>> publisher;
    private final MultiplayerIZombieConfig config;
    private final Clock clock;
    private final LongSupplier seedSupplier;
    private final int tickRate;
    private final double matchDurationSeconds;
    private final long reactionCooldownMillis;
    private final int broadcastEveryTicks;
    private final ScheduledExecutorService simulationScheduler;
    private final ScheduledFuture<?> simulationTask;
    private final ExecutorService reactionPublisher;
    private boolean closed;

    MultiplayerSessionService(Consumer<List<MatchmakingEvent>> publisher) {
        this(publisher, MultiplayerIZombieConfig.firstBiteDefaults(),
                Clock.systemUTC(), secureSeedSupplier(), configuredTickRate(),
                configuredMatchDuration(), configuredReactionCooldown(),
                createSimulationScheduler(), true);
    }

    /**
     * Deterministic constructor used by tests. No real-time task is installed;
     * tests advance the fixed-step simulation with {@link #tickOnceForTesting()}.
     */
    MultiplayerSessionService(Consumer<List<MatchmakingEvent>> publisher,
            MultiplayerIZombieConfig config, Clock clock, LongSupplier seedSupplier) {
        this(publisher, config, clock, seedSupplier, DEFAULT_TICK_RATE,
                DEFAULT_MATCH_DURATION_SECONDS,
                DEFAULT_REACTION_COOLDOWN_MILLIS, null, false);
    }

    MultiplayerSessionService(Consumer<List<MatchmakingEvent>> publisher,
            MultiplayerIZombieConfig config, Clock clock, LongSupplier seedSupplier,
            int tickRate, double matchDurationSeconds) {
        this(publisher, config, clock, seedSupplier, tickRate,
                matchDurationSeconds, DEFAULT_REACTION_COOLDOWN_MILLIS,
                null, false);
    }

    MultiplayerSessionService(Consumer<List<MatchmakingEvent>> publisher,
            MultiplayerIZombieConfig config, Clock clock, LongSupplier seedSupplier,
            int tickRate, double matchDurationSeconds,
            long reactionCooldownMillis) {
        this(publisher, config, clock, seedSupplier, tickRate,
                matchDurationSeconds, reactionCooldownMillis, null, false);
    }

    MultiplayerSessionService(Consumer<List<MatchmakingEvent>> publisher,
            MultiplayerIZombieConfig config, Clock clock, LongSupplier seedSupplier,
            int tickRate, double matchDurationSeconds,
            long reactionCooldownMillis, ExecutorService reactionPublisher) {
        this(publisher, config, clock, seedSupplier, tickRate,
                matchDurationSeconds, reactionCooldownMillis, null, false,
                reactionPublisher);
    }

    private MultiplayerSessionService(Consumer<List<MatchmakingEvent>> publisher,
            MultiplayerIZombieConfig config, Clock clock, LongSupplier seedSupplier,
            int tickRate, double matchDurationSeconds,
            long reactionCooldownMillis,
            ScheduledExecutorService simulationScheduler, boolean autoSchedule) {
        this(publisher, config, clock, seedSupplier, tickRate,
                matchDurationSeconds, reactionCooldownMillis,
                simulationScheduler, autoSchedule, null);
    }

    private MultiplayerSessionService(Consumer<List<MatchmakingEvent>> publisher,
            MultiplayerIZombieConfig config, Clock clock, LongSupplier seedSupplier,
            int tickRate, double matchDurationSeconds,
            long reactionCooldownMillis,
            ScheduledExecutorService simulationScheduler, boolean autoSchedule,
            ExecutorService reactionPublisher) {
        if (tickRate <= 0 || !Double.isFinite(matchDurationSeconds)
                || matchDurationSeconds <= 0.0 || reactionCooldownMillis < 0) {
            throw new IllegalArgumentException(
                    "Multiplayer timing configuration is invalid");
        }
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.config = Objects.requireNonNull(config, "config");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.seedSupplier = Objects.requireNonNull(seedSupplier, "seedSupplier");
        this.tickRate = tickRate;
        this.matchDurationSeconds = matchDurationSeconds;
        this.reactionCooldownMillis = reactionCooldownMillis;
        this.broadcastEveryTicks = Math.max(1,
                tickRate / SNAPSHOT_BROADCASTS_PER_SECOND);
        this.simulationScheduler = simulationScheduler;
        this.reactionPublisher = reactionPublisher == null
                ? createReactionPublisher() : reactionPublisher;
        if (autoSchedule) {
            long periodNanos = Math.max(1L,
                    Math.round(1_000_000_000.0 / tickRate));
            this.simulationTask = simulationScheduler.scheduleAtFixedRate(
                    this::scheduledTickSafely, periodNanos, periodNanos,
                    TimeUnit.NANOSECONDS);
        } else {
            this.simulationTask = null;
        }
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
            MultiplayerIZombieGame game = new MultiplayerIZombieGame(
                    config, seedSupplier.getAsLong());
            Session session = new Session(matchId, plants, zombies, createdAt,
                    game, new AuthoritativeIZombieSimulation(
                            config.getBoardRows(), config.getBoardColumns()));
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
        publish(outcome.events());
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

    MatchReactionReceipt sendReaction(String username, String matchId,
            MatchReactionType reactionType) throws MultiplayerSessionException {
        Session session = requireSession(matchId);
        ReactionOutcome outcome = session.createReaction(username, reactionType);
        // Registration happens atomically with acceptance while the session owns
        // its lifecycle lock. Opening the gate here guarantees that the worker
        // cannot route events until that lock has been released.
        outcome.publication().release();
        return outcome.receipt();
    }

    void leave(String username, String matchId) throws MultiplayerSessionException {
        List<MatchmakingEvent> events;
        synchronized (registryLock) {
            Session session = sessions.get(matchId);
            if (session == null) throw notFound();
            session.requireParticipant(username);
            events = cancelLocked(session, username, MatchFinishReason.PLAYER_LEFT);
        }
        publish(events);
    }

    void playerDisconnected(String username) {
        if (username == null) return;
        List<MatchmakingEvent> events = List.of();
        synchronized (registryLock) {
            if (closed) return;
            String matchId = matchByPlayer.get(username);
            Session session = matchId == null ? null : sessions.get(matchId);
            if (session != null) {
                events = cancelLocked(session, username,
                        MatchFinishReason.PLAYER_DISCONNECTED);
            }
        }
        publish(events);
    }

    int activeSessionCount() {
        synchronized (registryLock) { return sessions.size(); }
    }

    /** One exact fixed step; package-private so tests never need sleeps. */
    void tickOnceForTesting() {
        tickAllSessions();
    }

    private void scheduledTickSafely() {
        try {
            tickAllSessions();
        } catch (RuntimeException exception) {
            // Scheduled executors suppress all future executions if an unchecked
            // exception escapes. Keep other matches alive and report the fault.
            LOGGER.log(Level.SEVERE, "Multiplayer simulation tick failed", exception);
        }
    }

    private void tickAllSessions() {
        List<Session> current;
        synchronized (registryLock) {
            if (closed) return;
            current = List.copyOf(sessions.values());
        }

        List<MatchmakingEvent> events = new ArrayList<>();
        List<Session> finished = new ArrayList<>();
        for (Session session : current) {
            TickOutcome outcome;
            try {
                outcome = session.tick();
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING,
                        "Isolated failure in match " + session.matchId, exception);
                continue;
            }
            events.addAll(outcome.events());
            if (outcome.finished()) finished.add(session);
        }

        if (!finished.isEmpty()) {
            synchronized (registryLock) {
                for (Session session : finished) {
                    if (sessions.remove(session.matchId, session)) {
                        matchByPlayer.remove(session.plantsUsername, session.matchId);
                        matchByPlayer.remove(session.zombiesUsername, session.matchId);
                    }
                }
            }
        }
        // Never write to sockets while a session or registry monitor is held.
        publish(events);
    }

    private List<MatchmakingEvent> cancelLocked(Session session,
            String departingUsername, MatchFinishReason reason) {
        Cancellation cancellation = session.cancel(departingUsername, reason);
        sessions.remove(session.matchId, session);
        matchByPlayer.remove(session.plantsUsername, session.matchId);
        matchByPlayer.remove(session.zombiesUsername, session.matchId);
        if (cancellation.remainingUsername() == null) return List.of();
        return List.of(new MatchmakingEvent(cancellation.remainingUsername(),
                MessageType.MATCH_CANCELLED,
                new MatchCancelled(session.matchId, departingUsername,
                        reason.name())));
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
        List<MatchmakingEvent> cancellationEvents = new ArrayList<>();
        synchronized (registryLock) {
            if (closed) return;
            closed = true;
            for (Session session : sessions.values()) {
                session.cancelForShutdown();
                cancellationEvents.add(new MatchmakingEvent(
                        session.plantsUsername, MessageType.MATCH_CANCELLED,
                        new MatchCancelled(session.matchId,
                                session.zombiesUsername,
                                MatchFinishReason.SERVER_SHUTDOWN.name())));
                cancellationEvents.add(new MatchmakingEvent(
                        session.zombiesUsername, MessageType.MATCH_CANCELLED,
                        new MatchCancelled(session.matchId,
                                session.plantsUsername,
                                MatchFinishReason.SERVER_SHUTDOWN.name())));
            }
            sessions.clear();
            matchByPlayer.clear();
        }
        if (simulationTask != null) simulationTask.cancel(false);
        if (simulationScheduler != null) simulationScheduler.shutdownNow();
        reactionPublisher.shutdown();
        boolean interrupted = false;
        while (!reactionPublisher.isTerminated()) {
            try {
                reactionPublisher.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException exception) {
                // Accepted reactions are never discarded during shutdown. Finish
                // draining outside all gameplay locks, then restore interruption.
                interrupted = true;
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
        publish(cancellationEvents);
    }

    private void publish(List<MatchmakingEvent> events) {
        if (events == null || events.isEmpty()) return;
        try {
            publisher.accept(List.copyOf(events));
        } catch (RuntimeException exception) {
            // A failed/slow client route must never terminate the shared tick task.
            LOGGER.log(Level.WARNING, "Could not publish multiplayer event", exception);
        }
    }

    private static int configuredTickRate() {
        int value = Integer.getInteger(TICK_RATE_PROPERTY, DEFAULT_TICK_RATE);
        if (value <= 0) {
            throw new IllegalArgumentException(TICK_RATE_PROPERTY + " must be positive");
        }
        return value;
    }

    private static double configuredMatchDuration() {
        String configured = System.getProperty(MATCH_DURATION_PROPERTY);
        double value = configured == null || configured.isBlank()
                ? DEFAULT_MATCH_DURATION_SECONDS : Double.parseDouble(configured);
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(
                    MATCH_DURATION_PROPERTY + " must be positive");
        }
        return value;
    }

    private static long configuredReactionCooldown() {
        long value = Long.getLong(REACTION_COOLDOWN_PROPERTY,
                DEFAULT_REACTION_COOLDOWN_MILLIS);
        if (value < 0) {
            throw new IllegalArgumentException(
                    REACTION_COOLDOWN_PROPERTY + " cannot be negative");
        }
        return value;
    }

    private static ScheduledExecutorService createSimulationScheduler() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "pvz2-multiplayer-simulation");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static ExecutorService createReactionPublisher() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable,
                    "pvz2-multiplayer-reaction-events");
            thread.setDaemon(true);
            return thread;
        });
    }

    private ReactionPublication registerReactionPublication(
            MatchReactionEvent event, String plantsUsername,
            String zombiesUsername) throws MultiplayerSessionException {
        CompletableFuture<Void> releaseGate = new CompletableFuture<>();
        List<MatchmakingEvent> events = List.of(
                new MatchmakingEvent(plantsUsername,
                        MessageType.MATCH_REACTION_RECEIVED, event),
                new MatchmakingEvent(zombiesUsername,
                        MessageType.MATCH_REACTION_RECEIVED, event));
        try {
            reactionPublisher.execute(() -> {
                releaseGate.join();
                publish(events);
            });
        } catch (RejectedExecutionException exception) {
            throw failure(ProtocolErrorCode.MATCH_NOT_ACTIVE,
                    "The match is no longer accepting reactions");
        }
        return new ReactionPublication(releaseGate);
    }

    private static LongSupplier secureSeedSupplier() {
        SecureRandom seeds = new SecureRandom();
        return seeds::nextLong;
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
        private final AuthoritativeIZombieSimulation simulation;
        private boolean plantsReady;
        private boolean zombiesReady;
        private boolean simulationStarted;
        private MatchStatus status = MatchStatus.PRE_GAME;
        private long simulationTick;
        private long revision;
        private MatchRole winner;
        private MatchFinishReason finishReason;
        private MatchStateSnapshot snapshot;
        private final Map<String, Long> lastReactionAt = new HashMap<>();
        private long reactionSequence;

        private Session(String matchId, String plantsUsername,
                String zombiesUsername, long createdAt,
                MultiplayerIZombieGame game,
                AuthoritativeIZombieSimulation simulation) {
            this.matchId = matchId;
            this.plantsUsername = plantsUsername;
            this.zombiesUsername = zombiesUsername;
            this.createdAt = createdAt;
            this.game = game;
            this.simulation = simulation;
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
            if (status == MatchStatus.ACTIVE) simulationStarted = true;
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
                simulation.addPlant(id, plantType, row, column);
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
                String removed = game.removePlant(entityId);
                simulation.removePlant(entityId);
                return accepted(removed);
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
                // Stage 5 already validates the requested card. Its canonical
                // enum name is exposed by the accepted entity snapshot.
                String canonicalType = game.getZombies().stream()
                        .filter(entity -> entity.getEntityId().equals(id))
                        .findFirst().orElseThrow().getEntityType();
                simulation.addZombie(id, canonicalType, row, column);
                return accepted(id);
            } catch (MultiplayerRuleException exception) {
                throw ruleFailure(exception);
            }
        }

        synchronized ReactionOutcome createReaction(String username,
                MatchReactionType reactionType)
                throws MultiplayerSessionException {
            requireParticipant(username);
            if (status != MatchStatus.ACTIVE) {
                throw failure(ProtocolErrorCode.MATCH_NOT_ACTIVE,
                        "Reactions are available only during an active match");
            }
            if (reactionType == null) {
                throw failure(ProtocolErrorCode.VALIDATION_FAILED,
                        "A predefined reaction identifier is required");
            }
            long now = clock.millis();
            Long previous = lastReactionAt.get(username);
            if (previous != null && now - previous < reactionCooldownMillis) {
                throw failure(ProtocolErrorCode.REACTION_RATE_LIMITED,
                        "Wait for the reaction cooldown before sending again");
            }
            if (reactionSequence == Long.MAX_VALUE) {
                throw failure(ProtocolErrorCode.ACTION_NOT_ALLOWED,
                        "The match reaction sequence is exhausted");
            }
            long sequence = reactionSequence + 1L;
            MatchReactionEvent event = new MatchReactionEvent(matchId, username,
                    reactionType, reactionType.getKind(), sequence, now);
            MatchReactionReceipt receipt = new MatchReactionReceipt(matchId,
                    reactionType, sequence, now);
            ReactionPublication publication = registerReactionPublication(event,
                    plantsUsername, zombiesUsername);
            reactionSequence = sequence;
            lastReactionAt.put(username, now);
            return new ReactionOutcome(receipt, publication);
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

        synchronized TickOutcome tick() {
            if (!simulationStarted || status != MatchStatus.ACTIVE) {
                return TickOutcome.EMPTY;
            }
            AuthoritativeIZombieSimulation.TickResult result = simulation.tick(
                    1.0 / tickRate);
            simulationTick++;
            for (String removedId : result.removedEntityIds()) {
                game.removeEntityAfterSimulation(removedId);
            }
            for (AuthoritativeIZombieSimulation.ZombiePosition position
                    : simulation.zombiePositions()) {
                game.synchronizeZombiePosition(position.entityId(), position.row(),
                        position.columnPosition());
            }

            if (simulation.allBrainsEaten()) {
                finish(MatchRole.ZOMBIES, MatchFinishReason.ALL_BRAINS_EATEN);
                return terminalEvents();
            }
            if (elapsedSeconds() + 0.0000001 >= matchDurationSeconds) {
                finish(MatchRole.PLANTS, MatchFinishReason.TIME_EXPIRED);
                return terminalEvents();
            }

            refreshSnapshot();
            if (simulationTick % broadcastEveryTicks == 0) {
                return new TickOutcome(false, stateEvents(MessageType.MATCH_STATE_UPDATED));
            }
            return TickOutcome.EMPTY;
        }

        private void finish(MatchRole winningRole, MatchFinishReason reason) {
            if (status == MatchStatus.FINISHED) return;
            winner = winningRole;
            finishReason = reason;
            status = MatchStatus.FINISHED;
            clearReactionState();
            revision++; // lifecycle mutation; simulation ticks never touch revision.
            refreshSnapshot();
        }

        private TickOutcome terminalEvents() {
            return new TickOutcome(true, stateEvents(MessageType.MATCH_FINISHED));
        }

        private List<MatchmakingEvent> stateEvents(MessageType type) {
            return List.of(
                    new MatchmakingEvent(plantsUsername, type, snapshot),
                    new MatchmakingEvent(zombiesUsername, type, snapshot));
        }

        synchronized Cancellation cancel(String departingUsername,
                MatchFinishReason reason) {
            if (status != MatchStatus.CANCELLED && status != MatchStatus.FINISHED) {
                status = MatchStatus.CANCELLED;
                finishReason = reason;
                revision++;
                simulationStarted = false;
                clearReactionState();
                refreshSnapshot();
            }
            String remaining = plantsUsername.equals(departingUsername)
                    ? zombiesUsername : plantsUsername;
            return new Cancellation(remaining);
        }

        synchronized void cancelForShutdown() {
            if (status == MatchStatus.FINISHED) return;
            status = MatchStatus.CANCELLED;
            finishReason = MatchFinishReason.SERVER_SHUTDOWN;
            revision++;
            simulationStarted = false;
            clearReactionState();
            refreshSnapshot();
        }

        private void clearReactionState() {
            lastReactionAt.clear();
        }

        private ReadyStatus readyStatus() {
            return new ReadyStatus(matchId, status, plantsReady,
                    zombiesReady, revision);
        }

        private double elapsedSeconds() {
            return Math.min(matchDurationSeconds,
                    simulationTick / (double) tickRate);
        }

        private void refreshSnapshot() {
            double elapsed = elapsedSeconds();
            snapshot = new MatchStateSnapshot(matchId, status, simulationTick,
                    revision, clock.millis(), elapsed,
                    Math.max(0.0, matchDurationSeconds - elapsed),
                    game.getConfig().getLevel().name(), game.getSeed(),
                    game.getConfig().getBoardRows(),
                    game.getConfig().getBoardColumns(),
                    game.getConfig().getRedLineColumn(),
                    List.of(
                            new MatchPlayerSnapshot(plantsUsername,
                                    MatchRole.PLANTS, plantsReady),
                            new MatchPlayerSnapshot(zombiesUsername,
                                    MatchRole.ZOMBIES, zombiesReady)),
                    game.getPlantResource(), game.getZombieResource(),
                    simulation.plantSnapshots(), simulation.zombieSnapshots(),
                    simulation.projectileSnapshots(), simulation.brainsAvailable(),
                    winner, finishReason);
        }

        @SuppressWarnings("unused")
        long getCreatedAt() { return createdAt; }
    }

    private static MultiplayerSessionException ruleFailure(
            MultiplayerRuleException exception) {
        return failure(ProtocolErrorCode.valueOf(exception.getError().name()),
                exception.getMessage());
    }

    private record SessionOutcome<T>(T value, List<MatchmakingEvent> events) { }
    private record ReactionOutcome(MatchReactionReceipt receipt,
            ReactionPublication publication) { }
    private record ReactionPublication(CompletableFuture<Void> releaseGate) {
        void release() {
            releaseGate.complete(null);
        }
    }
    private record Cancellation(String remainingUsername) { }
    private record TickOutcome(boolean finished, List<MatchmakingEvent> events) {
        private static final TickOutcome EMPTY = new TickOutcome(false, List.of());
    }
}
