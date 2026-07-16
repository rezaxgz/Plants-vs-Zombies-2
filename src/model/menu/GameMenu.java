package model.menu;

import model.App;
import model.game.Game;
import model.game.GameStatus;
import model.roadmap.AdventureSession;
import model.roadmap.Level;

/**
 * Active game menu with optional adventure-level metadata.
 */
public class GameMenu extends Menu {
    private final Game game;
    private final String chapterId;
    private final int levelNumber;
    private final Level level;
    private boolean progressSynchronized;

    public GameMenu() {
        this(Level.createExampleLevel().createGame());
    }

    public GameMenu(Game game) {
        this(game, null, 0, null);
    }

    public GameMenu(Game game, String chapterId,
            int levelNumber, Level level) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null");
        }
        if (chapterId != null
                && (levelNumber <= 0
                        || level == null)) {
            throw new IllegalArgumentException(
                    "adventure game metadata is incomplete");
        }
        this.game = game;
        this.chapterId = chapterId;
        this.levelNumber = levelNumber;
        this.level = level;
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

    public void synchronizeAdventureProgress() {
        if (progressSynchronized
                || chapterId == null
                || game.getStatus()
                        != GameStatus.WON) {
            return;
        }
        AdventureSession.getInstance()
                .completeLevel(
                        chapterId, levelNumber);
        progressSynchronized = true;
    }

    @Override
    public void exit() {
        synchronizeAdventureProgress();
        App.getInstance().changeMenu(
                new MainMenu());
    }

    @Override
    public String getName() {
        return "game";
    }
}
