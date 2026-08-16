package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.controller.CollectionMenuController;
import io.github.Plants_Vs_Zombies_2.controller.MainController;
import io.github.Plants_Vs_Zombies_2.controller.PlantSelectionController;
import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.plantSelector.PlantSelection;
import io.github.Plants_Vs_Zombies_2.model.game.save.SavedGameManager;
import io.github.Plants_Vs_Zombies_2.model.menu.GameMenu;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Chapter;
import io.github.Plants_Vs_Zombies_2.model.roadmap.ChapterCatalog;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Level;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** Graphical shell for the active game and chapter board preview. */
public final class GameScreen extends AbstractScreen {
    private static final int BOARD_COLUMNS = 9;
    private static final int BOARD_ROWS = 5;
    private static final int PLANTS_PER_ROW = 6;

    private static final String SUN_ICON = "IMAGE_EFFECTS_SUN_SUN_78X78";
    private static final String GAME_SUN_ICON = "IMAGE_UI_HUD_INGAME_SUN";
    private static final String GAME_SUN_BACKGROUND =
            "IMAGE_UI_HUD_INGAME_BACKGROUND_3SLICE";
    private static final String DEBUG_PLUS_ICON =
            "IMAGE_UI_HUD_INGAME_COIN_BUY";
    private static final int DEBUG_SUN_INCREMENT = 200;
    private static final String COIN_ICON =
            "IMAGE_UI_THYMED_EVENTS_ECS_CONVRT_COIN";
    private static final String DIAMOND_ICON =
            "IMAGE_EFFECTS_COIN_DIAMOND_COIN_DIAMOND_141X146";
    private static final String BOOST_PACKET = "IMAGE_UI_PACKETS_BOOST";
    private static final String EGYPT_PACKET = "IMAGE_UI_PACKETS_EGYPT";
    private static final String ICEAGE_PACKET = "IMAGE_UI_PACKETS_ICEAGE";
    private static final String BEACH_PACKET = "IMAGE_UI_PACKETS_BEACH";
    private static final String DARK_PACKET = "IMAGE_UI_PACKETS_DARK";

    private static final float SEED_TRAY_X = 16f;
    private static final float SEED_TRAY_Y = 76f;
    private static final float SEED_TRAY_WIDTH = 116f;
    private static final float SEED_TRAY_HEIGHT = 568f;
    private static final float SEED_SLOT_WIDTH = 110f;
    private static final float SEED_SLOT_HEIGHT = 68f;
    private static final float PAUSE_BUTTON_X = 44f;
    private static final float PAUSE_BUTTON_Y = 650f;
    private static final float PAUSE_BUTTON_SIZE = 58f;
    private static final float SUN_HUD_X = 210f;
    private static final float SUN_HUD_Y = 648f;
    private static final float SUN_HUD_WIDTH = 218f;
    private static final float SUN_HUD_HEIGHT = 60f;

    private static final BoardLayout EGYPT_BOARD = new BoardLayout(
            "IMAGE_BACKGROUNDS_EGYPT_TEXTURE",
            1024f, 768f,
            256f, 200f, 994f, 688f);
    private static final BoardLayout ICEAGE_BOARD = new BoardLayout(
            "IMAGE_BACKGROUNDS_ICEAGE_TEXTURE",
            1022f, 785f,
            256f, 205f, 984f, 688f);
    private static final BoardLayout BEACH_BOARD = new BoardLayout(
            "IMAGE_BACKGROUNDS_BEACH_TEXTURE",
            1024f, 768f,
            256f, 200f, 994f, 688f);
    private static final BoardLayout DARK_BOARD = new BoardLayout(
            "IMAGE_BACKGROUNDS_DARK_TEXTURE",
            1024f, 768f,
            256f, 200f, 994f, 688f);

    private BoardGridActor gridActor;

