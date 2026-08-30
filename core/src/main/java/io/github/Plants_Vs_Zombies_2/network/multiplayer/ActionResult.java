package io.github.Plants_Vs_Zombies_2.network.multiplayer;

public final class ActionResult {
    private final String matchId;
    private final long revision;
    private final String entityId;
    private final MatchStateSnapshot snapshot;

    public ActionResult(String matchId, long revision,
            String entityId, MatchStateSnapshot snapshot) {
        this.matchId = matchId;
        this.revision = revision;
        this.entityId = entityId;
        this.snapshot = snapshot;
    }

    public String getMatchId() { return matchId; }
    public long getRevision() { return revision; }
    public String getEntityId() { return entityId; }
    public MatchStateSnapshot getSnapshot() { return snapshot; }
}
