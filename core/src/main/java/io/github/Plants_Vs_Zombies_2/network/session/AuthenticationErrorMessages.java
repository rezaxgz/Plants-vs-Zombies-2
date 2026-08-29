package io.github.Plants_Vs_Zombies_2.network.session;

import java.net.ConnectException;
import java.net.SocketException;
import java.util.concurrent.TimeoutException;

import io.github.Plants_Vs_Zombies_2.network.auth.AuthenticationException;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

public final class AuthenticationErrorMessages {
    private AuthenticationErrorMessages() {
    }

    public static String forFailure(Throwable failure) {
        Throwable root = RemoteAccountSession.unwrap(failure);
        AuthenticationException authentication = find(root, AuthenticationException.class);
        if (authentication != null) {
            ProtocolErrorCode code = authentication.getErrorCode();
            return switch (code) {
                case INVALID_CREDENTIALS -> "Invalid username or password.";
                case USER_ALREADY_ONLINE -> "This user is already online.";
                case USERNAME_EXISTS -> "That username is already registered.";
                case VALIDATION_FAILED, MALFORMED_PAYLOAD ->
                        "The server rejected the submitted data: " + safeMessage(authentication);
                case AUTH_REQUIRED -> "Your connection is no longer authenticated. Please log in again.";
                case ALREADY_AUTHENTICATED -> "This connection is already authenticated.";
                case INTERNAL_SERVER_ERROR -> "The server could not complete the request. Please try again.";
                default -> "Unexpected server response. Please try again.";
            };
        }
        if (find(root, TimeoutException.class) != null) {
            return "The server request timed out. You can retry.";
        }
        if (find(root, ConnectException.class) != null
                || messageContains(root, "could not connect")) {
            return "Server unavailable. Check that it is running, then retry.";
        }
        if (find(root, SocketException.class) != null
                || messageContains(root, "disconnected")
                || messageContains(root, "not connected")) {
            return "The connection was lost. Please retry.";
        }
        return "Could not complete the request: " + safeMessage(root);
    }

    private static boolean messageContains(Throwable failure, String text) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getMessage() != null
                    && current.getMessage().toLowerCase().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static <T extends Throwable> T find(Throwable failure, Class<T> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
        }
        return null;
    }

    private static String safeMessage(Throwable failure) {
        return failure != null && failure.getMessage() != null
                && !failure.getMessage().isBlank()
                ? failure.getMessage()
                : "unexpected error";
    }
}
