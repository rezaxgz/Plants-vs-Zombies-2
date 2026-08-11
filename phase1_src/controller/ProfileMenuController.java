package controller;

import java.util.List;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.user.User;
import model.user.UserDataValidator;

public final class ProfileMenuController {
    private ProfileMenuController() {
    }

    public static CommandResult handleChangeUsername(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        String username = matcher.group("username");
        if (username.equals(user.getUsername())) {
            return CommandResult.error(
                    "new username is the same as the current username");
        }
        if (!UserDataValidator.isValidUsername(username)) {
            return CommandResult.error(
                    "username can only contain English letters, numbers and hyphen");
        }
        if (UserManager.usernameExists(username)) {
            return CommandResult.error("username already exists!");
        }

        UserManager.renameUser(user, username);
        return CommandResult.success("username changed successfully");
    }

    public static CommandResult handleChangeNickname(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        String nickname = matcher.group("nickname").trim();
        if (nickname.equals(user.getNickName())) {
            return CommandResult.error(
                    "new nickname is the same as the current nickname");
        }
        if (!UserDataValidator.isValidNickname(nickname)) {
            return CommandResult.error(
                    "nickname length must be between 3 and 30 characters");
        }

        user.changeNickname(nickname);
        UserManager.saveAllUsers();
        return CommandResult.success("nickname changed successfully");
    }

    public static CommandResult handleChangeEmail(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        String email = matcher.group("email");
        if (email.equals(user.getEmail())) {
            return CommandResult.error(
                    "new email is the same as the current email");
        }
        String emailError = UserDataValidator.validateEmail(email);
        if (emailError != null) {
            return CommandResult.error(emailError);
        }

        user.changeEmail(email);
        UserManager.saveAllUsers();
        return CommandResult.success("email changed successfully");
    }

    public static CommandResult handleChangePassword(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        String oldPassword = matcher.group("oldPassword");
        if (!user.doesMatchPassword(oldPassword)) {
            return CommandResult.error("old password is incorrect");
        }

        String newPassword = matcher.group("newPassword");
        if (user.doesMatchPassword(newPassword)) {
            return CommandResult.error(
                    "new password is the same as the current password");
        }
        List<String> errors =
                UserDataValidator.validatePassword(newPassword);
        if (!errors.isEmpty()) {
            return CommandResult.error(errors.get(0));
        }

        user.changePassword(newPassword);
        UserManager.saveAllUsers();
        return CommandResult.success("password changed successfully");
    }

    public static CommandResult handleShowInfo(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }

        String lineSeparator = System.lineSeparator();
        String output = "username: " + user.getUsername()
                + lineSeparator + "nickname: " + user.getNickName()
                + lineSeparator + "games played: "
                + user.getGameProgerss().getGamesPlayed()
                + lineSeparator + "coins: " + user.getCoins()
                + lineSeparator + "diamonds: " + user.getDiamonds()
                + lineSeparator + "completed levels: "
                + user.getAdventureProgress().getTotalCompletedLevelCount()
                + lineSeparator + "highest mew point: "
                + user.getGameProgerss().getHighestScore();
        return CommandResult.success(output);
    }

    private static User getLoggedInUser() {
        return App.getInstance().getLoggedInUser();
    }

    private static CommandResult loginRequired() {
        return CommandResult.error("login is required!");
    }
}
