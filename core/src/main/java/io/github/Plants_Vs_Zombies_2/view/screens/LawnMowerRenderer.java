package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;

import io.github.Plants_Vs_Zombies_2.model.game.Game;
import io.github.Plants_Vs_Zombies_2.model.game.defense.LawnMower;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Chapter;
import pvz.libpvz.pam.PamPlayer;

/**
 * Phase-2 lawn-mower renderer.
 *
 * <p>The mower model now owns the actual sweep position. This actor mirrors
 * that position so the mower stays visible while crossing the row and the
 * hit effect appears as it reaches zombies.</p>
 */
final class LawnMowerRenderer extends Group {
    private static final int BOARD_COLUMNS = 9;
    private static final int BOARD_ROWS = 5;

    private static final String MOWER_EGYPT =
            "768/INITIAL/MOWERS/MOWER_EGYPT/MOWER_EGYPT.PAM";
    private static final String MOWER_ICEAGE =
            "768/FULL/MOWERS/MOWER_ICEAGE/MOWER_ICEAGE.PAM";
    private static final String MOWER_BEACH =
            "768/FULL/MOWERS/MOWER_BEACH/MOWER_BEACH.PAM";
    private static final String MOWER_DARK =
            "768/FULL/MOWERS/MOWER_DARK/MOWER_DARK.PAM";
    private static final String MOWER_TUTORIAL =
            "768/INITIAL/MOWERS/MOWER_TUTORIAL/MOWER_TUTORIAL.PAM";
    private static final String MOWER_ZOMBIE_HIT =
            "768/INITIAL/EFFECTS/MOWER_ZOMBIE_HIT/MOWER_ZOMBIE_HIT.PAM";

    private static final float MOWER_WIDTH_IN_CELLS = 0.95f;
    private static final float MOWER_HEIGHT_IN_CELLS = 1.08f;
    private static final float MOWER_CENTER_LEFT_OFFSET_IN_CELLS = 0.46f;
    private static final float MOWER_FOOT_LINE_RATIO = 0.16f;
    private static final float HIT_EFFECT_SIZE_IN_CELLS = 1.30f;
    private static final double MOWER_HIT_LEAD_COLUMNS = 0.55;

    private final PamPlayer player;
    private final Game game;
    private final BoardLayout layout;
    private final String mowerPam;
    private final Map<Integer, PamAnimationActor> mowerActors = new HashMap<>();
    private final Map<Integer, Boolean> mowerWasUsed = new HashMap<>();
    private final Map<Integer, List<ZombieSnapshot>> sweepTargets =
            new HashMap<>();
    private final Map<Integer, Integer> nextSweepTargetIndex =
            new HashMap<>();
    private List<ZombieSnapshot> previousZombies = new ArrayList<>();

    LawnMowerRenderer(PamPlayer player, Game game, Chapter chapter) {
        if (player == null || game == null) {
            throw new IllegalArgumentException("player and game cannot be null");
        }
        this.player = player;
        this.game = game;
        this.layout = layoutFor(chapter);
        this.mowerPam = mowerPamFor(chapter);
        setTouchable(Touchable.disabled);

        for (LawnMower mower : game.getLawnMowers()) {
            mowerWasUsed.put(mower.getRow(), mower.isUsed());
        }
        previousZombies = snapshotZombies();
        for (LawnMower mower : game.getLawnMowers()) {
            if (mower.isActive()) {
                beginSweepTracking(mower.getRow(),
                        zombieSnapshotsInRow(mower.getRow()));
            }
        }
        refreshMowers();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (layout == null) {
            return;
        }

        List<Integer> triggeredRows = detectTriggeredRows();
        if (!triggeredRows.isEmpty()) {
            for (int row : triggeredRows) {
                beginSweepTracking(row, zombieSnapshotsInRow(row));
            }
        }

        refreshMowers();
        updateSweepHitEffects();
        previousZombies = snapshotZombies();
    }

    private List<Integer> detectTriggeredRows() {
        List<Integer> triggered = new ArrayList<>();
        for (LawnMower mower : game.getLawnMowers()) {
            boolean used = mower.isUsed();
            boolean wasUsed = mowerWasUsed.getOrDefault(
                    mower.getRow(), Boolean.FALSE);
            if (used && !wasUsed) {
                triggered.add(mower.getRow());
            }
            mowerWasUsed.put(mower.getRow(), used);
        }
        return triggered;
    }

