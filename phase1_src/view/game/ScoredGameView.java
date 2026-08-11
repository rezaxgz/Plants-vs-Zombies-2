package view.game;

import model.game.scored.ScoreBreakdown;

/**
 * Builds scored-game rule and score-breakdown text.
 */
public final class ScoredGameView {
    private ScoredGameView() {
    }

    public static String formatBreakdown(ScoreBreakdown breakdown) {
        return "base kills: " + breakdown.getBaseKillPoints()
                + System.lineSeparator()
                + "quick kills: " + breakdown.getQuickKillPoints()
                + System.lineSeparator()
                + "long-range kills: " + breakdown.getLongRangePoints()
                + System.lineSeparator()
                + "armored kills: " + breakdown.getArmoredKillPoints()
                + System.lineSeparator()
                + "multi-kills: " + breakdown.getMultiKillPoints()
                + System.lineSeparator()
                + "finish bonus: " + breakdown.getFinishBonusPoints()
                + System.lineSeparator()
                + "total MowPoint: " + breakdown.getTotalPoints();
    }

    public static String formatRules() {
        return "Daily Scored Game rules"
                + System.lineSeparator()
                + "1. Base kill: points equal to at least the zombie wave cost."
                + System.lineSeparator()
                + "2. Quick kill: +150 within 8 seconds of spawning."
                + System.lineSeparator()
                + "3. Long-range kill: +75 in the right half of the lawn."
                + System.lineSeparator()
                + "4. Armored kill: +100 for a zombie with armor."
                + System.lineSeparator()
                + "5. Multi-kill: +125 for each extra zombie in one batch."
                + System.lineSeparator()
                + "Win bonus: +250 for each unused lawn mower."
                + System.lineSeparator()
                + "The UTC date fixes the waves and spawn randomness."
                + System.lineSeparator()
                + "Cheats are disabled and difficulty is fixed at 3.";
    }
}
