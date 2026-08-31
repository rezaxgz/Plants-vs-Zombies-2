package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.ArrayDeque;
import java.util.Deque;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import io.github.Plants_Vs_Zombies_2.Main;
import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.controller.MainController;
import io.github.Plants_Vs_Zombies_2.controller.TravelLogMenuController;
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
import io.github.Plants_Vs_Zombies_2.model.game.plantSelector.PlantSelection;
import io.github.Plants_Vs_Zombies_2.model.game.save.SavedGameManager;
import io.github.Plants_Vs_Zombies_2.model.roadmap.AdventureSession;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Chapter;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Level;
import io.github.Plants_Vs_Zombies_2.model.roadmap.SpecialLevelType;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchmakingClient;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.session.AccountSession;
import io.github.Plants_Vs_Zombies_2.network.session.AuthenticationErrorMessages;
import io.github.Plants_Vs_Zombies_2.network.session.ClientSessionState;
import io.github.Plants_Vs_Zombies_2.network.session.RemoteGameplayUserFactory;
import io.github.Plants_Vs_Zombies_2.network.session.RemoteGameplaySyncService;
import io.github.Plants_Vs_Zombies_2.network.session.UiDispatcher;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.ClientMatchmakingTransport;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.InvitationNotificationBridge;
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
    private final AccountSession accountSession;
    private final UiDispatcher uiDispatcher;
    private final InvitationNotificationBridge invitationBridge;
    private final RemoteGameplaySyncService gameplaySync;
    private Dialog invitationDialog;
    private final Deque<Menu> history = new ArrayDeque<>();

    private Menu displayedMenu;
    private boolean transientScreenVisible;
    private boolean logoutInProgress;
    private boolean disposed;
    private boolean exitInProgress;
    private String pendingAuthenticationNotice;

    public ScreenNavigator(Main game, Skin skin, TextureBank textureBank,
            AccountSession accountSession) {
        if (game == null || skin == null || textureBank == null
                || accountSession == null) {
            throw new IllegalArgumentException(
                    "game, skin and textureBank are required");
        }
        this.game = game;
        this.skin = skin;
        this.textureBank = textureBank;
        this.pamPlayer = new PamPlayer(
                textureBank, Gdx.files.internal(PVZ_ASSETS_ROOT));
        this.app = App.getInstance();
        this.accountSession = accountSession;
        this.uiDispatcher = UiDispatcher.libGdx();
        this.gameplaySync = new RemoteGameplaySyncService(accountSession, uiDispatcher);
        MatchmakingClient matchmakingClient = accountSession.getMatchmakingClient();
        this.invitationBridge = matchmakingClient == null ? null
                : new InvitationNotificationBridge(
                        new ClientMatchmakingTransport(matchmakingClient),
                        uiDispatcher, new InvitationNotificationBridge.Observer() {
                            @Override public void invitationChanged(
                                    InvitationNotificationBridge.InvitationView invitation) {
                                handleInvitationChanged(invitation);
                            }

                            @Override public void matchFound(MatchAssignment assignment) {
                                showMultiplayerPregame(assignment);
                            }
                        });
        accountSession.addStateListener((previous, current, failure) -> {
            if (previous == ClientSessionState.AUTHENTICATED
                    && current != ClientSessionState.AUTHENTICATED
                    && invitationBridge != null) {
                uiDispatcher.dispatch(invitationBridge::clearTransientState);
            }
            if (previous == ClientSessionState.AUTHENTICATED
                    && current != ClientSessionState.AUTHENTICATED) {
                uiDispatcher.dispatch(gameplaySync::detach);
            }
            if (previous == ClientSessionState.AUTHENTICATED
                    && current == ClientSessionState.DISCONNECTED) {
                uiDispatcher.dispatch(() -> handleAuthenticationLost(failure));
            }
        });
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

    public AccountSession getAccountSession() {
        return accountSession;
    }

    public UiDispatcher getUiDispatcher() {
        return uiDispatcher;
    }

    public RemoteGameplaySyncService getGameplaySync() { return gameplaySync; }

    public String consumeAuthenticationNotice() {
        String notice = pendingAuthenticationNotice;
        pendingAuthenticationNotice = null;
        return notice;
    }

    /**
     * Persistent remote login tokens are not implemented, so graphical startup
     * never treats SessionManager's stored local username as authenticated.
     */
    public void showStartupScreen() {
        history.clear();
        transientScreenVisible = false;
        app.setLoggedInUser(null);
        app.changeMenu(new SignUpMenu());
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
        if (logoutInProgress) {
            return;
        }
        if (invitationBridge != null) {
            invitationBridge.clearTransientState();
        }
        logoutInProgress = true;
        flushGameplay().whenComplete((snapshot, syncFailure) ->
                accountSession.logout().whenComplete((ignored, logoutFailure) ->
                        uiDispatcher.dispatch(() -> finishRemoteLogout(
                                syncFailure != null ? syncFailure : logoutFailure))));
    }

    private void finishRemoteLogout(Throwable failure) {
        if (disposed) {
            return;
        }
        logoutInProgress = false;
        history.clear();
        transientScreenVisible = false;
        AdventureSession.getInstance().reset();
        app.setLoggedInUser(null);
        if (failure != null) {
            pendingAuthenticationNotice = "Logged out locally. "
                    + AuthenticationErrorMessages.forFailure(failure);
        }
        app.changeMenu(new LoginMenu());
        showCurrentMenu();
    }

    public void completeRemoteLogin(AccountProfile profile) {
        if (invitationBridge != null) {
            invitationBridge.clearTransientState();
        }
        io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot snapshot =
                accountSession.getGameplayStateSnapshot();
        User compatibilityUser = RemoteGameplayUserFactory.create(profile, snapshot);
        if (snapshot == null) {
            snapshot = new io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot(
                    0L, io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState
                            .fromUser(compatibilityUser));
        }
        app.setLoggedInUser(compatibilityUser);
        gameplaySync.attach(compatibilityUser, snapshot);
        history.clear();
        app.changeMenu(new MainMenu());
        showCurrentMenu();
    }

    public void showLoginAfterRegistration() {
        history.clear();
        app.changeMenu(new LoginMenu());
        pendingAuthenticationNotice = "Account created. Log in with your new credentials.";
        showCurrentMenu();
    }

    private void handleAuthenticationLost(Throwable failure) {
        if (disposed || app.getLoggedInUser() == null) {
            return;
        }
        if (invitationBridge != null) {
            invitationBridge.clearTransientState();
        }
        history.clear();
        transientScreenVisible = false;
        AdventureSession.getInstance().reset();
        app.setLoggedInUser(null);
        pendingAuthenticationNotice = failure == null
                ? "The server connection was lost. Please log in again."
                : AuthenticationErrorMessages.forFailure(failure);
        app.changeMenu(new LoginMenu());
        showCurrentMenu();
    }

    public void exitApplication() {
        if (exitInProgress) return;
        exitInProgress = true;
        flushGameplay().whenComplete((snapshot, failure) ->
                uiDispatcher.dispatch(() -> {
                    if (failure != null) {
                        pendingAuthenticationNotice =
                                "Gameplay changes could not be synchronized: "
                                        + AuthenticationErrorMessages.forFailure(failure);
                    }
                    UserManager.saveAllUsers();
                    app.stop();
                    Gdx.app.exit();
                }));
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

    /** Opens Stage 7 without replacing the existing single-player Play route. */
    public void showMultiplayerIZombieMenu() {
        showMultiplayerIZombieMenu(null);
    }

    public void showMultiplayerIZombieMenu(String status) {
        if (accountSession.getState() != ClientSessionState.AUTHENTICATED
                || accountSession.getMatchmakingClient() == null
                || accountSession.getMultiplayerGameClient() == null) {
            pendingAuthenticationNotice =
                    "Multiplayer requires an authenticated remote account connection.";
            returnToCurrentMenu();
            return;
        }
        showTransient(new MultiplayerIZombieMenuScreen(this, status));
    }

    public void showMultiplayerPregame(MatchAssignment assignment) {
        if (disposed || assignment == null || assignment.getMatchId() == null) {
            return;
        }
        if (accountSession.getState() != ClientSessionState.AUTHENTICATED
                || accountSession.getMultiplayerGameClient() == null) {
            showMultiplayerIZombieMenu(
                    "A match was found, but the remote account is no longer authenticated.");
            return;
        }
        showTransient(new MultiplayerPregameScreen(this, assignment));
    }

    public void showMultiplayerIZombieGame(MatchAssignment assignment,
            MatchStateSnapshot initialSnapshot) {
        if (disposed || assignment == null
                || accountSession.getState() != ClientSessionState.AUTHENTICATED) {
            return;
        }
        showTransient(new MultiplayerIZombieGameScreen(
                this, assignment, initialSnapshot));
    }

    /** Opens Adventure with the Travel Log already on its Minigames tab. */
    public void showAdventureTravelLogMinigames() {
        showTransient(new AdventureScreen(this, true));
    }

    /** Starts a Travel Log minigame and switches to its model-backed game. */
    public CommandResult startMinigameFromTravelLog(
            String minigameId, int levelNumber) {
        CommandResult result = TravelLogMenuController.startMinigameFromGui(
                minigameId, levelNumber);
        if (result.isSuccsesful()) {
            history.clear();
            showCurrentMenu();
        }
        return result;
    }

    /** Finishes a minigame session and returns to the Minigames Travel Log. */
    public void exitMinigameToTravelLog() {
        Menu current = app.getCurrentMenu();
        if (current instanceof GameMenu) {
            ((GameMenu) current).synchronizeProgress();
        }
        history.clear();
        app.changeMenu(new MainMenu());
        displayedMenu = app.getCurrentMenu();
        showAdventureTravelLogMinigames();
    }

    /** Leaves an active game behind and makes Adventure's Back return to Main. */
    public void exitGameToAdventure() {
        history.clear();
        app.changeMenu(new MainMenu());
        displayedMenu = app.getCurrentMenu();
        showAdventureScreen();
    }

    public void showLevelGamePreview(Chapter chapter, Level level) {
        if (chapter == null || level == null) {
            throw new IllegalArgumentException("chapter and level cannot be null");
        }

        User user = app.getLoggedInUser();
        if (user == null) {
            showTransient(new GameScreen(this, chapter, level));
            return;
        }

        GameMenu savedGame = SavedGameManager.loadAdventureGame(
                user, chapter.getId(), level.getNumber());
        if (savedGame != null) {
            app.changeMenu(savedGame);
            showCurrentMenu();
            return;
        }

        // Match the phase-one level-start rules. Conveyor Belt levels choose
        // cards from the belt and Locked Plants levels use a forced loadout,
        // so both can enter the real model-backed level immediately.
        SpecialLevelType specialType = level.getSpecialLevelType();
        if (specialType == SpecialLevelType.CONVEYOR_BELT
                || specialType == SpecialLevelType.LOCKED_PLANTS) {
            MainController.launchAdventureGameFromGui(
                    chapter, level, null);
            showCurrentMenu();
            return;
        }

        PlantSelection selection = new PlantSelection(
                user.getPlantCollection(), level);
        if (selection.shouldStartAutomatically()) {
            selection.selectAllAvailable();
            MainController.launchAdventureGameFromGui(
                    chapter, level, selection);
            showCurrentMenu();
            return;
        }

        showTransient(new GameScreen(
                this, chapter, level, selection, true));
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
        gameplaySync.observeAndSynchronize();
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
        dismissInvitationDialog();
        Screen previous = game.getScreen();
        game.setScreen(next);
        if (previous != null && previous != next) {
            previous.dispose();
        }
        showCurrentInvitationOn(next);
    }

    private void handleInvitationChanged(
            InvitationNotificationBridge.InvitationView invitation) {
        if (disposed) return;
        dismissInvitationDialog();
        if (invitation != null) {
            showInvitationOn(game.getScreen(), invitation);
        }
    }

    private void showCurrentInvitationOn(Screen current) {
        if (disposed || invitationBridge == null
                || accountSession.getState() != ClientSessionState.AUTHENTICATED
                || !(current instanceof AbstractScreen host)
                || current instanceof MultiplayerPregameScreen
                || current instanceof MultiplayerIZombieGameScreen
                || current instanceof LoginScreen || current instanceof SignUpScreen) {
            return;
        }
        InvitationNotificationBridge.InvitationView invitation =
                invitationBridge.getCurrentInvitation();
        if (invitation == null) return;
        showInvitationOn(current, invitation);
    }

    private void showInvitationOn(Screen current,
            InvitationNotificationBridge.InvitationView invitation) {
        if (invitation == null || disposed
                || accountSession.getState() != ClientSessionState.AUTHENTICATED
                || !(current instanceof AbstractScreen host)
                || current instanceof MultiplayerPregameScreen
                || current instanceof MultiplayerIZombieGameScreen
                || current instanceof LoginScreen || current instanceof SignUpScreen) return;
        long secondsLeft = Math.max(0L,
                (invitation.expiresAtEpochMillis() - System.currentTimeMillis() + 999L) / 1000L);
        Dialog dialog = new Dialog("Multiplayer invitation", skin) {
            @Override protected void result(Object object) {
                if (disposed || invitationBridge == null) return;
                if (Boolean.TRUE.equals(object)) {
                    invitationBridge.accept();
                } else {
                    invitationBridge.reject();
                }
            }
        };
        dialog.text(invitation.inviter() + " invited you to I, Zombie.\n"
                + "Expires in about " + secondsLeft + "s.\n"
                + invitation.status());
        dialog.button("Accept", Boolean.TRUE);
        dialog.button("Reject", Boolean.FALSE);
        dialog.setModal(true);
        invitationDialog = dialog;
        dialog.show(host.stage);
    }

    private void dismissInvitationDialog() {
        if (invitationDialog != null) {
            invitationDialog.hide();
            invitationDialog = null;
        }
    }

    public void dispose() {
        disposed = true;
        gameplaySync.flushBestEffortOnShutdown();
        gameplaySync.close();
        dismissInvitationDialog();
        if (invitationBridge != null) {
            invitationBridge.close();
        }
        Screen current = game.getScreen();
        if (current != null) {
            current.dispose();
        }
    }

    private java.util.concurrent.CompletableFuture<io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot>
            flushGameplay() {
        if (!gameplaySync.getStatus().attached()) {
            return java.util.concurrent.CompletableFuture.completedFuture(
                    accountSession.getGameplayStateSnapshot());
        }
        return gameplaySync.flush();
    }
}
