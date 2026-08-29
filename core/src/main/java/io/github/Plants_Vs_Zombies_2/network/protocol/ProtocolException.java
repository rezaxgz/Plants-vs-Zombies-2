package io.github.Plants_Vs_Zombies_2.network.protocol;

public final class ProtocolException extends Exception {
    private final String errorCode;
    private final String requestId;

    public ProtocolException(String errorCode, String requestId, String message) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = requestId;
    }

    public ProtocolException(String errorCode, String requestId, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.requestId = requestId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getRequestId() {
        return requestId;
    }
}
