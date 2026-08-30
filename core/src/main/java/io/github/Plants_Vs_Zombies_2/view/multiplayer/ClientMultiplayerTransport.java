package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.github.Plants_Vs_Zombies_2.network.multiplayer.ActionResult;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameClient;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameListener;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ReadyStatus;

/** Adapter over the account session's already-connected MultiplayerGameClient. */
public final class ClientMultiplayerTransport implements MultiplayerTransport {
    private final MultiplayerGameClient client;

    public ClientMultiplayerTransport(MultiplayerGameClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override public CompletableFuture<ReadyStatus> markReady(String matchId) {
        return client.markReady(matchId);
    }
    @Override public CompletableFuture<MatchStateSnapshot> getState(String matchId) {
        return client.getState(matchId);
    }
    @Override public CompletableFuture<ActionResult> placePlant(String matchId,
            String plantType, int row, int column, long expectedRevision) {
        return client.placePlant(matchId, plantType, row, column, expectedRevision);
    }
    @Override public CompletableFuture<ActionResult> placeZombie(String matchId,
            String zombieType, int row, int column, long expectedRevision) {
        return client.placeZombie(matchId, zombieType, row, column, expectedRevision);
    }
    @Override public CompletableFuture<ActionResult> removePlant(String matchId,
            String entityId, long expectedRevision) {
        return client.removePlant(matchId, entityId, expectedRevision);
    }
    @Override public CompletableFuture<Void> leaveMatch(String matchId) {
        return client.leaveMatch(matchId);
    }
    @Override public void addListener(MultiplayerGameListener listener) { client.addListener(listener); }
    @Override public void removeListener(MultiplayerGameListener listener) { client.removeListener(listener); }
}
