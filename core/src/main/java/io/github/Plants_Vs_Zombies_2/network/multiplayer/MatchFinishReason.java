package io.github.Plants_Vs_Zombies_2.network.multiplayer;

/** Stable terminal and cancellation reasons for authoritative multiplayer. */
public enum MatchFinishReason {
    ALL_BRAINS_EATEN,
    TIME_EXPIRED,
    PLAYER_LEFT,
    PLAYER_DISCONNECTED,
    SERVER_SHUTDOWN
}
