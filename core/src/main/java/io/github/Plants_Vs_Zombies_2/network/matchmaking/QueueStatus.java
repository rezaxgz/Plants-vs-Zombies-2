package io.github.Plants_Vs_Zombies_2.network.matchmaking;

public final class QueueStatus {
    private final PlayerMatchmakingState state;
    private final long changedAtEpochMillis;
    private final int position;

    public QueueStatus(PlayerMatchmakingState state,
            long changedAtEpochMillis, int position) {
        this.state = state;
        this.changedAtEpochMillis = changedAtEpochMillis;
        this.position = position;
    }

    public PlayerMatchmakingState getState() { return state; }
    public long getChangedAtEpochMillis() { return changedAtEpochMillis; }
    public int getPosition() { return position; }
}
