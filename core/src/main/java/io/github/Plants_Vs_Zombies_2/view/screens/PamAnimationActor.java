package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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
    private String loopingClip;
    private Rectangle loopingAnimationBounds;
    private final boolean pingPong;
    private float loopingClipDurationSeconds;
    private String oneShotClip;
    private Rectangle oneShotAnimationBounds;
    private float oneShotDurationSeconds;
    private float oneShotStateTime;
    private Runnable oneShotCompletion;
    private float stateTime;
    private float hurtFlashRemainingSeconds;

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
        this.pingPong = pingPong;
        setLoopingClip(clip);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (oneShotClip != null) {
            oneShotStateTime += Math.max(0f, delta);
            if (oneShotStateTime >= oneShotDurationSeconds) {
                Runnable completion = oneShotCompletion;
                oneShotClip = null;
                oneShotAnimationBounds = null;
                oneShotStateTime = 0f;
                oneShotCompletion = null;
                stateTime = 0f;
                if (completion != null) {
                    completion.run();
                }
            }
        } else {
            stateTime += delta;
        }
        hurtFlashRemainingSeconds = HurtFlashEffect.advance(
                hurtFlashRemainingSeconds,
                HurtFlashEffect.realFrameDeltaSeconds());
    }

    void flashHurt() {
        hurtFlashRemainingSeconds = HurtFlashEffect.start(
                hurtFlashRemainingSeconds);
    }

    /** Plays a non-looping clip once, then resumes the requested idle clip. */
    boolean playOnce(String clip, String idleClipAfter) {
        return playOnce(clip, idleClipAfter, null);
    }

    /**
     * Plays a non-looping clip once and invokes {@code completion} on the frame
     * the clip ends, immediately before the actor resumes its idle clip.
     */
    boolean playOnce(String clip, String idleClipAfter, Runnable completion) {
        if (clip == null || clip.isBlank()
                || idleClipAfter == null || idleClipAfter.isBlank()) {
            return false;
        }
        Rectangle specialBounds = player.bounds(pamPath, clip);
        float duration = player.clipDurationSeconds(pamPath, clip);
        if (specialBounds == null || specialBounds.width <= 0f
                || specialBounds.height <= 0f || duration <= 0f) {
            return false;
        }
        setLoopingClip(idleClipAfter);
        oneShotClip = clip;
        oneShotAnimationBounds = specialBounds;
        oneShotDurationSeconds = Math.max(0.01f, duration);
        oneShotStateTime = 0f;
        oneShotCompletion = completion;
        return true;
    }

    private void setLoopingClip(String clip) {
        loopingClip = clip;
        loopingAnimationBounds = player.bounds(pamPath, clip);
        loopingClipDurationSeconds = pingPong
                ? Math.max(0.01f, player.clipDurationSeconds(pamPath, clip))
                : 0f;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Keep a stable plant scale while a one-shot clip plays. Production
        // clips can have wider motion bounds than idle; scaling to those
        // bounds would make the plant visibly shrink for the special.
        Rectangle animationBounds = loopingAnimationBounds != null
                ? loopingAnimationBounds
                : oneShotAnimationBounds;
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

        batch.flush();
        batch.setTransformMatrix(scaledTransform);

        String clip = oneShotClip != null ? oneShotClip : loopingClip;
        float playbackTime = oneShotClip != null
                ? Math.min(oneShotStateTime, oneShotDurationSeconds)
                : stateTime;
        boolean loop = oneShotClip == null;
        if (oneShotClip == null && pingPong) {
            float cycle = loopingClipDurationSeconds * 2f;
            float phase = stateTime % cycle;
            playbackTime = phase <= loopingClipDurationSeconds
                    ? phase
                    : cycle - phase;
            loop = false;
        }
        Color actorColor = getColor();
        batch.setColor(actorColor.r, actorColor.g, actorColor.b,
                actorColor.a * parentAlpha);
        player.draw(batch, pamPath, clip, playbackTime,
                centerX, centerY, loop);

        float hurtOverlayAlpha = HurtFlashEffect.overlayAlpha(
                hurtFlashRemainingSeconds);
        if (hurtOverlayAlpha > 0f) {
            batch.flush();
            int oldBlendSrc = batch.getBlendSrcFunc();
            int oldBlendDst = batch.getBlendDstFunc();
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 1f, 1f,
                    actorColor.a * parentAlpha * hurtOverlayAlpha);
            player.draw(batch, pamPath, clip, playbackTime,
                    centerX, centerY, loop);
            batch.flush();
            batch.setBlendFunction(oldBlendSrc, oldBlendDst);
        }

        batch.flush();
        batch.setTransformMatrix(oldTransform);
        batch.setColor(oldColor);
    }
}
