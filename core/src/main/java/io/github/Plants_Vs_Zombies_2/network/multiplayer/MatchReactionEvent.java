package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import java.util.Objects;

/** Immutable server-confirmed match-scoped reaction push event. */
public final class MatchReactionEvent {
    private final String matchId;
    private final String senderUsername;
    private final MatchReactionType reactionType;
    private final MatchReactionKind reactionKind;
    private final long sequence;
    private final long serverTimestampMillis;

    public MatchReactionEvent(String matchId, String senderUsername,
            MatchReactionType reactionType, MatchReactionKind reactionKind,
            long sequence, long serverTimestampMillis) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.senderUsername = Objects.requireNonNull(
                senderUsername, "senderUsername");
        this.reactionType = Objects.requireNonNull(reactionType, "reactionType");
        this.reactionKind = Objects.requireNonNull(reactionKind, "reactionKind");
        this.sequence = sequence;
        this.serverTimestampMillis = serverTimestampMillis;
    }

    public String getMatchId() { return matchId; }
    public String getSenderUsername() { return senderUsername; }
    public MatchReactionType getReactionType() { return reactionType; }
    public MatchReactionKind getReactionKind() { return reactionKind; }
    public long getSequence() { return sequence; }
    public long getServerTimestampMillis() { return serverTimestampMillis; }
}
