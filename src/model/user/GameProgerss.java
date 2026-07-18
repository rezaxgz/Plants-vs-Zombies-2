package model.user;

/**
 * Aggregate statistics used by the profile and leaderboard menus.
 *
 * <p>The class name is kept for compatibility with the existing codebase.</p>
 */
public class GameProgerss {
    private int lastCompletedChapter;
    private int lastCompletedLevel;
    private int completedMinigames;
    private int highestScore;
    private int gamesPlayed;

    public static GameProgerss fromStoredData(
            int lastCompletedChapter, int lastCompletedLevel,
            int completedMinigames, int highestScore,
            int gamesPlayed) {
        GameProgerss progress = new GameProgerss();
        progress.lastCompletedChapter = Math.max(0, lastCompletedChapter);
        progress.lastCompletedLevel = Math.max(0, lastCompletedLevel);
        progress.completedMinigames = Math.max(0, completedMinigames);
        progress.highestScore = Math.max(0, highestScore);
        progress.gamesPlayed = Math.max(0, gamesPlayed);
        return progress;
    }

    public int getLastCompletedChapter() {
        return lastCompletedChapter;
    }

    public void setLastCompletedChapter(int lastCompletedChapter) {
        this.lastCompletedChapter = Math.max(0, lastCompletedChapter);
    }

    public int getLastCompletedLevel() {
        return lastCompletedLevel;
    }

    public void setLastCompletedLevel(int lastCompletedLevel) {
        this.lastCompletedLevel = Math.max(0, lastCompletedLevel);
    }

    public void recordCompletedLevel(int chapterNumber, int levelNumber) {
        if (chapterNumber < 1 || levelNumber < 1) {
            return;
        }
        boolean isFurther = chapterNumber > lastCompletedChapter
                || (chapterNumber == lastCompletedChapter
                        && levelNumber > lastCompletedLevel);
        if (isFurther) {
            lastCompletedChapter = chapterNumber;
            lastCompletedLevel = levelNumber;
        }
    }

    public int getCompletedMinigames() {
        return completedMinigames;
    }

    public void addCompletedMinigame() {
        completedMinigames++;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(int highestScore) {
        if (highestScore > this.highestScore) {
            this.highestScore = highestScore;
        }
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void recordGameStarted() {
        gamesPlayed++;
    }
}
