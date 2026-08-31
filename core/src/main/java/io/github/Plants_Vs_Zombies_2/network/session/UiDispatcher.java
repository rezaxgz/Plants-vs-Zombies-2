package io.github.Plants_Vs_Zombies_2.network.session;

import com.badlogic.gdx.Gdx;

@FunctionalInterface
public interface UiDispatcher {
    void dispatch(Runnable runnable);

    static UiDispatcher direct() {
        return Runnable::run;
    }

    static UiDispatcher libGdx() {
        return runnable -> {
            if (Gdx.app == null) {
                throw new IllegalStateException("LibGDX application is not initialized");
            }
            Gdx.app.postRunnable(runnable);
        };
    }
}
