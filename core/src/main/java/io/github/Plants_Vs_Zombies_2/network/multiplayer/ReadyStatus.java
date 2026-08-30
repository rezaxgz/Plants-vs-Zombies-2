package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;

public final class ReadyStatus {
    private final String matchId;
    private final MatchStatus status;
    private final boolean plantsReady;
    private final boolean zombiesReady;
    private final long revision;

    public ReadyStatus(String matchId, MatchStatus status,
            boolean plantsReady, boolean zombiesReady, long revision) {
        this.matchId = matchId;
        this.status = status;
        this.plantsReady = plantsReady;
        this.zombiesReady = zombiesReady;
        this.revision = revision;
    }

    public String getMatchId() { return matchId; }
    public MatchStatus getStatus() { return status; }
    public boolean isPlantsReady() { return plantsReady; }
    public boolean isZombiesReady() { return zombiesReady; }
    public long getRevision() { return revision; }
}
