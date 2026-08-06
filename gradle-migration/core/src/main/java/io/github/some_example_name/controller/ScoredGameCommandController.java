package io.github.some_example_name.controller;

import java.util.regex.Matcher;

import io.github.some_example_name.model.App;
import io.github.some_example_name.model.CommandResult;
import io.github.some_example_name.model.game.scored.ScoredGame;
import io.github.some_example_name.model.menu.GameMenu;
import io.github.some_example_name.model.menu.Menu;

/**
 * Commands that expose the active Scored Game score and rules.
 */
public final class ScoredGameCommandController {
    private ScoredGameCommandController() {
    }

    public static CommandResult handleShowScore(
            Matcher matcher) {
        ScoredGame game = getCurrentScoredGame();
        if (game == null) {
            return CommandResult.error(
                    "Scored Game is not active!");
        }
        String output = "daily challenge: "
                + game.getChallengeDate()
                + System.lineSeparator()
                + "daily seed: " + game.getDailySeed()
                + System.lineSeparator()
                + game.getScoreBreakdown().format();
        return CommandResult.success(output);
    }

    public static CommandResult handleShowRules(
            Matcher matcher) {
        return CommandResult.success(
                ScoredGame.getRulesDescription());
    }

    private static ScoredGame getCurrentScoredGame() {
        Menu menu = App.getInstance().getCurrentMenu();
        if (!(menu instanceof GameMenu)) {
            return null;
        }
        if (!(((GameMenu) menu).getGame() instanceof ScoredGame)) {
            return null;
        }
        return (ScoredGame) ((GameMenu) menu).getGame();
    }
}
