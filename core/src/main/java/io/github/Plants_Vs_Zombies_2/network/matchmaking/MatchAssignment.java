package io.github.Plants_Vs_Zombies_2.network.matchmaking;

public final class MatchAssignment {
    private final String matchId;
    private final String localUsername;
    private final String opponentUsername;
    private final MatchRole role;
    private final long creationTimeEpochMillis;
    private final MatchStatus status;

    public MatchAssignment(String matchId, String localUsername,
            String opponentUsername, MatchRole role,
            long creationTimeEpochMillis, MatchStatus status) {
        this.matchId = matchId;
        this.localUsername = localUsername;
        this.opponentUsername = opponentUsername;
        this.role = role;
        this.creationTimeEpochMillis = creationTimeEpochMillis;
        this.status = status;
    }

    public String getMatchId() { return matchId; }
    public String getLocalUsername() { return localUsername; }
    public String getOpponentUsername() { return opponentUsername; }
    public MatchRole getRole() { return role; }
    public long getCreationTimeEpochMillis() { return creationTimeEpochMillis; }
    public MatchStatus getStatus() { return status; }
}
