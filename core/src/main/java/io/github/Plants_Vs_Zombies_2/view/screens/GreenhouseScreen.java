package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Timer;

import io.github.Plants_Vs_Zombies_2.controller.GreenhouseMenuController;
import io.github.Plants_Vs_Zombies_2.controller.ShopMenuController;
import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.greenHouse.GreenhouseBoard;
import io.github.Plants_Vs_Zombies_2.model.greenHouse.PlantedPlant;
import io.github.Plants_Vs_Zombies_2.model.greenHouse.Pot;
import io.github.Plants_Vs_Zombies_2.model.menu.GreenhouseMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.ShopMenu;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** Graphical shell for the greenhouse / Zen Garden menu. */
public final class GreenhouseScreen extends AbstractScreen {
    private static final String BACKGROUND_PATH =
            "pvz-assets/ATLASES/DELAYLOAD_BACKGROUND_ZEN_768_00_CROPED.PNG";
    private static final String POT_IMAGE_ID =
            "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161";
    private static final String GOLDEN_POT_IMAGE_ID =
            "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161_2";
    private static final String LOCK_IMAGE_ID =
            "IMAGE_ZEN_GARDEN_LOCKED_POT_ICON";
    private static final String SHOVEL_BUTTON_IMAGE_ID =
            "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON";
    private static final String SHOVEL_CURSOR_IMAGE_ID =
            "IMAGE_ZEN_GARDEN_CURSORS_REMOVAL_CURSOR_REMOVAL_CURSOR_133X115";
    private static final String SPROUT_UI_IMAGE_ID = "IMAGE_UI_SPROUTS_STACK_1";
    private static final String COIN_IMAGE_ID =
            "IMAGE_UI_THYMED_EVENTS_ECS_CONVRT_COIN";
    private static final String MARIGOLD_PACKET_IMAGE_ID =
            "IMAGE_UI_PACKETS_MARIGOLD";

    private static final int DEBUG_SPROUT_INCREMENT = 5;
    private static final float FEEDBACK_VISIBLE_SECONDS = 3f;
    private static final String PURPLE_BUTTON_NORMAL =
            "IMAGE_UI_GENERIC_SM_PURPLE_BTN_NORMAL";
    private static final String PURPLE_BUTTON_DOWN =
            "IMAGE_UI_GENERIC_SM_PURPLE_BTN_DOWN";
    private static final float GRID_X = 318f;
    private static final float GRID_Y = 132f;
    private static final float GRID_WIDTH = 660f;
    private static final float GRID_HEIGHT = 390f;
    private static final float POT_WIDTH = 126f;
    private static final float POT_HEIGHT = 96f;
    private static final float PLANT_WIDTH = 84f;
    private static final float PLANT_HEIGHT = 78f;

    private final Table boardGrid = new Table();
    private final Label feedbackLabel;
    private final Label sproutLabel;
    private final Table sproutHud;
    private final Image fallbackShovelCursor;
    private final Button shovelButton;
    private float boardRefreshTimer;
    private boolean shovelMode;
    private boolean sproutHudDebugMode;
    private Cursor shovelCursor;
    private boolean usingFallbackCursor;
    private Timer.Task feedbackClearTask;
    private final Label[][] timeLabels =
            new Label[GreenhouseBoard.ROWS][GreenhouseBoard.COLUMNS];
    private final Label[][] skipPriceLabels =
            new Label[GreenhouseBoard.ROWS][GreenhouseBoard.COLUMNS];
    private final ProgressBar[][] growthBars =
            new ProgressBar[GreenhouseBoard.ROWS][GreenhouseBoard.COLUMNS];

