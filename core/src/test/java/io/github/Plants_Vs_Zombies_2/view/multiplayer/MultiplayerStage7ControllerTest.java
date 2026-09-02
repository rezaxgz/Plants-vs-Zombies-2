package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.Invitation;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.InvitationStatus;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingListener;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.PlayerMatchmakingState;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.QueueStatus;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ActionResult;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchFinishReason;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionEvent;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionKind;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionReceipt;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionType;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchPlayerSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameException;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameListener;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ReadyStatus;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.session.UiDispatcher;

class MultiplayerStage7ControllerTest {
    @Test
    void directInvitePreventsDuplicateSubmissionAndPropagatesRejection() {
        FakeMatchmaking transport = new FakeMatchmaking();
        AtomicReference<MatchmakingFlowController.State> state = new AtomicReference<>();
        try (MatchmakingFlowController controller = new MatchmakingFlowController(
                transport, UiDispatcher.direct(), state::set)) {
            controller.invite("bob");
            controller.invite("bob");
            assertEquals(1, transport.inviteCalls);
            assertTrue(state.get().requestInFlight());

            Invitation sent = invitation("i1", InvitationStatus.PENDING);
            transport.inviteFuture.complete(sent);
            assertEquals("i1", state.get().pendingInvitationId());
            transport.fireInvitationResult(invitation("i1", InvitationStatus.REJECTED));
            assertNull(state.get().pendingInvitationId());
            assertTrue(state.get().status().toLowerCase().contains("rejected"));
        }
    }

    @Test
    void directInviteServerErrorsAreRecoverableAndNoUserManagerIsNeeded() {
        FakeMatchmaking transport = new FakeMatchmaking();
        AtomicReference<MatchmakingFlowController.State> state = new AtomicReference<>();
        try (MatchmakingFlowController controller = new MatchmakingFlowController(
                transport, UiDispatcher.direct(), state::set)) {
            controller.invite("offline-user");
            transport.inviteFuture.completeExceptionally(
                    new IllegalStateException("User is offline"));
            assertTrue(state.get().error());
            assertTrue(state.get().status().contains("offline"));
            assertFalse(state.get().requestInFlight());
        }
    }


    @Test
    void failedMatchmakingRequestCanBeRetriedWithoutDuplicateInFlightCalls() {
        FakeMatchmaking transport = new FakeMatchmaking();
        AtomicReference<MatchmakingFlowController.State> state = new AtomicReference<>();
        try (MatchmakingFlowController controller = new MatchmakingFlowController(
                transport, UiDispatcher.direct(), state::set)) {
            controller.invite("missing-user");
            transport.inviteFuture.completeExceptionally(
                    new IllegalStateException("User not found"));
            assertTrue(state.get().error());
            controller.retryLast();
            assertEquals(2, transport.inviteCalls);
        }
    }

    @Test
    void incomingInvitationRejectIsOneShotAndBridgeRemovesListenerOnClose() {
        FakeMatchmaking transport = new FakeMatchmaking();
        AtomicReference<InvitationNotificationBridge.InvitationView> current =
                new AtomicReference<>();
        InvitationNotificationBridge bridge = new InvitationNotificationBridge(
                transport, UiDispatcher.direct(), new InvitationNotificationBridge.Observer() {
                    @Override public void invitationChanged(
                            InvitationNotificationBridge.InvitationView invitation) {
                        current.set(invitation);
                    }
                    @Override public void matchFound(MatchAssignment assignment) { }
                });
        transport.fireInvitationReceived(invitation("i2", InvitationStatus.PENDING));
        bridge.reject();
        bridge.reject();
        assertEquals(1, transport.responseCalls);
        transport.responseFuture.complete(null);
        assertNull(current.get());
        bridge.close();
        assertEquals(0, transport.listeners.size());
        transport.fireInvitationReceived(invitation("late", InvitationStatus.PENDING));
        assertNull(current.get());
    }

