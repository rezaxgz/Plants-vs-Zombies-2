package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.Invitation;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingClient;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingListener;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.QueueStatus;

/** Adapter over the Stage 5/6 typed client. It never creates another socket. */
public final class ClientMatchmakingTransport implements MatchmakingTransport {
    private final MatchmakingClient client;

    public ClientMatchmakingTransport(MatchmakingClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override public CompletableFuture<Invitation> invitePlayer(String username) {
        return client.invitePlayer(username);
    }
    @Override public CompletableFuture<Void> respondToInvitation(String invitationId, boolean accept) {
        return client.respondToInvitation(invitationId, accept);
    }
    @Override public CompletableFuture<Void> cancelInvitation(String invitationId) {
        return client.cancelInvitation(invitationId);
    }
    @Override public CompletableFuture<QueueStatus> joinRandomQueue() {
        return client.joinRandomQueue();
    }
    @Override public CompletableFuture<Void> leaveRandomQueue() {
        return client.leaveRandomQueue();
    }
    @Override public void addListener(MatchmakingListener listener) { client.addListener(listener); }
    @Override public void removeListener(MatchmakingListener listener) { client.removeListener(listener); }
}
