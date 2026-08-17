package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.Locale;
import java.util.function.Supplier;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.menu.Menu;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/**
 * Shared Scene2D shell for every graphical menu.
 */
public abstract class AbstractScreen implements Screen {
    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    private static final String COIN_IMAGE_ID =
            "IMAGE_UI_THYMED_EVENTS_ECS_CONVRT_COIN";
    private static final String DIAMOND_IMAGE_ID =
            "IMAGE_EFFECTS_COIN_DIAMOND_COIN_DIAMOND_141X146";
    private static final String CURRENCY_BAR_IMAGE_ID =
            "IMAGE_UI_GENERIC_BUTTON_GENERIC_LTECURRENCY";
    private static final String PLUS_IMAGE_ID =
            "IMAGE_UI_HUD_INGAME_COIN_BUY";

    protected static final int DEBUG_COIN_INCREMENT = 100;
    protected static final int DEBUG_DIAMOND_INCREMENT = 5;
    protected static final float WALLET_HUD_WIDTH = 530f;
    protected static final float WALLET_HUD_HEIGHT = 64f;
    protected static final float WALLET_HUD_RIGHT_MARGIN = 24f;
    protected static final float WALLET_HUD_TOP_MARGIN = 24f;
    protected static final float DEBUG_SAVE_DELAY_SECONDS = 0.4f;

    private static Timer.Task pendingDebugWalletSave;

    protected final ScreenNavigator navigator;
    protected final Skin skin;
    protected final Stage stage;
    private final Stage backgroundStage;
    private final SpriteBatch backgroundBatch;
    private TextureRegion backgroundRegion;
    protected final Table root;
    protected final Table headerLeading;
    protected final Table content;
    protected final Table navigation;

    private final Label coinsLabel;
    private final Label diamondsLabel;
    private final Table walletHud;
    private boolean lastDebugMode;
    private Texture backgroundTexture;

