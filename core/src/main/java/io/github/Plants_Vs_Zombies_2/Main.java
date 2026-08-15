package io.github.Plants_Vs_Zombies_2;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.view.screens.PvzSkinCompatibility;
import io.github.Plants_Vs_Zombies_2.view.screens.ScreenNavigator;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

/**
 * LibGDX entry point.
 *
 * <p>The terminal launcher is intentionally kept separate in
 * {@link ConsoleLauncher}. This class owns only graphical resources and the
 * graphical screen lifecycle.</p>
 */
public class Main extends Game {
    private static final String PVZ_ASSETS_ROOT = "pvz-assets";

    private Skin skin;
    private TextureBank textureBank;
    private ScreenNavigator screenNavigator;

    @Override
    public void create() {
        skin = PvzSkin.get();
        PvzSkinCompatibility.installMissingStyles(skin);

        // Keep a single libPVZ texture bank for the whole application. The
        // Scene2D screens only borrow TextureRegions from it; they do not own
        // or dispose the atlas textures themselves.
        textureBank = new TextureBank(
                "768", Gdx.files.internal(PVZ_ASSETS_ROOT));

        screenNavigator = new ScreenNavigator(this, skin, textureBank);
        screenNavigator.showStartupScreen();
    }

    @Override
    public void render() {
        // Controllers still change model.App's current Menu. Synchronizing here
        // makes those existing controller transitions automatically switch the
        // graphical screen too.
        if (screenNavigator != null) {
            screenNavigator.synchronizeWithModel();
        }
        if (textureBank != null) {
            textureBank.update();
        }
        super.render();
    }

    @Override
    public void dispose() {
        if (screenNavigator != null) {
            screenNavigator.dispose();
        }
        UserManager.saveAllUsers();
        if (textureBank != null) {
            textureBank.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
    }
}
