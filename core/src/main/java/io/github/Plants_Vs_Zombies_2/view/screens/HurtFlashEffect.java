package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.Gdx;

/**
 * Shared timing for the brief damage flash used by board entities.
 *
 * <p>The effect deliberately uses fairly slow on/off steps so repeated hits
 * read as a hit reaction instead of a rapid strobe.</p>
 */
final class HurtFlashEffect {
    static final float DURATION_SECONDS = 0.80f;
    private static final float FLASH_STEP_SECONDS = 0.20f;
    private static final float BRIGHT_OVERLAY_ALPHA = 0.42f;

    private HurtFlashEffect() {
    }

    static float start(float remainingSeconds) {
        return remainingSeconds > 0f
                ? remainingSeconds
                : DURATION_SECONDS;
    }

    static float advance(float remainingSeconds, float deltaSeconds) {
        if (remainingSeconds <= 0f) {
            return 0f;
        }
        return Math.max(0f, remainingSeconds - Math.max(0f, deltaSeconds));
    }

    static float realFrameDeltaSeconds() {
        if (Gdx.graphics == null) {
            return 0f;
        }
        return Math.min(1f / 15f,
                Math.max(0f, Gdx.graphics.getDeltaTime()));
    }

    static float overlayAlpha(float remainingSeconds) {
        if (remainingSeconds <= 0f) {
            return 0f;
        }
        float elapsed = DURATION_SECONDS - remainingSeconds;
        int phase = (int) (elapsed / FLASH_STEP_SECONDS);
        return phase % 2 == 0 ? BRIGHT_OVERLAY_ALPHA : 0f;
    }
}