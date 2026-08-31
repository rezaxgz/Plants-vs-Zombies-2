package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import java.util.Objects;

/** Correlated acknowledgement; rendering still comes only from the push event. */
public final class MatchReactionReceipt {
    private final String matchId;
    private final MatchReactionType reactionType;
    private final long sequence;
    private final long serverTimestampMillis;

    public MatchReactionReceipt(String matchId, MatchReactionType reactionType,
            long sequence, long serverTimestampMillis) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.reactionType = Objects.requireNonNull(reactionType, "reactionType");
        this.sequence = sequence;
        this.serverTimestampMillis = serverTimestampMillis;
    }

    public String getMatchId() { return matchId; }
    public MatchReactionType getReactionType() { return reactionType; }
    public long getSequence() { return sequence; }
    public long getServerTimestampMillis() { return serverTimestampMillis; }
}
