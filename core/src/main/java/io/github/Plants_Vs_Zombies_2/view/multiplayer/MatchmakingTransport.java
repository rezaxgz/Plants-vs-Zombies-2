package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.concurrent.CompletableFuture;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.Invitation;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingListener;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.QueueStatus;

/** Small transport boundary so graphical matchmaking state can be unit-tested. */
public interface MatchmakingTransport {
    CompletableFuture<Invitation> invitePlayer(String username);
    CompletableFuture<Void> respondToInvitation(String invitationId, boolean accept);
    CompletableFuture<Void> cancelInvitation(String invitationId);
    CompletableFuture<QueueStatus> joinRandomQueue();
    CompletableFuture<Void> leaveRandomQueue();
    void addListener(MatchmakingListener listener);
    void removeListener(MatchmakingListener listener);
}