    // These fields are only populated for the Phase-2 empty level preview.
    // No Game object is created or advanced yet.
    private Chapter previewChapter;
    private Level previewLevel;
    private PlantSelection previewSelection;
    private Table seedTray;
    private Group plantSelectionModal;
    private Table plantGrid;
    private Table detailPanel;
    private Label selectionCountLabel;
    private Label selectionFeedbackLabel;
    private PlantCollectionItem focusedPlant;
    private ImageButton pauseButton;
    private Table sunHud;
    private Label sunAmountLabel;
    private Group pauseModal;
    private boolean gamePaused;
    private boolean sunHudDebugMode;
    private int previewSunCount;

    /** Normal model-backed game screen, including resumed saved games. */
    public GameScreen(ScreenNavigator navigator) {
        super(navigator, "");

        GameMenu menu = currentGameMenu();
        Chapter chapter = chapterForCurrentGame();
        if (chapter != null && menu.getLevel() != null) {
            installPreviewLevelTitle(chapter, menu.getLevel());
        }
        installChapterBoard(chapter);
        installGameHud();

        if (menu.getLevel() != null && menu.getGame().hasConfiguredPlantLoadout()) {
            installSeedTray();
            rebuildSeedTray();
        }
    }

    /** Empty level preview for levels that do not use normal plant choosing. */
    public GameScreen(ScreenNavigator navigator, Chapter chapter, Level level) {
        this(navigator, chapter, level, null, false);
    }

    /**
     * Empty level preview with the Phase-1 loadout selection state attached.
     * When {@code showPlantPicker} is true, the Phase-2 plant choosing modal is
     * displayed immediately. Pressing LET'S ROCK only closes that modal; game
     * simulation is deliberately not started yet.
     */
    public GameScreen(ScreenNavigator navigator, Chapter chapter, Level level,
            PlantSelection selection, boolean showPlantPicker) {
        super(navigator, "");
        if (chapter == null || level == null) {
            throw new IllegalArgumentException("chapter and level cannot be null");
        }
        installPreviewLevelTitle(chapter, level);
        this.previewChapter = chapter;
        this.previewLevel = level;
        this.previewSelection = selection;
        this.previewSunCount = level.getInitialSunCount();

        installChapterBoard(chapter);
        installGameHud();

        if (previewSelection != null) {
            installSeedTray();
            rebuildSeedTray();
            if (showPlantPicker) {
                showPlantSelectionModal();
            }
        }
    }

    private void installGameHud() {
        installPauseButton();
        installSunHud();
    }

