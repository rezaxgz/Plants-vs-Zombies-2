package io.github.Plants_Vs_Zombies_2.network.leaderboard;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

public final class LeaderboardException extends RuntimeException {
    private final ProtocolErrorCode errorCode;

    public LeaderboardException(ProtocolErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProtocolErrorCode getErrorCode() { return errorCode; }
}
