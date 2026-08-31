package io.github.Plants_Vs_Zombies_2.network.gameplay;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

public final class GameplaySyncException extends RuntimeException {
    private final ProtocolErrorCode errorCode;

    public GameplaySyncException(ProtocolErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProtocolErrorCode getErrorCode() { return errorCode; }
}
