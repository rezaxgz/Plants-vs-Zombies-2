package io.github.Plants_Vs_Zombies_2.network.leaderboard;

import java.util.List;

/** Immutable page plus global metadata for the requested ordering. */
public final class LeaderboardPage {
    private final List<LeaderboardEntry> entries;
    private final int totalPlayers;
    private final Integer authenticatedUserRank;
    private final int offset;
    private final int limit;

    public LeaderboardPage(List<LeaderboardEntry> entries, int totalPlayers,
            Integer authenticatedUserRank, int offset, int limit) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        this.totalPlayers = totalPlayers;
        this.authenticatedUserRank = authenticatedUserRank;
        this.offset = offset;
        this.limit = limit;
    }

    public List<LeaderboardEntry> getEntries() { return List.copyOf(entries); }
    public int getTotalPlayers() { return totalPlayers; }
    public Integer getAuthenticatedUserRank() { return authenticatedUserRank; }
    public int getOffset() { return offset; }
    public int getLimit() { return limit; }
}
