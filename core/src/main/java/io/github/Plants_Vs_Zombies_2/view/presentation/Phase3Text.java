package io.github.Plants_Vs_Zombies_2.view.presentation;

import java.util.Locale;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchFinishReason;
import io.github.Plants_Vs_Zombies_2.network.session.ClientSessionState;

/** Player-facing formatting for nullable Phase 3 data and wire enums. */
public final class Phase3Text {
    public static final String WAITING_FOR_ASSIGNMENT =
            "Waiting for assignment...";
    public static final String WAITING_FOR_OPPONENT =
            "Waiting for opponent...";

    private Phase3Text() {
    }

    public static String required(String value, String unavailableText) {
        return hasText(value) ? value.trim() : unavailableText;
    }

    public static String optional(String value) {
        return hasText(value) ? value.trim() : "Not provided";
    }

    public static String username(String value) {
        return required(value, WAITING_FOR_OPPONENT);
    }

    public static String role(MatchRole role) {
        if (role == null) {
            return WAITING_FOR_ASSIGNMENT;
        }
        return role == MatchRole.PLANTS
                ? "Plants - defend the brains"
                : "Zombies - eat every brain";
    }

    public static String roleShort(MatchRole role) {
        if (role == null) {
            return WAITING_FOR_ASSIGNMENT;
        }
        return role == MatchRole.PLANTS ? "Plants" : "Zombies";
    }

    public static String connection(ClientSessionState state) {
        if (state == null) {
            return "Connection unavailable";
        }
        return switch (state) {
            case DISCONNECTED -> "Disconnected";
            case CONNECTING -> "Connecting...";
            case CONNECTED -> "Connected";
            case REGISTERING -> "Creating account...";
            case AUTHENTICATING -> "Signing in...";
            case AUTHENTICATED -> "Authenticated";
            case LOGGING_OUT -> "Signing out...";
            case CLOSED -> "Connection closed";
        };
    }

    public static String finishReason(MatchFinishReason reason) {
        if (reason == null) {
            return "The server did not provide a finish reason.";
        }
        return switch (reason) {
            case ALL_BRAINS_EATEN -> "All brains were eaten.";
            case TIME_EXPIRED -> "The plants survived until time expired.";
            case PLAYER_LEFT -> "A player left the match.";
            case PLAYER_DISCONNECTED -> "A player disconnected.";
            case SERVER_SHUTDOWN -> "The server shut down the match.";
        };
    }

    public static String cancellationReason(String reason) {
        if (!hasText(reason)) {
            return "The match ended before a winner was declared.";
        }
        String normalized = reason.trim();
        try {
            return finishReason(MatchFinishReason.valueOf(
                    normalized.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return normalized.replace('_', ' ');
        }
    }

    public static String levelProgress(int chapter, int level) {
        return chapter > 0 && level > 0
                ? chapter + "-" + level
                : "Not completed yet";
    }

    public static String rank(int rank) {
        return rank > 0 ? Integer.toString(rank) : "-";
    }

    public static String status(String value, String fallback) {
        return required(value, fallback);
    }

    public static String prettyIdentifier(String value, String fallback) {
        if (!hasText(value)) {
            return fallback;
        }
        String raw = value.trim().replace('-', ' ').replace('_', ' ')
                .toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(raw.length());
        for (String word : raw.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }
        return result.length() == 0 ? fallback : result.toString();
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
