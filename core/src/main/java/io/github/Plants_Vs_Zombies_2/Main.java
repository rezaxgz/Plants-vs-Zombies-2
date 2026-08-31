package io.github.Plants_Vs_Zombies_2;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.network.session.RemoteAccountSession;
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
    private RemoteAccountSession remoteAccountSession;

    @Override
    public void create() {
        // Phase 3 graphical accounts are server-owned. Legacy controllers may
        // still call UserManager.saveAllUsers(), but those calls must never
        // rewrite the unrelated local console database in this process.
        UserManager.useRemoteOnlyMode();
        skin = PvzSkin.get();
        PvzSkinCompatibility.installMissingStyles(skin);

        // Keep a single libPVZ texture bank for the whole application. The
        // Scene2D screens only borrow TextureRegions from it; they do not own
        // or dispose the atlas textures themselves.
        textureBank = new TextureBank(
                "768", Gdx.files.internal(PVZ_ASSETS_ROOT));

        // A legacy SessionManager username is not proof of a remote session.
        // Keep terminal behavior intact, but graphical startup always requires
        // a fresh server login until persistent remote tokens exist.
        App.getInstance().setLoggedInUser(null);
        remoteAccountSession = RemoteAccountSession.fromSystemProperties();
        screenNavigator = new ScreenNavigator(
                this, skin, textureBank, remoteAccountSession);
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
        if (remoteAccountSession != null) {
            // Closing the socket also releases the server's online-session
            // ownership if the window closes before an explicit logout.
            remoteAccountSession.close();
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