    @Test
    void randomQueueJoinAndLeaveAreIdempotentInUiState() {
        FakeMatchmaking transport = new FakeMatchmaking();
        AtomicReference<MatchmakingFlowController.State> state = new AtomicReference<>();
        try (MatchmakingFlowController controller = new MatchmakingFlowController(
                transport, UiDispatcher.direct(), state::set)) {
            controller.joinQueue();
            controller.joinQueue();
            assertEquals(1, transport.joinCalls);
            transport.joinFuture.complete(new QueueStatus(
                    PlayerMatchmakingState.QUEUED, 10L, 2));
            assertTrue(state.get().queued());

            controller.leaveQueue();
            controller.leaveQueue();
            assertEquals(1, transport.leaveQueueCalls);
            transport.leaveQueueFuture.complete(null);
            assertFalse(state.get().queued());
        }
    }

    @Test
    void incomingInvitationUsesDispatcherAndPreventsDuplicateResponses() {
        FakeMatchmaking transport = new FakeMatchmaking();
        AtomicInteger dispatches = new AtomicInteger();
        UiDispatcher dispatcher = runnable -> {
            dispatches.incrementAndGet();
            runnable.run();
        };
        AtomicReference<InvitationNotificationBridge.InvitationView> current =
                new AtomicReference<>();
        try (InvitationNotificationBridge bridge = new InvitationNotificationBridge(
                transport, dispatcher, new InvitationNotificationBridge.Observer() {
                    @Override public void invitationChanged(
                            InvitationNotificationBridge.InvitationView invitation) {
                        current.set(invitation);
                    }
                    @Override public void matchFound(MatchAssignment assignment) { }
                })) {
            transport.fireInvitationReceived(invitation("i1", InvitationStatus.PENDING));
            assertNotNull(current.get());
            bridge.accept();
            bridge.accept();
            assertEquals(1, transport.responseCalls);
            assertTrue(current.get().responding());
            transport.responseFuture.complete(null);
            assertNull(current.get());
            assertTrue(dispatches.get() >= 2);
        }
    }

    @Test
    void invitationResponseErrorRemainsVisibleAndRecoverableForRetry() {
        FakeMatchmaking transport = new FakeMatchmaking();
        AtomicReference<InvitationNotificationBridge.InvitationView> current =
                new AtomicReference<>();
        try (InvitationNotificationBridge bridge = new InvitationNotificationBridge(
                transport, UiDispatcher.direct(), new InvitationNotificationBridge.Observer() {
                    @Override public void invitationChanged(
                            InvitationNotificationBridge.InvitationView invitation) {
                        current.set(invitation);
                    }
                    @Override public void matchFound(MatchAssignment assignment) { }
                })) {
            transport.fireInvitationReceived(invitation("retry", InvitationStatus.PENDING));
            bridge.accept();
            transport.responseFuture.completeExceptionally(
                    new IllegalStateException("Server rejected this invitation"));
            assertTrue(current.get().status().contains("Server rejected"));
            assertEquals(current.get(), bridge.getCurrentInvitation(),
                    "the navigator's current-view path must retain the real error");
            assertFalse(current.get().responding());

            transport.responseFuture = new CompletableFuture<>();
            bridge.accept();
            assertEquals(2, transport.responseCalls);
            assertTrue(current.get().responding());
            transport.fireInvitationResult(invitation("retry", InvitationStatus.EXPIRED));
            assertNull(current.get());
        }
    }

    @Test
    void navigatorUsesTheErrorSpecificInvitationViewInsteadOfRefetching()
            throws Exception {
        Path navigator = Path.of("src", "main", "java", "io", "github",
                "Plants_Vs_Zombies_2", "view", "screens", "ScreenNavigator.java");
        String source = Files.readString(navigator);
        int start = source.indexOf("private void handleInvitationChanged");
        int end = source.indexOf("private void showCurrentInvitationOn", start);
        String handler = source.substring(start, end);
        assertTrue(handler.contains("showInvitationOn(game.getScreen(), invitation)"));
        assertFalse(handler.contains("getCurrentInvitation()"));
    }

    @Test
    void invitationExpirationClearsPopupAndMatchFoundNavigatesExactlyOnce() {
        FakeMatchmaking transport = new FakeMatchmaking();
        AtomicReference<InvitationNotificationBridge.InvitationView> current =
                new AtomicReference<>();
        AtomicInteger found = new AtomicInteger();
        MatchAssignment assignment = assignment(MatchRole.PLANTS);
        try (InvitationNotificationBridge bridge = new InvitationNotificationBridge(
                transport, UiDispatcher.direct(), new InvitationNotificationBridge.Observer() {
                    @Override public void invitationChanged(
                            InvitationNotificationBridge.InvitationView invitation) {
                        current.set(invitation);
                    }
                    @Override public void matchFound(MatchAssignment value) {
                        found.incrementAndGet();
                    }
                })) {
            transport.fireInvitationReceived(invitation("i1", InvitationStatus.PENDING));
            transport.fireInvitationResult(invitation("i1", InvitationStatus.EXPIRED));
            assertNull(current.get());
            transport.fireMatchFound(assignment);
            transport.fireMatchFound(assignment);
            assertEquals(1, found.get());
        }
    }

