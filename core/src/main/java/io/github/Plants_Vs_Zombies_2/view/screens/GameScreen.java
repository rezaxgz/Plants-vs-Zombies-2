package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.menu.CollectionMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.GameMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.GreenhouseMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.LeaderboardMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.TravelLogMenu;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Chapter;
import io.github.Plants_Vs_Zombies_2.model.roadmap.ChapterCatalog;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Level;

/** Graphical shell for the active game and chapter board preview. */
public final class GameScreen extends AbstractScreen {
    private static final int BOARD_COLUMNS = 9;
    private static final int BOARD_ROWS = 5;

    private static final BoardLayout EGYPT_BOARD = new BoardLayout(
            "IMAGE_BACKGROUNDS_EGYPT_TEXTURE",
            1024f, 768f,
            256f, 200f, 994f, 688f);
    private static final BoardLayout ICEAGE_BOARD = new BoardLayout(
            "IMAGE_BACKGROUNDS_ICEAGE_TEXTURE",
            1022f, 785f,
            256f, 205f, 984f, 688f);
    private static final BoardLayout BEACH_BOARD = new BoardLayout(
            "IMAGE_BACKGROUNDS_BEACH_TEXTURE",
            1024f, 768f,
            256f, 200f, 994f, 688f);
    private static final BoardLayout DARK_BOARD = new BoardLayout(
            "IMAGE_BACKGROUNDS_DARK_TEXTURE",
            1024f, 768f,
            256f, 200f, 994f, 688f);

    private BoardGridActor gridActor;

    /** Normal model-backed game screen. */
    public GameScreen(ScreenNavigator navigator) {
        super(navigator, "Game");

        Chapter chapter = chapterForCurrentGame();
        installChapterBoard(chapter);

        addActionButton("Pause", navigator::showPauseScreen);
        addMenuButton("Collection", () -> new CollectionMenu(currentGameMenu()));
        addMenuButton("Greenhouse", () -> new GreenhouseMenu(currentGameMenu()));
        addMenuButton("Travel Log", () -> new TravelLogMenu(currentGameMenu()));
        addMenuButton("Leaderboard", () -> new LeaderboardMenu(currentGameMenu()));
        addBackButton();
    }

    /**
     * Empty level preview used by the Adventure level Play buttons. For now
     * it intentionally starts no level/game logic; it only shows the chapter
     * board background and the optional debug grid.
     */
    public GameScreen(ScreenNavigator navigator, Chapter chapter, Level level) {
        super(navigator, chapter == null || level == null
                ? "Game"
                : chapter.getDisplayName() + " - Level " + level.getNumber());
        if (chapter == null || level == null) {
            throw new IllegalArgumentException("chapter and level cannot be null");
        }
        installChapterBoard(chapter);
        addActionButton("Back to Adventure", navigator::showAdventureScreen);
    }

    private void installChapterBoard(Chapter chapter) {
        BoardLayout layout = layoutForChapter(chapter);
        if (layout == null) {
            return;
        }
        setAssetBackground(layout.backgroundAsset);

        if (shouldDrawGrid()) {
            gridActor = new BoardGridActor(layout);
            addBackgroundOverlay(gridActor);
        }
    }

    private boolean shouldDrawGrid() {
        return App.getInstance().getLoggedInUser() != null
                && App.getInstance().getLoggedInUser()
                        .getSettings().isShowGameMapGrid();
    }

    private static Chapter chapterForCurrentGame() {
        GameMenu menu = currentGameMenu();
        if (menu.getChapterId() == null) {
            return null;
        }
        return ChapterCatalog.findById(menu.getChapterId());
    }

    private static BoardLayout layoutForChapter(Chapter chapter) {
        if (chapter == null) {
            return null;
        }
        if ("ancient-egypt".equals(chapter.getId())) {
            return EGYPT_BOARD;
        }
        if ("frostbite-caves".equals(chapter.getId())) {
            return ICEAGE_BOARD;
        }
        if ("big-wave-beach".equals(chapter.getId())) {
            return BEACH_BOARD;
        }
        if ("dark-ages".equals(chapter.getId())) {
            return DARK_BOARD;
        }
        return null;
    }

    private static GameMenu currentGameMenu() {
        return (GameMenu) App.getInstance().getCurrentMenu();
    }

    @Override
    public void dispose() {
        if (gridActor != null) {
            gridActor.dispose();
            gridActor = null;
        }
        super.dispose();
    }

    /** Board rectangle measured from the supplied 768-resolution textures. */
    private static final class BoardLayout {
        private final String backgroundAsset;
        private final float sourceWidth;
        private final float sourceHeight;
        private final float left;
        private final float top;
        private final float right;
        private final float bottom;

        private BoardLayout(String backgroundAsset,
                float sourceWidth, float sourceHeight,
                float left, float top, float right, float bottom) {
            this.backgroundAsset = backgroundAsset;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    /**
     * Draws the logical 9x5 board directly on the stretched-background stage,
     * so the red lines stay aligned even when the OS window is not 16:9.
     */
    private static final class BoardGridActor extends Actor {
        private static final float LINE_THICKNESS = 2f;
        private static final float LINE_ALPHA = 0.88f;

        private final BoardLayout layout;
        private final Texture redPixel;

        private BoardGridActor(BoardLayout layout) {
            this.layout = layout;
            setTouchable(Touchable.disabled);

            Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixel.setColor(Color.RED);
            pixel.fill();
            redPixel = new Texture(pixel);
            pixel.dispose();
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (getStage() == null) {
                return;
            }

            float worldWidth = getStage().getViewport().getWorldWidth();
            float worldHeight = getStage().getViewport().getWorldHeight();
            float boardX = worldWidth * layout.left / layout.sourceWidth;
            float boardY = worldHeight
                    * (layout.sourceHeight - layout.bottom)
                    / layout.sourceHeight;
            float boardWidth = worldWidth
                    * (layout.right - layout.left) / layout.sourceWidth;
            float boardHeight = worldHeight
                    * (layout.bottom - layout.top) / layout.sourceHeight;

            Color previous = new Color(batch.getColor());
            batch.setColor(1f, 1f, 1f, LINE_ALPHA * parentAlpha);

            for (int column = 0; column <= BOARD_COLUMNS; column++) {
                float x = boardX
                        + boardWidth * column / BOARD_COLUMNS
                        - LINE_THICKNESS / 2f;
                batch.draw(redPixel, x, boardY,
                        LINE_THICKNESS, boardHeight);
            }
            for (int row = 0; row <= BOARD_ROWS; row++) {
                float y = boardY
                        + boardHeight * row / BOARD_ROWS
                        - LINE_THICKNESS / 2f;
                batch.draw(redPixel, boardX, y,
                        boardWidth, LINE_THICKNESS);
            }

            batch.setColor(previous);
        }

        private void dispose() {
            redPixel.dispose();
        }
    }
}
