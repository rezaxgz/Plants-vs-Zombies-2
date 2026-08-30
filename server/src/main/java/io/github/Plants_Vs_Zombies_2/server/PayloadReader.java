package io.github.Plants_Vs_Zombies_2.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.Plants_Vs_Zombies_2.network.auth.LoginCredentials;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;

final class PayloadReader {
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

    private static AccountServiceException malformed(String message) {
        return new AccountServiceException(ProtocolErrorCode.MALFORMED_PAYLOAD, message);
    }
}
