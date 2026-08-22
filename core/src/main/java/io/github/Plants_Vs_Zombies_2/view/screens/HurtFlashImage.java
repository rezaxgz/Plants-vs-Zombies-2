package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

/** Fallback image actor that supports the same damage flash as PAM artwork. */
final class HurtFlashImage extends Image {
    private float hurtFlashRemainingSeconds;

    HurtFlashImage(TextureRegion region) {
        super(region);
    }

    void flashHurt() {
        hurtFlashRemainingSeconds = HurtFlashEffect.start(
                hurtFlashRemainingSeconds);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        hurtFlashRemainingSeconds = HurtFlashEffect.advance(
                hurtFlashRemainingSeconds,
                HurtFlashEffect.realFrameDeltaSeconds());
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color oldBatchColor = new Color(batch.getColor());
        super.draw(batch, parentAlpha);

        float hurtOverlayAlpha = HurtFlashEffect.overlayAlpha(
                hurtFlashRemainingSeconds);
        if (hurtOverlayAlpha <= 0f) {
            batch.setColor(oldBatchColor);
            return;
        }

        batch.flush();
        int oldBlendSrc = batch.getBlendSrcFunc();
        int oldBlendDst = batch.getBlendDstFunc();
        float oldActorAlpha = getColor().a;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        getColor().a = oldActorAlpha * hurtOverlayAlpha;
        super.draw(batch, parentAlpha);
        batch.flush();
        getColor().a = oldActorAlpha;
        batch.setBlendFunction(oldBlendSrc, oldBlendDst);
        batch.setColor(oldBatchColor);
    }
}