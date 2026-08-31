package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import java.util.Objects;

/** Client intent: identity, ordering and time are deliberately absent. */
public final class MatchReaction {
    private final String matchId;
    private final MatchReactionType reactionType;

    public MatchReaction(String matchId, MatchReactionType reactionType) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.reactionType = Objects.requireNonNull(reactionType, "reactionType");
    }

    public String getMatchId() { return matchId; }
    public MatchReactionType getReactionType() { return reactionType; }
}
