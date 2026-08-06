package io.github.some_example_name.commands;

import io.github.some_example_name.controller.MainController;
import io.github.some_example_name.controller.ProjectValidationController;
import io.github.some_example_name.controller.WalletController;
import io.github.some_example_name.model.CommandResult;

public enum MainMenuCommand implements Command<CommandResult> {
    OPEN_LEADERBOARD("^menu\\s+leaderboard$",
            MainController::handleOpenLeaderboard),
    SHOW_COIN_WALLET("^menu\\s+coin(?:-|\\s+)wallet$",
            WalletController::handleShowCoins),
    SHOW_GEM_WALLET("^menu\\s+gem(?:-|\\s+)wallet$",
            WalletController::handleShowDiamonds),
    CHEAT_ADD_CURRENCY(
            "^menu\\s+cheat\\s+add\\s+(?<count>\\d+)\\s+"
                    + "(?<currency>coin(?:s)?|diamond(?:s)?)$",
            WalletController::handleCheatAddCurrency),
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
