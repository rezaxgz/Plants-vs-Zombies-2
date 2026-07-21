package commands;

import controller.MainController;
import controller.ProjectValidationController;
import model.CommandResult;

public enum MainMenuCommand implements Command<CommandResult> {
    RUN_PROJECT_CHECKS(
            "^run\\s+project\\s+checks$",
            ProjectValidationController::handleRunChecks),
    START_TIMED_WAR_CHALLENGE(
            "^start\\s+timed(?:-|\\s+)war\\s+-o\\s+"
                    + "(?<objective>kill|sun|produce-sun|produce_sun)$",
            MainController::handleStartTimedWarChallenge),
    START_SCORED_GAME(
            "^start\\s+scored(?:-|\\s+)game$",
            MainController::handleStartScoredGame),
    SHOW_SCORED_GAME_RULES(
            "^show\\s+scored(?:-|\\s+)game\\s+rules$",
            MainController::handleShowScoredGameRules),
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
            "^start\\s+level\\s+-l\\s+(?<level>\\d+)"
                    + "(?:\\s+-o\\s+(?<objective>"
                    + "kill|sun|produce-sun|produce_sun))?$",
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