    private void installPauseButton() {
        pauseButton = new ImageButton(skin, "ingame_pause");
        pauseButton.setBounds(PAUSE_BUTTON_X, PAUSE_BUTTON_Y,
                PAUSE_BUTTON_SIZE, PAUSE_BUTTON_SIZE);
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showPauseModal();
            }
        });
        stage.addActor(pauseButton);
    }

    private void installSunHud() {
        sunAmountLabel = new Label("0", skin, "medium_outline");
        sunAmountLabel.setFontScale(1.05f);
        sunAmountLabel.setAlignment(Align.center);

        sunHud = new Table();
        sunHud.left();
        sunHud.setBounds(SUN_HUD_X, SUN_HUD_Y,
                SUN_HUD_WIDTH, SUN_HUD_HEIGHT);
        stage.addActor(sunHud);
        sunHudDebugMode = !isDebugMode();
        refreshSunHud();
    }

    private boolean isDebugMode() {
        User user = currentUser();
        return user != null && user.getSettings().isDebugMode();
    }

    private void refreshSunHud() {
        if (sunHud == null || sunAmountLabel == null) {
            return;
        }
        boolean debugMode = isDebugMode();
        sunAmountLabel.setText(Integer.toString(currentSunCount()));
        if (sunHud.getChildren().size > 0
                && debugMode == sunHudDebugMode) {
            return;
        }

        sunHudDebugMode = debugMode;
        sunHud.clearChildren();

        Stack amountStack = new Stack();
        Image background = createAssetImage(GAME_SUN_BACKGROUND);
        background.setScaling(Scaling.stretch);
        amountStack.add(background);

        Table amountContents = new Table();
        amountContents.left();
        Image sun = createAssetImage(GAME_SUN_ICON);
        sun.setScaling(Scaling.fit);
        amountContents.add(sun).size(56f).padLeft(2f).padRight(-4f);
        amountContents.add(sunAmountLabel).width(76f).center();
        amountStack.add(amountContents);

        sunHud.add(amountStack).width(150f).height(50f);

        if (debugMode) {
            Button plus = createAssetButton(DEBUG_PLUS_ICON,
                    this::addDebugSuns);
            sunHud.add(plus).size(42f).padLeft(-5f);
        }
        sunHud.invalidateHierarchy();
    }

    private int currentSunCount() {
        if (previewLevel != null) {
            return previewSunCount;
        }
        GameMenu menu = currentGameMenu();
        return menu.getGame().getSunCount();
    }

    private void addDebugSuns() {
        if (!isDebugMode()) {
            return;
        }
        if (previewLevel != null) {
            if (previewSunCount <= Integer.MAX_VALUE - DEBUG_SUN_INCREMENT) {
                previewSunCount += DEBUG_SUN_INCREMENT;
            }
        } else {
            currentGameMenu().getGame().addSun(DEBUG_SUN_INCREMENT);
        }
        refreshSunHud();
    }

    private void showPauseModal() {
        if (pauseModal != null || plantSelectionModal != null) {
            return;
        }
        gamePaused = true;

        pauseModal = new Group();
        pauseModal.setBounds(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        pauseModal.setTouchable(Touchable.enabled);

        Table panel = new Table();
        panel.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(22f);
        panel.setBounds(360f, 220f, 560f, 300f);

        Label title = new Label("GAME PAUSED", skin, "big_outline");
        title.setAlignment(Align.center);
        panel.add(title).growX().height(58f).padBottom(18f).row();

        Label hint = new Label("The game is paused.",
                skin, "medium_outline");
        hint.setAlignment(Align.center);
        panel.add(hint).growX().height(44f).padBottom(24f).row();

        Table actions = new Table();
        TextButton saveAndExit = new TextButton(
                "SAVE AND EXIT", skin, "brown");
        saveAndExit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    saveCurrentGameAndExit();
                } catch (RuntimeException exception) {
                    hint.setColor(Color.SCARLET);
                    hint.setText("Save failed: " + exception.getMessage());
                }
            }
        });
        actions.add(saveAndExit).width(154f).height(52f).padRight(10f);

        TextButton restart = new TextButton("RESTART", skin, "brown");
        restart.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                restartLevel();
            }
        });
        actions.add(restart).width(132f).height(52f).padRight(10f);

        TextButton resume = new TextButton("RESUME", skin, "purple");
        resume.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closePauseModal();
            }
        });
        actions.add(resume).width(132f).height(52f);
        panel.add(actions).center();

        pauseModal.addActor(panel);
        stage.addActor(pauseModal);
    }

    private void saveCurrentGameAndExit() {
        if (previewLevel != null) {
            throw new IllegalStateException("level has not started yet");
        }
        GameMenu menu = currentGameMenu();
        User user = currentUser();
        SavedGameManager.saveAdventureGame(user, menu);
        gamePaused = false;
        navigator.exitGameToAdventure();
    }

    private void closePauseModal() {
        if (pauseModal != null) {
            pauseModal.remove();
            pauseModal = null;
        }
        gamePaused = false;
    }

    private void restartLevel() {
        gamePaused = false;
        if (previewChapter != null && previewLevel != null) {
            if (previewSelection == null) {
                navigator.showTransient(new GameScreen(
                        navigator, previewChapter, previewLevel));
            } else {
                navigator.showTransient(new GameScreen(
                        navigator, previewChapter, previewLevel,
                        previewSelection, false));
            }
            return;
        }
        closePauseModal();
        navigator.returnToCurrentMenu();
    }

    private void installPreviewLevelTitle(Chapter chapter, Level level) {
        Label levelTitle = new Label(
                chapter.getDisplayName() + " - Level " + level.getNumber(),
                skin, "big_outline");
        levelTitle.setFontScale(0.72f);
        levelTitle.setAlignment(Align.right);
        levelTitle.setBounds(382f, 646f, 334f, 48f);
        stage.addActor(levelTitle);
    }

    private void installChapterBoard(Chapter chapter) {
        BoardLayout layout = layoutForChapter(chapter);
        if (layout == null) {
            return;
        }
        setAssetBackground(layout.backgroundAsset);

        if (shouldDrawGrid()) {
            gridActor = new BoardGridActor(layout);
            addBackgroundOverlay(gridActor);
        }
    }

    private boolean shouldDrawGrid() {
        return App.getInstance().getLoggedInUser() != null
                && App.getInstance().getLoggedInUser()
                        .getSettings().isShowGameMapGrid();
    }

    private void installSeedTray() {
        seedTray = new Table();
        seedTray.top();
        seedTray.setBounds(SEED_TRAY_X, SEED_TRAY_Y,
                SEED_TRAY_WIDTH, SEED_TRAY_HEIGHT);
        stage.addActor(seedTray);
    }

    private void rebuildSeedTray() {
        if (seedTray == null) {
            return;
        }
        seedTray.clearChildren();
        List<PlantCollectionItem> selected = selectedPlantsForSeedTray();
        int slots = seedSlotCount();
        if (slots <= 0) {
            return;
        }
        float slotHeight = Math.min(SEED_SLOT_HEIGHT,
                Math.max(48f, (SEED_TRAY_HEIGHT - Math.max(0, slots - 1) * 2f)
                        / Math.max(1, slots)));
        float slotWidth = Math.min(SEED_SLOT_WIDTH, slotHeight * 1.62f);

        for (int index = 0; index < slots; index++) {
            PlantCollectionItem plant = index < selected.size()
                    ? selected.get(index)
                    : null;
            seedTray.add(createGameSeedSlot(plant))
                    .width(slotWidth).height(slotHeight)
                    .padBottom(index + 1 < slots ? 2f : 0f)
                    .row();
        }
    }

    private List<PlantCollectionItem> selectedPlantsForSeedTray() {
        if (previewSelection != null) {
            return previewSelection.getSelectedPlants();
        }
        List<PlantCollectionItem> selected = new ArrayList<>();
        User user = currentUser();
        if (user == null) {
            return selected;
        }
        for (BasePlant prototype : currentGameMenu().getGame()
                .getPlantLoadoutPrototypes()) {
            PlantCollectionItem item = user.getPlantCollection()
                    .findPlant(prototype.getName());
            if (item != null) {
                selected.add(item);
            }
        }
        return selected;
    }

    private int seedSlotCount() {
        if (previewSelection != null) {
            return previewSelection.getSlotCount();
        }
        Level level = currentGameMenu().getLevel();
        return level == null ? 0 : level.getPlantSlotCount();
    }

    private Chapter seedTrayChapter() {
        return previewChapter != null ? previewChapter : chapterForCurrentGame();
    }

    private Actor createGameSeedSlot(PlantCollectionItem plant) {
        Stack slot = new Stack();
        String packet = plant != null && isVisuallyBoosted(plant)
                ? BOOST_PACKET
                : packetAssetForChapter(seedTrayChapter());

        Image background = createAssetImage(packet);
        background.setScaling(Scaling.stretch);
        if (plant == null) {
            background.setColor(1f, 1f, 1f, 0.30f);
        }
        slot.add(background);

        if (plant == null) {
            return slot;
        }

        Table artworkLayer = new Table();
        Image artwork = createAssetImage(
                PlantPacketCard.packetAssetFor(plant.getName()));
        artwork.setScaling(Scaling.fit);
        artworkLayer.add(artwork).width(82f).height(52f).padTop(2f);
        slot.add(artworkLayer);

        Table costLayer = new Table();
        costLayer.bottom().right();
        Image sun = createAssetImage(SUN_ICON);
        sun.setScaling(Scaling.fit);
        Label cost = new Label(Integer.toString(plant.getCost()),
                skin, "medium_outline");
        cost.setFontScale(0.55f);
        costLayer.add(sun).size(16f).padRight(-2f);
        costLayer.add(cost).padRight(5f).padBottom(3f);
        slot.add(costLayer);
        slot.addListener(new TextTooltip(
                plant.getName() + (isVisuallyBoosted(plant)
                        ? "\nBoosted" : ""), skin));
        return slot;
    }

    private void showPlantSelectionModal() {
        if (plantSelectionModal != null || previewSelection == null) {
            return;
        }

        User user = currentUser();
        if (user == null) {
            return;
        }
        focusedPlant = firstFocusablePlant();

        plantSelectionModal = new Group();
        plantSelectionModal.setBounds(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        plantSelectionModal.setTouchable(Touchable.enabled);

        Table panel = new Table();
        panel.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(12f);
        panel.setBounds(144f, 92f, 830f, 520f);

        Table chooserTitle = new Table();
        Label title = new Label("CHOOSE YOUR PLANTS", skin, "big_outline");
        chooserTitle.add(title).left().expandX();
        selectionCountLabel = new Label("", skin, "medium_outline");
        chooserTitle.add(selectionCountLabel).right();
        panel.add(chooserTitle).growX().height(42f).row();

        detailPanel = new Table();
        panel.add(detailPanel).growX().height(116f).padTop(4f).row();

        plantGrid = new Table();
        plantGrid.top().left();
        ScrollPane scroll = new ScrollPane(plantGrid, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        panel.add(scroll).growX().height(302f).padTop(6f).row();

        selectionFeedbackLabel = new Label("", skin, "secondary");
        selectionFeedbackLabel.setAlignment(Align.center);
        selectionFeedbackLabel.setWrap(true);
        panel.add(selectionFeedbackLabel).growX().height(32f).padTop(2f);

        plantSelectionModal.addActor(panel);

        TextButton letsRock = new TextButton("LET'S ROCK!", skin, "purple");
        letsRock.setBounds(1000f, 34f, 220f, 58f);
        letsRock.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (previewSelection.getSelectedPlants().isEmpty()) {
                    showSelectionFeedback(CommandResult.error(
                            "select at least one plant before starting the game!"));
                    return;
                }
                CommandResult result = MainController.launchAdventureGameFromGui(
                        previewChapter, previewLevel, previewSelection);
                if (!result.isSuccsesful()) {
                    showSelectionFeedback(result);
                    return;
                }
                navigator.showCurrentMenu();
            }
        });
        plantSelectionModal.addActor(letsRock);

        stage.addActor(plantSelectionModal);
        refreshPlantSelectionUi();
    }

    private PlantCollectionItem firstFocusablePlant() {
        if (previewSelection == null) {
            return null;
        }
        List<PlantCollectionItem> available = previewSelection.getAvailablePlants();
        if (!available.isEmpty()) {
            return available.get(0);
        }
        List<PlantCollectionItem> all = previewSelection.getAllPlants();
        return all.isEmpty() ? null : all.get(0);
    }

    private void refreshPlantSelectionUi() {
        rebuildSeedTray();
        rebuildPlantGrid();
        rebuildPlantDetail();
        if (selectionCountLabel != null && previewSelection != null) {
            selectionCountLabel.setText("Selected "
                    + previewSelection.getSelectedPlants().size()
                    + "/" + previewSelection.getSlotCount());
        }
    }

    private void rebuildPlantGrid() {
        if (plantGrid == null || previewSelection == null) {
            return;
        }
        plantGrid.clearChildren();
        plantGrid.defaults().pad(3f);

        int column = 0;
        for (PlantCollectionItem plant : previewSelection.getAllPlants()) {
            plantGrid.add(createPlantChoiceCard(plant))
                    .width(116f).height(118f);
            column++;
            if (column == PLANTS_PER_ROW) {
                plantGrid.row();
                column = 0;
            }
        }
    }

    private Actor createPlantChoiceCard(PlantCollectionItem plant) {
        final Table wrapper = new Table();
        wrapper.pad(3f);

        boolean selected = previewSelection.isSelected(plant);
        boolean boosted = isVisuallyBoosted(plant);
        boolean available = plant.isUnlocked()
                && previewSelection.isAvailable(plant);

        if (boosted) {
            TextButtonStyle brown = skin.get("brown", TextButtonStyle.class);
            wrapper.setBackground(brown.up);
        } else if (selected) {
            TextButtonStyle green = skin.get("green", TextButtonStyle.class);
            wrapper.setBackground(green.up);
        }
        if (!available) {
            wrapper.getColor().a = 0.58f;
        }

        PlantPacketCard card = new PlantPacketCard(navigator, plant);
        wrapper.add(card)
                .width(PlantPacketCard.WIDTH)
                .height(PlantPacketCard.TOTAL_HEIGHT)
                .row();

        Table status = new Table();
        Image sun = createAssetImage(SUN_ICON);
        sun.setScaling(Scaling.fit);
        Label cost = new Label(Integer.toString(plant.getCost()),
                skin, "medium_outline");
        cost.setFontScale(0.50f);
        status.add(sun).size(15f).padRight(-2f);
        status.add(cost).left();

        String marker = boosted ? "BOOSTED"
                : selected ? "SELECTED"
                : !available ? "UNAVAILABLE" : "";
        Label state = new Label(marker, skin, "secondary");
        state.setFontScale(0.58f);
        status.add(state).expandX().right();
        wrapper.add(status).growX().height(18f);

        wrapper.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                focusedPlant = plant;
                CommandResult result;
                if (previewSelection.isSelected(plant)) {
                    result = PlantSelectionController.removePlant(
                            previewSelection, plant);
                } else {
                    result = PlantSelectionController.addPlant(
                            previewSelection, plant);
                }
                showSelectionFeedback(result);
                refreshPlantSelectionUi();
            }
        });
        return wrapper;
    }

    private void rebuildPlantDetail() {
        if (detailPanel == null) {
            return;
        }
        detailPanel.clearChildren();
        TextButtonStyle green = skin.get("green", TextButtonStyle.class);
        detailPanel.setBackground(green.up);
        detailPanel.pad(8f, 12f, 8f, 12f);

        if (focusedPlant == null) {
            detailPanel.add(new Label(
                    "Choose a plant to see its details.",
                    skin, "medium_outline"));
            return;
        }

        Actor previewActor = createPlantDetailActor(focusedPlant);
        detailPanel.add(previewActor).width(122f).height(98f).padRight(10f);

        Table info = new Table();
        info.left();
        Label name = new Label(focusedPlant.getName(), skin, "medium_outline");
        name.setFontScale(1.05f);
        info.add(name).left().row();

        Table meta = new Table();
        meta.left();
        Image sun = createAssetImage(SUN_ICON);
        sun.setScaling(Scaling.fit);
        meta.add(sun).size(22f).padRight(1f);
        Label cost = new Label(Integer.toString(focusedPlant.getCost()),
                skin, "medium_outline");
        meta.add(cost).padRight(14f);
        Label level = new Label("Lv " + focusedPlant.getCurrentLevel(),
                skin, "medium_outline");
        meta.add(level);
        if (isVisuallyBoosted(focusedPlant)) {
            Label boosted = new Label("  BOOSTED", skin, "medium_outline");
            boosted.setColor(Color.GOLD);
            meta.add(boosted).padLeft(8f);
        }
        info.add(meta).left().padTop(2f).row();

        Label description = new Label(focusedPlant.getBaseAbility(),
                skin, "secondary");
        description.setWrap(true);
        info.add(description).width(355f).left().padTop(3f);
        detailPanel.add(info).width(385f).expandX().left();

        Table actions = new Table();
        TextButton upgrade;
        if (focusedPlant.isAtMaximumLevel()) {
            upgrade = new TextButton("MAX LEVEL", skin, "purple");
        } else {
            upgrade = createCurrencyActionButton(
                    "UPGRADE",
                    Integer.toString(focusedPlant.getCoinsNeededForNextLevel()),
                    COIN_ICON, "purple");
        }
        setButtonEnabled(upgrade,
                focusedPlant.isUnlocked() && !focusedPlant.isAtMaximumLevel());
        upgrade.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                CommandResult result = CollectionMenuController.upgradePlant(
                        currentUser(), focusedPlant);
                showSelectionFeedback(result);
                refreshPlantSelectionUi();
            }
        });
        actions.add(upgrade).width(174f).height(44f).row();

        boolean alreadyBoosted = isVisuallyBoosted(focusedPlant);
        TextButton boost = alreadyBoosted
                ? new TextButton("BOOSTED", skin, "green")
                : createCurrencyActionButton(
                        "BOOST",
                        Integer.toString(
                                PlantSelectionController.getBoostDiamondCost()),
                        DIAMOND_ICON, "green");
        setButtonEnabled(boost,
                focusedPlant.isUnlocked()
                        && previewSelection.isAvailable(focusedPlant)
                        && previewSelection.isSelected(focusedPlant)
                        && !alreadyBoosted);
        boost.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                CommandResult result = PlantSelectionController.boostPlant(
                        currentUser(), previewSelection, focusedPlant);
                showSelectionFeedback(result);
                refreshPlantSelectionUi();
            }
        });
        actions.add(boost).width(174f).height(44f).padTop(6f);
        detailPanel.add(actions).width(184f).right();
    }

    private Actor createPlantDetailActor(PlantCollectionItem plant) {
        PlantAnimationCatalog.Preview preview =
                PlantAnimationCatalog.find(plant.getName());
        if (preview != null) {
            try {
                return new PamAnimationActor(
                        navigator.getPamPlayer(),
                        preview.getPath(), preview.getClip());
            } catch (RuntimeException ignored) {
                // Keep plant choosing usable with incomplete optional PAM data.
            }
        }
        Image fallback = createAssetImage(
                PlantPacketCard.packetAssetFor(plant.getName()));
        fallback.setScaling(Scaling.fit);
        return fallback;
    }

    private TextButton createCurrencyActionButton(String actionText,
            String amount, String iconId, String styleName) {
        TextButton button = new TextButton("", skin, styleName);
        button.clearChildren();

        Table contents = new Table();
        Label action = new Label(actionText, skin, "medium_outline");
        action.setFontScale(0.58f);
        contents.add(action).padRight(5f);

        Image icon = createAssetImage(iconId);
        icon.setScaling(Scaling.fit);
        contents.add(icon).size(23f).padRight(1f);

        Label value = new Label(amount, skin, "medium_outline");
        value.setFontScale(0.60f);
        contents.add(value);

        button.add(contents).grow();
        return button;
    }

    private void showSelectionFeedback(CommandResult result) {
        if (selectionFeedbackLabel == null || result == null) {
            return;
        }
        selectionFeedbackLabel.setText(result.getMessage());
        selectionFeedbackLabel.setColor(
                result.isSuccsesful() ? Color.FOREST : Color.SCARLET);
    }

    private void closePlantSelectionModal() {
        if (plantSelectionModal == null) {
            return;
        }
        plantSelectionModal.remove();
        plantSelectionModal = null;
        plantGrid = null;
        detailPanel = null;
        selectionCountLabel = null;
        selectionFeedbackLabel = null;
        rebuildSeedTray();
    }

    private boolean isVisuallyBoosted(PlantCollectionItem plant) {
        if (plant == null) {
            return false;
        }
        if (previewSelection != null && previewSelection.isBoosted(plant)) {
            return true;
        }
        if (previewLevel == null && App.getInstance().getCurrentMenu() instanceof GameMenu) {
            for (String boosted : currentGameMenu().getGame().getBoostedPlantNames()) {
                if (boosted.equalsIgnoreCase(plant.getName())) {
                    return true;
                }
            }
        }
        User user = currentUser();
        return user != null && user.hasPlantBoost(plant.getName());
    }

    private User currentUser() {
        return App.getInstance().getLoggedInUser();
    }

    private static String packetAssetForChapter(Chapter chapter) {
        if (chapter == null) {
            return EGYPT_PACKET;
        }
        if ("ancient-egypt".equals(chapter.getId())) {
            return EGYPT_PACKET;
        }
        if ("frostbite-caves".equals(chapter.getId())) {
            return ICEAGE_PACKET;
        }
        if ("big-wave-beach".equals(chapter.getId())) {
            return BEACH_PACKET;
        }
        if ("dark-ages".equals(chapter.getId())) {
            return DARK_PACKET;
        }
        return EGYPT_PACKET;
    }

    private static Chapter chapterForCurrentGame() {
        GameMenu menu = currentGameMenu();
        if (menu.getChapterId() == null) {
            return null;
        }
        return ChapterCatalog.findById(menu.getChapterId());
    }

    private static BoardLayout layoutForChapter(Chapter chapter) {
        if (chapter == null) {
            return null;
        }
        if ("ancient-egypt".equals(chapter.getId())) {
            return EGYPT_BOARD;
        }
        if ("frostbite-caves".equals(chapter.getId())) {
            return ICEAGE_BOARD;
        }
        if ("big-wave-beach".equals(chapter.getId())) {
            return BEACH_BOARD;
        }
        if ("dark-ages".equals(chapter.getId())) {
            return DARK_BOARD;
        }
        return null;
    }

    private static GameMenu currentGameMenu() {
        return (GameMenu) App.getInstance().getCurrentMenu();
    }

    private void setButtonEnabled(Button button, boolean enabled) {
        button.setDisabled(!enabled);
        button.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
        button.getColor().a = enabled ? 1f : 0.48f;
    }

    @Override
    protected boolean shouldAdvanceScene() {
        return !gamePaused;
    }

    @Override
    public void render(float delta) {
        refreshSunHud();
        if (previewLevel == null) {
            currentGameMenu().synchronizeProgress();
        }
        super.render(delta);
    }

    @Override
    public void dispose() {
        if (gridActor != null) {
            gridActor.dispose();
            gridActor = null;
        }
        super.dispose();
    }

    /** Board rectangle measured from the supplied 768-resolution textures. */
    private static final class BoardLayout {
        private final String backgroundAsset;
        private final float sourceWidth;
        private final float sourceHeight;
        private final float left;
        private final float top;
        private final float right;
        private final float bottom;

        private BoardLayout(String backgroundAsset,
                float sourceWidth, float sourceHeight,
                float left, float top, float right, float bottom) {
            this.backgroundAsset = backgroundAsset;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    /**
     * Draws the logical 9x5 board directly on the stretched-background stage,
     * so the red lines stay aligned even when the OS window is not 16:9.
     */
    private static final class BoardGridActor extends Actor {
        private static final float LINE_THICKNESS = 2f;
        private static final float LINE_ALPHA = 0.88f;

        private final BoardLayout layout;
        private final Texture redPixel;

        private BoardGridActor(BoardLayout layout) {
            this.layout = layout;
            setTouchable(Touchable.disabled);

            Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixel.setColor(Color.RED);
            pixel.fill();
            redPixel = new Texture(pixel);
            pixel.dispose();
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (getStage() == null) {
                return;
            }

            float worldWidth = getStage().getViewport().getWorldWidth();
            float worldHeight = getStage().getViewport().getWorldHeight();
            float boardX = worldWidth * layout.left / layout.sourceWidth;
            float boardY = worldHeight
                    * (layout.sourceHeight - layout.bottom)
                    / layout.sourceHeight;
            float boardWidth = worldWidth
                    * (layout.right - layout.left) / layout.sourceWidth;
            float boardHeight = worldHeight
                    * (layout.bottom - layout.top) / layout.sourceHeight;

            Color previous = new Color(batch.getColor());
            batch.setColor(1f, 1f, 1f, LINE_ALPHA * parentAlpha);

            for (int column = 0; column <= BOARD_COLUMNS; column++) {
                float x = boardX
                        + boardWidth * column / BOARD_COLUMNS
                        - LINE_THICKNESS / 2f;
                batch.draw(redPixel, x, boardY,
                        LINE_THICKNESS, boardHeight);
            }
            for (int row = 0; row <= BOARD_ROWS; row++) {
                float y = boardY
                        + boardHeight * row / BOARD_ROWS
                        - LINE_THICKNESS / 2f;
                batch.draw(redPixel, boardX, y,
                        boardWidth, LINE_THICKNESS);
            }

            batch.setColor(previous);
        }

        private void dispose() {
            redPixel.dispose();
        }
    }
}
