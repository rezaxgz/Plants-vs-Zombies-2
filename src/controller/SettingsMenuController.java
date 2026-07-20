package controller;

import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.Settings;
import model.auth.UserManager;
import model.user.User;

public final class SettingsMenuController {
    private SettingsMenuController() {
    }

    public static CommandResult handleChangeDifficulty(Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error("login is required!");
        }

        int difficulty;
        try {
            difficulty = Integer.parseInt(matcher.group("difficulty"));
        } catch (NumberFormatException exception) {
            return CommandResult.error("difficulty level is too large!");
        }
        if (difficulty < Settings.MIN_DIFFICULTY
                || difficulty > Settings.MAX_DIFFICULTY) {
            return CommandResult.error(
                    "difficulty level must be between 1 and 5");
        }

        user.getSettings().setDifficultyLevel(difficulty);
        UserManager.saveAllUsers();
        return CommandResult.success(
                "difficulty level changed to " + difficulty);
    }
}
