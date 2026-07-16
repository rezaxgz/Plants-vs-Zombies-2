package model.user;

public class GameProgerss {
    private int lastCompletedChapter = 0;
    private int lastCompletedLevel = 0;
    private int completedMinigames = 0;
    private int highestScore = 0;

    public int getLastCompletedChapter() {
        return lastCompletedChapter;
    }

    public void setLastCompletedChapter(int lastCompletedChapter) {
        this.lastCompletedChapter = lastCompletedChapter;
    }

    public int getLastCompletedLevel() {
        return lastCompletedLevel;
    }

    public void setLastCompletedLevel(int lastCompletedLevel) {
        this.lastCompletedLevel = lastCompletedLevel;
    }

    public int getCompletedMinigames() {
        return completedMinigames;
    }

    public void addCompletedMinigame() {
        this.completedMinigames++;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(int highestScore) {
        if (highestScore > this.highestScore) {
            this.highestScore = highestScore;
        }
    }
}