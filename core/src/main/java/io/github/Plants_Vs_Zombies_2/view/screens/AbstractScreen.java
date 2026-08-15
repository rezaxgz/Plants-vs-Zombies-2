package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.function.Supplier;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.menu.Menu;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/**
 * Shared Scene2D shell for every graphical menu.
 *
 * <p>Concrete screens are intentionally only navigation shells for now. The
 * content table is left available for the actual menu widgets that will be
 * implemented in later GUI steps.</p>
 */
public abstract class AbstractScreen implements Screen {
    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    protected final ScreenNavigator navigator;
    protected final Skin skin;
    protected final Stage stage;
    protected final Table root;
    protected final Table content;
    protected final Table navigation;

    private final Label coinsLabel;
    private final Label diamondsLabel;
    private Texture backgroundTexture;

    protected AbstractScreen(ScreenNavigator navigator, String title) {
        if (navigator == null) {
            throw new IllegalArgumentException("navigator cannot be null");
        }
        this.navigator = navigator;
        this.skin = navigator.getSkin();
        this.stage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));

        root = new Table();
        root.setFillParent(true);
        root.pad(24f);
        stage.addActor(root);

        Table header = new Table();
        Label titleLabel = new Label(title, skin, "big");
        coinsLabel = new Label("", skin);
        diamondsLabel = new Label("", skin);

        header.add(titleLabel).left().expandX();
        header.add(coinsLabel).right().padRight(24f);
        header.add(diamondsLabel).right();
        root.add(header).growX().row();

        content = new Table();
        root.add(content).grow().row();

        navigation = new Table();
        navigation.defaults().pad(6f).minWidth(190f).height(52f);
        root.add(navigation).growX().bottom().padTop(12f);

        refreshResourceLabels();
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
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        backgroundTexture = new Texture(Gdx.files.internal(internalPath));
        Image image = new Image(backgroundTexture);
        image.setScaling(Scaling.fill);
        image.setBounds(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage.getRoot().addActorAt(0, image);
    }

    private void refreshResourceLabels() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            coinsLabel.setText("Coins: --");
            diamondsLabel.setText("Diamonds: --");
            return;
        }
        coinsLabel.setText("Coins: " + user.getCoins());
        diamondsLabel.setText("Diamonds: " + user.getDiamonds());
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        refreshResourceLabels();
        Gdx.gl.glClearColor(0.08f, 0.12f, 0.16f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
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
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
    }
}
