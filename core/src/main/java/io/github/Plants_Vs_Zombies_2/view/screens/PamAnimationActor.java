package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;

import pvz.libpvz.pam.PamPlayer;

/**
 * Small Scene2D bridge for native PvZ2 PAM artwork rendered by libPVZ.
 *
 * <p>A specific PAM clip is rendered so UI artwork can stay in the intended
 * state instead of accidentally playing transition clips such as "open".</p>
 */
final class PamAnimationActor extends Actor {
    private final PamPlayer player;
    private final String pamPath;
    private final String clip;
    private final Rectangle animationBounds;
    private final boolean pingPong;
    private final float clipDurationSeconds;
    private float stateTime;

    PamAnimationActor(PamPlayer player, String pamPath, String clip) {
        this(player, pamPath, clip, false);
    }

    PamAnimationActor(PamPlayer player, String pamPath, String clip,
            boolean pingPong) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        if (pamPath == null || pamPath.isBlank()) {
            throw new IllegalArgumentException("pamPath cannot be blank");
        }
        if (clip == null || clip.isBlank()) {
            throw new IllegalArgumentException("clip cannot be blank");
        }
        this.player = player;
        this.pamPath = pamPath;
        this.clip = clip;
        this.animationBounds = player.bounds(pamPath, clip);
        this.pingPong = pingPong;
        this.clipDurationSeconds = pingPong
                ? Math.max(0.01f, player.clipDurationSeconds(pamPath, clip))
                : 0f;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (animationBounds == null
                || animationBounds.width <= 0f
                || animationBounds.height <= 0f
                || getWidth() <= 0f
                || getHeight() <= 0f) {
            return;
        }

        float scale = Math.min(
                getWidth() / animationBounds.width,
                getHeight() / animationBounds.height);

        float centerX = getX() + getWidth() * 0.5f;
        float centerY = getY() + getHeight() * 0.5f;

        Color oldColor = new Color(batch.getColor());
        Matrix4 oldTransform = new Matrix4(batch.getTransformMatrix());
        Matrix4 scaledTransform = new Matrix4(oldTransform);
        scaledTransform.translate(centerX, centerY, 0f);
        scaledTransform.scale(scale, scale, 1f);
        scaledTransform.translate(-centerX, -centerY, 0f);

        Color actorColor = getColor();
        batch.setColor(actorColor.r, actorColor.g, actorColor.b,
                actorColor.a * parentAlpha);
        batch.flush();
        batch.setTransformMatrix(scaledTransform);

        float playbackTime = stateTime;
        boolean loop = true;
        if (pingPong) {
            float cycle = clipDurationSeconds * 2f;
            float phase = stateTime % cycle;
            playbackTime = phase <= clipDurationSeconds
                    ? phase
                    : cycle - phase;
            loop = false;
        }
        player.draw(batch, pamPath, clip, playbackTime,
                centerX, centerY, loop);

        batch.flush();
        batch.setTransformMatrix(oldTransform);
        batch.setColor(oldColor);
    }
}