    @Test
    void pregameRoleReadyAndStartAreServerDrivenAndOneShot() {
        FakeMultiplayer transport = new FakeMultiplayer();
        AtomicReference<PregameController.State> state = new AtomicReference<>();
        AtomicInteger starts = new AtomicInteger();
        PregameController controller = new PregameController(transport,
                UiDispatcher.direct(), assignment(MatchRole.ZOMBIES),
                new PregameController.Observer() {
                    @Override public void changed(PregameController.State value) {
                        state.set(value);
                    }
                    @Override public void matchStarted(MatchStateSnapshot snapshot) {
                        starts.incrementAndGet();
                    }
                    @Override public void leaveCompleted() { }
                });
        controller.ready();
        controller.ready();
        assertEquals(1, transport.readyCalls);
        transport.readyFuture.complete(new ReadyStatus("m1", MatchStatus.READY,
                false, true, 1L));
        assertEquals(MatchRole.ZOMBIES, state.get().role());
        assertTrue(state.get().localReady());
        assertFalse(state.get().opponentReady());

        transport.fireOpponentReady(new ReadyStatus("m1", MatchStatus.READY,
                true, true, 2L));
        assertTrue(state.get().opponentReady());
        MatchStateSnapshot started = snapshot(1, 2, MatchStatus.ACTIVE, null, null);
        transport.fireStarted(started);
        transport.fireStarted(started);
        assertEquals(1, starts.get());

        controller.close();
        assertEquals(0, transport.listeners.size());
        transport.fireStarted(snapshot(2, 2, MatchStatus.ACTIVE, null, null));
        assertEquals(1, starts.get());
    }


    @Test
    void pregameLeaveAndCancellationAreRecoverableAndDoNotDuplicateNavigation() {
        FakeMultiplayer leaveTransport = new FakeMultiplayer();
        AtomicInteger leaves = new AtomicInteger();
        try (PregameController controller = new PregameController(leaveTransport,
                UiDispatcher.direct(), assignment(MatchRole.PLANTS),
                new PregameController.Observer() {
                    @Override public void changed(PregameController.State value) { }
                    @Override public void matchStarted(MatchStateSnapshot snapshot) { }
                    @Override public void leaveCompleted() { leaves.incrementAndGet(); }
                })) {
            controller.leave();
            controller.leave();
            assertEquals(1, leaveTransport.leaveCalls);
            leaveTransport.leaveFuture.complete(null);
            assertEquals(1, leaves.get());
        }

        FakeMultiplayer cancelTransport = new FakeMultiplayer();
        AtomicReference<PregameController.State> cancelled = new AtomicReference<>();
        try (PregameController controller = new PregameController(cancelTransport,
                UiDispatcher.direct(), assignment(MatchRole.PLANTS),
                new PregameController.Observer() {
                    @Override public void changed(PregameController.State value) {
                        cancelled.set(value);
                    }
                    @Override public void matchStarted(MatchStateSnapshot snapshot) { }
                    @Override public void leaveCompleted() { }
                })) {
            cancelTransport.fireCancelled(new MatchCancelled(
                    "m1", "bob", "PLAYER_DISCONNECTED"));
            assertTrue(cancelled.get().cancelled());
            assertTrue(cancelled.get().status().contains("disconnected"));
            assertFalse(cancelled.get().status().contains("_"));
        }
    }

