package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchCancelled;

/** Callbacks run on the NetworkClient reader thread; UI callers must dispatch. */
public interface MultiplayerGameListener {
    default void opponentReady(ReadyStatus status) { }
    default void matchStarted(MatchStateSnapshot snapshot) { }
    default void matchStateUpdated(MatchStateSnapshot snapshot) { }
    default void matchFinished(MatchStateSnapshot snapshot) { }
    default void matchCancelled(MatchCancelled cancellation) { }
    default void reactionReceived(MatchReactionEvent reaction) { }
    default void connectionLost(Throwable cause) { }
}