    public GreenhouseScreen(ScreenNavigator navigator) {
        super(navigator, " ");
        setBackground(BACKGROUND_PATH);

        addMenuButton("Shop", () -> new ShopMenu(currentGreenhouseMenu()));
        addBackButton();

        content.add().expand();

        feedbackLabel = new Label("", skin, "medium_outline");
        feedbackLabel.setAlignment(Align.center);
        feedbackLabel.setWrap(true);
        feedbackLabel.setColor(Color.WHITE);
        feedbackLabel.setBounds(250f, 84f, 780f, 48f);
        stage.addActor(feedbackLabel);

        boardGrid.top().left();
        boardGrid.setBounds(GRID_X, GRID_Y, GRID_WIDTH, GRID_HEIGHT);
        boardGrid.defaults().width(150f).height(122f).pad(2f, 5f, 8f, 5f);
        stage.addActor(boardGrid);

        sproutLabel = new Label("0", skin, "medium_outline");
        sproutLabel.setFontScale(1.1f);
        sproutHud = new Table();
        sproutHud.setBounds(
                VIRTUAL_WIDTH - WALLET_HUD_RIGHT_MARGIN - WALLET_HUD_WIDTH - 164f,
                VIRTUAL_HEIGHT - WALLET_HUD_TOP_MARGIN - WALLET_HUD_HEIGHT,
                164f, WALLET_HUD_HEIGHT);
        stage.addActor(sproutHud);

        shovelButton = createAssetButton(SHOVEL_BUTTON_IMAGE_ID,
                this::toggleShovelMode);
        shovelButton.setSize(78f, 78f);
        shovelButton.setPosition(VIRTUAL_WIDTH - 118f, 30f);
        stage.addActor(shovelButton);

        fallbackShovelCursor = createAssetImage(SHOVEL_CURSOR_IMAGE_ID);
        fallbackShovelCursor.setScaling(Scaling.fit);
        fallbackShovelCursor.setSize(46f, 86f);
        fallbackShovelCursor.setTouchable(Touchable.disabled);
        fallbackShovelCursor.setVisible(false);
        stage.addActor(fallbackShovelCursor);

        rebuildBoard();
        sproutHudDebugMode = isDebugMode();
        refreshSproutHud();
    }

    private boolean isDebugMode() {
        User user = App.getInstance().getLoggedInUser();
        return user != null && user.getSettings().isDebugMode();
    }

    private void refreshSproutHud() {
        User user = App.getInstance().getLoggedInUser();
        boolean debugMode = isDebugMode();
        sproutLabel.setText(user == null ? "--"
                : Integer.toString(user.getSprouts()));
        if (debugMode == sproutHudDebugMode && sproutHud.getChildren().size > 0) {
            return;
        }
        sproutHudDebugMode = debugMode;
        sproutHud.clearChildren();
        sproutHud.defaults().padLeft(2f).right();

        Image icon = createAssetImage(SPROUT_UI_IMAGE_ID);
        icon.setScaling(Scaling.fit);
        sproutHud.add(icon).size(68f).padRight(-22f).padTop(1f);

        Table bar = new Table();
        bar.setBackground(requireAssetDrawable(
                "IMAGE_UI_GENERIC_BUTTON_GENERIC_LTECURRENCY"));
        bar.pad(6f, 24f, 6f, 28f);
        bar.add(sproutLabel).minWidth(66f).right();
        sproutHud.add(bar).width(116f).height(54f);

        if (debugMode) {
            sproutHud.add(createAssetButton("IMAGE_UI_HUD_INGAME_COIN_BUY",
                    () -> {
                        User activeUser = App.getInstance().getLoggedInUser();
                        if (activeUser == null) {
                            return;
                        }
                        activeUser.addSprouts(DEBUG_SPROUT_INCREMENT);
                        scheduleDebugWalletSave();
                        sproutLabel.setText(Integer.toString(
                                activeUser.getSprouts()));
                    })).size(44f).padLeft(-8f);
        }
        sproutHud.invalidateHierarchy();
    }

