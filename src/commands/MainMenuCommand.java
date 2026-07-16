package commands;

import controller.MainController;
import model.CommandResult;

public enum MainMenuCommand implements Command<CommandResult> {
    SHOW_CHAPTERS("^show\\s+chapters$",
            MainController::handleShowChapters),
    SHOW_LEVELS(
            "^show\\s+levels(?:\\s+-c\\s+(?<chapter>.+))?$",
            MainController::handleShowLevels),
    ENTER_CHAPTER(
            "^menu\\s+enter\\s+chapter\\s+-c\\s+"
                    + "(?<chapter>.+)$",
            MainController::handleEnterChapter),
    START_LEVEL(
            "^start\\s+level\\s+-l\\s+(?<level>\\d+)$",
            MainController::handleStartLevel),
    SHOW_CURRENT_LEVEL("^show\\s+current\\s+level$",
            MainController::handleShowCurrentLevel),
    START_GAME("^start\\s+game$",
            MainController::handleStartGame),
    LOGOUT("^menu\\s+logout$",
            MainController::handleLogout);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    MainMenuCommand(String pattern, CommandAction<CommandResult> action) {
        this.pattern = pattern;
        this.action = action;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public CommandAction<CommandResult> getAction() {
        return action;
    }
}
