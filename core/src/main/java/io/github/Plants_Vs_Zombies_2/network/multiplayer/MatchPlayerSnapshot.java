package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;

public final class MatchPlayerSnapshot {
    private final String username;
    private final MatchRole role;
    private final boolean ready;

    public MatchPlayerSnapshot(String username, MatchRole role, boolean ready) {
        this.username = username;
        this.role = role;
        this.ready = ready;
    }

    public String getUsername() { return username; }
    public MatchRole getRole() { return role; }
    public boolean isReady() { return ready; }
}