    @Test
    void liveControllerKeepsNewestTickAndEnforcesRoleCommands() {
        FakeMultiplayer transport = new FakeMultiplayer();
        transport.stateFuture.complete(snapshot(5, 3, MatchStatus.ACTIVE, null, null));
        AtomicReference<LiveMatchController.State> state = new AtomicReference<>();
        try (LiveMatchController controller = new LiveMatchController(transport,
                UiDispatcher.direct(), assignment(MatchRole.PLANTS),
                snapshot(4, 2, MatchStatus.ACTIVE, null, null), state::set)) {
            transport.fireState(snapshot(7, 3, MatchStatus.ACTIVE, null, null));
            transport.fireState(snapshot(6, 99, MatchStatus.ACTIVE, null, null));
            assertEquals(7, state.get().snapshot().getSimulationTick());

            controller.placeZombie("BASIC", 0, 4);
            assertEquals(0, transport.placeZombieCalls);
            controller.placePlant("Peashooter", 0, 0);
            assertEquals(1, transport.placePlantCalls);
            assertEquals("Peashooter", transport.lastPlantType);
            assertEquals(3, transport.lastExpectedRevision);
        }
    }

    @Test
    void placementCommandsPreserveTypeCoordinatesAndPreventDuplicates() {
        MatchStateSnapshot initial = snapshot(5, 3, MatchStatus.ACTIVE,
                null, null);
        FakeMultiplayer plants = new FakeMultiplayer();
        plants.stateFuture.complete(initial);
        try (LiveMatchController controller = new LiveMatchController(plants,
                UiDispatcher.direct(), assignment(MatchRole.PLANTS), initial,
                ignored -> { })) {
            controller.placePlant("Cabbage-pult", 3, 2);
            controller.placePlant("Cabbage-pult", 3, 2);
            assertEquals(1, plants.placePlantCalls);
            assertEquals("Cabbage-pult", plants.lastPlantType);
            assertEquals(3, plants.lastPlantRow);
            assertEquals(2, plants.lastPlantColumn);
            plants.placePlantFuture.complete(new ActionResult(
                    "m1", 4L, "plant-1",
                    snapshot(5, 4, MatchStatus.ACTIVE, null, null)));

            controller.removePlant("plant-1");
            assertEquals(1, plants.removePlantCalls);
            assertEquals("plant-1", plants.lastRemovedEntityId);
        }

        FakeMultiplayer zombies = new FakeMultiplayer();
        zombies.stateFuture.complete(initial);
        try (LiveMatchController controller = new LiveMatchController(zombies,
                UiDispatcher.direct(), assignment(MatchRole.ZOMBIES), initial,
                ignored -> { })) {
            controller.placeZombie("NEWSPAPER", 4, 7);
            controller.placeZombie("NEWSPAPER", 4, 7);
            assertEquals(1, zombies.placeZombieCalls);
            assertEquals("NEWSPAPER", zombies.lastZombieType);
            assertEquals(4, zombies.lastZombieRow);
            assertEquals(7, zombies.lastZombieColumn);
        }
    }

    @Test
    void staleRevisionRefreshesAndRejectedPlacementCreatesNoOptimisticEntity() {
        FakeMultiplayer transport = new FakeMultiplayer();
        MatchStateSnapshot initial = snapshot(10, 4, MatchStatus.ACTIVE, null, null);
        transport.stateFuture.complete(initial);
        AtomicReference<LiveMatchController.State> state = new AtomicReference<>();
        try (LiveMatchController controller = new LiveMatchController(transport,
                UiDispatcher.direct(), assignment(MatchRole.PLANTS), initial, state::set)) {
            controller.placePlant("Peashooter", 0, 0);
            transport.placePlantFuture.completeExceptionally(
                    new MultiplayerGameException(ProtocolErrorCode.STALE_MATCH_REVISION,
                            "stale"));
            assertSame(initial, state.get().snapshot());
            assertTrue(state.get().snapshot().getPlants().isEmpty());
            assertTrue(transport.getStateCalls >= 2);
        }
    }

    @Test
    void malformedSnapshotKeepsTheLastValidGraphicalState() {
        FakeMultiplayer transport = new FakeMultiplayer();
        MatchStateSnapshot initial = snapshot(10, 4, MatchStatus.ACTIVE,
                null, null);
        transport.stateFuture.complete(initial);
        AtomicReference<LiveMatchController.State> state =
                new AtomicReference<>();
        try (LiveMatchController controller = new LiveMatchController(
                transport, UiDispatcher.direct(),
                assignment(MatchRole.PLANTS), initial, state::set)) {
            MatchStateSnapshot malformed = new MatchStateSnapshot(
                    "different-match", MatchStatus.ACTIVE, 11L, 5L,
                    1_000L, 1.0, 119.0, "FIRST_BITE", 7L,
                    5, 9, 3,
                    List.of(
                            new MatchPlayerSnapshot("alice",
                                    MatchRole.PLANTS, true),
                            new MatchPlayerSnapshot("bob",
                                    MatchRole.ZOMBIES, true)),
                    500, 300, List.of(), List.of(), List.of(),
                    List.of(true, true, true, true, true), null, null);
            transport.fireState(malformed);

            assertSame(initial, state.get().snapshot());
            assertTrue(state.get().status().contains("different match"));
        }
    }


