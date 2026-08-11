package io.github.Plants_Vs_Zombies_2.model.game.scored;

import io.github.Plants_Vs_Zombies_2.view.game.ScoredGameView;

/**
 * Mutable score categories for the five required scoring patterns.
 */
public final class ScoreBreakdown {
    private int baseKillPoints;
    private int quickKillPoints;
    private int longRangePoints;
    private int armoredKillPoints;
    private int multiKillPoints;
    private int finishBonusPoints;

    void addBaseKillPoints(int points) {
        baseKillPoints += Math.max(0, points);
    }

    void addQuickKillPoints(int points) {
        quickKillPoints += Math.max(0, points);
    }

    void addLongRangePoints(int points) {
        longRangePoints += Math.max(0, points);
    }

    void addArmoredKillPoints(int points) {
        armoredKillPoints += Math.max(0, points);
    }

    void addMultiKillPoints(int points) {
        multiKillPoints += Math.max(0, points);
    }

    void addFinishBonusPoints(int points) {
        finishBonusPoints += Math.max(0, points);
    }

    public int getBaseKillPoints() {
        return baseKillPoints;
    }

    public int getQuickKillPoints() {
        return quickKillPoints;
    }

    public int getLongRangePoints() {
        return longRangePoints;
    }

    public int getArmoredKillPoints() {
        return armoredKillPoints;
    }

    public int getMultiKillPoints() {
        return multiKillPoints;
    }

    public int getFinishBonusPoints() {
        return finishBonusPoints;
    }

    public int getTotalPoints() {
        return baseKillPoints + quickKillPoints
                + longRangePoints + armoredKillPoints
                + multiKillPoints + finishBonusPoints;
    }

    public String format() {
        return ScoredGameView.formatBreakdown(this);
    }
}
