package io.github.Plants_Vs_Zombies_2.network.matchmaking;

/** Callbacks run on the NetworkClient reader thread; UI callers must dispatch. */
public interface MatchmakingListener {
    default void invitationReceived(Invitation invitation) { }
    default void invitationResult(Invitation invitation) { }
    default void queueStatusChanged(QueueStatus status) { }
    default void matchFound(MatchAssignment assignment) { }
    default void matchCancelled(MatchCancelled cancellation) { }
}
