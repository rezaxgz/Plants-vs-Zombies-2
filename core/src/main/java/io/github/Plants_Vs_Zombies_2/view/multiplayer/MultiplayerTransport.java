package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.concurrent.CompletableFuture;

import io.github.Plants_Vs_Zombies_2.network.multiplayer.ActionResult;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MultiplayerGameListener;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.ReadyStatus;

/** Testable boundary over the authoritative Stage 6 multiplayer API. */
public interface MultiplayerTransport {
    CompletableFuture<ReadyStatus> markReady(String matchId);
    CompletableFuture<MatchStateSnapshot> getState(String matchId);
    CompletableFuture<ActionResult> placePlant(String matchId, String plantType,
            int row, int column, long expectedRevision);
    CompletableFuture<ActionResult> placeZombie(String matchId, String zombieType,
            int row, int column, long expectedRevision);
    CompletableFuture<ActionResult> removePlant(String matchId, String entityId,
            long expectedRevision);
    CompletableFuture<Void> leaveMatch(String matchId);
    void addListener(MultiplayerGameListener listener);
    void removeListener(MultiplayerGameListener listener);
}