    private void refreshMowers() {
        if (layout == null) {
            return;
        }
        for (LawnMower mower : game.getLawnMowers()) {
            PamAnimationActor actor = mowerActors.get(mower.getRow());
            if (actor == null) {
                try {
                    actor = new PamAnimationActor(player, mowerPam, "idle");
                } catch (RuntimeException ignored) {
                    continue;
                }
                actor.setTouchable(Touchable.disabled);
                mowerActors.put(mower.getRow(), actor);
                addActor(actor);
            }

            if (mower.isAvailable()) {
                positionMower(actor, mower.getRow());
                actor.setVisible(true);
            } else if (mower.isActive()) {
                positionMovingMower(actor, mower.getRow(),
                        mower.getColumnPosition());
                actor.setVisible(true);
                actor.toFront();
            } else {
                actor.setVisible(false);
            }
        }
    }

    /**
     * Keep using the exact same PAM actor that was visible before the trigger.
     * This avoids swapping the stationary actor for a nested temporary actor
     * on the trigger frame, which could make the mower blink out completely.
     */
    private void positionMovingMower(
            com.badlogic.gdx.scenes.scene2d.Actor actor,
            int row, double sweepColumn) {
        CellGeometry geometry = geometry();
        if (geometry == null || row < 0 || row >= BOARD_ROWS) {
            actor.setVisible(false);
            return;
        }

        float laneBottom = geometry.boardY
                + (BOARD_ROWS - 1 - row) * geometry.cellHeight;
        float footLine = laneBottom
                + geometry.cellHeight * MOWER_FOOT_LINE_RATIO;
        float width = geometry.cellWidth * MOWER_WIDTH_IN_CELLS;
        float height = geometry.cellHeight * MOWER_HEIGHT_IN_CELLS;
        float startCenterX = geometry.boardX
                - geometry.cellWidth * MOWER_CENTER_LEFT_OFFSET_IN_CELLS;
        float centerX = startCenterX
                + (float) sweepColumn * geometry.cellWidth;

        actor.setBounds(centerX - width * 0.5f,
                footLine, width, height);
    }

    private void beginSweepTracking(int row,
            List<ZombieSnapshot> targets) {
        sweepTargets.put(row, targets == null
                ? new ArrayList<>() : new ArrayList<>(targets));
        nextSweepTargetIndex.put(row, 0);
    }

    private void updateSweepHitEffects() {
        for (LawnMower mower : game.getLawnMowers()) {
            int row = mower.getRow();
            if (!mower.isActive()) {
                if (mower.isUsed()) {
                    sweepTargets.remove(row);
                    nextSweepTargetIndex.remove(row);
                }
                continue;
            }

            List<ZombieSnapshot> targets = sweepTargets.get(row);
            if (targets == null) {
                targets = zombieSnapshotsInRow(row);
                beginSweepTracking(row, targets);
            }
            int nextIndex = nextSweepTargetIndex.getOrDefault(row, 0);
            double collisionColumn = mower.getColumnPosition()
                    + MOWER_HIT_LEAD_COLUMNS;
            while (nextIndex < targets.size()
                    && targets.get(nextIndex).column <= collisionColumn) {
                playZombieHitEffect(targets.get(nextIndex));
                nextIndex++;
            }
            nextSweepTargetIndex.put(row, nextIndex);
        }
    }

    private List<ZombieSnapshot> zombieSnapshotsInRow(int row) {
        List<ZombieSnapshot> result = new ArrayList<>();
        for (ZombieSnapshot zombie : previousZombies) {
            if (zombie.row == row) {
                result.add(zombie);
            }
        }
        result.sort((first, second) ->
                Double.compare(first.column, second.column));
        return result;
    }

    private void playZombieHitEffect(ZombieSnapshot zombie) {
        if (zombie == null) {
            return;
        }
        try {
            OneShotPamActor hit = new OneShotPamActor(
                    player, MOWER_ZOMBIE_HIT, "animation");
            positionHitEffect(hit, zombie.row, zombie.column);
            addActor(hit);
        } catch (RuntimeException ignored) {
            // A missing optional effect must never stop gameplay.
        }
    }

    private List<ZombieSnapshot> snapshotZombies() {
        List<ZombieSnapshot> result = new ArrayList<>();
        for (Zombie zombie : game.getBoard().getZombies()) {
            if (zombie == null || zombie.isDead() || zombie.isRemoved()
                    || zombie.getType().isBoss()) {
                continue;
            }
            result.add(new ZombieSnapshot(
                    zombie.getLane(), zombie.getColumnPosition()));
        }
        return result;
    }

    private void positionMower(com.badlogic.gdx.scenes.scene2d.Actor actor,
            int row) {
        CellGeometry geometry = geometry();
        if (geometry == null || row < 0 || row >= BOARD_ROWS) {
            actor.setVisible(false);
            return;
        }

        float laneBottom = geometry.boardY
                + (BOARD_ROWS - 1 - row) * geometry.cellHeight;
        float footLine = laneBottom
                + geometry.cellHeight * MOWER_FOOT_LINE_RATIO;
        float width = geometry.cellWidth * MOWER_WIDTH_IN_CELLS;
        float height = geometry.cellHeight * MOWER_HEIGHT_IN_CELLS;
        float centerX = geometry.boardX
                - geometry.cellWidth * MOWER_CENTER_LEFT_OFFSET_IN_CELLS;

        actor.setBounds(centerX - width * 0.5f,
                footLine, width, height);
    }