    protected AbstractScreen(ScreenNavigator navigator, String title) {
        if (navigator == null) {
            throw new IllegalArgumentException("navigator cannot be null");
        }
        this.navigator = navigator;
        this.skin = navigator.getSkin();
        // Backgrounds use a pixel-based viewport rather than the UI's
        // FitViewport. This guarantees that the background always covers the
        // complete OS window, even when the window is not 16:9.
        this.backgroundStage = new Stage(new ScreenViewport());
        this.backgroundBatch = new SpriteBatch();
        this.stage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));

        root = new Table();
        root.setFillParent(true);
        root.pad(24f);
        stage.addActor(root);

        Table header = new Table();
        headerLeading = new Table();
        Label titleLabel = new Label(title, skin, "big");
        coinsLabel = new Label("", skin, "medium_outline");
        diamondsLabel = new Label("", skin, "medium_outline");
        coinsLabel.setFontScale(1.1f);
        diamondsLabel.setFontScale(1.1f);
        walletHud = new Table();

        header.add(headerLeading).left().padRight(10f);
        header.add(titleLabel).left().expandX();
        // Reserve the same top-right area on every screen, but keep the actual
        // wallet HUD out of the Table layout. Some wide menu contents can make
        // the root table's preferred width exceed the viewport; an absolute
        // child keeps the wallet pinned to the exact same viewport position.
        header.add().width(WALLET_HUD_WIDTH).height(WALLET_HUD_HEIGHT);
        root.add(header).growX().row();

        walletHud.setBounds(
                VIRTUAL_WIDTH - WALLET_HUD_RIGHT_MARGIN - WALLET_HUD_WIDTH,
                VIRTUAL_HEIGHT - WALLET_HUD_TOP_MARGIN - WALLET_HUD_HEIGHT,
                WALLET_HUD_WIDTH, WALLET_HUD_HEIGHT);
        walletHud.right();
        root.addActor(walletHud);

        content = new Table();
        root.add(content).grow().row();

        navigation = new Table();
        navigation.defaults().pad(6f).minWidth(190f).height(52f);
        root.add(navigation).growX().bottom().padTop(12f);

        lastDebugMode = false;
        rebuildWalletHud(lastDebugMode);
        refreshResourceLabels();
    }

    private void rebuildWalletHud(boolean debugMode) {
        walletHud.clearChildren();
        walletHud.defaults().padLeft(14f).right();
        walletHud.add(createWalletCounter(DIAMOND_IMAGE_ID, diamondsLabel,
                64f, debugMode, DEBUG_DIAMOND_INCREMENT, false))
                .right().padRight(8f);
        walletHud.add(createWalletCounter(COIN_IMAGE_ID, coinsLabel,
                58f, debugMode, DEBUG_COIN_INCREMENT, true))
                .right();
        lastDebugMode = debugMode;
    }

    private Table createWalletCounter(String imageId, Label amountLabel,
            float iconSize, boolean debugMode, int increment,
            boolean coinCurrency) {
        Table counter = new Table();

        Image icon = createAssetImage(imageId);
        icon.setScaling(Scaling.fit);
        counter.add(icon).size(iconSize).padRight(-14f).padTop(2f);

        Table bar = new Table();
        bar.setBackground(requireAssetDrawable(CURRENCY_BAR_IMAGE_ID));
        bar.pad(6f, 24f, 6f, 28f);
        bar.add(amountLabel).minWidth(92f).right();
        counter.add(bar).width(152f).height(54f);

        if (debugMode) {
            counter.add(createAssetButton(PLUS_IMAGE_ID, () -> {
                User user = App.getInstance().getLoggedInUser();
                if (user == null) {
                    return;
                }
                if (coinCurrency) {
                    user.addCoins(increment);
                } else {
                    user.addDiamonds(increment);
                }
                scheduleDebugWalletSave();
                refreshResourceLabels();
            })).size(44f).padLeft(-8f);
        }
        return counter;
    }


    protected static synchronized void scheduleDebugWalletSave() {
        if (pendingDebugWalletSave != null) {
            pendingDebugWalletSave.cancel();
        }
        pendingDebugWalletSave = new Timer.Task() {
            @Override
            public void run() {
                try {
                    UserManager.saveAllUsers();
                } finally {
                    synchronized (AbstractScreen.class) {
                        if (pendingDebugWalletSave == this) {
                            pendingDebugWalletSave = null;
                        }
                    }
                }
            }
        };
        Timer.schedule(pendingDebugWalletSave, DEBUG_SAVE_DELAY_SECONDS);
    }

    protected final Button createAssetButton(String imageId, Runnable action) {
        Drawable drawable = requireAssetDrawable(imageId);
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.up = drawable;
        style.down = drawable;
        style.over = drawable;
        Button button = new Button(style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

    protected final TextureRegion requireAssetRegion(String imageId) {
        TextureRegion region = navigator.getTextureBank().region(imageId);
        if (region == null) {
            throw new IllegalStateException(
                    "libPVZ could not resolve required image: " + imageId);
        }
        return region;
    }

    protected final Drawable requireAssetDrawable(String imageId) {
        return new TextureRegionDrawable(requireAssetRegion(imageId));
    }

    protected final Image createAssetImage(String imageId) {
        return new Image(requireAssetRegion(imageId));
    }

    /**
     * Adds a normal graphical navigation button that also updates the model
     * menu, so the old controller/model code and the new GUI remain in sync.
     */
    protected final TextButton addMenuButton(
            String label, Supplier<Menu> destination) {
        return addActionButton(label, () -> navigator.navigate(destination.get()));
    }

    protected final TextButton addActionButton(String label, Runnable action) {
        TextButton button = new TextButton(label, skin, "green");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        navigation.add(button);
        return button;
    }

    protected final TextButton addBackButton() {
        TextButton button = new TextButton("Back", skin, "brown");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.back();
            }
        });
        navigation.add(button);
        return button;
    }

    protected final TextButton addReturnToCurrentMenuButton(String label) {
        TextButton button = new TextButton(label, skin, "brown");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.returnToCurrentMenu();
            }
        });
        navigation.add(button);
        return button;
    }

    protected final void nextNavigationRow() {
        navigation.row();
    }

    /**
     * Places an internal asset behind the Scene2D UI. LibGDX internal paths
     * are relative to the project's assets directory, so callers must not
     * include the leading "assets/" segment.
     */
    protected final void setBackground(String internalPath) {
        backgroundStage.getRoot().clearChildren();
        disposeOwnedBackgroundTexture();
        backgroundTexture = new Texture(Gdx.files.internal(internalPath));
        backgroundRegion = new TextureRegion(backgroundTexture);
    }

    /**
     * Uses an image from libPVZ's TextureBank as a full-window stretched
     * background. The TextureBank owns the underlying texture, so this screen
     * only keeps a reference to the region and never disposes that texture.
     */
    protected final void setAssetBackground(String imageId) {
        backgroundStage.getRoot().clearChildren();
        disposeOwnedBackgroundTexture();
        backgroundRegion = requireAssetRegion(imageId);
    }

    /** Adds an actor above the stretched background but below the normal UI. */
    protected final void addBackgroundOverlay(Actor actor) {
        if (actor == null) {
            throw new IllegalArgumentException("background overlay cannot be null");
        }
        backgroundStage.addActor(actor);
    }

    private void drawFullWindowBackground() {
        if (backgroundRegion == null) {
            return;
        }

        // Draw directly to the entire framebuffer before either Scene2D stage
        // changes the GL viewport. This deliberately ignores the UI
        // FitViewport's letterbox area, so a background always stretches over
        // every pixel of the client window on 16:9, 16:10, ultrawide, and
        // high-DPI displays.
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        HdpiUtils.glViewport(0, 0, width, height);
        backgroundBatch.getProjectionMatrix().setToOrtho2D(
                0f, 0f, width, height);
        backgroundBatch.begin();
        backgroundBatch.draw(backgroundRegion, 0f, 0f, width, height);
        backgroundBatch.end();
    }

    private void disposeOwnedBackgroundTexture() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
    }

    private void refreshResourceLabels() {
        User user = App.getInstance().getLoggedInUser();
        boolean debugMode = user != null && user.getSettings().isDebugMode();
        if (debugMode != lastDebugMode) {
            rebuildWalletHud(debugMode);
        }
        if (user == null) {
            coinsLabel.setText("--");
            diamondsLabel.setText("--");
            return;
        }
        coinsLabel.setText(String.format(Locale.US, "%,d", user.getCoins()));
        diamondsLabel.setText(
                String.format(Locale.US, "%,d", user.getDiamonds()));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Screens can temporarily freeze Scene2D actions/animations while still
     * drawing and receiving input. GameScreen uses this for its pause modal.
     */
    protected boolean shouldAdvanceScene() {
        return true;
    }

    @Override
    public void render(float delta) {
        refreshResourceLabels();
        Gdx.gl.glClearColor(0.08f, 0.12f, 0.16f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        float frameDelta = Math.min(delta, 1f / 15f);
        if (shouldAdvanceScene()) {
            backgroundStage.act(frameDelta);
            stage.act(frameDelta);
        }
        drawFullWindowBackground();
        backgroundStage.draw();
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        backgroundStage.getViewport().update(width, height, true);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        backgroundStage.dispose();
        backgroundBatch.dispose();
        disposeOwnedBackgroundTexture();
        backgroundRegion = null;
    }
}