    @Test
    void acceptedMutationUsesOnlyReturnedAuthoritativeSnapshotAndCleansListener() {
        FakeMultiplayer transport = new FakeMultiplayer();
        MatchStateSnapshot initial = snapshot(10, 4, MatchStatus.ACTIVE, null, null);
        transport.stateFuture.complete(initial);
        AtomicReference<LiveMatchController.State> state = new AtomicReference<>();
        LiveMatchController controller = new LiveMatchController(transport,
                UiDispatcher.direct(), assignment(MatchRole.ZOMBIES), initial, state::set);
        controller.placeZombie("BASIC", 0, 4);
        assertSame(initial, state.get().snapshot());
        assertEquals("BASIC", transport.lastZombieType);
        MatchStateSnapshot accepted = snapshot(10, 5, MatchStatus.ACTIVE, null, null);
        transport.placeZombieFuture.complete(new ActionResult("m1", 5L,
                "zombie-1", accepted));
        assertSame(accepted, state.get().snapshot());
        controller.close();
        assertEquals(0, transport.listeners.size());
        transport.fireState(snapshot(11, 5, MatchStatus.ACTIVE, null, null));
        assertSame(accepted, state.get().snapshot());
    }

    @Test
    void finishIsShownOnceAndCancellationIsDifferentFromVictory() {
        FakeMultiplayer transport = new FakeMultiplayer();
        MatchStateSnapshot initial = snapshot(1, 1, MatchStatus.ACTIVE, null, null);
        transport.stateFuture.complete(initial);
        AtomicReference<LiveMatchController.State> state = new AtomicReference<>();
        AtomicInteger publications = new AtomicInteger();
        try (LiveMatchController controller = new LiveMatchController(transport,
                UiDispatcher.direct(), assignment(MatchRole.PLANTS), initial, value -> {
                    state.set(value);
                    publications.incrementAndGet();
                })) {
            MatchStateSnapshot terminal = snapshot(30, 2, MatchStatus.FINISHED,
                    MatchRole.PLANTS, MatchFinishReason.TIME_EXPIRED);
            transport.fireFinished(terminal);
            int afterFinish = publications.get();
            transport.fireFinished(terminal);
            transport.fireCancelled(new MatchCancelled("m1", "bob", "disconnect"));
            assertEquals(afterFinish, publications.get());
            assertEquals(LiveMatchController.TerminalKind.VICTORY,
                    state.get().terminalKind());
        }

        FakeMultiplayer cancelledTransport = new FakeMultiplayer();
        cancelledTransport.stateFuture.complete(initial);
        AtomicReference<LiveMatchController.State> cancelled = new AtomicReference<>();
        try (LiveMatchController controller = new LiveMatchController(cancelledTransport,
                UiDispatcher.direct(), assignment(MatchRole.ZOMBIES), initial,
                cancelled::set)) {
            cancelledTransport.fireCancelled(new MatchCancelled(
                    "m1", "alice", "PLAYER_DISCONNECTED"));
            assertEquals(LiveMatchController.TerminalKind.CANCELLATION,
                    cancelled.get().terminalKind());
            assertNull(cancelled.get().snapshot().getWinner());
        }
    }

