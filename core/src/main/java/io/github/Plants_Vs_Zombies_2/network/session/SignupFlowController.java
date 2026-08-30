package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.user.UserDataValidator;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;

/** Render-framework-independent validation and orchestration for signup. */
public final class SignupFlowController {
    private final AccountSession session;
    private final UiDispatcher dispatcher;
    private final View view;
    private final AtomicBoolean submitting = new AtomicBoolean();

    public SignupFlowController(AccountSession session, UiDispatcher dispatcher, View view) {
        this.session = Objects.requireNonNull(session, "session");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.view = Objects.requireNonNull(view, "view");
    }

    /** Returns null when the first-page fields are locally valid. */
    public static String validateAccountDetails(String username, String password,
            String passwordConfirmation, String nickname, String email, String gender) {
        if (!UserDataValidator.isValidUsername(username)) {
            return "Username can only contain English letters, numbers and hyphen.";
        }
        List<String> passwordErrors = UserDataValidator.validatePassword(password);
        if (!passwordErrors.isEmpty()) {
            return passwordErrors.get(0);
        }
        if (!Objects.equals(password, passwordConfirmation)) {
            return "Password and confirmation do not match.";
        }
        if (!UserDataValidator.isValidNickname(nickname == null ? null : nickname.trim())) {
            return "Nickname length must be between 3 and 30 characters.";
        }
        String emailError = UserDataValidator.validateEmail(email);
        if (emailError != null) {
            return emailError;
        }
        if (gender == null || Gender.getByName(gender) == null) {
            return "Gender must be Male or Female.";
        }
        return null;
    }

    public static String validateSecurityAnswer(int questionNumber, String answer,
            String answerConfirmation) {
        if (questionNumber < 1) {
            return "Choose a security question.";
        }
        if (answer == null || answer.isBlank()) {
            return "Security answer cannot be empty.";
        }
        if (!answer.equals(answerConfirmation)) {
            return "Answer confirmation must match the answer exactly.";
        }
        return null;
    }

    public boolean submit(RegistrationDetails details) {
        Objects.requireNonNull(details, "details");
        if (!submitting.compareAndSet(false, true)) {
            return false;
        }
        view.setSubmitting(true, "Connecting and creating account...");
        session.register(details).whenComplete((ignored, failure) ->
                dispatcher.dispatch(() -> {
                    submitting.set(false);
                    view.setSubmitting(false, failure == null ? "Connected." : "Ready to retry.");
                    if (failure == null) {
                        view.signupSucceeded();
                    } else {
                        view.showError(AuthenticationErrorMessages.forFailure(failure));
                    }
                }));
        return true;
    }

    public boolean isSubmitting() {
        return submitting.get();
    }

    public interface View {
        void setSubmitting(boolean submitting, String message);

        void signupSucceeded();

        void showError(String message);
    }
}
