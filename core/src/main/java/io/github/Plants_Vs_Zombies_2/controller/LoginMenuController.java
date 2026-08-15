package io.github.Plants_Vs_Zombies_2.controller;

import java.util.List;
import java.util.regex.Matcher;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.auth.SessionManager;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.menu.LoginMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.MainMenu;
import io.github.Plants_Vs_Zombies_2.model.news.NewsMessages;
import io.github.Plants_Vs_Zombies_2.model.roadmap.AdventureSession;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.model.user.UserDataValidator;
import io.github.Plants_Vs_Zombies_2.view.AppView;

public final class LoginMenuController {
    private LoginMenuController() {
    }

    public static CommandResult handleLogin(Matcher matcher) {
        return login(
                matcher.group("username"),
                matcher.group("password"),
                matcher.group("stayLoggedIn") != null);
    }

    /**
     * GUI-friendly form of the terminal login command.
     */
    public static CommandResult login(
            String username, String password, boolean stayLoggedIn) {
        LoginMenu menu = requireLoginMenu();
        if (!UserManager.usernameExists(username)) {
            return CommandResult.error("username does not exist!");
        }
        if (!menu.isCorrectPassword(username, password)) {
            return CommandResult.error("Incorrect password!");
        }

        menu.setStayLoggedIn(stayLoggedIn);
        menu.login(username);
        User loggedInUser = App.getInstance().getLoggedInUser();
        if (stayLoggedIn) {
            SessionManager.persist(loggedInUser);
        } else {
            SessionManager.clearPersistentSession();
        }
        AdventureSession.getInstance().reset();
        App.getInstance().changeMenu(new MainMenu());

        String message = "Logged in successfully."
                + System.lineSeparator()
                + "you're now in main menu";
        String newsBadge = NewsMessages.unreadBadge(loggedInUser);
        if (!newsBadge.isBlank()) {
            message += System.lineSeparator() + newsBadge;
        }
        return CommandResult.success(message);
    }

    public static CommandResult handleForgetPassword(Matcher matcher) {
        return beginPasswordReset(
                matcher.group("username"), matcher.group("email"));
    }

    /**
     * GUI-friendly form of the terminal forget-password command.
     */
    public static CommandResult beginPasswordReset(String username, String email) {
        LoginMenu menu = requireLoginMenu();
        if (!UserManager.usernameExists(username)) {
            return CommandResult.error("username does not exist!");
        }
        String emailError = UserDataValidator.validateEmail(email);
        if (emailError != null) {
            return CommandResult.error(emailError);
        }
        if (!menu.isCorrectEmail(username, email)) {
            return CommandResult.error("Incorrect email!");
        }

        menu.setTempUserByName(username);
        String question = menu.getTempUser().getSecurityQuestion();
        if (question == null) {
            menu.setTempUser(null);
            return CommandResult.error(
                    "this account does not have a security question");
        }
        return CommandResult.success(
                "Answer the security question: " + question);
    }

    public static CommandResult handleAnswer(Matcher matcher) {
        LoginMenu menu = requireLoginMenu();
        User user = menu.getTempUser();
        CommandResult answerResult = validateResetAnswer(
                menu, user, matcher.group("answer"));
        if (!answerResult.isSuccsesful()) {
            return answerResult;
        }

        AppView.printOutput("Enter new password: ");
        if (!AppView.getInstance().hasNext()) {
            return CommandResult.error("new password was not provided");
        }
        String password = AppView.getInstance().getInput();
        CommandResult passwordResult = validateNewPassword(user, password);
        if (!passwordResult.isSuccsesful()) {
            return passwordResult;
        }

        AppView.printOutput("confirm password: ");
        if (!AppView.getInstance().hasNext()) {
            return CommandResult.error("password confirmation was not provided");
        }
        String confirmedPassword = AppView.getInstance().getInput();
        return applyPasswordChange(menu, user, password, confirmedPassword);
    }

    /**
     * Completes the same password-reset flow as the terminal answer command,
     * but receives the new password from graphical TextFields instead of
     * reading from the console.
     */
    public static CommandResult completePasswordReset(
            String answer, String password, String confirmedPassword) {
        LoginMenu menu = requireLoginMenu();
        User user = menu.getTempUser();
        CommandResult answerResult = validateResetAnswer(menu, user, answer);
        if (!answerResult.isSuccsesful()) {
            return answerResult;
        }
        CommandResult passwordResult = validateNewPassword(user, password);
        if (!passwordResult.isSuccsesful()) {
            return passwordResult;
        }
        return applyPasswordChange(menu, user, password, confirmedPassword);
    }

    public static void cancelPasswordReset() {
        if (App.getInstance().getCurrentMenu() instanceof LoginMenu menu) {
            menu.setTempUser(null);
        }
    }

    private static CommandResult validateResetAnswer(
            LoginMenu menu, User user, String answer) {
        if (user == null) {
            return CommandResult.error(
                    "use forget password before answering a security question");
        }
        if (!menu.isCorrectAnswer(answer, user)) {
            menu.setTempUser(null);
            return CommandResult.error("Incorrect answer!");
        }
        return CommandResult.success("");
    }

    private static CommandResult validateNewPassword(User user, String password) {
        List<String> passwordErrors = UserDataValidator.validatePassword(password);
        if (!passwordErrors.isEmpty()) {
            return CommandResult.error(passwordErrors.get(0));
        }
        if (user.doesMatchPassword(password)) {
            return CommandResult.error(
                    "new password must be different from the current password");
        }
        return CommandResult.success("");
    }

    private static CommandResult applyPasswordChange(
            LoginMenu menu,
            User user,
            String password,
            String confirmedPassword) {
        if (!password.equals(confirmedPassword)) {
            return CommandResult.error(
                    "password and confirmation do not match");
        }

        user.changePassword(password);
        menu.setTempUser(null);
        UserManager.saveAllUsers();
        return CommandResult.success("password changed successfully.");
    }

    private static LoginMenu requireLoginMenu() {
        if (!(App.getInstance().getCurrentMenu() instanceof LoginMenu menu)) {
            throw new IllegalStateException("login action requires the login menu");
        }
        return menu;
    }
}
