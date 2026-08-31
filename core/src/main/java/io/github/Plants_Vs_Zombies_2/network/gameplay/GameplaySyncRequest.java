package io.github.Plants_Vs_Zombies_2.network.gameplay;

/** Contains intent only; account identity comes from the authenticated socket. */
public final class GameplaySyncRequest {
    private final long expectedRevision;
    private final GameplayState state;

    public GameplaySyncRequest(long expectedRevision, GameplayState state) {
        this.expectedRevision = expectedRevision;
        this.state = state;
    }

    public long getExpectedRevision() { return expectedRevision; }
    public GameplayState getState() { return state; }
}
