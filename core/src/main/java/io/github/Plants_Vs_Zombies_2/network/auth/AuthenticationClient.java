package io.github.Plants_Vs_Zombies_2.network.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolException;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class AuthenticationClient {
    private final NetworkClient networkClient;
    private final ProtocolCodec codec = new ProtocolCodec();

    public AuthenticationClient(NetworkClient networkClient) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
    }

    public CompletableFuture<Void> register(RegistrationDetails details) {
        Objects.requireNonNull(details, "details");
        ProtocolMessage request = ProtocolMessages.withPayload(
                MessageType.REGISTER_REQUEST,
                ProtocolMessages.newRequestId(),
                details);
        return exchange(request, MessageType.REGISTER_RESPONSE).thenApply(response -> null);
    }

    public CompletableFuture<Void> register(
            String username,
            String password,
            String passwordConfirmation,
            String nickname,
            String email,
            String gender,
            int securityQuestionNumber,
            String securityAnswer,
            String securityAnswerConfirmation) {
        return register(new RegistrationDetails(
                username,
                password,
                passwordConfirmation,
                nickname,
                email,
                gender,
                securityQuestionNumber,
                securityAnswer,
                securityAnswerConfirmation));
    }

    public CompletableFuture<AccountProfile> login(String username, String password) {
        ProtocolMessage request = ProtocolMessages.withPayload(
                MessageType.LOGIN_REQUEST,
                ProtocolMessages.newRequestId(),
                new LoginCredentials(username, password));
        return exchange(request, MessageType.LOGIN_RESPONSE).thenApply(this::readProfile);
    }

    public CompletableFuture<PersistentLoginToken> createPersistentLogin() {
        ProtocolMessage request = ProtocolMessages.empty(
                MessageType.CREATE_PERSISTENT_LOGIN_REQUEST,
                ProtocolMessages.newRequestId());
        return exchange(request, MessageType.CREATE_PERSISTENT_LOGIN_RESPONSE)
                .thenApply(response -> readPayload(response,
                        PersistentLoginToken.class, "persistent login token"));
    }

    public CompletableFuture<AccountProfile> login(PersistentLoginCredentials credentials) {
        Objects.requireNonNull(credentials, "credentials");
        ProtocolMessage request = ProtocolMessages.withPayload(
                MessageType.PERSISTENT_LOGIN_REQUEST,
                ProtocolMessages.newRequestId(), credentials);
        return exchange(request, MessageType.PERSISTENT_LOGIN_RESPONSE)
                .thenApply(this::readProfile);
    }

    public CompletableFuture<PasswordResetChallenge> lookupPasswordReset(
            String username, String email) {
        ProtocolMessage request = ProtocolMessages.withPayload(
                MessageType.PASSWORD_RESET_LOOKUP_REQUEST,
                ProtocolMessages.newRequestId(),
                new PasswordResetLookup(username, email));
        return exchange(request, MessageType.PASSWORD_RESET_LOOKUP_RESPONSE)
                .thenApply(response -> readPayload(response,
                        PasswordResetChallenge.class, "password reset challenge"));
    }

    public CompletableFuture<Void> resetPassword(PasswordResetRequest details) {
        Objects.requireNonNull(details, "details");
        ProtocolMessage request = ProtocolMessages.withPayload(
                MessageType.PASSWORD_RESET_REQUEST,
                ProtocolMessages.newRequestId(), details);
        return exchange(request, MessageType.PASSWORD_RESET_RESPONSE)
                .thenApply(response -> null);
    }

    public CompletableFuture<Void> logout() {
        ProtocolMessage request = ProtocolMessages.empty(
                MessageType.LOGOUT_REQUEST, ProtocolMessages.newRequestId());
        return exchange(request, MessageType.LOGOUT_RESPONSE).thenApply(response -> null);
    }

    public CompletableFuture<AccountProfile> getProfile() {
        ProtocolMessage request = ProtocolMessages.empty(
                MessageType.GET_PROFILE_REQUEST, ProtocolMessages.newRequestId());
        return exchange(request, MessageType.GET_PROFILE_RESPONSE).thenApply(this::readProfile);
    }

    private CompletableFuture<ProtocolMessage> exchange(
            ProtocolMessage request, MessageType expectedResponse) {
        return networkClient.sendRequest(request).thenApply(response -> {
            if (response.getType() == MessageType.ERROR) {
                throw readServerError(response);
            }
            if (response.getType() != expectedResponse) {
                throw new AuthenticationException(
                        ProtocolErrorCode.UNEXPECTED_RESPONSE,
                        "Expected " + expectedResponse + " but received " + response.getType());
            }
            return response;
        });
    }

    private AccountProfile readProfile(ProtocolMessage response) {
        AccountProfile profile = readPayload(response, AccountProfile.class,
                "account profile");
        if (profile.getUsername() == null) {
            throw new AuthenticationException(
                    ProtocolErrorCode.UNEXPECTED_RESPONSE,
                    "The server returned an incomplete account profile");
        }
        return profile;
    }

    private <T> T readPayload(ProtocolMessage response, Class<T> type,
            String description) {
        try {
            T value = codec.deserializePayload(response, type);
            if (value == null) {
                throw new AuthenticationException(
                        ProtocolErrorCode.UNEXPECTED_RESPONSE,
                        "The server returned an incomplete " + description);
            }
            return value;
        } catch (ProtocolException exception) {
            throw new AuthenticationException(
                    ProtocolErrorCode.UNEXPECTED_RESPONSE,
                    "The server returned an invalid " + description,
                    exception);
        }
    }

    private AuthenticationException readServerError(ProtocolMessage response) {
        JsonElement payload = response.getPayload();
        if (!payload.isJsonObject()) {
            return malformedServerError();
        }
        JsonObject object = payload.getAsJsonObject();
        String codeName = readString(object, "code");
        String message = readString(object, "message");
        if (codeName == null || message == null) {
            return malformedServerError();
        }
        try {
            return new AuthenticationException(ProtocolErrorCode.valueOf(codeName), message);
        } catch (IllegalArgumentException exception) {
            return malformedServerError();
        }
    }

    private static String readString(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString()
                ? value.getAsString()
                : null;
    }

    private static AuthenticationException malformedServerError() {
        return new AuthenticationException(
                ProtocolErrorCode.UNEXPECTED_RESPONSE,
                "The server returned a malformed error response");
    }
}
