package io.github.Plants_Vs_Zombies_2.network.auth;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

public final class AuthenticationException extends RuntimeException {
    private final ProtocolErrorCode errorCode;

    public AuthenticationException(ProtocolErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthenticationException(
            ProtocolErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ProtocolErrorCode getErrorCode() {
        return errorCode;
    }
}