    @Test
    void reactionAndGameplayRequestsUseIndependentInFlightStates() {
        FakeMultiplayer transport = new FakeMultiplayer();
        MatchStateSnapshot initial = snapshot(1, 2, MatchStatus.ACTIVE, null, null);
        transport.stateFuture.complete(initial);
        AtomicReference<LiveMatchController.State> state = new AtomicReference<>();
        try (LiveMatchController controller = new LiveMatchController(transport,
                UiDispatcher.direct(), assignment(MatchRole.PLANTS), initial, state::set)) {
            controller.sendReaction(MatchReactionType.GOOD_LUCK);
            controller.sendReaction(MatchReactionType.GOOD_LUCK);
            assertEquals(1, transport.reactionCalls);
            assertTrue(state.get().reactionInFlight());

            controller.placePlant("Peashooter", 0, 0);
            assertEquals(1, transport.placePlantCalls);
            assertTrue(state.get().commandInFlight());

            transport.fireReaction(reaction(1, "alice", MatchReactionType.GOOD_LUCK));
            transport.reactionFuture.complete(new MatchReactionReceipt(
                    "m1", MatchReactionType.GOOD_LUCK, 1L, 1_000L));
            assertFalse(state.get().reactionInFlight());
            assertTrue(state.get().commandInFlight());
            transport.placePlantFuture.complete(new ActionResult(
                    "m1", 3L, "plant-1",
                    snapshot(1, 3, MatchStatus.ACTIVE, null, null)));
            assertFalse(state.get().commandInFlight());
        }
    }

    @Test
    void reactionEventsUseDispatcherFilterOrderAndBoundRecentHistory() {
        FakeMultiplayer transport = new FakeMultiplayer();
        MatchStateSnapshot initial = snapshot(1, 2, MatchStatus.ACTIVE, null, null);
        transport.stateFuture.complete(initial);
        List<Runnable> dispatched = new ArrayList<>();
        AtomicReference<LiveMatchController.State> state = new AtomicReference<>();
        try (LiveMatchController controller = new LiveMatchController(transport,
                dispatched::add, assignment(MatchRole.PLANTS), initial, state::set)) {
            transport.fireReaction(reaction(1, "bob", MatchReactionType.SMILE));
            assertTrue(state.get().recentReactions().isEmpty(),
                    "network callback must wait for UiDispatcher");
            dispatched.remove(0).run();
            transport.fireReaction(new MatchReactionEvent("other", "carol",
                    MatchReactionType.LAUGH, MatchReactionKind.EMOJI, 2L, 1_001L));
            transport.fireReaction(reaction(1, "bob", MatchReactionType.SMILE));
            for (long sequence = 2; sequence <= 7; sequence++) {
                transport.fireReaction(reaction(sequence, "bob", MatchReactionType.NICE_MOVE));
            }
            while (!dispatched.isEmpty()) dispatched.remove(0).run();

            assertEquals(5, state.get().recentReactions().size());
            assertEquals(List.of(3L, 4L, 5L, 6L, 7L),
                    state.get().recentReactions().stream()
                            .map(MatchReactionEvent::getSequence).toList());
            transport.fireConnectionLost(new IllegalStateException("disconnected"));
            while (!dispatched.isEmpty()) dispatched.remove(0).run();
            assertEquals(LiveMatchController.TerminalKind.CANCELLATION,
                    state.get().terminalKind());
            assertTrue(state.get().recentReactions().isEmpty());
        }
    }

    @Test
    void reactionFailuresAreRecoverableNotRetriedAndLateCompletionIsIgnored() {
        FakeMultiplayer transport = new FakeMultiplayer();
        MatchStateSnapshot initial = snapshot(1, 2, MatchStatus.ACTIVE, null, null);
        transport.stateFuture.complete(initial);
        AtomicReference<LiveMatchController.State> state = new AtomicReference<>();
        AtomicInteger publications = new AtomicInteger();
        LiveMatchController controller = new LiveMatchController(transport,
                UiDispatcher.direct(), assignment(MatchRole.PLANTS), initial, value -> {
                    state.set(value);
                    publications.incrementAndGet();
                });
        controller.sendReaction(MatchReactionType.ANGRY);
        transport.reactionFuture.completeExceptionally(new TimeoutException("late"));
        assertEquals(1, transport.reactionCalls);
        assertTrue(state.get().reactionStatus().contains("not retried"));
        assertFalse(state.get().reactionInFlight());

        transport.reactionFuture = new CompletableFuture<>();
        controller.sendReaction(MatchReactionType.ANGRY);
        assertEquals(2, transport.reactionCalls);
        transport.reactionFuture.completeExceptionally(new MultiplayerGameException(
                ProtocolErrorCode.REACTION_RATE_LIMITED, "cooldown"));
        assertTrue(state.get().reactionStatus().contains("cooldown"));
        assertFalse(state.get().reactionInFlight());

        transport.reactionFuture = new CompletableFuture<>();
        controller.sendReaction(MatchReactionType.ANGRY);
        assertEquals(3, transport.reactionCalls);
        controller.close();
        int afterClose = publications.get();
        assertEquals(0, transport.listeners.size());
        transport.reactionFuture.complete(new MatchReactionReceipt(
                "m1", MatchReactionType.ANGRY, 3L, 1_003L));
        assertEquals(afterClose, publications.get());
        assertTrue(controller.getState().recentReactions().isEmpty());
    }

