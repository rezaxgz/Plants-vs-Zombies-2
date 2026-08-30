package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.model.auth.UserRepository;
import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.security.Question;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.model.user.UserDataValidator;
import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.LoginCredentials;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

import java.util.List;
import java.util.Locale;

final class ServerAccountService {
    private final UserRepository repository;

    ServerAccountService(UserRepository repository) {
        this.repository = repository;
    }

    void register(RegistrationDetails details) throws AccountServiceException {
        validateRegistration(details);
        String nickname = details.getNickname().trim();
        Gender gender = Gender.valueOf(details.getGender().trim().toUpperCase(Locale.ROOT));
        User user = new User(
                details.getUsername(),
                details.getPassword(),
                nickname,
                details.getEmail(),
                gender);
        user.setSecurityQuestion(
                details.getSecurityQuestionNumber(), details.getSecurityAnswer());
        if (!repository.addIfUsernameAvailable(user)) {
            throw new AccountServiceException(
                    ProtocolErrorCode.USERNAME_EXISTS,
                    "An account with that username already exists");
        }
    }

    AccountProfile login(ConnectionContext context, LoginCredentials credentials)
            throws AccountServiceException {
        if (context.getAuthenticatedUsername() != null) {
            throw new AccountServiceException(
                    ProtocolErrorCode.ALREADY_AUTHENTICATED,
                    "This connection is already authenticated");
        }
        User user = repository.findByUsername(credentials.getUsername()).orElse(null);
        if (user == null || !user.doesMatchPassword(credentials.getPassword())) {
            throw new AccountServiceException(
                    ProtocolErrorCode.INVALID_CREDENTIALS,
                    "The username or password is incorrect");
        }

        if (!context.authenticate(user.getUsername())) {
            throw new AccountServiceException(
                    ProtocolErrorCode.ALREADY_AUTHENTICATED,
                    "This connection is already authenticated");
        }
        return AccountProfile.fromUser(user);
    }

    String logout(ConnectionContext context) throws AccountServiceException {
        String username = context.clearAuthentication();
        if (username == null) {
            throw new AccountServiceException(
                    ProtocolErrorCode.AUTH_REQUIRED,
                    "Authentication is required");
        }
        return username;
    }

    AccountProfile getProfile(ConnectionContext context) throws AccountServiceException {
        String username = context.getAuthenticatedUsername();
        if (username == null) {
            throw new AccountServiceException(
                    ProtocolErrorCode.AUTH_REQUIRED,
                    "Authentication is required");
        }
        User user = repository.findByUsername(username).orElseThrow(() ->
                new IllegalStateException("Authenticated account is missing from repository"));
        return AccountProfile.fromUser(user);
    }

    String connectionClosed(ConnectionContext context) {
        return context.clearAuthentication();
    }

    boolean usernameExists(String username) {
        return repository.findByUsername(username).isPresent();
    }

    private static void validateRegistration(RegistrationDetails details)
            throws AccountServiceException {
        if (!UserDataValidator.isValidUsername(details.getUsername())) {
            validationFailure("Username can only contain English letters, numbers and hyphen");
        }
        List<String> passwordErrors = UserDataValidator.validatePassword(details.getPassword());
        if (!passwordErrors.isEmpty()) {
            validationFailure(passwordErrors.get(0));
        }
        if (!details.getPassword().equals(details.getPasswordConfirmation())) {
            validationFailure("Password and confirmation do not match");
        }
        String nickname = details.getNickname() == null ? "" : details.getNickname().trim();
        if (!UserDataValidator.isValidNickname(nickname)) {
            validationFailure("Nickname length must be between 3 and 30 characters");
        }
        String emailError = UserDataValidator.validateEmail(details.getEmail());
        if (emailError != null) {
            validationFailure(emailError);
        }
        if (!isValidGender(details.getGender())) {
            validationFailure("Gender must be either male or female");
        }
        if (Question.getByNumber(details.getSecurityQuestionNumber()) == null) {
            validationFailure("Invalid security question number");
        }
        if (details.getSecurityAnswer() == null || details.getSecurityAnswer().isBlank()) {
            validationFailure("Security answer cannot be empty");
        }
        if (!details.getSecurityAnswer().equals(details.getSecurityAnswerConfirmation())) {
            validationFailure("Security answer and confirmation do not match");
        }
    }

    private static boolean isValidGender(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.equals(Gender.MALE.name()) || normalized.equals(Gender.FEMALE.name());
    }

    private static void validationFailure(String message) throws AccountServiceException {
        throw new AccountServiceException(ProtocolErrorCode.VALIDATION_FAILED, message);
    }
}
