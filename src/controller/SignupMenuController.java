package controller;

import java.util.List;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.enums.Gender;
import model.menu.SignUpMenu;
import model.security.Question;
import model.user.User;
import model.user.UserDataValidator;
import view.AppView;

public class SignupMenuController {
    public static CommandResult handleRegister(Matcher matcher) {
        SignUpMenu menu = (SignUpMenu) App.getInstance().getCurrentMenu();
        if (menu.getTempUser() != null) {
            return CommandResult.error("you must pick a question for the previous signup command.");
        }

        String username = matcher.group("username");
        if (!UserDataValidator.isValidUsername(username)) {
            return CommandResult.error("username can only contain letters numbers and hyphen");
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
            String passwordDontMatchError = "passwords don't match. please confirm the password or type 'back' to go to signup menu";
            AppView.printOutput(passwordDontMatchError);
            while (AppView.getInstance().hasNext()) {
                String input = AppView.getInstance().getInput();

                if (input.equals("back"))
                    return CommandResult.error("you're now in signup menu");

                if (input.equals(password))
                    break;

                AppView.printOutput(passwordDontMatchError);
            }
        }

        String nickname = matcher.group("nickname");
        if (!UserDataValidator.isValidNickname(nickname)) {
            return CommandResult.error("invalid nickname lenght");
        }

        String email = matcher.group("email");
        String emailErr = UserDataValidator.validateEmail(email);
        if (emailErr != null) {
            return CommandResult.error(emailErr);
        }

        Gender gender = Gender.getByName(matcher.group("gender"));
        if (gender == null) {
            return CommandResult.error("invalid gender. put either male or female!");
        }

        User user = new User(username, password, nickname, email, gender);
        menu.setTempUser(user);

        return CommandResult
                .success("user data saved! pick a security quesion and answer it.\n" + Question.getAllQuestions());
    }

    public static CommandResult handlePickQuestion(Matcher matcher) {
        SignUpMenu menu = (SignUpMenu) App.getInstance().getCurrentMenu();
        if (menu.getTempUser() == null) {
            return CommandResult.error("try signup command first.");
        }

        int n = Integer.parseInt(matcher.group("questionNumber"));
        Question q = Question.getByNumber(n);
        if (q == null) {
            return CommandResult.error("invalid question number.");
        }

        String answer = matcher.group("answer");
        String answerC = matcher.group("answerConfirm");

        if (!answer.equals(answerC)) {
            return CommandResult.error("answer confirm must match answer exactly");
        }

        menu.getTempUser().setSecurityQuestion(n, answer);
        UserManager.addUserToDatabase(menu.getTempUser());

        menu.exit();

        return CommandResult.success("user registered successfully\nyou're now in login menu");
    }
}
