package io.github.some_example_name.model.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.some_example_name.model.App;
import io.github.some_example_name.model.auth.UserManager;
import io.github.some_example_name.model.game.Game;
import io.github.some_example_name.model.game.GameStatus;
import io.github.some_example_name.model.game.scored.ScoredGame;
import io.github.some_example_name.model.roadmap.AdventureSession;
import io.github.some_example_name.model.roadmap.Level;
import io.github.some_example_name.model.user.GameProgerss;
import io.github.some_example_name.model.user.User;

/**
 * Active game menu with optional adventure or minigame metadata.
 */
public class GameMenu extends Menu {
    private final Game game;
    private final String chapterId;
    private final int levelNumber;
    private final Level level;
    private final String minigameId;
    private final String minigameDisplayName;
    private final int minigameLevel;
    private final int minigameMaximumLevel;
    private final List<String> pendingProgressResults = new ArrayList<>();
    private boolean progressSynchronized;

    public GameMenu() {
        this(Level.createExampleLevel().createGame());
    }

    public GameMenu(Game game) {
        this(game, null, 0, null, null, null, 0, 0);
    }

    public GameMenu(Game game, String chapterId,
            int levelNumber, Level level) {
        this(game, chapterId, levelNumber, level,
                null, null, 0, 0);
    }

    private GameMenu(Game game, String chapterId,
            int levelNumber, Level level,
            String minigameId, String minigameDisplayName,
            int minigameLevel, int minigameMaximumLevel) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        validateAdventureMetadata(chapterId, levelNumber, level);
        validateMinigameMetadata(minigameId, minigameDisplayName,
                minigameLevel, minigameMaximumLevel);
        if (chapterId != null && minigameId != null) {
            throw new IllegalArgumentException(
                    "a game cannot be adventure and minigame simultaneously");
        }
        this.game = game;
        this.chapterId = chapterId;
        this.levelNumber = levelNumber;
        this.level = level;
        this.minigameId = minigameId;
        this.minigameDisplayName = minigameDisplayName;
        this.minigameLevel = minigameLevel;
        this.minigameMaximumLevel = minigameMaximumLevel;
    }

    public static GameMenu forMinigame(Game game, String minigameId,
            String displayName, int levelNumber, int maximumLevel) {
        return new GameMenu(game, null, 0, null,
                minigameId, displayName, levelNumber, maximumLevel);
    }

    private static void validateAdventureMetadata(String chapterId,
            int levelNumber, Level level) {
        if (chapterId != null && (levelNumber <= 0 || level == null)) {
            throw new IllegalArgumentException(
                    "adventure game metadata is incomplete");
        }
        if (chapterId == null && (levelNumber != 0 || level != null)) {
            throw new IllegalArgumentException(
                    "adventure metadata requires a chapter id");
        }
    }

    private static void validateMinigameMetadata(String minigameId,
            String displayName, int levelNumber, int maximumLevel) {
        if (minigameId == null) {
            if (displayName != null || levelNumber != 0 || maximumLevel != 0) {
                throw new IllegalArgumentException(
                        "minigame metadata requires a minigame id");
            }
            return;
        }
        if (minigameId.isBlank() || displayName == null
                || displayName.isBlank() || levelNumber < 1
                || maximumLevel < 1 || levelNumber > maximumLevel) {
            throw new IllegalArgumentException(
                    "minigame metadata is incomplete");
        }
    }

    public Game getGame() {
        return game;
    }

    public Level getLevel() {
        return level;
    }

    public String getChapterId() {
        return chapterId;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public boolean isMinigame() {
        return minigameId != null;
    }

    public String getMinigameId() {
        return minigameId;
    }

    public int getMinigameLevel() {
        return minigameLevel;
    }

    public void synchronizeProgress() {
        if (progressSynchronized
                || game.getStatus() == GameStatus.ACTIVE) {
            return;
        }
        if (game instanceof ScoredGame) {
            synchronizeScoredGameProgress(
                    (ScoredGame) game);
            progressSynchronized = true;
            return;
        }
        if (game.getStatus() != GameStatus.WON) {
            return;
        }
        if (chapterId != null) {
            AdventureSession.getInstance()
                    .completeLevel(chapterId, levelNumber);
        } else if (minigameId != null) {
            synchronizeMinigameProgress();
        }
        progressSynchronized = true;
    }

    public void synchronizeAdventureProgress() {
        synchronizeProgress();
    }

    private void synchronizeScoredGameProgress(
            ScoredGame scoredGame) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return;
        }
        GameProgerss progress = user.getGameProgerss();
        int previousHighScore = progress.getHighestScore();
        int score = scoredGame.getScore();
        progress.setHighestScore(score);
        if (score > previousHighScore) {
            pendingProgressResults.add(
                    "New Scored Game high score: "
                            + score + " MowPoint.");
        } else {
            pendingProgressResults.add(
                    "Scored Game finished with "
                            + score + " MowPoint; high score remains "
                            + previousHighScore + ".");
        }
        UserManager.saveAllUsers();
    }

    private void synchronizeMinigameProgress() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return;
        }
        GameProgerss progress = user.getGameProgerss();
        boolean newlyCompleted = progress.recordCompletedMinigameLevel(
                minigameId, minigameLevel, minigameMaximumLevel);
        if (!newlyCompleted) {
            return;
        }
        pendingProgressResults.add(minigameDisplayName + " level "
                + minigameLevel + " was recorded as completed.");
        int nextLevel = minigameLevel + 1;
        if (nextLevel <= minigameMaximumLevel) {
            String unlockName = minigameDisplayName + " level " + nextLevel;
            user.addMinigameUnlockNews(unlockName);
            pendingProgressResults.add(unlockName
                    + " is now unlocked in the Travel Log.");
        } else {
            pendingProgressResults.add("All " + minigameMaximumLevel + " "
                    + minigameDisplayName + " levels are complete.");
        }
        UserManager.saveAllUsers();
    }

    public List<String> drainProgressResults() {
        if (pendingProgressResults.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>(pendingProgressResults);
        pendingProgressResults.clear();
        return Collections.unmodifiableList(results);
    }

    @Override
    public void exit() {
        synchronizeProgress();
        if (isMinigame()) {
            App.getInstance().changeMenu(new TravelLogMenu());
        } else {
            App.getInstance().changeMenu(new MainMenu());
        }
    }

    @Override
    public String getName() {
        return "game";
    }
}
