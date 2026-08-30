package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

public final class MultiplayerGameException extends RuntimeException {
    private final ProtocolErrorCode errorCode;

    public MultiplayerGameException(ProtocolErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProtocolErrorCode getErrorCode() { return errorCode; }
}
