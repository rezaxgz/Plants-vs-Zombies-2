package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.model.game.GameStatus;

/**
 * End-of-level result window shown after an adventure game is won or lost.
 * The visual language intentionally matches the Phase-2 objective and pause
 * dialogs: brown frame, orange header, cream body, and purple primary action.
 */
final class GameResultOverlay extends Group {
    private static final String ACTOR_NAME = "game-result-overlay";

    private static final float PANEL_WIDTH = 620f;
    private static final float PANEL_HEIGHT = 330f;
    private static final float HEADER_HEIGHT = 68f;
    private static final float BUTTON_WIDTH = 176f;
    private static final float BUTTON_HEIGHT = 54f;

    private static final Color BACKDROP = new Color(0f, 0f, 0f, 0.36f);
    private static final Color HEADER = new Color(1f, 0.63f, 0.04f, 1f);
    private static final Color CONTENT = new Color(0.96f, 0.91f, 0.74f, 1f);
    private static final Color BODY_TEXT = new Color(0.28f, 0.20f, 0.09f, 1f);
    private static final Color WIN_TEXT = new Color(0.14f, 0.56f, 0.16f, 1f);
    private static final Color LOSS_TEXT = new Color(0.72f, 0.10f, 0.08f, 1f);

    private Texture pixelTexture;
    private final Runnable onRestart;
    private final Runnable onExit;
    private boolean actionTaken;

    GameResultOverlay(Skin skin, GameStatus status,
            float screenWidth, float screenHeight,
            Runnable onRestart, Runnable onExit) {
        if (skin == null || status == null
                || onRestart == null || onExit == null) {
            throw new IllegalArgumentException(
                    "skin, status, and result actions are required");
        }
        if (status == GameStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "result menu cannot be shown for an active game");
        }

        this.onRestart = onRestart;
        this.onExit = onExit;
        setName(ACTOR_NAME);
        setBounds(0f, 0f, screenWidth, screenHeight);
        setTouchable(Touchable.enabled);

        createPixelTexture();
        installInputBlocker();
        installCard(skin, status, screenWidth, screenHeight);
    }

    private void createPixelTexture() {
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        pixelTexture = new Texture(pixel);
        pixel.dispose();
    }

    private void installInputBlocker() {
        Image shade = new Image(solid(BACKDROP));
        shade.setScaling(Scaling.stretch);
        shade.setBounds(0f, 0f, getWidth(), getHeight());
        shade.setTouchable(Touchable.enabled);
        shade.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y,
                    int pointer, int button) {
                return true;
            }
        });
        addActor(shade);
    }

    private void installCard(Skin skin, GameStatus status,
            float screenWidth, float screenHeight) {
        boolean won = status == GameStatus.WON;
        float panelX = (screenWidth - PANEL_WIDTH) * 0.5f;
        float panelY = (screenHeight - PANEL_HEIGHT) * 0.5f + 10f;

        Table frame = new Table();
        frame.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"));
        frame.pad(19f);
        frame.setBounds(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        Table header = new Table();
        header.setBackground(solid(HEADER));
        Label title = new Label(
                won ? "LEVEL COMPLETE" : "LEVEL FAILED",
                skin, "big_outline");
        title.setAlignment(Align.center);
        title.setFontScale(0.82f);
        header.add(title).grow();
        frame.add(header).growX().height(HEADER_HEIGHT).row();

        Table body = new Table();
        body.setBackground(solid(CONTENT));
        body.pad(24f, 30f, 24f, 30f);

        Label result = new Label(
                won ? "YOU WON!" : "YOU LOST!",
                skin, "big_outline");
        result.setAlignment(Align.center);
        result.setColor(won ? WIN_TEXT : LOSS_TEXT);
        result.setFontScale(0.86f);
        body.add(result).growX().height(60f).row();

        Label description = new Label(
                won
                        ? "The lawn is safe. Great job!"
                        : "The zombies got through your defenses.",
                skin, "secondary");
        description.setColor(BODY_TEXT);
        description.setAlignment(Align.center);
        description.setWrap(true);
        description.setFontScale(0.96f);
        body.add(description).growX().height(54f).padBottom(12f).row();

        Table actions = new Table();
        if (!won) {
            TextButton restart = new TextButton(
                    "RESTART LEVEL", skin, "brown");
            restart.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    takeAction(onRestart);
                }
            });
            actions.add(restart)
                    .width(BUTTON_WIDTH).height(BUTTON_HEIGHT)
                    .padRight(14f);
        }

        TextButton exit = new TextButton("EXIT", skin, "purple");
        exit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                takeAction(onExit);
            }
        });
        actions.add(exit).width(BUTTON_WIDTH).height(BUTTON_HEIGHT);
        body.add(actions).center();

        frame.add(body).grow();
        addActor(frame);
    }

    private void takeAction(Runnable action) {
        if (actionTaken) {
            return;
        }
        actionTaken = true;
        remove();
        disposePixelTexture();
        action.run();
    }

    private Drawable solid(Color color) {
        return new TextureRegionDrawable(
                new TextureRegion(pixelTexture)).tint(color);
    }

    private void disposePixelTexture() {
        if (pixelTexture != null) {
            pixelTexture.dispose();
            pixelTexture = null;
        }
    }
}