    @Test
    void liveScreenReactionBoundaryHasSixCatalogControlsAndNoFreeFormChat() throws Exception {
        Path screen = Path.of("src", "main", "java", "io", "github",
                "Plants_Vs_Zombies_2", "view", "screens",
                "GameScreen.java");
        String source = Files.readString(screen);
        String chatSource = source.substring(
                source.indexOf("private void installMultiplayerChat()"),
                source.indexOf("private void applyMultiplayerState"));
        assertEquals(6, MatchReactionType.values().length);
        assertTrue(chatSource.contains("MatchReactionType.values()"));
        assertTrue(chatSource.contains(
                "multiplayerController.sendReaction(type)"));
        assertTrue(chatSource.contains(
                "MultiplayerVisualCatalog.reactionAsset(type)"));
        assertTrue(source.contains(
                "reaction.getReactionKind() == MatchReactionKind.EMOJI"));
        assertFalse(chatSource.contains("TextField"));
        assertFalse(chatSource.contains("UserManager"));
        assertFalse(chatSource.contains("NetworkClient"));
    }

    private static Invitation invitation(String id, InvitationStatus status) {
        return new Invitation(id, "alice", "bob", 1L, 10_000L, status);
    }

    private static MatchAssignment assignment(MatchRole role) {
        return new MatchAssignment("m1", "alice", "bob", role,
                1L, MatchStatus.PRE_GAME);
    }

    private static MatchStateSnapshot snapshot(long tick, long revision,
            MatchStatus status, MatchRole winner, MatchFinishReason reason) {
        return new MatchStateSnapshot("m1", status, tick, revision, 1_000L,
                tick / 20.0, 120.0 - tick / 20.0, "FIRST_BITE", 7L,
                5, 9, 3,
                List.of(new MatchPlayerSnapshot("alice", MatchRole.PLANTS, true),
                        new MatchPlayerSnapshot("bob", MatchRole.ZOMBIES, true)),
                500, 300, List.of(), List.of(), List.of(),
                List.of(true, true, true, true, true), winner, reason);
    }

    private static MatchReactionEvent reaction(long sequence, String sender,
            MatchReactionType type) {
        return new MatchReactionEvent("m1", sender, type, type.getKind(),
                sequence, 1_000L + sequence);
    }

    private static final class FakeMatchmaking implements MatchmakingTransport {
        final List<MatchmakingListener> listeners = new ArrayList<>();
        CompletableFuture<Invitation> inviteFuture = new CompletableFuture<>();
        CompletableFuture<Void> responseFuture = new CompletableFuture<>();
        CompletableFuture<Void> cancelFuture = new CompletableFuture<>();
        CompletableFuture<QueueStatus> joinFuture = new CompletableFuture<>();
        CompletableFuture<Void> leaveQueueFuture = new CompletableFuture<>();
        int inviteCalls;
        int responseCalls;
        int joinCalls;
        int leaveQueueCalls;

        @Override public CompletableFuture<Invitation> invitePlayer(String username) {
            inviteCalls++;
            return inviteFuture;
        }
        @Override public CompletableFuture<Void> respondToInvitation(String id, boolean accept) {
            responseCalls++;
            return responseFuture;
        }
        @Override public CompletableFuture<Void> cancelInvitation(String id) {
            return cancelFuture;
        }
        @Override public CompletableFuture<QueueStatus> joinRandomQueue() {
            joinCalls++;
            return joinFuture;
        }
        @Override public CompletableFuture<Void> leaveRandomQueue() {
            leaveQueueCalls++;
            return leaveQueueFuture;
        }
        @Override public void addListener(MatchmakingListener listener) { listeners.add(listener); }
        @Override public void removeListener(MatchmakingListener listener) { listeners.remove(listener); }
        void fireInvitationReceived(Invitation invitation) {
            List.copyOf(listeners).forEach(l -> l.invitationReceived(invitation));
        }
        void fireInvitationResult(Invitation invitation) {
            List.copyOf(listeners).forEach(l -> l.invitationResult(invitation));
        }
        void fireMatchFound(MatchAssignment assignment) {
            List.copyOf(listeners).forEach(l -> l.matchFound(assignment));
        }
    }

