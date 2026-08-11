package io.github.Plants_Vs_Zombies_2.controller;

import java.util.regex.Matcher;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.game.scored.ScoredGame;
import io.github.Plants_Vs_Zombies_2.model.menu.GameMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.Menu;

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
