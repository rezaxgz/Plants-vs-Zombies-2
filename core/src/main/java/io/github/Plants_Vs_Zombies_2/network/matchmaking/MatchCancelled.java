package io.github.Plants_Vs_Zombies_2.network.matchmaking;

public final class MatchCancelled {
    private final String matchId;
    private final String opponentUsername;
    private final String reason;

    public MatchCancelled(String matchId, String opponentUsername, String reason) {
        this.matchId = matchId;
        this.opponentUsername = opponentUsername;
        this.reason = reason;
    }

    public String getMatchId() { return matchId; }
    public String getOpponentUsername() { return opponentUsername; }
    public String getReason() { return reason; }
}
