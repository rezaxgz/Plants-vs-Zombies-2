package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

final class AccountServiceException extends Exception {
    private final ProtocolErrorCode errorCode;

    AccountServiceException(ProtocolErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    ProtocolErrorCode getErrorCode() {
        return errorCode;
    }
}
