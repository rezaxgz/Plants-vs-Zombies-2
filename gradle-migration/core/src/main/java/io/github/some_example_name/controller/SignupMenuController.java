package io.github.some_example_name.controller;

import java.util.List;
import java.util.regex.Matcher;

import io.github.some_example_name.model.App;
import io.github.some_example_name.model.CommandResult;
import io.github.some_example_name.model.auth.UserManager;
import io.github.some_example_name.model.enums.Gender;
import io.github.some_example_name.model.menu.LoginMenu;
import io.github.some_example_name.model.menu.SignUpMenu;
import io.github.some_example_name.model.security.Question;
import io.github.some_example_name.model.user.User;
import io.github.some_example_name.model.user.UserDataValidator;

public final class SignupMenuController {
    private SignupMenuController() {
    }

    public static CommandResult handleRegister(Matcher matcher) {
        SignUpMenu menu = (SignUpMenu) App.getInstance().getCurrentMenu();
        if (menu.getTempUser() != null) {
            return CommandResult.error(
                    "you must pick a question for the previous signup command.");
        }

        String username = matcher.group("username");
        if (!UserDataValidator.isValidUsername(username)) {
            return CommandResult.error(
                    "username can only contain English letters, numbers and hyphen");
        }
        if (UserManager.usernameExists(username)) {
            return CommandResult.error("username already exists!");
        }

        String password = matcher.group("password");
        List<String> passwordErrors = UserDataValidator.validatePassword(password);
        if (!passwordErrors.isEmpty()) {
            return CommandResult.error(passwordErrors.get(0));
        }
        if (!password.equals(matcher.group("passwordConfirm"))) {
            return CommandResult.error("password and confirmation do not match");
        }

        String nickname = matcher.group("nickname").trim();
        if (!UserDataValidator.isValidNickname(nickname)) {
            return CommandResult.error(
                    "nickname length must be between 3 and 30 characters");
        }

        String email = matcher.group("email");
        String emailError = UserDataValidator.validateEmail(email);
        if (emailError != null) {
            return CommandResult.error(emailError);
        }

        Gender gender = Gender.getByName(matcher.group("gender"));
        if (gender == null) {
            return CommandResult.error(
                    "invalid gender. put either male or female!");
        }

        menu.setTempUser(new User(
                username, password, nickname, email, gender));
        return CommandResult.success(
                "user data saved! pick a security question and answer it."
                        + System.lineSeparator()
                        + Question.getAllQuestions());
    }

    public static CommandResult handlePickQuestion(Matcher matcher) {
        SignUpMenu menu = (SignUpMenu) App.getInstance().getCurrentMenu();
        if (menu.getTempUser() == null) {
            return CommandResult.error("try signup command first.");
        }

        int number;
        try {
            number = Integer.parseInt(matcher.group("questionNumber"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("invalid question number.");
        }
        if (Question.getByNumber(number) == null) {
            return CommandResult.error("invalid question number.");
        }

        String answer = matcher.group("answer");
        String answerConfirmation = matcher.group("answerConfirm");
        if (answer.isBlank()) {
            return CommandResult.error("security answer cannot be empty");
        }
        if (!answer.equals(answerConfirmation)) {
            return CommandResult.error(
                    "answer confirmation must match the answer exactly");
        }

        User user = menu.getTempUser();
        user.setSecurityQuestion(number, answer);
        UserManager.addUserToDatabase(user);
        menu.setTempUser(null);
        App.getInstance().changeMenu(new LoginMenu());
        return CommandResult.success(
                "user registered successfully"
                        + System.lineSeparator()
                        + "you're now in login menu");
    }
}
