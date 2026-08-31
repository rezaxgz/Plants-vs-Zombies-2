package io.github.Plants_Vs_Zombies_2.network.gameplay;

/** Immutable server-acknowledged gameplay state and optimistic revision. */
public final class GameplayStateSnapshot {
    private final long revision;
    private final GameplayState state;

    public GameplayStateSnapshot(long revision, GameplayState state) {
        this.revision = revision;
        this.state = state;
    }

    public long getRevision() { return revision; }
    public GameplayState getState() { return state; }
}
