package io.github.Plants_Vs_Zombies_2.network.leaderboard;

import java.util.Objects;

/** Immutable credential-free leaderboard row. */
public final class LeaderboardEntry {
    private final int rank;
    private final String username;
    private final int lastCompletedChapter;
    private final int lastCompletedLevel;
    private final int completedMinigames;
    private final int completedDailyQuests;
    private final int completedNonDailyQuests;
    private final int totalCompletedQuests;
    private final int highestScore;

    public LeaderboardEntry(int rank, String username,
            int lastCompletedChapter, int lastCompletedLevel,
            int completedMinigames, int completedDailyQuests,
            int completedNonDailyQuests, int highestScore) {
        this.rank = rank;
        this.username = Objects.requireNonNull(username, "username");
        this.lastCompletedChapter = lastCompletedChapter;
        this.lastCompletedLevel = lastCompletedLevel;
        this.completedMinigames = completedMinigames;
        this.completedDailyQuests = completedDailyQuests;
        this.completedNonDailyQuests = completedNonDailyQuests;
        this.totalCompletedQuests = Math.addExact(
                completedDailyQuests, completedNonDailyQuests);
        this.highestScore = highestScore;
    }

    public LeaderboardEntry withRank(int assignedRank) {
        return new LeaderboardEntry(assignedRank, username,
                lastCompletedChapter, lastCompletedLevel, completedMinigames,
                completedDailyQuests, completedNonDailyQuests, highestScore);
    }

    public int getRank() { return rank; }
    public String getUsername() { return username; }
    public int getLastCompletedChapter() { return lastCompletedChapter; }
    public int getLastCompletedLevel() { return lastCompletedLevel; }
    public int getCompletedMinigames() { return completedMinigames; }
    public int getCompletedDailyQuests() { return completedDailyQuests; }
    public int getCompletedNonDailyQuests() { return completedNonDailyQuests; }
    public int getTotalCompletedQuests() { return totalCompletedQuests; }
    public int getHighestScore() { return highestScore; }
}
