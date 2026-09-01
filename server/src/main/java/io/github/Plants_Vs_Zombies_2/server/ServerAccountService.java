package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.model.auth.GameplayUpdateException;
import io.github.Plants_Vs_Zombies_2.model.auth.GameplayUpdateFailure;
import io.github.Plants_Vs_Zombies_2.model.auth.UserRepository;
import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.security.Question;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.model.user.UserDataValidator;
import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.LoginCredentials;
import io.github.Plants_Vs_Zombies_2.network.auth.PasswordResetChallenge;
import io.github.Plants_Vs_Zombies_2.network.auth.PasswordResetLookup;
import io.github.Plants_Vs_Zombies_2.network.auth.PasswordResetRequest;
import io.github.Plants_Vs_Zombies_2.network.auth.PersistentLoginCredentials;
import io.github.Plants_Vs_Zombies_2.network.auth.PersistentLoginToken;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardPage;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardQuery;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

final class ServerAccountService {
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();
    private final UserRepository repository;
    private final LeaderboardService leaderboardService;

    ServerAccountService(UserRepository repository) {
        this.repository = repository;
        this.leaderboardService = new LeaderboardService(repository);
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

    synchronized AccountProfile login(ConnectionContext context,
            LoginCredentials credentials)
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

        // A successful password login supersedes every older remembered
        // session. If requested, the client creates one fresh token next.
        if (user.getPersistentLoginTokenHashForStorage() != null) {
            String previous = user.getPersistentLoginTokenHashForStorage();
            try {
                user.clearPersistentLoginToken();
                repository.save(user);
            } catch (RuntimeException exception) {
                user.setPersistentLoginTokenHashForStorage(previous);
                throw exception;
            }
        }

        if (!context.authenticate(user.getUsername())) {
            throw new AccountServiceException(
                    ProtocolErrorCode.ALREADY_AUTHENTICATED,
                    "This connection is already authenticated");
        }
        return AccountProfile.fromUser(user);
    }

    AccountProfile login(ConnectionContext context,
            PersistentLoginCredentials credentials) throws AccountServiceException {
        if (context.getAuthenticatedUsername() != null) {
            throw new AccountServiceException(
                    ProtocolErrorCode.ALREADY_AUTHENTICATED,
                    "This connection is already authenticated");
        }
        User user = repository.findByUsername(credentials.getUsername()).orElse(null);
        if (user == null || !user.matchesPersistentLoginToken(
                credentials.getToken())) {
            throw new AccountServiceException(
                    ProtocolErrorCode.INVALID_CREDENTIALS,
                    "The saved login is no longer valid");
        }
        if (!context.authenticate(user.getUsername())) {
            throw new AccountServiceException(
                    ProtocolErrorCode.ALREADY_AUTHENTICATED,
                    "This connection is already authenticated");
        }
        return AccountProfile.fromUser(user);
    }

    synchronized PersistentLoginToken createPersistentLogin(
            ConnectionContext context) throws AccountServiceException {
        String username = requireAuthentication(context);
        User user = repository.findByUsername(username).orElseThrow(() ->
                new AccountServiceException(ProtocolErrorCode.USER_NOT_FOUND,
                        "The authenticated account no longer exists"));
        byte[] random = new byte[32];
        TOKEN_RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(random);
        String previous = user.getPersistentLoginTokenHashForStorage();
        try {
            user.setPersistentLoginToken(token);
            repository.save(user);
        } catch (RuntimeException exception) {
            user.setPersistentLoginTokenHashForStorage(previous);
            throw exception;
        }
        return new PersistentLoginToken(username, token);
    }

    PasswordResetChallenge lookupPasswordReset(PasswordResetLookup lookup)
            throws AccountServiceException {
        User user = repository.findByUsername(lookup.getUsername()).orElse(null);
        if (user == null || !user.doesMatchEmail(lookup.getEmail())) {
            throw new AccountServiceException(ProtocolErrorCode.INVALID_CREDENTIALS,
                    "The username or email is incorrect");
        }
        String question = user.getSecurityQuestion();
        if (question == null) {
            throw new AccountServiceException(ProtocolErrorCode.VALIDATION_FAILED,
                    "This account does not have a security question");
        }
        return new PasswordResetChallenge(user.getUsername(), question);
    }

