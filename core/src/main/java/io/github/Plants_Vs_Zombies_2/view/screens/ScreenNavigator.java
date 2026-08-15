package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.ArrayDeque;
import java.util.Deque;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import io.github.Plants_Vs_Zombies_2.Main;
import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.menu.CollectionMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.GameMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.GreenhouseMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.LeaderboardMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.LoginMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.MainMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.Menu;
import io.github.Plants_Vs_Zombies_2.model.menu.NetworkMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.NewsMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.PlantSelectionMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.ProfileMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.SettingsMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.ShopMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.SignUpMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.TravelLogMenu;
import io.github.Plants_Vs_Zombies_2.model.roadmap.AdventureSession;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

/**
 * Bridges the existing model-menu state machine to LibGDX Screen objects.
 */
public final class ScreenNavigator {
    private static final String PVZ_ASSETS_ROOT = "pvz-assets";

    private final Main game;
    private final Skin skin;
    private final TextureBank textureBank;
    private final PamPlayer pamPlayer;
    private final App app;
    private final Deque<Menu> history = new ArrayDeque<>();

    private Menu displayedMenu;
    private boolean transientScreenVisible;

    public ScreenNavigator(Main game, Skin skin, TextureBank textureBank) {
        if (game == null || skin == null || textureBank == null) {
            throw new IllegalArgumentException(
                    "game, skin and textureBank are required");
        }
        this.game = game;
        this.skin = skin;
        this.textureBank = textureBank;
        this.pamPlayer = new PamPlayer(
                textureBank, Gdx.files.internal(PVZ_ASSETS_ROOT));
        this.app = App.getInstance();
    }

    public Skin getSkin() {
        return skin;
    }

    public TextureBank getTextureBank() {
        return textureBank;
    }

    public PamPlayer getPamPlayer() {
        return pamPlayer;
    }

    /**
     * Chooses the first graphical screen from the restored login session. A
     * valid persistent session starts directly in the main menu; otherwise
     * the application always starts at registration.
     */
    public void showStartupScreen() {
        history.clear();
        transientScreenVisible = false;
        if (app.getLoggedInUser() == null) {
            app.changeMenu(new SignUpMenu());
        } else {
            app.changeMenu(new MainMenu());
        }
        showCurrentMenu();
    }

    public void showCurrentMenu() {
        transientScreenVisible = false;
        Menu currentMenu = app.getCurrentMenu();
        displayedMenu = currentMenu;
        switchScreen(createScreen(currentMenu));
    }

    /**
     * Enter a model-backed menu from a graphical button.
     */
    public void navigate(Menu destination) {
        if (destination == null) {
            throw new IllegalArgumentException("destination cannot be null");
        }
        Menu current = app.getCurrentMenu();
        if (current != destination) {
            history.push(current);
        }
        app.changeMenu(destination);
        showCurrentMenu();
    }

    /**
     * Graphical back navigation prefers the exact previous Menu object. This
     * preserves parent GameMenu instances when opening collection/greenhouse
     * from an active game. If there is no GUI history, the existing model
     * Menu.exit() behavior is used.
     */
    public void back() {
        transientScreenVisible = false;
        if (!history.isEmpty()) {
            app.changeMenu(history.pop());
            showCurrentMenu();
            return;
        }
        exitCurrentMenu();
    }

    public void exitCurrentMenu() {
        transientScreenVisible = false;
        Menu current = app.getCurrentMenu();
        current.exit();
        if (!app.isRunning()) {
            exitApplication();
            return;
        }
        showCurrentMenu();
    }

    /**
     * Graphical logout deliberately lands on Login rather than SignUp. The
     * phase-one App.logout() behavior is left untouched for terminal commands.
     */
    public void logout() {
        history.clear();
        transientScreenVisible = false;
        AdventureSession.getInstance().reset();
        UserManager.saveAllUsers();
        app.logout();
        app.changeMenu(new LoginMenu());
        showCurrentMenu();
    }

    public void exitApplication() {
        UserManager.saveAllUsers();
        app.stop();
        Gdx.app.exit();
    }

    /**
     * Shows a Phase-2-only overlay/screen without inventing a new model Menu.
     * Pause/start/result screens can therefore be added without changing the
     * phase-one model hierarchy.
     */
    public void showTransient(AbstractScreen screen) {
        transientScreenVisible = true;
        switchScreen(screen);
    }

    public void returnToCurrentMenu() {
        showCurrentMenu();
    }

    public void showAdventureScreen() {
        showTransient(new AdventureScreen(this));
    }

    public void showPauseScreen() {
        showTransient(new PauseScreen(this));
    }

    public void showLevelStartScreen() {
        showTransient(new LevelStartScreen(this));
    }

    public void showGameResultScreen(boolean won) {
        showTransient(new GameResultScreen(this, won));
    }

    /**
     * Existing controllers can continue to call App.changeMenu(). The next
     * render automatically notices that and displays the corresponding GUI.
     */
    public void synchronizeWithModel() {
        if (transientScreenVisible) {
            return;
        }
        Menu current = app.getCurrentMenu();
        if (current != displayedMenu) {
            // The change came from an existing controller rather than a GUI
            // navigation button. Old graphical history is no longer a valid
            // representation of the model state, so discard it.
            history.clear();
            displayedMenu = current;
            switchScreen(createScreen(current));
        }
    }

    private AbstractScreen createScreen(Menu menu) {
        if (menu instanceof SignUpMenu) {
            return new SignUpScreen(this);
        }
        if (menu instanceof LoginMenu) {
            return new LoginScreen(this);
        }
        if (menu instanceof MainMenu) {
            return new MainMenuScreen(this);
        }
        if (menu instanceof PlantSelectionMenu) {
            return new PlantSelectionScreen(this);
        }
        if (menu instanceof GameMenu) {
            return new GameScreen(this);
        }
        if (menu instanceof CollectionMenu) {
            return new CollectionScreen(this);
        }
        if (menu instanceof GreenhouseMenu) {
            return new GreenhouseScreen(this);
        }
        if (menu instanceof ShopMenu) {
            return new ShopScreen(this);
        }
        if (menu instanceof LeaderboardMenu) {
            return new LeaderboardScreen(this);
        }
        if (menu instanceof TravelLogMenu) {
            return new TravelLogScreen(this);
        }
        if (menu instanceof NewsMenu) {
            return new NewsScreen(this);
        }
        if (menu instanceof SettingsMenu) {
            return new SettingsScreen(this);
        }
        if (menu instanceof ProfileMenu) {
            return new ProfileScreen(this);
        }
        if (menu instanceof NetworkMenu) {
            return new NetworkScreen(this);
        }
        throw new IllegalStateException(
                "No graphical screen registered for menu: "
                        + menu.getClass().getName());
    }

    private void switchScreen(Screen next) {
        Screen previous = game.getScreen();
        game.setScreen(next);
        if (previous != null && previous != next) {
            previous.dispose();
        }
    }

    public void dispose() {
        Screen current = game.getScreen();
        if (current != null) {
            current.dispose();
        }
    }
}
