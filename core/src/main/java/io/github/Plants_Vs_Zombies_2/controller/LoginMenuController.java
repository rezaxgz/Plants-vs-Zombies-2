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
        LoginMenu menu = (LoginMenu) App.getInstance().getCurrentMenu();
        String username = matcher.group("username");
        if (!UserManager.usernameExists(username)) {
            return CommandResult.error("username does not exist!");
        }
        String password = matcher.group("password");
        if (!menu.isCorrectPassword(username, password)) {
            return CommandResult.error("Incorrect password!");
        }

        boolean stayLoggedIn = matcher.group("stayLoggedIn") != null;
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
        LoginMenu menu = (LoginMenu) App.getInstance().getCurrentMenu();
        String username = matcher.group("username");
        if (!UserManager.usernameExists(username)) {
            return CommandResult.error("username does not exist!");
        }
        String email = matcher.group("email");
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
        LoginMenu menu = (LoginMenu) App.getInstance().getCurrentMenu();
        User user = menu.getTempUser();
        if (user == null) {
            return CommandResult.error(
                    "use forget password before answering a security question");
        }
        String answer = matcher.group("answer");
        if (!menu.isCorrectAnswer(answer, user)) {
            menu.setTempUser(null);
            return CommandResult.error("Incorrect answer!");
        }

        AppView.printOutput("Enter new password: ");
        if (!AppView.getInstance().hasNext()) {
            return CommandResult.error("new password was not provided");
        }
        String password = AppView.getInstance().getInput();
        List<String> passwordErrors = UserDataValidator.validatePassword(password);
        if (!passwordErrors.isEmpty()) {
            return CommandResult.error(passwordErrors.get(0));
        }
        if (user.doesMatchPassword(password)) {
            return CommandResult.error(
                    "new password must be different from the current password");
        }

        AppView.printOutput("confirm password: ");
        if (!AppView.getInstance().hasNext()) {
            return CommandResult.error("password confirmation was not provided");
        }
        String confirmedPassword = AppView.getInstance().getInput();
        if (!confirmedPassword.equals(password)) {
            return CommandResult.error(
                    "password and confirmation do not match");
        }

        user.changePassword(password);
        menu.setTempUser(null);
        UserManager.saveAllUsers();
        return CommandResult.success("password changed successfully.");
    }
}
