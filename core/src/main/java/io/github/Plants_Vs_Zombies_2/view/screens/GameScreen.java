package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.controller.CollectionMenuController;
import io.github.Plants_Vs_Zombies_2.controller.MainController;
import io.github.Plants_Vs_Zombies_2.controller.PlantSelectionController;
import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.Constants;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.game.Game;
import io.github.Plants_Vs_Zombies_2.model.game.ZombieWave;
import io.github.Plants_Vs_Zombies_2.model.game.PlantPlacementResult;
import io.github.Plants_Vs_Zombies_2.model.game.RewardCollectionResult;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.Coin;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.CollectibleDrop;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.Diamond;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.PlantFoodDrop;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.PotDrop;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.Sun;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.SunType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
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
    private static final String BOARD_SUN_SPECIAL =
            "IMAGE_EFFECTS_SUN_SUN_166X166";
    private static final String BOARD_SUN_RADIOACTIVE =
            "IMAGE_EFFECTS_SUN_SUN_203X203";
    private static final String GAME_SUN_BACKGROUND =
            "IMAGE_UI_HUD_INGAME_BACKGROUND_3SLICE";
    private static final String GAME_PLANT_FOOD_ICON =
            "IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON";
    private static final String DEBUG_PLUS_ICON =
            "IMAGE_UI_HUD_INGAME_COIN_BUY";
    private static final int DEBUG_SUN_INCREMENT = 200;
    private static final int DEBUG_PLANT_FOOD_INCREMENT = 1;
    private static final int PREVIEW_MAX_PLANT_FOOD = 3;
    private static final String COIN_ICON =
            "IMAGE_UI_THYMED_EVENTS_ECS_CONVRT_COIN";
    private static final String DIAMOND_ICON =
            "IMAGE_EFFECTS_COIN_DIAMOND_COIN_DIAMOND_141X146";
    private static final String BOOST_PACKET = "IMAGE_UI_PACKETS_BOOST";
    private static final String SELECTION_BACK_BUTTON_UP =
            "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_BACK_NORMAL";
    private static final String SELECTION_BACK_BUTTON_DOWN =
            "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_BACK_SELECTED";
    private static final String RESET_COOLDOWNS_BUTTON =
            "IMAGE_UI_CHOOSER_SEED_CHOOSER_RECALL_BUTTON_ICON";
    private static final String SHOVEL_BUTTON_IMAGE_ID =
            "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON";
    private static final String SHOVEL_CURSOR_IMAGE_ID =
            "IMAGE_ZEN_GARDEN_CURSORS_REMOVAL_CURSOR_REMOVAL_CURSOR_133X115";
    private static final String WAVE_PROGRESS_ZOMBIE_HEAD =
            "IMAGE_UI_HUD_INGAME_PROGRESS_METER_ZOMBIEHEAD";
    private static final String WAVE_PROGRESS_FLAG_POLE =
            "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FLAG_POLE";
    private static final String WAVE_PROGRESS_FLAG =
            "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FLAG_DEFAULT";
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
    private static final float SELECTION_BACK_BUTTON_X = PAUSE_BUTTON_X;
    private static final float SELECTION_BACK_BUTTON_Y = PAUSE_BUTTON_Y;
    private static final float SELECTION_BACK_BUTTON_SIZE = PAUSE_BUTTON_SIZE;
    private static final float RESET_COOLDOWNS_BUTTON_X = 47f;
    private static final float RESET_COOLDOWNS_BUTTON_Y = 14f;
    private static final float RESET_COOLDOWNS_BUTTON_SIZE = 54f;
    private static final float SHOVEL_BUTTON_X = VIRTUAL_WIDTH - 104f;
    private static final float SHOVEL_BUTTON_Y = 16f;
    private static final float SHOVEL_BUTTON_SIZE = 76f;
    private static final float WAVE_PROGRESS_X = 440f;
    private static final float WAVE_PROGRESS_Y = 8f;
    private static final float WAVE_PROGRESS_WIDTH = 400f;
    private static final float WAVE_PROGRESS_HEIGHT = 66f;
    private static final float SUN_HUD_X = 210f;
    private static final float SUN_HUD_Y = 648f;
    private static final float SUN_HUD_WIDTH = 218f;
    private static final float SUN_HUD_HEIGHT = 60f;
    private static final float PLANT_FOOD_HUD_X = SUN_HUD_X;
    private static final float PLANT_FOOD_HUD_Y = 586f;
    private static final float PLANT_FOOD_HUD_WIDTH = SUN_HUD_WIDTH;
    private static final float PLANT_FOOD_HUD_HEIGHT = SUN_HUD_HEIGHT;
    private static final float NORMAL_BOARD_SUN_SIZE = 108f;
    private static final float SPECIAL_BOARD_SUN_SIZE = 152f;
    private static final float RADIOACTIVE_BOARD_SUN_SIZE = 164f;
    private static final float SUN_FALL_SWAY_PIXELS = 5f;
    private static final float SUN_FLASH_HZ = 1.25f;
    private static final float PLANT_FOOD_DROP_SIZE = 72f;
    private static final float REWARD_NOTICE_SECONDS = 3f;
    private static final float GAME_ANNOUNCEMENT_SECONDS = 3f;
    private static final String FIRST_WAVE_ANNOUNCEMENT =
            "ZOMBIES ARE COMING!";
    private static final String NEXT_WAVE_ANNOUNCEMENT =
            "A HUGE WAVE OF ZOMBIES IS APPROACHING!";
    private static final String NECROMANCY_ANNOUNCEMENT =
            "NECROMANCY!";
    private static final String LOW_BEACH_ANNOUNCEMENT =
            "ZOMBIES ARE EMERGING FROM THE LOW BEACH!";

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
    private ImageButton selectionBackButton;
    private Button resetCooldownsButton;
    private Button shovelButton;
    private Image fallbackShovelCursor;
    private Cursor shovelCursor;
    private boolean shovelMode;
    private boolean usingFallbackShovelCursor;
    private WaveProgressActor waveProgressActor;
    private Table sunHud;
    private Label sunAmountLabel;
    private Table plantFoodHud;
    private Label plantFoodAmountLabel;
    private Group pauseModal;
    private boolean gamePaused;
    private boolean sunHudDebugMode;
    private boolean plantFoodHudDebugMode;
    private int previewSunCount;
    private int previewPlantFoodCount;

    private Texture plantingOverlayPixel;
    private Group plantedPlantLayer;
    private Image hoveredBoardCell;
    private Actor cursorPlantActor;
    private PlantCollectionItem selectedPlantForPlacement;
    private String plantedPlantRenderSignature = "";

    private Group sunLayer;
    private final Map<Sun, SunActor> sunActors = new IdentityHashMap<>();

    private Group collectibleDropLayer;
    private final Map<PlantFoodDrop, PlantFoodDropActor> plantFoodDropActors =
            new IdentityHashMap<>();
    private Label rewardNoticeLabel;
    private float rewardNoticeRemainingSeconds;

    private Group zombieLayer;
    private final Map<Zombie, ZombiePamActor> zombieActors =
            new IdentityHashMap<>();

    private Label gameAnnouncementLabel;
    private final List<String> queuedGameAnnouncements = new ArrayList<>();
    private float gameAnnouncementRemainingSeconds;
    private int pendingAnnouncementWaveNumber;
    private boolean startZombieWavesAfterAnnouncement;

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
        installWaveProgressHud();

        if (menu.getLevel() != null && menu.getGame().hasConfiguredPlantLoadout()) {
            installSeedTray();
            installCooldownResetButton();
            rebuildSeedTray();
        }
        installPlantingInteraction();
        installShovelButton();
        installZombieRendering();
        installSunRendering();
        installCollectibleDropRendering();
        installRewardNotice();
        installGameAnnouncementSystem();
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
        installPlantFoodHud();
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

    private void installPlantFoodHud() {
        plantFoodAmountLabel = new Label("0", skin, "medium_outline");
        plantFoodAmountLabel.setFontScale(1.05f);
        plantFoodAmountLabel.setAlignment(Align.center);

        plantFoodHud = new Table();
        plantFoodHud.left();
        plantFoodHud.setBounds(PLANT_FOOD_HUD_X, PLANT_FOOD_HUD_Y,
                PLANT_FOOD_HUD_WIDTH, PLANT_FOOD_HUD_HEIGHT);
        stage.addActor(plantFoodHud);
        plantFoodHudDebugMode = !isDebugMode();
        refreshPlantFoodHud();
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

    private void refreshPlantFoodHud() {
        if (plantFoodHud == null || plantFoodAmountLabel == null) {
            return;
        }
        boolean debugMode = isDebugMode();
        plantFoodAmountLabel.setText(currentPlantFoodCount() + "/"
                + currentMaximumPlantFoodCount());
        if (plantFoodHud.getChildren().size > 0
                && debugMode == plantFoodHudDebugMode) {
            return;
        }

        plantFoodHudDebugMode = debugMode;
        plantFoodHud.clearChildren();

        Stack amountStack = new Stack();
        Image background = createAssetImage(GAME_SUN_BACKGROUND);
        background.setScaling(Scaling.stretch);
        amountStack.add(background);

        Table amountContents = new Table();
        amountContents.left();
        Image plantFood = createAssetImage(GAME_PLANT_FOOD_ICON);
        plantFood.setScaling(Scaling.fit);
        amountContents.add(plantFood).size(56f).padLeft(2f).padRight(-4f);
        amountContents.add(plantFoodAmountLabel).width(76f).center();
        amountStack.add(amountContents);

        plantFoodHud.add(amountStack).width(150f).height(50f);

        if (debugMode) {
            Button plus = createAssetButton(DEBUG_PLUS_ICON,
                    this::addDebugPlantFood);
            plantFoodHud.add(plus).size(42f).padLeft(-5f);
        }
        plantFoodHud.invalidateHierarchy();
    }

    private int currentPlantFoodCount() {
        if (previewLevel != null) {
            return Math.min(previewPlantFoodCount, PREVIEW_MAX_PLANT_FOOD);
        }
        return Math.min(currentGameMenu().getGame().getPlantFoodCount(),
                currentMaximumPlantFoodCount());
    }

    private int currentMaximumPlantFoodCount() {
        if (previewLevel != null) {
            return PREVIEW_MAX_PLANT_FOOD;
        }
        return currentGameMenu().getGame().getMaximumPlantFoodCount();
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

    private void addDebugPlantFood() {
        if (!isDebugMode()) {
            return;
        }
        if (previewLevel != null) {
            previewPlantFoodCount = Math.min(PREVIEW_MAX_PLANT_FOOD,
                    previewPlantFoodCount + DEBUG_PLANT_FOOD_INCREMENT);
        } else {
            currentGameMenu().getGame().addPlantFood();
        }
        refreshPlantFoodHud();
    }

    private void showPauseModal() {
        if (pauseModal != null || plantSelectionModal != null) {
            return;
        }
        if (shovelMode) {
            setShovelMode(false);
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

        GameMenu menu = currentGameMenu();
        User user = currentUser();
        Chapter chapter = menu == null || menu.getChapterId() == null
                ? null
                : ChapterCatalog.findById(menu.getChapterId());
        Level level = menu == null ? null : menu.getLevel();
        if (user != null && chapter != null && level != null) {
            // Restart must never resume the checkpoint we are abandoning.
            // Delete it before entering the normal fresh-level launch flow.
            SavedGameManager.deleteAdventureGame(
                    user, menu.getChapterId(), menu.getLevelNumber());
            navigator.showLevelGamePreview(chapter, level);
            return;
        }

        closePauseModal();
        navigator.returnToCurrentMenu();
    }

    private void installGameAnnouncementSystem() {
        Game game = activeGame();
        if (game == null || gameAnnouncementLabel != null) {
            return;
        }

        game.setGuiWaveAdvanceHeld(true);

        gameAnnouncementLabel = new Label("", skin, "big_outline");
        gameAnnouncementLabel.setAlignment(Align.center);
        gameAnnouncementLabel.setWrap(true);
        gameAnnouncementLabel.setColor(Color.SCARLET);
        gameAnnouncementLabel.setFontScale(1.05f);
        gameAnnouncementLabel.setBounds(220f, 286f, 840f, 148f);
        gameAnnouncementLabel.setTouchable(Touchable.disabled);
        gameAnnouncementLabel.setVisible(false);
        stage.addActor(gameAnnouncementLabel);

        if (!game.haveZombieWavesStarted()) {
            queueWaveAnnouncements(1, true);
        } else {
            maybeQueueReadyWaveAnnouncement();
        }
    }

    private void queueWaveAnnouncements(int waveNumber,
            boolean startWavesWhenFinished) {
        Game game = activeGame();
        if (game == null || waveNumber <= 0
                || pendingAnnouncementWaveNumber != 0) {
            return;
        }

        queuedGameAnnouncements.clear();
        queuedGameAnnouncements.add(waveNumber == 1
                ? FIRST_WAVE_ANNOUNCEMENT
                : NEXT_WAVE_ANNOUNCEMENT);
        if (game.willNextWaveTriggerNecromancy()) {
            queuedGameAnnouncements.add(NECROMANCY_ANNOUNCEMENT);
        }
        if (game.willNextWaveTriggerLowBeachEmergence()) {
            queuedGameAnnouncements.add(LOW_BEACH_ANNOUNCEMENT);
        }

        pendingAnnouncementWaveNumber = waveNumber;
        startZombieWavesAfterAnnouncement = startWavesWhenFinished;
        showNextQueuedGameAnnouncement();
    }

    private void showNextQueuedGameAnnouncement() {
        if (gameAnnouncementLabel == null) {
            return;
        }
        if (queuedGameAnnouncements.isEmpty()) {
            gameAnnouncementLabel.setText("");
            gameAnnouncementLabel.setVisible(false);
            gameAnnouncementRemainingSeconds = 0f;
            finishWaveAnnouncementSequence();
            return;
        }

        String text = queuedGameAnnouncements.remove(0);
        gameAnnouncementLabel.setText(text);
        gameAnnouncementLabel.setVisible(true);
        gameAnnouncementRemainingSeconds = GAME_ANNOUNCEMENT_SECONDS;
    }

    private void updateGameAnnouncement(float deltaSeconds) {
        if (gameAnnouncementLabel == null
                || !gameAnnouncementLabel.isVisible()
                || gamePaused) {
            return;
        }
        gameAnnouncementRemainingSeconds -= Math.max(0f, deltaSeconds);
        if (gameAnnouncementRemainingSeconds <= 0f) {
            showNextQueuedGameAnnouncement();
        }
    }

    private void finishWaveAnnouncementSequence() {
        Game game = activeGame();
        int targetWave = pendingAnnouncementWaveNumber;
        boolean shouldStartWaves = startZombieWavesAfterAnnouncement;
        pendingAnnouncementWaveNumber = 0;
        startZombieWavesAfterAnnouncement = false;

        if (game == null || targetWave <= 0) {
            return;
        }
        if (shouldStartWaves && !game.startZombieWavesFromGui()) {
            return;
        }
        if (game.getNextWaveNumberForGui() == targetWave) {
            game.spawnNextWaveForGui();
        }
    }

    private void maybeQueueReadyWaveAnnouncement() {
        Game game = activeGame();
        if (game == null || gameAnnouncementLabel == null
                || pendingAnnouncementWaveNumber != 0
                || !queuedGameAnnouncements.isEmpty()
                || gameAnnouncementLabel.isVisible()
                || !game.isNextWaveReadyForGui()) {
            return;
        }
        int waveNumber = game.getNextWaveNumberForGui();
        if (waveNumber > 0) {
            queueWaveAnnouncements(waveNumber, false);
        }
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

    private void installWaveProgressHud() {
        Game game = activeGame();
        if (game == null || game.getZombieWaves().isEmpty()
                || waveProgressActor != null) {
            return;
        }
        waveProgressActor = new WaveProgressActor(game);
        waveProgressActor.setBounds(WAVE_PROGRESS_X, WAVE_PROGRESS_Y,
                WAVE_PROGRESS_WIDTH, WAVE_PROGRESS_HEIGHT);
        waveProgressActor.setTouchable(Touchable.disabled);
        stage.addActor(waveProgressActor);
    }

    private void installSeedTray() {
        seedTray = new Table();
        seedTray.top();
        seedTray.setBounds(SEED_TRAY_X, SEED_TRAY_Y,
                SEED_TRAY_WIDTH, SEED_TRAY_HEIGHT);
        stage.addActor(seedTray);
    }

    private void installCooldownResetButton() {
        if (!isModelBackedGame() || resetCooldownsButton != null) {
            return;
        }
        resetCooldownsButton = createAssetButton(RESET_COOLDOWNS_BUTTON, () -> {
            Game game = activeGame();
            if (game == null || gamePaused || pauseModal != null
                    || plantSelectionModal != null) {
                return;
            }
            game.removePlantCooldowns();
            rebuildSeedTray();
        });
        resetCooldownsButton.setBounds(RESET_COOLDOWNS_BUTTON_X,
                RESET_COOLDOWNS_BUTTON_Y, RESET_COOLDOWNS_BUTTON_SIZE,
                RESET_COOLDOWNS_BUTTON_SIZE);
        resetCooldownsButton.addListener(new TextTooltip(
                "Reset all plant cooldowns", skin));
        stage.addActor(resetCooldownsButton);
    }

    private void installShovelButton() {
        if (!isModelBackedGame() || shovelButton != null) {
            return;
        }

        shovelButton = createAssetButton(SHOVEL_BUTTON_IMAGE_ID,
                this::toggleShovelMode);
        shovelButton.setBounds(SHOVEL_BUTTON_X, SHOVEL_BUTTON_Y,
                SHOVEL_BUTTON_SIZE, SHOVEL_BUTTON_SIZE);
        shovelButton.addListener(new TextTooltip("Shovel", skin));
        stage.addActor(shovelButton);

        fallbackShovelCursor = createAssetImage(SHOVEL_CURSOR_IMAGE_ID);
        fallbackShovelCursor.setScaling(Scaling.fit);
        fallbackShovelCursor.setSize(46f, 86f);
        fallbackShovelCursor.setTouchable(Touchable.disabled);
        fallbackShovelCursor.setVisible(false);
        stage.addActor(fallbackShovelCursor);
    }

    private void toggleShovelMode() {
        setShovelMode(!shovelMode);
    }

    private void setShovelMode(boolean enabled) {
        if (enabled && !canUseShovel()) {
            return;
        }
        if (enabled && selectedPlantForPlacement != null) {
            clearSelectedPlantForPlacement();
        }
        shovelMode = enabled;
        if (shovelButton != null) {
            shovelButton.setColor(enabled
                    ? new Color(1f, 1f, 0.8f, 1f) : Color.WHITE);
        }
        if (enabled) {
            applyShovelCursor();
        } else {
            resetShovelCursor();
            if (hoveredBoardCell != null) {
                hoveredBoardCell.setVisible(false);
            }
        }
    }

    private boolean canUseShovel() {
        return activeGame() != null
                && !gamePaused
                && pauseModal == null
                && plantSelectionModal == null;
    }

    private void applyShovelCursor() {
        try {
            if (shovelCursor == null) {
                shovelCursor = createCursorFromRegion(
                        requireAssetRegion(SHOVEL_CURSOR_IMAGE_ID), 10, 8);
            }
            Gdx.graphics.setCursor(shovelCursor);
            if (fallbackShovelCursor != null) {
                fallbackShovelCursor.setVisible(false);
            }
            usingFallbackShovelCursor = false;
        } catch (RuntimeException exception) {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
            if (fallbackShovelCursor != null) {
                fallbackShovelCursor.setVisible(true);
            }
            usingFallbackShovelCursor = true;
        }
    }

    private void resetShovelCursor() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        if (fallbackShovelCursor != null) {
            fallbackShovelCursor.setVisible(false);
        }
        usingFallbackShovelCursor = false;
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

        if (isModelBackedGame()) {
            BasePlant prototype = loadoutPrototypeFor(plant.getName());
            if (prototype != null) {
                CooldownShadeActor cooldownShade =
                        new CooldownShadeActor(prototype);
                cooldownShade.setTouchable(Touchable.disabled);
                slot.add(cooldownShade);
            }

            SunAffordabilityShadeActor affordabilityShade =
                    new SunAffordabilityShadeActor(plant);
            affordabilityShade.setTouchable(Touchable.disabled);
            slot.add(affordabilityShade);

            if (isSelectedForPlacement(plant)) {
                SelectionOutlineActor outline = new SelectionOutlineActor();
                outline.setTouchable(Touchable.disabled);
                slot.add(outline);
            }
            slot.addListener(new ClickListener(Input.Buttons.LEFT) {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectPlantForPlacement(plant);
                }
            });
        }

        slot.addListener(new TextTooltip(
                plant.getName() + (isVisuallyBoosted(plant)
                        ? "\nBoosted" : ""), skin));
        return slot;
    }

    private void installPlantingInteraction() {
        if (!isModelBackedGame() || plantingOverlayPixel != null) {
            return;
        }

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        plantingOverlayPixel = new Texture(pixel);
        pixel.dispose();

        plantedPlantLayer = new Group();
        plantedPlantLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(plantedPlantLayer);

        hoveredBoardCell = new Image(plantingOverlayPixel);
        hoveredBoardCell.setTouchable(Touchable.disabled);
        hoveredBoardCell.setColor(1f, 1f, 1f, 0.28f);
        hoveredBoardCell.setVisible(false);
        addBackgroundOverlay(hoveredBoardCell);

        stage.getRoot().addCaptureListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y,
                    int pointer, int button) {
                if (button == Input.Buttons.RIGHT) {
                    if (shovelMode) {
                        setShovelMode(false);
                        event.stop();
                        return true;
                    }
                    if (selectedPlantForPlacement != null) {
                        clearSelectedPlantForPlacement();
                        event.stop();
                        return true;
                    }
                    return false;
                }
                if (button != Input.Buttons.LEFT) {
                    return false;
                }

                EntityPosition boardPosition = boardPositionAtScreen(
                        Gdx.input.getX(), Gdx.input.getY());
                if (boardPosition == null) {
                    return false;
                }

                if (shovelMode && canUseShovel()) {
                    pluckPlantAt(boardPosition);
                    event.stop();
                    return true;
                }
                if (!canInteractWithBoard()
                        || selectedPlantForPlacement == null) {
                    return false;
                }

                plantSelectedPlantAt(boardPosition);
                event.stop();
                return true;
            }
        });

        rebuildPlantedPlantLayer();
    }

    private boolean isModelBackedGame() {
        return previewLevel == null
                && App.getInstance().getCurrentMenu() instanceof GameMenu;
    }

    private Game activeGame() {
        return isModelBackedGame() ? currentGameMenu().getGame() : null;
    }

    private boolean canInteractWithBoard() {
        Game game = activeGame();
        return game != null
                && !gamePaused
                && pauseModal == null
                && plantSelectionModal == null
                && game.allowsDirectPlanting();
    }

    private BasePlant loadoutPrototypeFor(String plantName) {
        Game game = activeGame();
        if (game == null || plantName == null) {
            return null;
        }
        for (BasePlant prototype : game.getPlantLoadoutPrototypes()) {
            if (prototype.getName().equalsIgnoreCase(plantName)) {
                return prototype;
            }
        }
        return null;
    }

    private boolean isSelectedForPlacement(PlantCollectionItem plant) {
        return plant != null && selectedPlantForPlacement != null
                && selectedPlantForPlacement.getName()
                        .equalsIgnoreCase(plant.getName());
    }

    private void selectPlantForPlacement(PlantCollectionItem plant) {
        if (shovelMode) {
            setShovelMode(false);
        }
        if (!canInteractWithBoard() || plant == null) {
            return;
        }
        BasePlant prototype = loadoutPrototypeFor(plant.getName());
        if (prototype == null) {
            return;
        }
        if (isPlantCoolingDown(prototype)) {
            showGameNotice(plant.getName() + " is still recharging!",
                    Color.RED);
            return;
        }
        if (plant.getCost() > currentSunCount()) {
            showGameNotice("Not enough sun for " + plant.getName() + "!",
                    Color.RED);
            return;
        }
        selectedPlantForPlacement = plant;
        rebuildCursorPlantActor();
        rebuildSeedTray();
    }

    private void clearSelectedPlantForPlacement() {
        selectedPlantForPlacement = null;
        if (cursorPlantActor != null) {
            cursorPlantActor.remove();
            cursorPlantActor = null;
        }
        if (hoveredBoardCell != null) {
            hoveredBoardCell.setVisible(false);
        }
        rebuildSeedTray();
    }

    private boolean isPlantCoolingDown(BasePlant prototype) {
        Game game = activeGame();
        return game != null && prototype != null
                && game.getPlantCooldownRemainingSeconds(prototype) > 0.001;
    }

    private float cooldownFraction(BasePlant prototype) {
        Game game = activeGame();
        if (game == null || prototype == null
                || prototype.getRechargeSeconds() <= 0f) {
            return 0f;
        }
        double remaining = game.getPlantCooldownRemainingSeconds(prototype);
        return Math.max(0f, Math.min(1f,
                (float) (remaining / prototype.getRechargeSeconds())));
    }

    private void plantSelectedPlantAt(EntityPosition position) {
        Game game = activeGame();
        if (game == null || selectedPlantForPlacement == null
                || position == null) {
            return;
        }

        BasePlant plant = game.createPlantFromLoadout(
                selectedPlantForPlacement.getName(), position);
        if (plant == null) {
            return;
        }
        PlantPlacementResult result = game.plant(plant);
        if (result != PlantPlacementResult.SUCCESS) {
            return;
        }

        selectedPlantForPlacement = null;
        if (cursorPlantActor != null) {
            cursorPlantActor.remove();
            cursorPlantActor = null;
        }
        if (hoveredBoardCell != null) {
            hoveredBoardCell.setVisible(false);
        }
        refreshSunHud();
        rebuildSeedTray();
        rebuildPlantedPlantLayer();
    }

    private void pluckPlantAt(EntityPosition position) {
        Game game = activeGame();
        if (game == null || position == null
                || game.isProtectedSeedAt(position)) {
            return;
        }
        BasePlant removed = game.pluckPlantAt(position);
        if (removed == null) {
            return;
        }
        rebuildPlantedPlantLayer();
        refreshBoardHover();
    }

    private EntityPosition boardPositionAtScreen(int screenX, int screenY) {
        BoardLayout layout = layoutForChapter(seedTrayChapter());
        if (layout == null || Gdx.graphics.getWidth() <= 0
                || Gdx.graphics.getHeight() <= 0) {
            return null;
        }

        float sourceX = screenX * layout.sourceWidth
                / Gdx.graphics.getWidth();
        float sourceY = screenY * layout.sourceHeight
                / Gdx.graphics.getHeight();
        if (sourceX < layout.left || sourceX >= layout.right
                || sourceY < layout.top || sourceY >= layout.bottom) {
            return null;
        }

        int column = (int) ((sourceX - layout.left)
                / (layout.right - layout.left) * BOARD_COLUMNS);
        int row = (int) ((sourceY - layout.top)
                / (layout.bottom - layout.top) * BOARD_ROWS);
        if (row < 0 || row >= BOARD_ROWS
                || column < 0 || column >= BOARD_COLUMNS) {
            return null;
        }
        return new EntityPosition(row, column);
    }

    private CellBounds screenBoundsForCell(EntityPosition position) {
        BoardLayout layout = layoutForChapter(seedTrayChapter());
        if (layout == null || position == null) {
            return null;
        }
        float windowWidth = Gdx.graphics.getWidth();
        float windowHeight = Gdx.graphics.getHeight();
        float boardX = windowWidth * layout.left / layout.sourceWidth;
        float boardY = windowHeight
                * (layout.sourceHeight - layout.bottom)
                / layout.sourceHeight;
        float boardWidth = windowWidth
                * (layout.right - layout.left) / layout.sourceWidth;
        float boardHeight = windowHeight
                * (layout.bottom - layout.top) / layout.sourceHeight;
        float cellWidth = boardWidth / BOARD_COLUMNS;
        float cellHeight = boardHeight / BOARD_ROWS;
        return new CellBounds(
                boardX + position.getColumn() * cellWidth,
                boardY + (BOARD_ROWS - 1 - position.getRow()) * cellHeight,
                cellWidth, cellHeight);
    }

    private void refreshBoardHover() {
        if (hoveredBoardCell == null) {
            return;
        }

        boolean planting = selectedPlantForPlacement != null
                && canInteractWithBoard();
        boolean shoveling = shovelMode && canUseShovel();
        if (!planting && !shoveling) {
            hoveredBoardCell.setVisible(false);
            return;
        }

        EntityPosition position = boardPositionAtScreen(
                Gdx.input.getX(), Gdx.input.getY());
        if (position == null) {
            hoveredBoardCell.setVisible(false);
            return;
        }
        if (shoveling) {
            Game game = activeGame();
            if (game == null || game.isProtectedSeedAt(position)
                    || game.getBoard().getPlantAt(position) == null) {
                hoveredBoardCell.setVisible(false);
                return;
            }
        }

        CellBounds bounds = screenBoundsForCell(position);
        if (bounds == null) {
            hoveredBoardCell.setVisible(false);
            return;
        }
        hoveredBoardCell.setBounds(
                bounds.x, bounds.y, bounds.width, bounds.height);
        hoveredBoardCell.setVisible(true);
    }

    private void rebuildCursorPlantActor() {
        if (cursorPlantActor != null) {
            cursorPlantActor.remove();
            cursorPlantActor = null;
        }
        if (selectedPlantForPlacement == null) {
            return;
        }
        cursorPlantActor = createPlantIdleActor(
                selectedPlantForPlacement.getName());
        cursorPlantActor.setTouchable(Touchable.disabled);
        cursorPlantActor.setColor(1f, 1f, 1f, 0.86f);
        addBackgroundOverlay(cursorPlantActor);
        refreshCursorPlantPosition();
    }

    private void refreshCursorPlantPosition() {
        if (selectedPlantForPlacement != null
                && selectedPlantForPlacement.getCost() > currentSunCount()) {
            clearSelectedPlantForPlacement();
            return;
        }
        if (cursorPlantActor == null) {
            return;
        }
        if (!canInteractWithBoard() || selectedPlantForPlacement == null) {
            cursorPlantActor.setVisible(false);
            return;
        }
        BoardLayout layout = layoutForChapter(seedTrayChapter());
        if (layout == null) {
            cursorPlantActor.setVisible(false);
            return;
        }
        float cellWidth = Gdx.graphics.getWidth()
                * (layout.right - layout.left) / layout.sourceWidth
                / BOARD_COLUMNS;
        float cellHeight = Gdx.graphics.getHeight()
                * (layout.bottom - layout.top) / layout.sourceHeight
                / BOARD_ROWS;
        float width = cellWidth * 0.92f;
        float height = cellHeight * 1.18f;
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        cursorPlantActor.setBounds(
                mouseX - width * 0.5f,
                mouseY - height * 0.42f,
                width, height);
        cursorPlantActor.setVisible(true);
    }

    private Actor createPlantIdleActor(String plantName) {
        PlantAnimationCatalog.Preview preview =
                PlantAnimationCatalog.find(plantName);
        if (preview != null) {
            try {
                return new PamAnimationActor(
                        navigator.getPamPlayer(),
                        preview.getPath(), preview.getClip());
            } catch (RuntimeException ignored) {
                // Fall through to packet artwork when an optional PAM is absent.
            }
        }
        Image fallback = createAssetImage(
                PlantPacketCard.packetAssetFor(plantName));
        fallback.setScaling(Scaling.fit);
        return fallback;
    }

    private void rebuildPlantedPlantLayer() {
        Game game = activeGame();
        if (plantedPlantLayer == null || game == null) {
            return;
        }
        plantedPlantLayer.clearChildren();
        for (BasePlant plant : game.getBoard().getPlants()) {
            Actor actor = createPlantIdleActor(plant.getName());
            actor.setTouchable(Touchable.disabled);
            positionPlantActor(actor, plant.getEntityPosition());
            plantedPlantLayer.addActor(actor);
        }
        plantedPlantRenderSignature = createPlantRenderSignature(game);
    }

    private void positionPlantActor(Actor actor, EntityPosition position) {
        CellBounds bounds = screenBoundsForCell(position);
        if (actor == null || bounds == null) {
            return;
        }
        float width = bounds.width * 0.90f;
        float height = bounds.height * 1.16f;
        actor.setBounds(
                bounds.x + (bounds.width - width) * 0.5f,
                bounds.y - bounds.height * 0.03f,
                width, height);
    }

    private String createPlantRenderSignature(Game game) {
        StringBuilder signature = new StringBuilder();
        for (BasePlant plant : game.getBoard().getPlants()) {
            EntityPosition position = plant.getEntityPosition();
            signature.append(plant.getName()).append('@')
                    .append(position == null ? "?" : position.getRow())
                    .append(',')
                    .append(position == null ? "?" : position.getColumn())
                    .append(';');
        }
        return signature.toString();
    }

    private void refreshPlantedPlantLayerIfNeeded() {
        Game game = activeGame();
        if (game == null || plantedPlantLayer == null) {
            return;
        }
        String signature = createPlantRenderSignature(game);
        if (!signature.equals(plantedPlantRenderSignature)) {
            rebuildPlantedPlantLayer();
        }
    }

    private void installZombieRendering() {
        if (!isModelBackedGame() || zombieLayer != null) {
            return;
        }
        zombieLayer = new Group();
        zombieLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(zombieLayer);
        refreshZombieRendering();
    }

    private void refreshZombieRendering() {
        Game game = activeGame();
        if (game == null || zombieLayer == null) {
            return;
        }

        List<Zombie> currentZombies = game.getBoard().getZombies();
        IdentityHashMap<Zombie, Boolean> present = new IdentityHashMap<>();
        for (Zombie zombie : currentZombies) {
            if (zombie == null || zombie.isDead() || zombie.isRemoved()) {
                continue;
            }
            present.put(zombie, Boolean.TRUE);
            ZombiePamActor actor = zombieActors.get(zombie);
            if (actor == null) {
                ZombieVisualCatalog.Visual visual =
                        ZombieVisualCatalog.find(zombie.getType());
                if (visual == null) {
                    continue;
                }
                try {
                    actor = new ZombiePamActor(
                            navigator.getPamPlayer(), zombie, visual);
                } catch (RuntimeException ignored) {
                    // A missing optional PAM should not stop the game model.
                    continue;
                }
                actor.setTouchable(Touchable.disabled);
                zombieActors.put(zombie, actor);
                zombieLayer.addActor(actor);
            }
            actor.setEating(isZombieEatingPlant(game, zombie));
            positionZombieActor(actor, zombie);
        }

        Iterator<Map.Entry<Zombie, ZombiePamActor>> iterator =
                zombieActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Zombie, ZombiePamActor> entry = iterator.next();
            if (!present.containsKey(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }
    }

    private boolean isZombieEatingPlant(Game game, Zombie zombie) {
        if (game == null || zombie == null || zombie.isDead()
                || zombie.isHypnotized() || zombie.isFrozen()
                || zombie.isStunned() || zombie.isEncasedInIce()) {
            return false;
        }

        BasePlant nearest = null;
        int nearestColumn = -1;
        for (BasePlant plant : game.getBoard().getPlants()) {
            if (plant == null || plant.isRemoved() || plant.isDestroyed()
                    || plant.isTransformedToSheep()
                    || plant.getEntityPosition() == null
                    || plant.getEntityPosition().getRow() != zombie.getLane()) {
                continue;
            }
            int column = plant.getEntityPosition().getColumn();
            if (column <= zombie.getColumnPosition() + 0.001
                    && column > nearestColumn) {
                nearest = plant;
                nearestColumn = column;
            }
        }
        if (nearest == null) {
            return false;
        }
        double attackColumn = nearest.getEntityPosition().getColumn()
                + Zombie.ATTACK_REACH;
        return zombie.getColumnPosition() <= attackColumn + 0.001;
    }

    private void positionZombieActor(ZombiePamActor actor, Zombie zombie) {
        BoardLayout layout = layoutForChapter(seedTrayChapter());
        if (actor == null || zombie == null || layout == null
                || Gdx.graphics.getWidth() <= 0
                || Gdx.graphics.getHeight() <= 0) {
            return;
        }

        float windowWidth = Gdx.graphics.getWidth();
        float windowHeight = Gdx.graphics.getHeight();
        float boardX = windowWidth * layout.left / layout.sourceWidth;
        float boardY = windowHeight
                * (layout.sourceHeight - layout.bottom)
                / layout.sourceHeight;
        float boardWidth = windowWidth
                * (layout.right - layout.left) / layout.sourceWidth;
        float boardHeight = windowHeight
                * (layout.bottom - layout.top) / layout.sourceHeight;
        float cellWidth = boardWidth / BOARD_COLUMNS;
        float cellHeight = boardHeight / BOARD_ROWS;

        float centerX = boardX
                + (float) (zombie.getColumnPosition() + 0.5) * cellWidth;
        float laneBottom = boardY
                + (BOARD_ROWS - 1 - zombie.getLane()) * cellHeight;

        float widthScale = zombie.getType().isLarge() ? 1.55f : 1.08f;
        float heightScale = zombie.getType().isLarge() ? 2.25f : 1.62f;
        switch (zombie.getType()) {
        case IMP:
        case EGYPT_IMP:
        case ICEAGE_IMP:
        case BEACH_IMP:
        case DARK_IMP:
        case DRAGON_IMP:
        case WEASEL:
            widthScale = 0.88f;
            heightScale = 1.22f;
            break;
        default:
            break;
        }

        float width = cellWidth * widthScale;
        float height = cellHeight * heightScale;
        // Keep the zombie feet slightly inside their model lane. The previous
        // full-cell offset was compensating for the old PAM Y-axis anchoring
        // bug; now that the PAM foot anchor is correct, that extra lane shift
        // would place every zombie one row too low.
        float footLine = laneBottom + cellHeight * 0.18f;
        actor.setBounds(centerX - width * 0.5f,
                footLine, width, height);
        actor.setVisible(true);
    }

    private void installSunRendering() {
        if (!isModelBackedGame() || sunLayer != null) {
            return;
        }
        sunLayer = new Group();
        sunLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(sunLayer);
        refreshSunRendering();
    }

    private void refreshSunRendering() {
        Game game = activeGame();
        if (game == null || sunLayer == null) {
            return;
        }

        List<Sun> currentSuns = game.getBoard().getSuns();
        IdentityHashMap<Sun, Boolean> present = new IdentityHashMap<>();
        for (Sun sun : currentSuns) {
            present.put(sun, Boolean.TRUE);
            SunActor actor = sunActors.get(sun);
            if (actor == null) {
                actor = new SunActor(sun);
                actor.setTouchable(Touchable.disabled);
                sunActors.put(sun, actor);
                sunLayer.addActor(actor);
            }
            positionSunActor(actor);
        }

        Iterator<Map.Entry<Sun, SunActor>> iterator =
                sunActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Sun, SunActor> entry = iterator.next();
            if (!present.containsKey(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }

        collectHoveredSuns(currentSuns);
    }

    private void positionSunActor(SunActor actor) {
        if (actor == null || actor.sun == null) {
            return;
        }
        CellBounds cell = screenBoundsForCell(actor.sun.getEntityPosition());
        if (cell == null) {
            actor.setVisible(false);
            return;
        }

        float size = boardSunSize(actor.sun);
        float centerX = cell.x + cell.width * 0.5f;
        float targetY = cell.y + cell.height * 0.52f;
        float centerY = targetY;

        if (actor.sun.isDropping()) {
            float progress = 1f - actor.sun.getRemainingFallSeconds()
                    / Constants.SKY_SUN_FALL_SECONDS;
            progress = Math.max(0f, Math.min(1f, progress));
            float startY = Gdx.graphics.getHeight() + size * 0.6f;
            centerY = startY + (targetY - startY) * progress;
            centerX += (float) Math.sin(progress * Math.PI * 4.0)
                    * SUN_FALL_SWAY_PIXELS;
        }

        actor.setBounds(centerX - size * 0.5f,
                centerY - size * 0.5f, size, size);
        actor.setVisible(true);
    }

    private float boardSunSize(Sun sun) {
        if (sun != null && sun.getType() == SunType.SPECIAL) {
            return SPECIAL_BOARD_SUN_SIZE;
        }
        if (sun != null && sun.getType() == SunType.RADIOACTIVE) {
            return RADIOACTIVE_BOARD_SUN_SIZE;
        }
        return NORMAL_BOARD_SUN_SIZE;
    }

    private void collectHoveredSuns(List<Sun> currentSuns) {
        if (!canCollectSuns() || currentSuns == null || currentSuns.isEmpty()) {
            return;
        }
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        List<Sun> hovered = new ArrayList<>();
        for (Sun sun : currentSuns) {
            SunActor actor = sunActors.get(sun);
            if (actor != null && actor.isVisible()
                    && mouseX >= actor.getX()
                    && mouseX <= actor.getX() + actor.getWidth()
                    && mouseY >= actor.getY()
                    && mouseY <= actor.getY() + actor.getHeight()) {
                hovered.add(sun);
            }
        }
        if (hovered.isEmpty()) {
            return;
        }

        Game game = activeGame();
        boolean collectedAny = false;
        for (Sun sun : hovered) {
            if (game.collectSun(sun)) {
                collectedAny = true;
                SunActor actor = sunActors.remove(sun);
                if (actor != null) {
                    actor.remove();
                }
            }
        }
        if (collectedAny) {
            refreshSunHud();
        }
    }

    private boolean canCollectSuns() {
        return isModelBackedGame()
                && !gamePaused
                && pauseModal == null
                && plantSelectionModal == null;
    }

    private void installCollectibleDropRendering() {
        if (!isModelBackedGame() || collectibleDropLayer != null) {
            return;
        }
        collectibleDropLayer = new Group();
        collectibleDropLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(collectibleDropLayer);
        refreshCollectibleDrops();
    }

    private void installRewardNotice() {
        if (!isModelBackedGame() || rewardNoticeLabel != null) {
            return;
        }
        rewardNoticeLabel = new Label("", skin, "big_outline");
        rewardNoticeLabel.setAlignment(Align.center);
        rewardNoticeLabel.setWrap(true);
        rewardNoticeLabel.setColor(Color.WHITE);
        rewardNoticeLabel.setFontScale(0.78f);
        rewardNoticeLabel.setBounds(410f, 558f, 600f, 62f);
        rewardNoticeLabel.setTouchable(Touchable.disabled);
        rewardNoticeLabel.setVisible(false);
        stage.addActor(rewardNoticeLabel);
    }

    private void refreshCollectibleDrops() {
        Game game = activeGame();
        if (game == null || collectibleDropLayer == null) {
            return;
        }

        List<CollectibleDrop> currentDrops =
                game.getBoard().getCollectibleDrops();
        autoCollectRewardDrops(game, currentDrops);

        IdentityHashMap<PlantFoodDrop, Boolean> present =
                new IdentityHashMap<>();
        for (CollectibleDrop drop : game.getBoard().getCollectibleDrops()) {
            if (!(drop instanceof PlantFoodDrop)) {
                continue;
            }
            PlantFoodDrop plantFood = (PlantFoodDrop) drop;
            present.put(plantFood, Boolean.TRUE);
            PlantFoodDropActor actor = plantFoodDropActors.get(plantFood);
            if (actor == null) {
                actor = new PlantFoodDropActor(plantFood);
                actor.setTouchable(Touchable.disabled);
                plantFoodDropActors.put(plantFood, actor);
                collectibleDropLayer.addActor(actor);
            }
            positionPlantFoodDropActor(actor);
        }

        Iterator<Map.Entry<PlantFoodDrop, PlantFoodDropActor>> iterator =
                plantFoodDropActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PlantFoodDrop, PlantFoodDropActor> entry =
                    iterator.next();
            if (!present.containsKey(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }

        collectHoveredPlantFood();
    }

    private void autoCollectRewardDrops(Game game,
            List<CollectibleDrop> currentDrops) {
        User user = currentUser();
        if (game == null || user == null || currentDrops == null
                || currentDrops.isEmpty()) {
            return;
        }

        List<EntityPosition> rewardPositions = new ArrayList<>();
        for (CollectibleDrop drop : currentDrops) {
            if (!(drop instanceof Coin) && !(drop instanceof Diamond)
                    && !(drop instanceof PotDrop)) {
                continue;
            }
            EntityPosition position = drop.getEntityPosition();
            if (position != null && !rewardPositions.contains(position)) {
                rewardPositions.add(position);
            }
        }
        if (rewardPositions.isEmpty()) {
            return;
        }

        int coins = 0;
        int diamonds = 0;
        int pots = 0;
        int count = 0;
        for (EntityPosition position : rewardPositions) {
            RewardCollectionResult result =
                    game.collectRewardDropsAt(position, user);
            count += result.getDropCount();
            coins += result.getCoins();
            diamonds += result.getDiamonds();
            pots += result.getPots();
        }
        if (count <= 0) {
            return;
        }

        UserManager.saveAllUsers();
        showRewardNotice(coins, diamonds, pots);
    }

    private void showRewardNotice(int coins, int diamonds, int pots) {
        if (rewardNoticeLabel == null) {
            return;
        }
        List<String> earned = new ArrayList<>();
        if (coins > 0) {
            earned.add("+" + coins + " coin" + (coins == 1 ? "" : "s"));
        }
        if (diamonds > 0) {
            earned.add("+" + diamonds + " diamond"
                    + (diamonds == 1 ? "" : "s"));
        }
        if (pots > 0) {
            earned.add("+" + pots + " pot" + (pots == 1 ? "" : "s"));
        }
        if (earned.isEmpty()) {
            return;
        }
        showGameNotice("Earned " + String.join(", ", earned) + "!");
    }

    private void showGameNotice(String text) {
        showGameNotice(text, Color.WHITE);
    }

    private void showGameNotice(String text, Color color) {
        if (rewardNoticeLabel == null || text == null || text.isBlank()) {
            return;
        }
        rewardNoticeLabel.setText(text);
        rewardNoticeLabel.setColor(color == null ? Color.WHITE : color);
        rewardNoticeLabel.setVisible(true);
        rewardNoticeRemainingSeconds = REWARD_NOTICE_SECONDS;
        rewardNoticeLabel.toFront();
    }

    private void updateRewardNotice(float deltaSeconds) {
        if (rewardNoticeLabel == null || !rewardNoticeLabel.isVisible()
                || gamePaused) {
            return;
        }
        rewardNoticeRemainingSeconds -= Math.max(0f, deltaSeconds);
        if (rewardNoticeRemainingSeconds <= 0f) {
            rewardNoticeRemainingSeconds = 0f;
            rewardNoticeLabel.setText("");
            rewardNoticeLabel.setVisible(false);
        }
    }

    private void positionPlantFoodDropActor(PlantFoodDropActor actor) {
        if (actor == null || actor.drop == null) {
            return;
        }
        CellBounds cell = screenBoundsForCell(actor.drop.getEntityPosition());
        if (cell == null) {
            actor.setVisible(false);
            return;
        }
        float centerX = cell.x + cell.width * 0.5f;
        float centerY = cell.y + cell.height * 0.52f;
        actor.setBounds(centerX - PLANT_FOOD_DROP_SIZE * 0.5f,
                centerY - PLANT_FOOD_DROP_SIZE * 0.5f,
                PLANT_FOOD_DROP_SIZE, PLANT_FOOD_DROP_SIZE);
        actor.setVisible(true);
    }

    private void collectHoveredPlantFood() {
        if (!canCollectSuns() || plantFoodDropActors.isEmpty()) {
            return;
        }
        Game game = activeGame();
        if (game == null
                || game.getPlantFoodCount() >= game.getMaximumPlantFoodCount()) {
            return;
        }

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        List<EntityPosition> hoveredPositions = new ArrayList<>();
        for (Map.Entry<PlantFoodDrop, PlantFoodDropActor> entry
                : plantFoodDropActors.entrySet()) {
            PlantFoodDropActor actor = entry.getValue();
            if (actor == null || !actor.isVisible()
                    || mouseX < actor.getX()
                    || mouseX > actor.getX() + actor.getWidth()
                    || mouseY < actor.getY()
                    || mouseY > actor.getY() + actor.getHeight()) {
                continue;
            }
            EntityPosition position = entry.getKey().getEntityPosition();
            if (position != null && !hoveredPositions.contains(position)) {
                hoveredPositions.add(position);
            }
        }

        boolean collectedAny = false;
        for (EntityPosition position : hoveredPositions) {
            if (game.collectPlantFoodDropsAt(position) > 0) {
                collectedAny = true;
            }
            if (game.getPlantFoodCount() >= game.getMaximumPlantFoodCount()) {
                break;
            }
        }
        if (collectedAny) {
            refreshPlantFoodHud();
        }
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
        installSelectionBackButton();
        if (pauseButton != null) {
            pauseButton.setVisible(false);
        }

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
        if (selectionBackButton != null) {
            selectionBackButton.toFront();
        }
        refreshPlantSelectionUi();
    }

    private void installSelectionBackButton() {
        if (selectionBackButton != null) {
            selectionBackButton.setVisible(true);
            return;
        }

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(
                requireAssetRegion(SELECTION_BACK_BUTTON_UP));
        style.imageDown = new TextureRegionDrawable(
                requireAssetRegion(SELECTION_BACK_BUTTON_DOWN));
        style.imageOver = style.imageDown;

        selectionBackButton = new ImageButton(style);
        selectionBackButton.getImage().setScaling(Scaling.fit);
        selectionBackButton.setBounds(SELECTION_BACK_BUTTON_X,
                SELECTION_BACK_BUTTON_Y, SELECTION_BACK_BUTTON_SIZE,
                SELECTION_BACK_BUTTON_SIZE);
        selectionBackButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.exitGameToAdventure();
            }
        });
        selectionBackButton.addListener(new TextTooltip(
                "Back to Adventure", skin));
        stage.addActor(selectionBackButton);
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
        if (selectionBackButton != null) {
            selectionBackButton.setVisible(false);
        }
        if (pauseButton != null) {
            pauseButton.setVisible(true);
        }
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

    private float currentGameSpeed() {
        User user = currentUser();
        if (user == null || user.getSettings() == null) {
            return 1f;
        }
        return Math.max(1f, Math.min(3f,
                user.getSettings().getGameSpeed()));
    }

    @Override
    protected boolean shouldAdvanceScene() {
        return !gamePaused;
    }

    @Override
    protected float getSceneTimeScale() {
        return currentGameSpeed();
    }

    private void refreshFallbackShovelCursorPosition() {
        if (!usingFallbackShovelCursor || !shovelMode
                || fallbackShovelCursor == null) {
            return;
        }
        com.badlogic.gdx.math.Vector2 stageCoords =
                stage.screenToStageCoordinates(
                        new com.badlogic.gdx.math.Vector2(
                                Gdx.input.getX(), Gdx.input.getY()));
        fallbackShovelCursor.setPosition(stageCoords.x - 6f,
                stageCoords.y - fallbackShovelCursor.getHeight() + 14f);
        fallbackShovelCursor.toFront();
    }

    @Override
    public void render(float delta) {
        if (isModelBackedGame() && !gamePaused) {
            updateGameAnnouncement(Math.min(delta, 0.10f));
            updateRewardNotice(Math.min(delta, 0.10f));
            float gameDelta = Math.min(delta, 1f / 15f)
                    * currentGameSpeed();
            activeGame().update(gameDelta);
            maybeQueueReadyWaveAnnouncement();
        }

        refreshZombieRendering();
        refreshSunRendering();
        refreshCollectibleDrops();
        refreshSunHud();
        refreshPlantFoodHud();
        refreshBoardHover();
        refreshCursorPlantPosition();
        refreshFallbackShovelCursorPosition();
        refreshPlantedPlantLayerIfNeeded();
        if (previewLevel == null) {
            currentGameMenu().synchronizeProgress();
        }
        super.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (isModelBackedGame()) {
            rebuildPlantedPlantLayer();
            refreshZombieRendering();
            refreshSunRendering();
            refreshCollectibleDrops();
            refreshBoardHover();
            refreshCursorPlantPosition();
        }
    }

    @Override
    public void hide() {
        if (shovelMode) {
            shovelMode = false;
            if (shovelButton != null) {
                shovelButton.setColor(Color.WHITE);
            }
        }
        resetShovelCursor();
        super.hide();
    }

    @Override
    public void dispose() {
        if (gridActor != null) {
            gridActor.dispose();
            gridActor = null;
        }
        if (plantingOverlayPixel != null) {
            plantingOverlayPixel.dispose();
            plantingOverlayPixel = null;
        }
        sunActors.clear();
        plantFoodDropActors.clear();
        collectibleDropLayer = null;
        rewardNoticeLabel = null;
        if (waveProgressActor != null) {
            waveProgressActor.dispose();
            waveProgressActor = null;
        }
        sunLayer = null;
        zombieActors.clear();
        zombieLayer = null;
        queuedGameAnnouncements.clear();
        gameAnnouncementLabel = null;
        if (shovelCursor != null) {
            shovelCursor.dispose();
            shovelCursor = null;
        }
        super.dispose();
    }

    private final class PlantFoodDropActor extends Actor {
        private final PlantFoodDrop drop;

        private PlantFoodDropActor(PlantFoodDrop drop) {
            this.drop = drop;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (drop == null || drop.isRemoved()) {
                return;
            }
            Color old = new Color(batch.getColor());
            float alpha = getColor().a * parentAlpha;
            double remainingSeconds = drop.getLifeSpanSeconds()
                    - drop.getElapsedSeconds();
            if (remainingSeconds > 0.0
                    && remainingSeconds <= Constants.SUN_DESPAWN_WARNING_SECONDS) {
                int flashPhase = (int) Math.floor(
                        drop.getElapsedSeconds() * SUN_FLASH_HZ * 2f);
                if ((flashPhase & 1) != 0) {
                    alpha = 0f;
                }
            }
            batch.setColor(getColor().r, getColor().g, getColor().b, alpha);
            batch.draw(requireAssetRegion(GAME_PLANT_FOOD_ICON),
                    getX(), getY(), getWidth(), getHeight());
            batch.setColor(old);
        }
    }

    private final class SunActor extends Actor {
        private final Sun sun;

        private SunActor(Sun sun) {
            this.sun = sun;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (sun == null || sun.isRemoved()) {
                return;
            }
            Color old = new Color(batch.getColor());
            float alpha = getColor().a * parentAlpha;
            if (sun.isCloseToDespawning()) {
                int flashPhase = (int) Math.floor(
                        sun.getElapsedGroundSeconds() * SUN_FLASH_HZ * 2f);
                if ((flashPhase & 1) != 0) {
                    alpha = 0f;
                }
            }
            batch.setColor(getColor().r, getColor().g, getColor().b, alpha);
            batch.draw(requireAssetRegion(boardSunAsset(sun)),
                    getX(), getY(), getWidth(), getHeight());
            batch.setColor(old);
        }
    }

    private String boardSunAsset(Sun sun) {
        if (sun != null && sun.getType() == SunType.SPECIAL) {
            return BOARD_SUN_SPECIAL;
        }
        if (sun != null && sun.getType() == SunType.RADIOACTIVE) {
            return BOARD_SUN_RADIOACTIVE;
        }
        return GAME_SUN_ICON;
    }

    private final class CooldownShadeActor extends Actor {
        private final BasePlant prototype;

        private CooldownShadeActor(BasePlant prototype) {
            this.prototype = prototype;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (plantingOverlayPixel == null) {
                return;
            }
            float fraction = cooldownFraction(prototype);
            if (fraction <= 0f) {
                return;
            }
            Color previous = new Color(batch.getColor());
            batch.setColor(0f, 0f, 0f, 0.68f * parentAlpha);
            batch.draw(plantingOverlayPixel,
                    getX(), getY(), getWidth(), getHeight() * fraction);
            batch.setColor(previous);
        }
    }

    private final class SunAffordabilityShadeActor extends Actor {
        private final PlantCollectionItem plant;

        private SunAffordabilityShadeActor(PlantCollectionItem plant) {
            this.plant = plant;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (plantingOverlayPixel == null || plant == null
                    || plant.getCost() <= currentSunCount()) {
                return;
            }
            Color previous = new Color(batch.getColor());
            batch.setColor(0f, 0f, 0f, 0.52f * parentAlpha);
            batch.draw(plantingOverlayPixel,
                    getX(), getY(), getWidth(), getHeight());
            batch.setColor(previous);
        }
    }

    private final class SelectionOutlineActor extends Actor {
        private static final float THICKNESS = 3f;

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (plantingOverlayPixel == null) {
                return;
            }
            Color previous = new Color(batch.getColor());
            batch.setColor(1f, 1f, 1f, 0.92f * parentAlpha);
            batch.draw(plantingOverlayPixel,
                    getX(), getY(), getWidth(), THICKNESS);
            batch.draw(plantingOverlayPixel,
                    getX(), getY() + getHeight() - THICKNESS,
                    getWidth(), THICKNESS);
            batch.draw(plantingOverlayPixel,
                    getX(), getY(), THICKNESS, getHeight());
            batch.draw(plantingOverlayPixel,
                    getX() + getWidth() - THICKNESS, getY(),
                    THICKNESS, getHeight());
            batch.setColor(previous);
        }
    }

    private static final class CellBounds {
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        private CellBounds(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * PvZ-style wave meter inspired by phase-two image 19. Progress is based
     * on planned wave zombies that have actually spawned. Because a model
     * wave is spawned as one batch, the head advances exactly when that wave's
     * zombies enter the board. Flag positions are weighted by the number of
     * zombies in each wave instead of being spaced arbitrarily.
     */
    private final class WaveProgressActor extends Actor {
        private static final float BAR_LEFT = 30f;
        private static final float BAR_RIGHT_MARGIN = 30f;
        private static final float BAR_Y = 9f;
        private static final float BAR_HEIGHT = 15f;
        private static final float FRAME_THICKNESS = 4f;
        private static final float FLAG_POLE_WIDTH = 29f;
        private static final float FLAG_POLE_HEIGHT = 38f;
        private static final float FLAG_WIDTH = 27f;
        private static final float FLAG_HEIGHT = 22f;
        private static final float HEAD_WIDTH = 42f;
        private static final float HEAD_HEIGHT = 45f;

        private final Game game;
        private final Texture pixel;
        private final TextureRegion zombieHead;
        private final TextureRegion flagPole;
        private final TextureRegion flag;

        private WaveProgressActor(Game game) {
            this.game = game;
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixel = new Texture(pixmap);
            pixmap.dispose();
            zombieHead = requireAssetRegion(WAVE_PROGRESS_ZOMBIE_HEAD);
            flagPole = requireAssetRegion(WAVE_PROGRESS_FLAG_POLE);
            flag = requireAssetRegion(WAVE_PROGRESS_FLAG);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            List<ZombieWave> waves = game.getZombieWaves();
            if (waves.isEmpty()) {
                return;
            }

            int totalZombies = totalWaveZombieCount(waves);
            if (totalZombies <= 0) {
                return;
            }
            int spawnedZombies = spawnedWaveZombieCount(waves,
                    game.getZombieWaveNumber());
            float progress = Math.max(0f, Math.min(1f,
                    spawnedZombies / (float) totalZombies));

            float x = getX();
            float y = getY();
            float barLeft = x + BAR_LEFT;
            float barWidth = getWidth() - BAR_LEFT - BAR_RIGHT_MARGIN;
            float barRight = barLeft + barWidth;
            float barBottom = y + BAR_Y;

            Color previous = new Color(batch.getColor());

            // Thick, dark outer casing like the original PvZ meter.
            batch.setColor(0.12f, 0.09f, 0.05f, 0.96f * parentAlpha);
            batch.draw(pixel,
                    barLeft - FRAME_THICKNESS,
                    barBottom - FRAME_THICKNESS,
                    barWidth + FRAME_THICKNESS * 2f,
                    BAR_HEIGHT + FRAME_THICKNESS * 2f);

            // Empty portion of the meter.
            batch.setColor(0.20f, 0.17f, 0.10f, parentAlpha);
            batch.draw(pixel, barLeft, barBottom, barWidth, BAR_HEIGHT);

            // Image 19 progresses from right toward the house on the left.
            // Therefore the completed green section grows from right to left.
            float completedWidth = barWidth * progress;
            if (completedWidth > 0f) {
                batch.setColor(0.16f, 0.88f, 0.12f, parentAlpha);
                batch.draw(pixel, barRight - completedWidth, barBottom,
                        completedWidth, BAR_HEIGHT);
                batch.setColor(0.35f, 1f, 0.28f, 0.72f * parentAlpha);
                batch.draw(pixel, barRight - completedWidth,
                        barBottom + BAR_HEIGHT * 0.58f,
                        completedWidth, BAR_HEIGHT * 0.24f);
            }

            // Every wave boundary is shown with a pole and red flag. The
            // positions are proportional to how many zombies that wave adds.
            int cumulative = 0;
            for (ZombieWave wave : waves) {
                cumulative += wave.getZombieTypes().size();
                float boundaryProgress = cumulative / (float) totalZombies;
                float markerX = barRight - barWidth * boundaryProgress;
                boolean passed = progress + 0.0001f >= boundaryProgress;
                float alpha = passed ? 1f : 0.72f;

                batch.setColor(1f, 1f, 1f, alpha * parentAlpha);
                batch.draw(flagPole,
                        markerX - FLAG_POLE_WIDTH * 0.5f,
                        barBottom - 2f,
                        FLAG_POLE_WIDTH, FLAG_POLE_HEIGHT);
                batch.draw(flag,
                        markerX - FLAG_WIDTH * 0.58f,
                        barBottom + FLAG_POLE_HEIGHT * 0.48f,
                        FLAG_WIDTH, FLAG_HEIGHT);
            }

            // The zombie head rides on the boundary between completed and
            // remaining progress, matching the reference meter's scale.
            float headX = barRight - barWidth * progress;
            batch.setColor(1f, 1f, 1f, parentAlpha);
            batch.draw(zombieHead,
                    headX - HEAD_WIDTH * 0.5f,
                    barBottom - HEAD_HEIGHT * 0.32f,
                    HEAD_WIDTH, HEAD_HEIGHT);

            batch.setColor(previous);
        }

        private int totalWaveZombieCount(List<ZombieWave> waves) {
            int total = 0;
            for (ZombieWave wave : waves) {
                total += wave.getZombieTypes().size();
            }
            return total;
        }

        private int spawnedWaveZombieCount(List<ZombieWave> waves,
                int currentWaveNumber) {
            int spawned = 0;
            int spawnedWaveCount = Math.max(0,
                    Math.min(currentWaveNumber, waves.size()));
            for (int index = 0; index < spawnedWaveCount; index++) {
                spawned += waves.get(index).getZombieTypes().size();
            }
            return spawned;
        }

        private void dispose() {
            pixel.dispose();
        }
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