    private void positionHitEffect(com.badlogic.gdx.scenes.scene2d.Actor actor,
            int row, double column) {
        CellGeometry geometry = geometry();
        if (geometry == null || row < 0 || row >= BOARD_ROWS) {
            actor.setVisible(false);
            return;
        }

        float centerX = geometry.boardX
                + ((float) column + 0.5f) * geometry.cellWidth;
        float laneBottom = geometry.boardY
                + (BOARD_ROWS - 1 - row) * geometry.cellHeight;
        float footLine = laneBottom + geometry.cellHeight * 0.18f;
        float size = Math.max(geometry.cellWidth, geometry.cellHeight)
                * HIT_EFFECT_SIZE_IN_CELLS;

        actor.setBounds(centerX - size * 0.5f,
                footLine - geometry.cellHeight * 0.08f,
                size, size);
    }

    private CellGeometry geometry() {
        if (layout == null || Gdx.graphics.getWidth() <= 0
                || Gdx.graphics.getHeight() <= 0) {
            return null;
        }
        float windowWidth = Gdx.graphics.getWidth();
        float windowHeight = Gdx.graphics.getHeight();
        float boardX = windowWidth * layout.left / layout.sourceWidth;
        float boardY = windowHeight
                * (layout.sourceHeight - layout.bottom)
                / layout.sourceHeight;
        float boardWidth = windowWidth
                * (layout.right - layout.left) / layout.sourceWidth;
        float boardHeight = windowHeight
                * (layout.bottom - layout.top) / layout.sourceHeight;
        return new CellGeometry(boardX, boardY,
                boardWidth / BOARD_COLUMNS, boardHeight / BOARD_ROWS);
    }

    private static String mowerPamFor(Chapter chapter) {
        if (chapter == null || chapter.getId() == null) {
            return MOWER_TUTORIAL;
        }
        switch (chapter.getId()) {
        case "ancient-egypt":
            return MOWER_EGYPT;
        case "frostbite-caves":
            return MOWER_ICEAGE;
        case "big-wave-beach":
            return MOWER_BEACH;
        case "dark-ages":
            return MOWER_DARK;
        default:
            return MOWER_TUTORIAL;
        }
    }

    private static BoardLayout layoutFor(Chapter chapter) {
        if (chapter == null || chapter.getId() == null) {
            return null;
        }
        switch (chapter.getId()) {
        case "ancient-egypt":
            return new BoardLayout(1024f, 768f,
                    256f, 200f, 994f, 688f);
        case "frostbite-caves":
            return new BoardLayout(1022f, 785f,
                    256f, 205f, 984f, 688f);
        case "big-wave-beach":
            return new BoardLayout(1024f, 768f,
                    256f, 200f, 994f, 688f);
        case "dark-ages":
            return new BoardLayout(1024f, 768f,
                    256f, 200f, 994f, 688f);
        default:
            return null;
        }
    }

    private static final class OneShotPamActor extends Stack {
        private final PamAnimationActor animation;
        private final float durationSeconds;
        private float elapsedSeconds;

        private OneShotPamActor(PamPlayer player, String pamPath, String clip) {
            setTouchable(Touchable.disabled);
            animation = new PamAnimationActor(player, pamPath, clip);
            add(animation);
            durationSeconds = Math.max(0.05f,
                    player.clipDurationSeconds(pamPath, clip));
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            elapsedSeconds += Math.max(0f, delta);
            if (elapsedSeconds >= durationSeconds) {
                remove();
            }
        }

        @Override
        protected void sizeChanged() {
            super.sizeChanged();
            animation.setBounds(0f, 0f, getWidth(), getHeight());
        }
    }

    private static final class ZombieSnapshot {
        private final int row;
        private final double column;

        private ZombieSnapshot(int row, double column) {
            this.row = row;
            this.column = column;
        }
    }

    private static final class CellGeometry {
        private final float boardX;
        private final float boardY;
        private final float cellWidth;
        private final float cellHeight;

        private CellGeometry(float boardX, float boardY,
                float cellWidth, float cellHeight) {
            this.boardX = boardX;
            this.boardY = boardY;
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
        }
    }

    private static final class BoardLayout {
        private final float sourceWidth;
        private final float sourceHeight;
        private final float left;
        private final float top;
        private final float right;
        private final float bottom;

        private BoardLayout(float sourceWidth, float sourceHeight,
                float left, float top, float right, float bottom) {
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