    synchronized void resetPassword(PasswordResetRequest details)
            throws AccountServiceException {
        User user = repository.findByUsername(details.getUsername()).orElse(null);
        if (user == null || !user.doesMatchEmail(details.getEmail())
                || !user.isCorrectSecurityAnswer(details.getAnswer())) {
            throw new AccountServiceException(ProtocolErrorCode.INVALID_CREDENTIALS,
                    "The recovery answer is incorrect");
        }
        List<String> passwordErrors = UserDataValidator.validatePassword(
                details.getPassword());
        if (!passwordErrors.isEmpty()) validationFailure(passwordErrors.get(0));
        if (!details.getPassword().equals(details.getPasswordConfirmation())) {
            validationFailure("Password and confirmation do not match");
        }
        if (user.doesMatchPassword(details.getPassword())) {
            validationFailure(
                    "New password must be different from the current password");
        }
        String previousPasswordHash = user.getPasswordHashForStorage();
        String previousTokenHash = user.getPersistentLoginTokenHashForStorage();
        try {
            user.changePassword(details.getPassword());
            user.clearPersistentLoginToken();
            repository.save(user);
        } catch (RuntimeException exception) {
            user.setPasswordHashForStorage(previousPasswordHash);
            user.setPersistentLoginTokenHashForStorage(previousTokenHash);
            throw exception;
        }
    }

    synchronized String logout(ConnectionContext context) throws AccountServiceException {
        String username = context.getAuthenticatedUsername();
        if (username == null) {
            throw new AccountServiceException(
                    ProtocolErrorCode.AUTH_REQUIRED,
                    "Authentication is required");
        }
        User user = repository.findByUsername(username).orElse(null);
        if (user != null && user.getPersistentLoginTokenHashForStorage() != null) {
            String previous = user.getPersistentLoginTokenHashForStorage();
            try {
                user.clearPersistentLoginToken();
                repository.save(user);
            } catch (RuntimeException exception) {
                user.setPersistentLoginTokenHashForStorage(previous);
                throw exception;
            }
        }
        context.clearAuthentication();
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

    GameplayStateSnapshot getGameplayState(ConnectionContext context)
            throws AccountServiceException {
        String username = requireAuthentication(context);
        return repository.findGameplayState(username).orElseThrow(() ->
                new AccountServiceException(ProtocolErrorCode.USER_NOT_FOUND,
                        "The authenticated account no longer exists"));
    }

    GameplayStateSnapshot synchronizeGameplayState(ConnectionContext context,
            long expectedRevision, GameplayState state)
            throws AccountServiceException {
        String username = requireAuthentication(context);
        if (expectedRevision < 0) {
            throw new AccountServiceException(ProtocolErrorCode.VALIDATION_FAILED,
                    "expectedRevision cannot be negative");
        }
        try {
            return repository.updateGameplayState(username, expectedRevision, state);
        } catch (GameplayUpdateException exception) {
            ProtocolErrorCode code = exception.getFailure()
                    == GameplayUpdateFailure.STALE_REVISION
                            ? ProtocolErrorCode.STALE_ACCOUNT_REVISION
                            : exception.getFailure() == GameplayUpdateFailure.USER_NOT_FOUND
                                    ? ProtocolErrorCode.USER_NOT_FOUND
                                    : ProtocolErrorCode.VALIDATION_FAILED;
            throw new AccountServiceException(code, exception.getMessage());
        }
    }

    LeaderboardPage getLeaderboard(String authenticatedUsername,
            LeaderboardQuery query) throws AccountServiceException {
        return leaderboardService.getPage(authenticatedUsername, query);
    }

    private static String requireAuthentication(ConnectionContext context)
            throws AccountServiceException {
        String username = context.getAuthenticatedUsername();
        if (username == null) {
            throw new AccountServiceException(ProtocolErrorCode.AUTH_REQUIRED,
                    "Authentication is required");
        }
        return username;
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
