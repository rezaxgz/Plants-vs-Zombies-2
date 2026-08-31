package io.github.Plants_Vs_Zombies_2.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.Set;
import io.github.Plants_Vs_Zombies_2.network.auth.LoginCredentials;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardQuery;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardSortColumn;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardSortDirection;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionType;

final class PayloadReader {
    private static final Gson GSON = new Gson();
    private final JsonObject payload;

    private PayloadReader(ProtocolMessage message) throws AccountServiceException {
        JsonElement value = message.getPayload();
        if (!value.isJsonObject()) {
            throw malformed("Payload must be a JSON object");
        }
        payload = value.getAsJsonObject();
    }

    static PayloadReader from(ProtocolMessage message) throws AccountServiceException {
        return new PayloadReader(message);
    }

    RegistrationDetails registration() throws AccountServiceException {
        return new RegistrationDetails(
                requiredString("username"),
                requiredString("password"),
                requiredString("passwordConfirmation"),
                requiredString("nickname"),
                requiredString("email"),
                requiredString("gender"),
                requiredInteger("securityQuestionNumber"),
                requiredString("securityAnswer"),
                requiredString("securityAnswerConfirmation"));
    }

    LoginCredentials login() throws AccountServiceException {
        return new LoginCredentials(requiredString("username"), requiredString("password"));
    }

    LeaderboardQuery leaderboardQuery() throws AccountServiceException {
        final LeaderboardSortColumn column;
        final LeaderboardSortDirection direction;
        try {
            column = LeaderboardSortColumn.valueOf(requiredString("sortColumn"));
            direction = LeaderboardSortDirection.valueOf(
                    requiredString("sortDirection"));
        } catch (IllegalArgumentException exception) {
            throw new AccountServiceException(ProtocolErrorCode.VALIDATION_FAILED,
                    "Unknown leaderboard sort column or direction");
        }
        return new LeaderboardQuery(column, direction,
                requiredInteger("offset"), requiredInteger("limit"));
    }

    MatchReactionType reactionType() throws AccountServiceException {
        try {
            return MatchReactionType.valueOf(
                    requiredBoundedString("reactionType", 64));
        } catch (IllegalArgumentException exception) {
            throw new AccountServiceException(ProtocolErrorCode.VALIDATION_FAILED,
                    "Unknown predefined match reaction identifier");
        }
    }

    String requiredBoundedString(String field, int maximumLength)
            throws AccountServiceException {
        String value = requiredString(field);
        if (value.isBlank() || value.length() > maximumLength) {
            throw malformed(field + " must contain between 1 and "
                    + maximumLength + " characters");
        }
        return value;
    }

    void requireOnlyFields(String... fields) throws AccountServiceException {
        Set<String> allowed = Set.of(fields);
        for (String field : payload.keySet()) {
            if (!allowed.contains(field)) {
                throw malformed("Unexpected payload field: " + field);
            }
        }
    }

    String requiredString(String field) throws AccountServiceException {
        JsonElement value = payload.get(field);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            throw malformed(field + " must be a string");
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (!primitive.isString()) {
            throw malformed(field + " must be a string");
        }
        return primitive.getAsString();
    }

    int requiredInteger(String field) throws AccountServiceException {
        JsonElement value = payload.get(field);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw malformed(field + " must be an integer");
        }
        try {
            double numeric = value.getAsDouble();
            int integer = value.getAsInt();
            if (!Double.isFinite(numeric) || numeric != integer) {
                throw new NumberFormatException();
            }
            return integer;
        } catch (NumberFormatException exception) {
            throw malformed(field + " must be an integer");
        }
    }

    boolean requiredBoolean(String field) throws AccountServiceException {
        JsonElement value = payload.get(field);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) {
            throw malformed(field + " must be a boolean");
        }
        return value.getAsBoolean();
    }

    long requiredLong(String field) throws AccountServiceException {
        JsonElement value = payload.get(field);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw malformed(field + " must be an integer");
        }
        try {
            return value.getAsBigDecimal().longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw malformed(field + " must be an integer");
        }
    }

    <T> T requiredObject(String field, Class<T> type)
            throws AccountServiceException {
        JsonElement value = payload.get(field);
        if (value == null || !value.isJsonObject()) {
            throw malformed(field + " must be an object");
        }
        try {
            T result = GSON.fromJson(value, type);
            if (result == null) throw new JsonParseException("null object");
            return result;
        } catch (JsonParseException | IllegalStateException exception) {
            throw malformed(field + " is malformed");
        }
    }

    private static AccountServiceException malformed(String message) {
        return new AccountServiceException(ProtocolErrorCode.MALFORMED_PAYLOAD, message);
    }
}
