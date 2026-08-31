package io.github.Plants_Vs_Zombies_2.network.leaderboard;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface LeaderboardTransport {
    CompletableFuture<LeaderboardPage> load(LeaderboardQuery query);
}
