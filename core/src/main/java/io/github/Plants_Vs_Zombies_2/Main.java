package io.github.Plants_Vs_Zombies_2;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.view.screens.ScreenNavigator;
import pvz.skin.PvzSkin;

/**
 * LibGDX entry point.
 *
 * <p>The terminal launcher is intentionally kept separate in
 * {@link ConsoleLauncher}. This class owns only graphical resources and the
 * graphical screen lifecycle.</p>
 */
public class Main extends Game {
    private Skin skin;
    private ScreenNavigator screenNavigator;

    @Override
    public void create() {
        skin = PvzSkin.get();
        screenNavigator = new ScreenNavigator(this, skin);
        screenNavigator.showCurrentMenu();
    }

    @Override
    public void render() {
        // Controllers still change model.App's current Menu. Synchronizing here
        // makes those existing controller transitions automatically switch the
        // graphical screen too.
        if (screenNavigator != null) {
            screenNavigator.synchronizeWithModel();
        }
        super.render();
    }

    @Override
    public void dispose() {
        if (screenNavigator != null) {
            screenNavigator.dispose();
        }
        UserManager.saveAllUsers();
        if (skin != null) {
            skin.dispose();
        }
    }
}
