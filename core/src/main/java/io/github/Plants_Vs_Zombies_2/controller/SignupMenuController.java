package io.github.Plants_Vs_Zombies_2.controller;

import java.util.List;
import java.util.regex.Matcher;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.menu.LoginMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.SignUpMenu;
import io.github.Plants_Vs_Zombies_2.model.security.Question;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.model.user.UserDataValidator;

public final class SignupMenuController {
    private SignupMenuController() {
    }

    public static CommandResult handleRegister(Matcher matcher) {
        return register(
                matcher.group("username"),
                matcher.group("password"),
                matcher.group("passwordConfirm"),
                matcher.group("nickname"),
                matcher.group("email"),
                matcher.group("gender"));
    }

    /**
     * GUI-friendly form of the terminal register command.
     */
    public static CommandResult register(
            String username,
            String password,
            String passwordConfirm,
            String nickname,
            String email,
            String genderName) {
        SignUpMenu menu = requireSignUpMenu();
        if (menu.getTempUser() != null) {
            return CommandResult.error(
                    "you must pick a question for the previous signup command.");
        }

        if (!UserDataValidator.isValidUsername(username)) {
            return CommandResult.error(
                    "username can only contain English letters, numbers and hyphen");
        }
        if (UserManager.usernameExists(username)) {
            return CommandResult.error("username already exists!");
        }

        List<String> passwordErrors = UserDataValidator.validatePassword(password);
        if (!passwordErrors.isEmpty()) {
            return CommandResult.error(passwordErrors.get(0));
        }
        if (!password.equals(passwordConfirm)) {
            return CommandResult.error("password and confirmation do not match");
        }

        String trimmedNickname = nickname == null ? "" : nickname.trim();
        if (!UserDataValidator.isValidNickname(trimmedNickname)) {
            return CommandResult.error(
                    "nickname length must be between 3 and 30 characters");
        }

        String emailError = UserDataValidator.validateEmail(email);
        if (emailError != null) {
            return CommandResult.error(emailError);
        }

        Gender gender = genderName == null ? null : Gender.getByName(genderName);
        if (gender == null) {
            return CommandResult.error(
                    "invalid gender. put either male or female!");
        }

        menu.setTempUser(new User(
                username, password, trimmedNickname, email, gender));
        return CommandResult.success(
                "user data saved! pick a security question and answer it."
                        + System.lineSeparator()
                        + Question.getAllQuestions());
    }

    public static CommandResult handlePickQuestion(Matcher matcher) {
        int number;
        try {
            number = Integer.parseInt(matcher.group("questionNumber"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("invalid question number.");
        }
        return pickQuestion(
                number,
                matcher.group("answer"),
                matcher.group("answerConfirm"));
    }

    /**
     * GUI-friendly form of the terminal pick-question command.
     */
    public static CommandResult pickQuestion(
            int questionNumber,
            String answer,
            String answerConfirmation) {
        SignUpMenu menu = requireSignUpMenu();
        if (menu.getTempUser() == null) {
            return CommandResult.error("try signup command first.");
        }
        if (Question.getByNumber(questionNumber) == null) {
            return CommandResult.error("invalid question number.");
        }
        if (answer == null || answer.isBlank()) {
            return CommandResult.error("security answer cannot be empty");
        }
        if (!answer.equals(answerConfirmation)) {
            return CommandResult.error(
                    "answer confirmation must match the answer exactly");
        }

        User user = menu.getTempUser();
        user.setSecurityQuestion(questionNumber, answer);
        UserManager.addUserToDatabase(user);
        menu.setTempUser(null);
        App.getInstance().changeMenu(new LoginMenu());
        return CommandResult.success(
                "user registered successfully"
                        + System.lineSeparator()
                        + "you're now in login menu");
    }

    public static void cancelPendingRegistration() {
        if (App.getInstance().getCurrentMenu() instanceof SignUpMenu menu) {
            menu.setTempUser(null);
        }
    }

    private static SignUpMenu requireSignUpMenu() {
        if (!(App.getInstance().getCurrentMenu() instanceof SignUpMenu menu)) {
            throw new IllegalStateException("signup action requires the signup menu");
        }
        return menu;
    }
}
