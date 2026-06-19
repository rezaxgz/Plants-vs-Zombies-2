package controller;

import java.math.MathContext;
import java.util.List;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.menu.LoginMenu;
import model.user.User;
import model.user.UserDataValidator;
import view.AppView;

public class LoginMenuController {
    public static CommandResult handleLogin(Matcher matcher) {
        LoginMenu menu = (LoginMenu) App.getInstance().getCurrentMenu();
        String username = matcher.group("username");
        if (!UserManager.usernameExists(username)) {
            return CommandResult.error("username does not exist!");
        }
        String password = matcher.group("password");
        if (!menu.isCorrectPassword(username, password)) {
            return CommandResult.error("Incorrect password!");
        }
        boolean stayLoggedIn = matcher.group("-stay-logged-in") != null;
        menu.setStayLoggedIn(stayLoggedIn);

        menu.login(username);

        menu.exit();

        return CommandResult.success("Logged in successfully.");
    }

    public static CommandResult handleForgetPassword(Matcher matcher) {
        LoginMenu menu = (LoginMenu) App.getInstance().getCurrentMenu();
        String username = matcher.group("username");
        if (!UserManager.usernameExists(username)) {
            return CommandResult.error("username does not exist!");
        }
        String email = matcher.group("email");
        if (!menu.isCorrectEmail(username, email)) {
            return CommandResult.error("Incorrect email!");
        }

        menu.setTempUserByName(username);
        return CommandResult.success("Answer the security question:" + menu.getTempUser().getSecurityQuestion());
    }

    public static CommandResult handleAnswer(Matcher matcher) {
        LoginMenu menu = (LoginMenu) App.getInstance().getCurrentMenu();
        User user = menu.getTempUser();
        String answer = matcher.group("answer");
        if (!menu.isCorrectAnswer(answer, user)) {
            return CommandResult.error("Incorrect answer!");
        }

        AppView.printOutput("Enter new password: ");
        String password = AppView.getInstance().getInput();
        List<String> passwordErrors = UserDataValidator.validatePassword(password);
        if (!passwordErrors.isEmpty()) {
            return CommandResult.error(passwordErrors.get(0));
        }

        AppView.printOutput("confirm password: ");
        String confirmedPassword = AppView.getInstance().getInput();
        if (!confirmedPassword.equals(password)) {
            String passwordDontMatchError = "passwords don't match. please confirm the password or type 'back' to go to login menu";
            AppView.printOutput(passwordDontMatchError);
            while (AppView.getInstance().hasNext()) {
                String input = AppView.getInstance().getInput();

                if (input.equals("back"))
                    return CommandResult.error("you're now in login menu");

                if (input.equals(password))
                    break;

                AppView.printOutput(passwordDontMatchError);
            }
        }
        user.changePassword(confirmedPassword);
        return CommandResult.success("password changed successfully.");
    }
}