    private void rebuildBoard() {
        boardGrid.clearChildren();
        clearGrowingIndicatorReferences();
        GreenhouseBoard board = currentUser().getGreenHouse().getBoard();
        int[] unlockable = firstLockedCoordinates(board);
        for (int y = 1; y <= GreenhouseBoard.ROWS; y++) {
            float rowPadTop = rowPadTop(y);
            for (int x = 1; x <= GreenhouseBoard.COLUMNS; x++) {
                boolean firstLocked = unlockable != null
                        && unlockable[0] == x && unlockable[1] == y;
                boardGrid.add(buildPotSlot(x, y, board.getPotAt(x, y),
                        firstLocked)).padTop(rowPadTop);
            }
            boardGrid.row();
        }
    }

    private float rowPadTop(int row) {
        switch (row) {
        case 2:
            return 10f;
        case 3:
            return 30f;
        default:
            return 0f;
        }
    }

    private User currentUser() {
        return App.getInstance().getLoggedInUser();
    }

    private int[] firstLockedCoordinates(GreenhouseBoard board) {
        for (int y = 1; y <= GreenhouseBoard.ROWS; y++) {
            for (int x = 1; x <= GreenhouseBoard.COLUMNS; x++) {
                Pot pot = board.getPotAt(x, y);
                if (pot != null && pot.isLocked()) {
                    return new int[] { x, y };
                }
            }
        }
        return null;
    }