    private static final class FakeMultiplayer implements MultiplayerTransport {
        final List<MultiplayerGameListener> listeners = new ArrayList<>();
        CompletableFuture<ReadyStatus> readyFuture = new CompletableFuture<>();
        CompletableFuture<MatchStateSnapshot> stateFuture = new CompletableFuture<>();
        CompletableFuture<ActionResult> placePlantFuture = new CompletableFuture<>();
        CompletableFuture<ActionResult> placeZombieFuture = new CompletableFuture<>();
        CompletableFuture<ActionResult> removeFuture = new CompletableFuture<>();
        CompletableFuture<Void> leaveFuture = new CompletableFuture<>();
        CompletableFuture<MatchReactionReceipt> reactionFuture = new CompletableFuture<>();
        int readyCalls;
        int getStateCalls;
        int placePlantCalls;
        int placeZombieCalls;
        int removePlantCalls;
        int leaveCalls;
        int reactionCalls;
        long lastExpectedRevision;
        String lastPlantType;
        String lastZombieType;
        int lastPlantRow;
        int lastPlantColumn;
        int lastZombieRow;
        int lastZombieColumn;
        String lastRemovedEntityId;

        @Override public CompletableFuture<ReadyStatus> markReady(String matchId) {
            readyCalls++;
            return readyFuture;
        }
        @Override public CompletableFuture<MatchStateSnapshot> getState(String matchId) {
            getStateCalls++;
            return stateFuture;
        }
        @Override public CompletableFuture<ActionResult> placePlant(String matchId,
                String plantType, int row, int column, long expectedRevision) {
            placePlantCalls++;
            lastPlantType = plantType;
            lastPlantRow = row;
            lastPlantColumn = column;
            lastExpectedRevision = expectedRevision;
            return placePlantFuture;
        }
        @Override public CompletableFuture<ActionResult> placeZombie(String matchId,
                String zombieType, int row, int column, long expectedRevision) {
            placeZombieCalls++;
            lastZombieType = zombieType;
            lastZombieRow = row;
            lastZombieColumn = column;
            lastExpectedRevision = expectedRevision;
            return placeZombieFuture;
        }
        @Override public CompletableFuture<ActionResult> removePlant(String matchId,
                String entityId, long expectedRevision) {
            removePlantCalls++;
            lastRemovedEntityId = entityId;
            lastExpectedRevision = expectedRevision;
            return removeFuture;
        }
        @Override public CompletableFuture<Void> leaveMatch(String matchId) {
            leaveCalls++;
            return leaveFuture;
        }
        @Override public CompletableFuture<MatchReactionReceipt> sendReaction(
                String matchId, MatchReactionType reactionType) {
            reactionCalls++;
            return reactionFuture;
        }
        @Override public void addListener(MultiplayerGameListener listener) { listeners.add(listener); }
        @Override public void removeListener(MultiplayerGameListener listener) { listeners.remove(listener); }
        void fireOpponentReady(ReadyStatus status) {
            List.copyOf(listeners).forEach(l -> l.opponentReady(status));
        }
        void fireStarted(MatchStateSnapshot snapshot) {
            List.copyOf(listeners).forEach(l -> l.matchStarted(snapshot));
        }
        void fireState(MatchStateSnapshot snapshot) {
            List.copyOf(listeners).forEach(l -> l.matchStateUpdated(snapshot));
        }
        void fireFinished(MatchStateSnapshot snapshot) {
            List.copyOf(listeners).forEach(l -> l.matchFinished(snapshot));
        }
        void fireCancelled(MatchCancelled cancellation) {
            List.copyOf(listeners).forEach(l -> l.matchCancelled(cancellation));
        }
        void fireReaction(MatchReactionEvent reaction) {
            List.copyOf(listeners).forEach(l -> l.reactionReceived(reaction));
        }
        void fireConnectionLost(Throwable cause) {
            List.copyOf(listeners).forEach(l -> l.connectionLost(cause));
        }
    }
}
