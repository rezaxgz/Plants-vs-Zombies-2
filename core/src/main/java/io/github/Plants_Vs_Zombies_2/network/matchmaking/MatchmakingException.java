package io.github.Plants_Vs_Zombies_2.network.matchmaking;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

public final class MatchmakingException extends RuntimeException {
    private final ProtocolErrorCode errorCode;

    public MatchmakingException(ProtocolErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProtocolErrorCode getErrorCode() { return errorCode; }
}