    private Actor buildPotSlot(int x, int y, Pot pot, boolean unlockable) {
        Stack slot = new Stack();
        slot.setTouchable(Touchable.enabled);

        if (!pot.isLocked()) {
            Image potImage = createAssetImage(chooseBasePotAsset(pot));
            potImage.setScaling(Scaling.fit);
            slot.add(centered(potImage, POT_WIDTH, POT_HEIGHT));
        } else if (!unlockable) {
            Image potImage = createAssetImage(POT_IMAGE_ID);
            potImage.setScaling(Scaling.fit);
            potImage.setColor(1f, 1f, 1f, 0.35f);
            slot.add(centered(potImage, POT_WIDTH, POT_HEIGHT));
        }

        if (!pot.isEmpty()) {
            slot.add(topAligned(createPotPlantActor(pot.getPlant()),
                    PLANT_WIDTH, PLANT_HEIGHT, 2f));
        }

        Table overlay = new Table();
        overlay.top();

        if (pot.isLocked()) {
            overlay.add().expandX().row();
            overlay.center();
            if (unlockable) {
                overlay.add(buildUnlockTag()).padTop(70f).center();
            } else {
                Image lock = createAssetImage(LOCK_IMAGE_ID);
                lock.setScaling(Scaling.fit);
                overlay.add(lock).size(48f, 48f).padTop(52f).center();
            }
        } else if (!pot.isEmpty()) {
            PlantedPlant plant = pot.getPlant();
            if (plant.isGrown()) {
                overlay.add(buildReadyInfo(() -> runAndRefresh(
                        GreenhouseMenuController.collectPot(
                                currentUser(), x, y)))).grow();
            } else {
                overlay.add(buildGrowingInfo(x, y, plant,
                        () -> runAndRefresh(
                                GreenhouseMenuController.growPot(
                                        currentUser(), x, y)))).grow();
            }
        }

        slot.add(overlay);
        slot.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float px, float py) {
                if (shovelMode) {
                    if (!pot.isLocked() && !pot.isEmpty()) {
                        runAndRefresh(GreenhouseMenuController.pluckPlant(
                                currentUser(), x, y));
                    } else {
                        showFeedback("Nothing to pluck here.", true);
                    }
                    return;
                }
                if (pot.isLocked()) {
                    if (unlockable) {
                        unlockNextPot();
                    } else {
                        showFeedback("Unlock the earlier slot first.", true);
                    }
                    return;
                }
                if (pot.isEmpty()) {
                    runAndRefresh(
                            GreenhouseMenuController.plantPot(
                                    currentUser(), x, y));
                    return;
                }
                if (pot.getPlant().isGrown()) {
                    runAndRefresh(
                            GreenhouseMenuController.collectPot(
                                    currentUser(), x, y));
                }
            }
        });
        return slot;
    }

    private String chooseBasePotAsset(Pot pot) {
        if (!pot.isEmpty() && pot.getPlant().isGrown()) {
            return GOLDEN_POT_IMAGE_ID;
        }
        return POT_IMAGE_ID;
    }

    private Table buildReadyInfo(Runnable collectAction) {
        Table info = new Table();
        info.bottom();
        Label ready = whiteSmallLabel("Ready!");
        ready.setFontScale(0.62f);
        TextButton collect = createPurpleActionButton("Collect", collectAction);
        collect.getLabel().setFontScale(0.55f);
        info.add(ready).height(14f).padBottom(2f).row();
        info.add(collect).width(78f).height(28f).padBottom(1f);
        return info;
    }

    private Table buildGrowingInfo(int x, int y, PlantedPlant plant,
            Runnable skipAction) {
        Table info = new Table();
        info.bottom();

        Label time = whiteSmallLabel(formatRemaining(plant));
        time.setFontScale(0.56f);
        ProgressBar progressBar = new ProgressBar(0f, 1f, 0.01f,
                false, skin, "xp_green");
        progressBar.setAnimateDuration(0f);
        progressBar.setValue(progressFraction(plant));

        int skipCost = GreenhouseMenuController.getSkipDiamondCost(plant);
        Label diamonds = whiteSmallLabel(skipCost + " diamonds");
        diamonds.setFontScale(0.50f);

        TextButton skipButton = createPurpleActionButton("Skip", skipAction);
        skipButton.getLabel().setFontScale(0.50f);

        timeLabels[y - 1][x - 1] = time;
        skipPriceLabels[y - 1][x - 1] = diamonds;
        growthBars[y - 1][x - 1] = progressBar;

        info.add(time).height(13f).padBottom(1f).row();
        info.add(progressBar).width(88f).height(8f).padBottom(1f).row();
        info.add(diamonds).height(11f).padBottom(1f).row();
        info.add(skipButton).width(66f).height(27f).padBottom(0f);
        return info;
    }

    private Label whiteSmallLabel(String text) {
        Label.LabelStyle style = new Label.LabelStyle(
                skin.get("secondary", Label.LabelStyle.class));
        style.fontColor = Color.WHITE;
        return new Label(text, style);
    }

    private float progressFraction(PlantedPlant plant) {
        if (plant == null || plant.getDurationMillis() <= 0L) {
            return 0f;
        }
        float progress = 1f
                - (float) plant.getRemainingMillis()
                        / (float) plant.getDurationMillis();
        return Math.max(0f, Math.min(1f, progress));
    }

    private Table buildUnlockTag() {
        Table tag = new Table();
        tag.defaults().center();

        Table costRow = new Table();
        Image coin = createAssetImage(COIN_IMAGE_ID);
        coin.setScaling(Scaling.fit);
        costRow.add(coin).size(18f, 18f).padRight(4f);
        Label price = whiteSmallLabel("2000");
        price.setFontScale(0.64f);
        costRow.add(price);

        TextButton buyButton = createPurpleActionButton("Buy",
                this::unlockNextPot);
        buyButton.getLabel().setFontScale(0.62f);
        buyButton.setDisabled(currentUser().getCoins() < 2000);
        buyButton.getColor().a = currentUser().getCoins() < 2000 ? 0.75f : 1f;

        tag.add(costRow).padBottom(3f).row();
        tag.add(buyButton).width(86f).height(31f);
        return tag;
    }

    private Actor createPotPlantActor(PlantedPlant planted) {
        if (planted == null) {
            return new Group();
        }
        String name = planted.getPlantName();
        try {
            if (planted.isMarigold()) {
                return new PamAnimationActor(navigator.getPamPlayer(),
                        "768/INITIAL/PLANT/MARIGOLD/MARIGOLD.PAM",
                        "idle", true);
            }
            PlantAnimationCatalog.Preview preview =
                    PlantAnimationCatalog.find(name);
            if (preview != null) {
                return new PamAnimationActor(navigator.getPamPlayer(),
                        preview.getPath(), preview.getClip(), true);
            }
        } catch (RuntimeException ignored) {
            // Fall back to still imagery when an optional PAM group is absent.
        }

        Image fallback = createAssetImage(planted.isMarigold()
                ? MARIGOLD_PACKET_IMAGE_ID
                : "IMAGE_UI_PACKETS_EMPTY_PACKET");
        fallback.setScaling(Scaling.fit);
        return fallback;
    }

    private Actor centered(Actor actor, float width, float height) {
        Table wrapper = new Table();
        wrapper.add(actor).size(width, height).center();
        return wrapper;
    }

    private Actor topAligned(Actor actor, float width, float height,
            float padTop) {
        Table wrapper = new Table();
        wrapper.top();
        wrapper.add(actor).size(width, height).padTop(padTop).top();
        return wrapper;
    }

    private TextButton createPurpleActionButton(String text, Runnable action) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(
                skin.get("green", TextButton.TextButtonStyle.class));
        style.up = requireAssetDrawable(PURPLE_BUTTON_NORMAL);
        style.over = style.up;
        style.down = requireAssetDrawable(PURPLE_BUTTON_DOWN);
        TextButton button = new TextButton(text, style);
        button.pad(0f, 5f, 0f, 5f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                action.run();
            }
        });
        return button;
    }

    private void unlockNextPot() {
        CommandResult result = ShopMenuController.purchase("pot", 1, null);
        runAndRefresh(result);
    }

    private void runAndRefresh(CommandResult result) {
        showFeedback(result.getMessage(), !result.isSuccsesful());
        if (result.isSuccsesful()) {
            boardRefreshTimer = 0f;
            refreshSproutHud();
            rebuildBoard();
        }
    }

    private void showFeedback(String message, boolean error) {
        if (feedbackClearTask != null) {
            feedbackClearTask.cancel();
            feedbackClearTask = null;
        }
        feedbackLabel.setText(message == null ? "" : message);
        feedbackLabel.setColor(error ? Color.SCARLET : Color.WHITE);
        if (message == null || message.isBlank()) {
            return;
        }
        feedbackClearTask = new Timer.Task() {
            @Override
            public void run() {
                feedbackLabel.setText("");
                feedbackClearTask = null;
            }
        };
        Timer.schedule(feedbackClearTask, FEEDBACK_VISIBLE_SECONDS);
    }

    private void toggleShovelMode() {
        setShovelMode(!shovelMode);
    }

    private void setShovelMode(boolean enabled) {
        shovelMode = enabled;
        shovelButton.setColor(enabled ? new Color(1f, 1f, 0.8f, 1f)
                : Color.WHITE);
        if (enabled) {
            applyShovelCursor();
            showFeedback("Shovel selected. Left click a planted pot to pluck it. Right click to drop it.",
                    false);
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            fallbackShovelCursor.setVisible(false);
            usingFallbackCursor = false;
            showFeedback("Shovel put away.", false);
        }
    }

    private void applyShovelCursor() {
        try {
            if (shovelCursor == null) {
                shovelCursor = createCursorFromRegion(
                        requireAssetRegion(SHOVEL_CURSOR_IMAGE_ID), 10, 8);
            }
            Gdx.graphics.setCursor(shovelCursor);
            fallbackShovelCursor.setVisible(false);
            usingFallbackCursor = false;
        } catch (RuntimeException exception) {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
            fallbackShovelCursor.setVisible(true);
            usingFallbackCursor = true;
        }
    }

    private Cursor createCursorFromRegion(TextureRegion region,
            int hotspotX, int hotspotY) {
        TextureData textureData = region.getTexture().getTextureData();
        if (!textureData.isPrepared()) {
            textureData.prepare();
        }
        Pixmap source = textureData.consumePixmap();
        Pixmap cursorPixmap = new Pixmap(region.getRegionWidth(),
                region.getRegionHeight(), Pixmap.Format.RGBA8888);
        cursorPixmap.drawPixmap(source, 0, 0,
                region.getRegionX(), region.getRegionY(),
                region.getRegionWidth(), region.getRegionHeight());
        if (textureData.disposePixmap()) {
            source.dispose();
        }
        Cursor cursor = Gdx.graphics.newCursor(cursorPixmap,
                hotspotX, hotspotY);
        cursorPixmap.dispose();
        return cursor;
    }

    private String formatRemaining(PlantedPlant plant) {
        long remainingMillis = plant.getRemainingMillis();
        long totalMinutes = (long) Math.ceil(remainingMillis / 60000.0);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) {
            return minutes > 0
                    ? hours + "h " + minutes + "m"
                    : hours + "h";
        }
        return Math.max(1, minutes) + "m";
    }

    private void clearGrowingIndicatorReferences() {
        for (int y = 0; y < GreenhouseBoard.ROWS; y++) {
            for (int x = 0; x < GreenhouseBoard.COLUMNS; x++) {
                timeLabels[y][x] = null;
                skipPriceLabels[y][x] = null;
                growthBars[y][x] = null;
            }
        }
    }

    private void refreshGrowingIndicators() {
        GreenhouseBoard board = currentUser().getGreenHouse().getBoard();
        boolean needsRebuild = false;
        for (int y = 1; y <= GreenhouseBoard.ROWS; y++) {
            for (int x = 1; x <= GreenhouseBoard.COLUMNS; x++) {
                Label time = timeLabels[y - 1][x - 1];
                ProgressBar bar = growthBars[y - 1][x - 1];
                Label price = skipPriceLabels[y - 1][x - 1];
                if (time == null && bar == null && price == null) {
                    continue;
                }
                Pot pot = board.getPotAt(x, y);
                if (pot == null || pot.isEmpty() || pot.getPlant().isGrown()) {
                    needsRebuild = true;
                    continue;
                }
                PlantedPlant plant = pot.getPlant();
                time.setText(formatRemaining(plant));
                bar.setValue(progressFraction(plant));
                price.setText(GreenhouseMenuController
                        .getSkipDiamondCost(plant) + " diamonds");
            }
        }
        if (needsRebuild) {
            rebuildBoard();
        }
    }

    @Override
    public void render(float delta) {
        boardRefreshTimer += delta;
        if (boardRefreshTimer >= 1f) {
            boardRefreshTimer = 0f;
            refreshGrowingIndicators();
        }
        if (shovelMode && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            setShovelMode(false);
        }
        if (usingFallbackCursor && shovelMode) {
            com.badlogic.gdx.math.Vector2 stageCoords =
                    stage.screenToStageCoordinates(
                            new com.badlogic.gdx.math.Vector2(
                                    Gdx.input.getX(), Gdx.input.getY()));
            fallbackShovelCursor.setPosition(stageCoords.x - 6f,
                    stageCoords.y - fallbackShovelCursor.getHeight() + 14f);
        }
        refreshSproutHud();
        super.render(delta);
    }

    @Override
    public void hide() {
        if (shovelMode) {
            shovelMode = false;
            shovelButton.setColor(Color.WHITE);
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            fallbackShovelCursor.setVisible(false);
            usingFallbackCursor = false;
        }
        super.hide();
    }

    @Override
    public void dispose() {
        if (feedbackClearTask != null) {
            feedbackClearTask.cancel();
            feedbackClearTask = null;
        }
        if (shovelCursor != null) {
            shovelCursor.dispose();
            shovelCursor = null;
        }
        super.dispose();
    }

    private static GreenhouseMenu currentGreenhouseMenu() {
        return (GreenhouseMenu) App.getInstance().getCurrentMenu();
    }
}
