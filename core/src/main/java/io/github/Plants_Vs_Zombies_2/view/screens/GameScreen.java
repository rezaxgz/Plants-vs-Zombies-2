package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.ArrayList;
import java.util.HashMap;
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
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.VaseSeedPacket;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.BouncingGrape;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.LobbedProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.Projectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.lobber.Lobber;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.melee.Melee;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.melee.MeleePlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.shooter.Shooter;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.sunProducer.SunProducer;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.wallnut.Wallnut;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.wallnut.WallnutPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.tile.Tile;
import io.github.Plants_Vs_Zombies_2.model.game.tile.TileType;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.IZombie;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.IZombieCard;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.IZombiePlacementResult;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.BowlingWallnut;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.BowlingWallnutType;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.VaseBreakResult;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.VaseBreaker;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.VaseSeedPlantingResult;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.WallnutBowling;
import io.github.Plants_Vs_Zombies_2.model.game.plantSelector.PlantSelection;
import io.github.Plants_Vs_Zombies_2.model.game.structure.BaseStructure;
import io.github.Plants_Vs_Zombies_2.model.game.structure.Grave;
import io.github.Plants_Vs_Zombies_2.model.game.structure.GraveReward;
import io.github.Plants_Vs_Zombies_2.model.game.structure.Vase;
import io.github.Plants_Vs_Zombies_2.model.game.structure.VaseType;
import io.github.Plants_Vs_Zombies_2.model.game.save.SavedGameManager;
import io.github.Plants_Vs_Zombies_2.model.game.special.ConveyorPlacementResult;
import io.github.Plants_Vs_Zombies_2.model.game.special.ConveyorPlantPacket;
import io.github.Plants_Vs_Zombies_2.model.menu.GameMenu;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Chapter;
import io.github.Plants_Vs_Zombies_2.model.roadmap.ChapterCatalog;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Level;
import io.github.Plants_Vs_Zombies_2.model.roadmap.LevelKind;
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
    private static final String LOVE_YOUR_PLANTS_ICON =
            "IMAGE_UI_PAUSEMENU_SUNFLOWER_TOPPER";
    private static final String WAVE_PROGRESS_FLAG_POLE =
            "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FLAG_POLE";
    private static final String WAVE_PROGRESS_FLAG =
            "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FLAG_DEFAULT";
    private static final String EGYPT_PACKET = "IMAGE_UI_PACKETS_EGYPT";
    private static final String ICEAGE_PACKET = "IMAGE_UI_PACKETS_ICEAGE";
    private static final String BEACH_PACKET = "IMAGE_UI_PACKETS_BEACH";
    private static final String DARK_PACKET = "IMAGE_UI_PACKETS_DARK";
    private static final String EGYPT_GRAVE_PAM =
            "768/INITIAL/GRAVESTONES/EGYPT_HIEROGLYPH/"
                    + "EGYPT_HIEROGLYPH.PAM";
    private static final String DARK_GRAVE_EMPTY_PAM =
            "768/FULL/GRAVESTONES/DARK_NOOP/DARK_NOOP.PAM";
    private static final String DARK_GRAVE_SUN_PAM =
            "768/FULL/GRAVESTONES/DARK_SUN/DARK_SUN.PAM";
    private static final String DARK_GRAVE_PLANT_FOOD_PAM =
            "768/FULL/GRAVESTONES/DARK_PLANTFOOD/DARK_PLANTFOOD.PAM";
    private static final String DARK_NECROMANCY_DISC_OUTER_ASSET =
            "IMAGE_EFFECTS_TOMBSTONE_DARK_SPAWN_EFFECT_"
                    + "ZOMBIE_EGYPT_TOMBRAISER_DISC_01";
    private static final String DARK_NECROMANCY_DISC_INNER_ASSET =
            "IMAGE_EFFECTS_TOMBSTONE_DARK_SPAWN_EFFECT_"
                    + "ZOMBIE_EGYPT_TOMBRAISER_DISC_02";
    private static final String DARK_BURNING_TILE_ASSET =
            "IMAGE_BACKGROUNDS_FIRETILE_FIRETILE_117X117";
    private static final String EGYPT_SANDSTORM_REAR_PAM =
            "768/INITIAL/EFFECTS/SANDSTORM_REAR/SANDSTORM_REAR.PAM";
    private static final String EGYPT_SANDSTORM_TOP_PAM =
            "768/INITIAL/EFFECTS/SANDSTORM_TOP/SANDSTORM_TOP.PAM";
    private static final String FROSTBITE_WIND_PAM =
            "768/FULL/EFFECTS/FROSTBITE_CHILL_WIND/"
                    + "FROSTBITE_CHILL_WIND.PAM";
    private static final String FROSTBITE_SLIPPERY_TILE_ASSET =
            "IMAGE_EFFECTS_ZOMBONI_TILE_ICE_"
                    + "ZOMBONI_TILE_ICE_133X157";
    private static final String[] FROSTBITE_ZOMBIE_ICE_ASSETS = {
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_"
                    + "FROSTBITE_ICE_BLOCK_ZOMBIE_153X243",
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_"
                    + "FROSTBITE_ICE_BLOCK_ZOMBIE_153X243_2",
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_"
                    + "FROSTBITE_ICE_BLOCK_ZOMBIE_153X243_3",
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_"
                    + "FROSTBITE_ICE_BLOCK_ZOMBIE_153X243_4",
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_"
                    + "FROSTBITE_ICE_BLOCK_ZOMBIE_153X243_5",
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_"
                    + "FROSTBITE_ICE_BLOCK_ZOMBIE_153X243_6" };
    // The plant images are ordered exactly as supplied for the six visible
    // ice-health states, from intact to most damaged.
    private static final String[] FROSTBITE_PLANT_ICE_ASSETS = {
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_PLANT_"
                    + "FROSTBITE_ICE_BLOCK_PLANT_164X169",
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_PLANT_"
                    + "FROSTBITE_ICE_BLOCK_PLANT_167X172_4",
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_PLANT_"
                    + "FROSTBITE_ICE_BLOCK_PLANT_167X172_5",
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_PLANT_"
                    + "FROSTBITE_ICE_BLOCK_PLANT_167X172_3",
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_PLANT_"
                    + "FROSTBITE_ICE_BLOCK_PLANT_167X172_2",
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_PLANT_"
                    + "FROSTBITE_ICE_BLOCK_PLANT_167X172" };
    private static final float FROSTBITE_WIND_DURATION_SECONDS = 2.5667f;
    private static final String[] BIG_WAVE_BEACH_WATER_TILE_ASSETS = {
            "IMAGE_BACKGROUNDS_WATER_SQUARE_WATER_SQUARE_174X205",
            "IMAGE_BACKGROUNDS_WATER_SQUARE_WATER_SQUARE_175X204",
            "IMAGE_BACKGROUNDS_WATER_SQUARE_WATER_SQUARE_178X201",
            "IMAGE_BACKGROUNDS_WATER_SQUARE_WATER_SQUARE_180X200",
            "IMAGE_BACKGROUNDS_WATER_SQUARE_WATER_SQUARE_182X207" };
    private static final String BIG_WAVE_BEACH_TIDE_LIMIT_ASSET =
            "IMAGE_BACKGROUNDS_WATER_TIDE_LINE_"
                    + "WATER_TIDE_LINE_161X397";
    private static final String BIG_WAVE_BEACH_LOW_TILE_MARKER_ASSET =
            "IMAGE_BACKGROUNDS_BEACH_WATERSIGN";
    private static final String VASE_BROWN_ASSET =
            "IMAGE_VASEBREAKER_VASE_BROWN_VASE_BROWN_115X150";
    private static final String VASE_GREEN_ASSET =
            "IMAGE_VASEBREAKER_VASE_GREEN_VASE_GREEN_115X150";
    private static final String VASE_GARGANTUAR_ASSET =
            "IMAGE_VASEBREAKER_VASE_GARGANTUAR_VASE_GARGANTUAR_115X150";
    private static final String I_ZOMBIE_BRAIN_PAM =
            "768/FULL/ZOMBIE/POWER_BRAIN_PROJECTILE/"
                    + "POWER_BRAIN_PROJECTILE.PAM";
    private static final String I_ZOMBIE_BRAIN_FALLBACK_ASSET =
            "IMAGE_EFFECTS_PRIZE_PINATA_VALENBRAINZ_"
                    + "PRIZE_PINATA_VALENBRAINZ_109X109";

    private static final float SEED_TRAY_X = 16f;
    private static final float SEED_TRAY_Y = 76f;
    private static final float SEED_TRAY_WIDTH = 116f;
    private static final float SEED_TRAY_HEIGHT = 568f;
    private static final float SEED_SLOT_WIDTH = 110f;
    private static final float SEED_SLOT_HEIGHT = 68f;
    private static final float CONVEYOR_CARD_WIDTH = 108f;
    private static final float CONVEYOR_CARD_HEIGHT = 64f;
    private static final float CONVEYOR_CARD_GAP = 4f;
    private static final float CONVEYOR_CARD_INSET = 4f;
    private static final float CONVEYOR_CARD_TRAVEL_SPEED = 120f;
    private static final float VASE_SEED_CARD_WIDTH = 108f;
    private static final float VASE_SEED_CARD_HEIGHT = 64f;
    private static final float VASE_SEED_CARD_GAP = 4f;
    private static final float VASE_SEED_CARD_INSET = 4f;
    private static final float VASE_SEED_HEADER_HEIGHT = 30f;
    private static final float I_ZOMBIE_CARD_WIDTH = 108f;
    private static final float I_ZOMBIE_CARD_HEIGHT = 98f;
    private static final float I_ZOMBIE_CARD_GAP = 5f;
    private static final float I_ZOMBIE_HEADER_HEIGHT = 30f;
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
    private static final float PLANT_WHAT_YOU_GET_WAVE_BUTTON_X =
            WAVE_PROGRESS_X + WAVE_PROGRESS_WIDTH + 12f;
    private static final float PLANT_WHAT_YOU_GET_WAVE_BUTTON_Y = 15f;
    private static final float PLANT_WHAT_YOU_GET_WAVE_BUTTON_WIDTH = 168f;
    private static final float PLANT_WHAT_YOU_GET_WAVE_BUTTON_HEIGHT = 46f;
    private static final float SUN_HUD_X = 210f;
    private static final float SUN_HUD_Y = 648f;
    private static final float SUN_HUD_WIDTH = 218f;
    private static final float SUN_HUD_HEIGHT = 60f;
    private static final float PLANT_FOOD_HUD_X = SUN_HUD_X;
    private static final float PLANT_FOOD_HUD_Y = 586f;
    private static final float PLANT_FOOD_HUD_WIDTH = SUN_HUD_WIDTH;
    private static final float PLANT_FOOD_HUD_HEIGHT = SUN_HUD_HEIGHT;
    private static final float TIMED_WAR_HUD_X = 440f;
    private static final float TIMED_WAR_HUD_Y = 566f;
    private static final float TIMED_WAR_HUD_WIDTH = 276f;
    private static final float TIMED_WAR_HUD_HEIGHT = 76f;
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
    private DeadlineLineActor deadlineLineActor;
    private DeadlineLineActor wallnutBowlingLineActor;

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
    private BossHealthActor bossHealthActor;
    private TextButton plantWhatYouGetWaveButton;
    private Table sunHud;
    private Label sunAmountLabel;
    private Table plantFoodHud;
    private Label plantFoodAmountLabel;
    private Table timedWarObjectivesHud;
    private Label timedWarSunLeftLabel;
    private Label timedWarZombieKillsLabel;
    private Table loveYourPlantsHud;
    private Label loveYourPlantsLabel;
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
    private Group conveyorBelt;
    private final Map<Long, ConveyorPacketActor> conveyorPacketActors =
            new HashMap<>();
    private Long selectedConveyorPacketSequence;
    private String selectedConveyorPlantName;
    private String plantedPlantRenderSignature = "";
    private final Map<BasePlant, Actor> plantedPlantActors =
            new IdentityHashMap<>();
    private final Map<BasePlant, Integer> plantedPlantHealth =
            new IdentityHashMap<>();
    private final Map<SunProducer, Integer> plantedSunProductionSequences =
            new IdentityHashMap<>();
    private final Map<Shooter, Integer> plantedShooterAttackSequences =
            new IdentityHashMap<>();
    private final Map<Lobber, Integer> plantedLobberAttackSequences =
            new IdentityHashMap<>();

    private Group sunLayer;
    private final Map<Sun, SunActor> sunActors = new IdentityHashMap<>();

    private Group collectibleDropLayer;
    private final Map<PlantFoodDrop, PlantFoodDropActor> plantFoodDropActors =
            new IdentityHashMap<>();
    private Label rewardNoticeLabel;
    private float rewardNoticeRemainingSeconds;

    private Group bigWaveBeachTerrainLayer;
    private String bigWaveBeachTerrainSignature = "";
    private Group darkAgesTerrainLayer;
    private String darkAgesTerrainSignature = "";
    private Group frostbiteTerrainLayer;
    private String frostbiteTerrainSignature = "";
    private Group structureLayer;
    private final Map<Grave, PamAnimationActor> graveActors =
            new IdentityHashMap<>();
    private final Map<Grave, String> graveVisualKeys =
            new IdentityHashMap<>();
    private final Map<Grave, Integer> graveHitPoints =
            new IdentityHashMap<>();
    private final Map<Vase, Image> vaseActors = new IdentityHashMap<>();

    private Group vaseSeedTray;
    private final Map<VaseSeedPacket, VaseSeedPacketActor> vaseSeedPacketActors =
            new IdentityHashMap<>();
    private VaseSeedPacket selectedVaseSeedPacket;

    private Table iZombieTray;
    private IZombieCard selectedIZombieCard;
    private Group iZombieBoardOverlay;
    private final List<Actor> iZombieBrainActors = new ArrayList<>();

    private Group frostbitePlantIceLayer;
    private final Map<BasePlant, Image> frostbitePlantIceActors =
            new IdentityHashMap<>();
    private final Map<BasePlant, String> frostbitePlantIceKeys =
            new IdentityHashMap<>();

    private Group egyptSandstormRearLayer;
    private Group zombieLayer;
    private Group frostbiteZombieIceLayer;
    private final Map<Zombie, Image> frostbiteZombieIceActors =
            new IdentityHashMap<>();
    private final Map<Zombie, String> frostbiteZombieIceKeys =
            new IdentityHashMap<>();
    private Group egyptSandstormTopLayer;
    private Group frostbiteWindLayer;
    private double renderedFrostbiteWindAtSeconds = Double.NEGATIVE_INFINITY;
    private final Map<Zombie, ZombiePamActor> zombieActors =
            new IdentityHashMap<>();
    private final Map<Zombie, Integer> zombieDurability =
            new IdentityHashMap<>();
    private final Map<Zombie, EgyptSandstormEffect> egyptSandstorms =
            new IdentityHashMap<>();

    private Group bowlingWallnutLayer;
    private final Map<BowlingWallnut, BowlingWallnutActor> bowlingWallnutActors =
            new IdentityHashMap<>();

    private Group projectileLayer;
    private final Map<Projectile, ProjectileActor> projectileActors =
            new IdentityHashMap<>();
    private final Map<BouncingGrape, PamAnimationActor> grapeActors =
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
        } else if (menu.isMinigame()) {
            installMinigameTitle(menu);
        }
        installChapterBoard(chapter);
        installFrostbiteTerrainRendering();
        installGameHud();
        installWaveProgressHud();
        installPlantWhatYouGetWaveButton();
        installTimedWarObjectivesHud();
        installLoveYourPlantsHud();

        if (menu.getLevel() != null && menu.getGame().hasConfiguredPlantLoadout()) {
            installSeedTray();
            installCooldownResetButton();
            rebuildSeedTray();
        }
        installStructureRendering();
        addBackgroundOverlay(new LawnMowerRenderer(
                navigator.getPamPlayer(), menu.getGame(), chapter));
        installPlantingInteraction();
        installFrostbitePlantIceRendering();
        installIZombieBoardOverlay();
        installVaseBreakerSeedTray();
        installConveyorBelt();
        installIZombieTray();
        installShovelButton();
        installZombieRendering();
        installFrostbiteZombieIceRendering();
        installFrostbiteWindRendering();
        installBowlingWallnutRendering();
        installProjectileRendering();
        installSunRendering();
        installCollectibleDropRendering();
        installRewardNotice();
        installGameAnnouncementSystem();
        if (menu.getLevel() != null) {
            gamePaused = true;
            stage.addActor(new LevelObjectivesOverlay(
                    skin, menu.getLevel(), VIRTUAL_WIDTH, VIRTUAL_HEIGHT,
                    () -> gamePaused = false));
        } else if (menu.isMinigame()) {
            gamePaused = true;
            stage.addActor(new LevelObjectivesOverlay(
                    skin, menu.getMinigameDisplayName() + " Objectives",
                    buildMinigameObjectives(menu),
                    VIRTUAL_WIDTH, VIRTUAL_HEIGHT,
                    () -> gamePaused = false));
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

    private void installTimedWarObjectivesHud() {
        Game game = activeGame();
        if (game == null || !game.hasTimedWar()
                || game.getTimedWarCollectedSunTarget() <= 0
                || timedWarObjectivesHud != null) {
            return;
        }

        timedWarSunLeftLabel = new Label("", skin, "medium_outline");
        timedWarSunLeftLabel.setFontScale(0.62f);
        timedWarSunLeftLabel.setAlignment(Align.left);

        timedWarZombieKillsLabel = new Label("", skin, "medium_outline");
        timedWarZombieKillsLabel.setFontScale(0.62f);
        timedWarZombieKillsLabel.setAlignment(Align.left);

        timedWarObjectivesHud = new Table();
        timedWarObjectivesHud.left().top();
        timedWarObjectivesHud.setBounds(
                TIMED_WAR_HUD_X, TIMED_WAR_HUD_Y,
                TIMED_WAR_HUD_WIDTH, TIMED_WAR_HUD_HEIGHT);
        timedWarObjectivesHud.add(createSpecialObjectiveRow(
                GAME_SUN_ICON, timedWarSunLeftLabel))
                .width(TIMED_WAR_HUD_WIDTH).height(37f).row();
        timedWarObjectivesHud.add(createSpecialObjectiveRow(
                WAVE_PROGRESS_ZOMBIE_HEAD, timedWarZombieKillsLabel))
                .width(TIMED_WAR_HUD_WIDTH).height(37f);
        stage.addActor(timedWarObjectivesHud);
        refreshTimedWarObjectivesHud();
    }

    private Actor createSpecialObjectiveRow(String iconAsset, Label label) {
        Stack row = new Stack();
        Image background = createAssetImage(GAME_SUN_BACKGROUND);
        background.setScaling(Scaling.stretch);
        background.setColor(1f, 1f, 1f, 0.92f);
        row.add(background);

        Table contents = new Table();
        contents.left();
        Image icon = createAssetImage(iconAsset);
        icon.setScaling(Scaling.fit);
        contents.add(icon).size(34f).padLeft(5f).padRight(4f);
        contents.add(label).growX().left().padRight(8f);
        row.add(contents);
        return row;
    }

    private void refreshTimedWarObjectivesHud() {
        Game game = activeGame();
        if (timedWarObjectivesHud == null || game == null
                || !game.hasTimedWar()) {
            return;
        }

        int sunsLeft = game.getTimedWarSunLeftToCollect();
        timedWarSunLeftLabel.setText(sunsLeft + " suns left"
                + (game.isTimedWarCollectedSunRequirementMet()
                        ? " (met)" : ""));

        int recentKills = game.getTimedWarRecentZombieKills();
        int killTarget = game.getTimedWarTarget();
        int windowSeconds = Math.max(1,
                (int) Math.round(game.getTimedWarKillWindowSeconds()));
        timedWarZombieKillsLabel.setText(
                recentKills + " / " + killTarget + " in last "
                        + windowSeconds + "s"
                        + (game.isTimedWarZombieKillRequirementMet()
                                ? " (met)" : ""));
    }

    private void installLoveYourPlantsHud() {
        Game game = activeGame();
        if (game == null || !game.hasLoveYourPlants()
                || loveYourPlantsHud != null) {
            return;
        }

        loveYourPlantsLabel = new Label("", skin, "medium_outline");
        loveYourPlantsLabel.setFontScale(0.62f);
        loveYourPlantsLabel.setAlignment(Align.left);

        loveYourPlantsHud = new Table();
        loveYourPlantsHud.left().top();
        loveYourPlantsHud.setBounds(
                TIMED_WAR_HUD_X, TIMED_WAR_HUD_Y,
                TIMED_WAR_HUD_WIDTH, TIMED_WAR_HUD_HEIGHT);
        loveYourPlantsHud.add(createSpecialObjectiveRow(
                LOVE_YOUR_PLANTS_ICON, loveYourPlantsLabel))
                .width(TIMED_WAR_HUD_WIDTH).height(37f);
        stage.addActor(loveYourPlantsHud);
        refreshLoveYourPlantsHud();
    }

    private void refreshLoveYourPlantsHud() {
        Game game = activeGame();
        if (loveYourPlantsHud == null || loveYourPlantsLabel == null
                || game == null || !game.hasLoveYourPlants()) {
            return;
        }

        int lost = game.getLostPlantCount();
        int remaining = game.getRemainingPlantLossAllowance();
        loveYourPlantsLabel.setText(
                lost + " lost | " + remaining + " more allowed");
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
        GameMenu activeMenu = previewLevel == null ? currentGameMenu() : null;
        boolean minigame = activeMenu != null && activeMenu.isMinigame();
        TextButton saveAndExit = new TextButton(
                minigame ? "EXIT" : "SAVE AND EXIT", skin, "brown");
        saveAndExit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (minigame) {
                    navigator.exitMinigameToTravelLog();
                    return;
                }
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
        if (menu.isMinigame()) {
            navigator.exitMinigameToTravelLog();
            return;
        }
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
        if (menu != null && menu.isMinigame()) {
            CommandResult result = navigator.startMinigameFromTravelLog(
                    menu.getMinigameId(), menu.getMinigameLevel());
            if (result.isSuccsesful()) {
                return;
            }
            closePauseModal();
            return;
        }
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

        GameMenu menu = currentGameMenu();
        // Vase Breaker and I, Zombie do not advance through normal zombie
        // waves.  Do not hold their simulation behind a bogus "ZOMBIES ARE
        // COMING" announcement.  Wall-nut Bowling does have waves and keeps
        // the standard announcement flow.
        if (menu != null && menu.isMinigame()
                && !(game instanceof WallnutBowling)) {
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

        if (!game.hasPlantWhatYouGet()) {
            if (!game.haveZombieWavesStarted()) {
                queueWaveAnnouncements(1, true);
            } else {
                maybeQueueReadyWaveAnnouncement();
            }
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
        if (game == null || game.hasPlantWhatYouGet()
                || gameAnnouncementLabel == null
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

    private void installMinigameTitle(GameMenu menu) {
        Label title = new Label(
                menu.getMinigameDisplayName() + " - Level "
                        + menu.getMinigameLevel(),
                skin, "big_outline");
        title.setFontScale(0.72f);
        title.setAlignment(Align.right);
        title.setBounds(382f, 646f, 334f, 48f);
        stage.addActor(title);
    }

    private List<String> buildMinigameObjectives(GameMenu menu) {
        List<String> objectives = new ArrayList<>();
        Game game = menu.getGame();
        if (game instanceof VaseBreaker) {
            VaseBreaker vaseBreaker = (VaseBreaker) game;
            objectives.add("Break every vase and defeat every hostile zombie "
                    + "released from them.");
            objectives.add("Plant revealed one-use seed packets before their "
                    + "time limit expires ("
                    + formatObjectiveSeconds(vaseBreaker.getLevel()
                            .getSeedPacketLifeSpanSeconds())
                    + " seconds).");
        } else if (game instanceof WallnutBowling) {
            WallnutBowling bowling = (WallnutBowling) game;
            objectives.add("Defeat all " + bowling.getLevel().getZombieCount()
                    + " zombies across " + bowling.getLevel().getWaveCount()
                    + " waves.");
            objectives.add("Launch Wall-nuts from the conveyor only from "
                    + "columns 1 through "
                    + (bowling.getRedLineColumn() + 1) + ".");
        } else if (game instanceof IZombie) {
            IZombie iZombie = (IZombie) game;
            objectives.add("Eat all five brains by placing zombies on the "
                    + "right side of the red line.");
            objectives.add("Choose zombies from the card tray. Each card "
                    + "shows its sun cost and must recharge after use.");
            objectives.add("Use your starting " + IZombie.INITIAL_SUN
                    + " sun and the five sun-producer zombies to keep your "
                    + "attack going.");
            objectives.add("The red line is after column "
                    + (iZombie.getRedLineColumn() + 1) + ".");
        } else {
            objectives.add("Complete the minigame objective.");
        }
        return objectives;
    }

    private static String formatObjectiveSeconds(float seconds) {
        if (Math.abs(seconds - Math.round(seconds)) < 0.001f) {
            return Integer.toString(Math.round(seconds));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }

    private void installChapterBoard(Chapter chapter) {
        BoardLayout layout = layoutForChapter(chapter);
        if (layout == null && isModelBackedGame()) {
            GameMenu menu = currentGameMenu();
            if (menu != null && menu.isMinigame()) {
                // Minigames have no adventure chapter metadata. Phase 2
                // allows an arbitrary minigame background, so use the Egypt
                // lawn to keep their 5x9 board visible and interactive.
                layout = EGYPT_BOARD;
            }
        }
        if (layout == null) {
            return;
        }
        setAssetBackground(layout.backgroundAsset);

        Game game = activeGame();
        if (game != null && chapter != null
                && "big-wave-beach".equals(chapter.getId())) {
            installBigWaveBeachTerrainRendering();
        }
        if (game != null && chapter != null
                && "dark-ages".equals(chapter.getId())) {
            installDarkAgesTerrainRendering();
        }

        if (shouldDrawGrid()) {
            gridActor = new BoardGridActor(layout);
            addBackgroundOverlay(gridActor);
        }

        if (game != null && game.hasDeadLine()) {
            deadlineLineActor = new DeadlineLineActor(
                    layout, game.getDeadLineColumn());
            // Added after the optional map grid so this thicker marker is
            // always drawn on top of the ordinary red grid lines. Later
            // board entities (plants/zombies) still render over it.
            addBackgroundOverlay(deadlineLineActor);
        }
        if (game instanceof WallnutBowling) {
            wallnutBowlingLineActor = new DeadlineLineActor(
                    layout,
                    ((WallnutBowling) game).getRedLineColumn() + 0.5);
            // Wall-nut Bowling's red line sits after the last launchable
            // column. DeadlineLineActor draws at the center of the supplied
            // logical position, so shift by half a cell to place the line on
            // the right edge of the permitted launch zone.
            addBackgroundOverlay(wallnutBowlingLineActor);
        }
    }

    private boolean shouldDrawGrid() {
        return App.getInstance().getLoggedInUser() != null
                && App.getInstance().getLoggedInUser()
                        .getSettings().isShowGameMapGrid();
    }

    private boolean isBigWaveBeachGame() {
        Chapter chapter = seedTrayChapter();
        return chapter != null && "big-wave-beach".equals(chapter.getId());
    }

    private void installBigWaveBeachTerrainRendering() {
        if (!isModelBackedGame() || !isBigWaveBeachGame()
                || bigWaveBeachTerrainLayer != null) {
            return;
        }
        bigWaveBeachTerrainLayer = new Group();
        bigWaveBeachTerrainLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(bigWaveBeachTerrainLayer);
        rebuildBigWaveBeachTerrainRendering();
    }

    private void rebuildBigWaveBeachTerrainRendering() {
        Game game = activeGame();
        if (!isBigWaveBeachGame() || game == null
                || bigWaveBeachTerrainLayer == null
                || !game.getBoard().isBigWaveBeachRulesEnabled()) {
            return;
        }

        bigWaveBeachTerrainLayer.clearChildren();

        // The Phase-1 model exposes the current tide through WATER tiles.
        // Draw one real PvZ2 water-square asset over every covered board cell
        // so the visible water boundary changes at exactly the same discrete
        // moments as the model's tide level.
        for (Tile tile : game.getBoard().getTiles()) {
            if (tile == null || tile.getPosition() == null
                    || tile.getTileType() != TileType.WATER) {
                continue;
            }
            EntityPosition position = tile.getPosition();
            int variant = Math.floorMod(
                    position.getRow() * BOARD_COLUMNS
                            + position.getColumn(),
                    BIG_WAVE_BEACH_WATER_TILE_ASSETS.length);
            Image water = createAssetImage(
                    BIG_WAVE_BEACH_WATER_TILE_ASSETS[variant]);
            water.setScaling(Scaling.stretch);
            water.setTouchable(Touchable.disabled);
            positionBigWaveBeachWaterTile(water, position);
            bigWaveBeachTerrainLayer.addActor(water);
        }

        // Low-beach cells are a Phase-1-specific concept, so mark them even
        // while submerged. The small water sign remains readable above the
        // water-square layer without covering plants or zombies.
        for (Tile tile : game.getBoard().getTiles()) {
            if (tile == null || tile.getPosition() == null
                    || !game.getBoard().isLowBeachTile(tile.getPosition())) {
                continue;
            }
            Image marker = createAssetImage(
                    BIG_WAVE_BEACH_LOW_TILE_MARKER_ASSET);
            marker.setScaling(Scaling.fit);
            marker.setTouchable(Touchable.disabled);
            marker.setColor(1f, 1f, 1f, 0.72f);
            positionBigWaveBeachLowTileMarker(marker, tile.getPosition());
            bigWaveBeachTerrainLayer.addActor(marker);
        }

        // This line is fixed at the furthest column the Phase-1 tide is
        // allowed to reach. It is intentionally separate from the current
        // water edge, which is communicated by the water-square cells.
        Image tideLimit = createAssetImage(BIG_WAVE_BEACH_TIDE_LIMIT_ASSET);
        tideLimit.setScaling(Scaling.stretch);
        tideLimit.setTouchable(Touchable.disabled);
        tideLimit.setColor(1f, 1f, 1f, 0.88f);
        positionBigWaveBeachTideLimit(tideLimit,
                game.getBoard().getWaterBoundaryColumn());
        bigWaveBeachTerrainLayer.addActor(tideLimit);

        bigWaveBeachTerrainSignature =
                createBigWaveBeachTerrainSignature(game);
    }

    private void refreshBigWaveBeachTerrainRendering() {
        Game game = activeGame();
        if (!isBigWaveBeachGame() || game == null
                || bigWaveBeachTerrainLayer == null
                || !game.getBoard().isBigWaveBeachRulesEnabled()) {
            return;
        }
        String signature = createBigWaveBeachTerrainSignature(game);
        if (!signature.equals(bigWaveBeachTerrainSignature)) {
            rebuildBigWaveBeachTerrainRendering();
        }
    }

    private String createBigWaveBeachTerrainSignature(Game game) {
        if (game == null || !game.getBoard().isBigWaveBeachRulesEnabled()) {
            return "";
        }
        StringBuilder signature = new StringBuilder();
        signature.append(game.getBoard().getWaterColumnCount())
                .append('/')
                .append(game.getBoard().getMaximumWaterColumnCount())
                .append('|');
        for (Tile tile : game.getBoard().getTiles()) {
            if (tile == null || tile.getPosition() == null) {
                continue;
            }
            EntityPosition position = tile.getPosition();
            if (tile.getTileType() == TileType.WATER
                    || game.getBoard().isLowBeachTile(position)) {
                signature.append(position.getRow()).append(',')
                        .append(position.getColumn()).append(':')
                        .append(tile.getTileType()).append(';');
            }
        }
        return signature.toString();
    }

    private void positionBigWaveBeachWaterTile(Actor actor,
            EntityPosition position) {
        CellBounds cell = screenBoundsForCell(position);
        if (actor == null || cell == null) {
            return;
        }
        // Tiny overlap hides seams between neighboring texture cells while
        // keeping the water aligned with the underlying 5x9 board.
        float overlapX = cell.width * 0.035f;
        float overlapY = cell.height * 0.035f;
        actor.setBounds(cell.x - overlapX,
                cell.y - overlapY,
                cell.width + overlapX * 2f,
                cell.height + overlapY * 2f);
        actor.setVisible(true);
    }

    private void positionBigWaveBeachLowTileMarker(Actor actor,
            EntityPosition position) {
        CellBounds cell = screenBoundsForCell(position);
        if (actor == null || cell == null) {
            return;
        }
        float width = cell.width * 0.26f;
        float height = cell.height * 0.50f;
        actor.setBounds(cell.x + cell.width * 0.06f,
                cell.y + cell.height * 0.06f,
                width, height);
        actor.setVisible(true);
    }

    private void positionBigWaveBeachTideLimit(Actor actor,
            int boundaryColumn) {
        if (actor == null || boundaryColumn < 0
                || boundaryColumn >= BOARD_COLUMNS) {
            return;
        }
        CellBounds topCell = screenBoundsForCell(
                new EntityPosition(0, boundaryColumn));
        CellBounds bottomCell = screenBoundsForCell(
                new EntityPosition(BOARD_ROWS - 1, boundaryColumn));
        if (topCell == null || bottomCell == null) {
            return;
        }
        float lineWidth = topCell.width * 0.56f;
        float boardBottom = bottomCell.y;
        float boardTop = topCell.y + topCell.height;
        actor.setBounds(topCell.x - lineWidth * 0.5f,
                boardBottom - bottomCell.height * 0.03f,
                lineWidth,
                boardTop - boardBottom + bottomCell.height * 0.06f);
        actor.setVisible(true);
    }

    private boolean isDarkAgesGame() {
        Chapter chapter = seedTrayChapter();
        return chapter != null && "dark-ages".equals(chapter.getId());
    }

    private void installDarkAgesTerrainRendering() {
        if (!isModelBackedGame() || !isDarkAgesGame()
                || darkAgesTerrainLayer != null) {
            return;
        }
        darkAgesTerrainLayer = new Group();
        darkAgesTerrainLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(darkAgesTerrainLayer);
        rebuildDarkAgesTerrainRendering();
    }

    private void rebuildDarkAgesTerrainRendering() {
        Game game = activeGame();
        if (!isDarkAgesGame() || game == null
                || darkAgesTerrainLayer == null) {
            return;
        }
        darkAgesTerrainLayer.clearChildren();

        for (int row = 0; row < game.getBoard().getNumberOfRows(); row++) {
            for (int column = 0;
                    column < game.getBoard().getNumberOfColumns(); column++) {
                EntityPosition position = new EntityPosition(row, column);
                Tile tile = game.getBoard().getTileAt(position);
                if (tile != null && tile.getTileType() == TileType.BURNING) {
                    Actor fire = createBurningGroundMarker();
                    positionBurningGroundMarker(fire, position);
                    darkAgesTerrainLayer.addActor(fire);
                    continue;
                }
                if (!isNecromancyGroundCell(game, position)) {
                    continue;
                }
                Actor marker = createNecromancyGroundMarker();
                positionNecromancyGroundMarker(marker, position);
                darkAgesTerrainLayer.addActor(marker);
            }
        }
        darkAgesTerrainSignature = createDarkAgesTerrainSignature(game);
    }

    private void refreshDarkAgesTerrainRendering() {
        Game game = activeGame();
        if (!isDarkAgesGame() || game == null
                || darkAgesTerrainLayer == null) {
            return;
        }
        String signature = createDarkAgesTerrainSignature(game);
        if (!signature.equals(darkAgesTerrainSignature)) {
            rebuildDarkAgesTerrainRendering();
        }
    }

    private String createDarkAgesTerrainSignature(Game game) {
        StringBuilder signature = new StringBuilder();
        for (int row = 0; row < game.getBoard().getNumberOfRows(); row++) {
            for (int column = 0;
                    column < game.getBoard().getNumberOfColumns(); column++) {
                EntityPosition position = new EntityPosition(row, column);
                Tile tile = game.getBoard().getTileAt(position);
                if (tile != null && tile.getTileType() == TileType.BURNING) {
                    signature.append('F').append(row).append(',')
                            .append(column).append(';');
                } else if (isNecromancyGroundCell(game, position)) {
                    signature.append('N').append(row).append(',')
                            .append(column).append(';');
                }
            }
        }
        return signature.toString();
    }

    private boolean isNecromancyGroundCell(Game game,
            EntityPosition position) {
        if (game == null || position == null) {
            return false;
        }
        Tile tile = game.getBoard().getTileAt(position);
        if (tile != null && tile.getTileType() == TileType.NECROMANCY) {
            return true;
        }
        BaseStructure structure = game.getBoard().getStructureAt(position);
        return structure instanceof Grave
                && ((Grave) structure).isNecromancyGrave();
    }

    private Actor createNecromancyGroundMarker() {
        Stack marker = new Stack();
        marker.setTouchable(Touchable.disabled);

        Image outer = createAssetImage(DARK_NECROMANCY_DISC_OUTER_ASSET);
        outer.setScaling(Scaling.fit);
        outer.setTouchable(Touchable.disabled);
        outer.setColor(1f, 1f, 1f, 0.78f);
        marker.add(outer);

        Image inner = createAssetImage(DARK_NECROMANCY_DISC_INNER_ASSET);
        inner.setScaling(Scaling.fit);
        inner.setTouchable(Touchable.disabled);
        inner.setColor(1f, 1f, 1f, 0.92f);
        marker.add(inner);
        return marker;
    }

    private Actor createBurningGroundMarker() {
        Image fire = createAssetImage(DARK_BURNING_TILE_ASSET);
        fire.setScaling(Scaling.stretch);
        fire.setTouchable(Touchable.disabled);
        fire.setColor(1f, 1f, 1f, 0.92f);
        return fire;
    }

    private void positionBurningGroundMarker(Actor marker,
            EntityPosition position) {
        CellBounds cell = screenBoundsForCell(position);
        if (marker == null || cell == null) {
            return;
        }
        marker.setBounds(cell.x - cell.width * 0.03f,
                cell.y - cell.height * 0.02f,
                cell.width * 1.06f, cell.height * 1.06f);
        marker.setVisible(true);
    }

    private void positionNecromancyGroundMarker(Actor marker,
            EntityPosition position) {
        CellBounds cell = screenBoundsForCell(position);
        if (marker == null || cell == null) {
            return;
        }
        float width = cell.width * 0.88f;
        float height = cell.height * 0.62f;
        marker.setBounds(
                cell.x + (cell.width - width) * 0.5f,
                cell.y + cell.height * 0.02f,
                width, height);
        marker.setVisible(true);
    }

    private boolean isFrostbiteGame() {
        Chapter chapter = seedTrayChapter();
        return chapter != null && "frostbite-caves".equals(chapter.getId());
    }

    private void installFrostbiteTerrainRendering() {
        if (!isModelBackedGame() || !isFrostbiteGame()
                || frostbiteTerrainLayer != null) {
            return;
        }
        frostbiteTerrainLayer = new Group();
        frostbiteTerrainLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(frostbiteTerrainLayer);
        rebuildFrostbiteTerrainRendering();
    }

    private void rebuildFrostbiteTerrainRendering() {
        Game game = activeGame();
        if (!isFrostbiteGame() || game == null
                || frostbiteTerrainLayer == null) {
            return;
        }
        frostbiteTerrainLayer.clearChildren();
        for (Tile tile : game.getBoard().getTiles()) {
            if (!isFrostbiteSlipperyTile(tile)) {
                continue;
            }
            Image ice = createAssetImage(FROSTBITE_SLIPPERY_TILE_ASSET);
            ice.setScaling(Scaling.fit);
            ice.setTouchable(Touchable.disabled);
            positionFrostbiteTerrainActor(ice, tile.getPosition());
            frostbiteTerrainLayer.addActor(ice);
        }
        frostbiteTerrainSignature = createFrostbiteTerrainSignature(game);
    }

    private void refreshFrostbiteTerrainRendering() {
        Game game = activeGame();
        if (!isFrostbiteGame() || game == null
                || frostbiteTerrainLayer == null) {
            return;
        }
        String signature = createFrostbiteTerrainSignature(game);
        if (!signature.equals(frostbiteTerrainSignature)) {
            rebuildFrostbiteTerrainRendering();
        }
    }

    private boolean isFrostbiteSlipperyTile(Tile tile) {
        if (tile == null || tile.getPosition() == null) {
            return false;
        }
        TileType type = tile.getTileType();
        return type == TileType.SLIPPERY
                || type == TileType.SLIDER_UP
                || type == TileType.SLIDER_DOWN;
    }

    private String createFrostbiteTerrainSignature(Game game) {
        StringBuilder signature = new StringBuilder();
        for (Tile tile : game.getBoard().getTiles()) {
            if (isFrostbiteSlipperyTile(tile)) {
                signature.append(tile.getPosition()).append(':')
                        .append(tile.getTileType()).append(';');
            }
        }
        return signature.toString();
    }

    private void positionFrostbiteTerrainActor(Actor actor,
            EntityPosition position) {
        CellBounds bounds = screenBoundsForCell(position);
        if (actor == null || bounds == null) {
            return;
        }
        actor.setBounds(bounds.x - bounds.width * 0.02f,
                bounds.y - bounds.height * 0.03f,
                bounds.width * 1.04f, bounds.height * 1.06f);
    }

    private void installWaveProgressHud() {
        Game game = activeGame();
        if (game == null || game.getZombieWaves().isEmpty()) {
            return;
        }
        if (isBossLevel()) {
            installBossHealthHud(game);
            return;
        }
        if (waveProgressActor != null) {
            return;
        }
        waveProgressActor = new WaveProgressActor(game);
        waveProgressActor.setBounds(WAVE_PROGRESS_X, WAVE_PROGRESS_Y,
                WAVE_PROGRESS_WIDTH, WAVE_PROGRESS_HEIGHT);
        waveProgressActor.setTouchable(Touchable.disabled);
        stage.addActor(waveProgressActor);
    }

    private boolean isBossLevel() {
        GameMenu menu = isModelBackedGame() ? currentGameMenu() : null;
        Level level = menu == null ? previewLevel : menu.getLevel();
        return level != null && level.getKind() == LevelKind.BOSS;
    }

    private void installBossHealthHud(Game game) {
        if (game == null || bossHealthActor != null) {
            return;
        }
        bossHealthActor = new BossHealthActor(game);
        bossHealthActor.setBounds(WAVE_PROGRESS_X, WAVE_PROGRESS_Y,
                WAVE_PROGRESS_WIDTH, WAVE_PROGRESS_HEIGHT);
        bossHealthActor.setTouchable(Touchable.disabled);
        stage.addActor(bossHealthActor);
    }

    private void installPlantWhatYouGetWaveButton() {
        Game game = activeGame();
        if (game == null || !game.hasPlantWhatYouGet()
                || plantWhatYouGetWaveButton != null) {
            return;
        }

        plantWhatYouGetWaveButton = new TextButton(
                "START WAVE", skin, "green");
        plantWhatYouGetWaveButton.setBounds(
                PLANT_WHAT_YOU_GET_WAVE_BUTTON_X,
                PLANT_WHAT_YOU_GET_WAVE_BUTTON_Y,
                PLANT_WHAT_YOU_GET_WAVE_BUTTON_WIDTH,
                PLANT_WHAT_YOU_GET_WAVE_BUTTON_HEIGHT);
        plantWhatYouGetWaveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startNextPlantWhatYouGetWave();
            }
        });
        stage.addActor(plantWhatYouGetWaveButton);
        refreshPlantWhatYouGetWaveButton();
    }

    private void startNextPlantWhatYouGetWave() {
        Game game = activeGame();
        if (game == null || !game.hasPlantWhatYouGet()
                || gamePaused || pauseModal != null
                || plantSelectionModal != null
                || pendingAnnouncementWaveNumber != 0
                || gameAnnouncementLabel != null
                        && gameAnnouncementLabel.isVisible()) {
            return;
        }

        if (!game.haveZombieWavesStarted()
                && !game.startZombieWavesFromGui()) {
            return;
        }
        if (!game.isNextWaveReadyForGui()) {
            refreshPlantWhatYouGetWaveButton();
            return;
        }

        int waveNumber = game.getNextWaveNumberForGui();
        if (waveNumber > 0) {
            queueWaveAnnouncements(waveNumber, false);
        }
        refreshPlantWhatYouGetWaveButton();
    }

    private void refreshPlantWhatYouGetWaveButton() {
        Game game = activeGame();
        if (plantWhatYouGetWaveButton == null || game == null
                || !game.hasPlantWhatYouGet()) {
            return;
        }

        int nextWave = game.getNextWaveNumberForGui();
        if (!game.haveZombieWavesStarted()) {
            plantWhatYouGetWaveButton.setText("START WAVE");
        } else if (nextWave <= 0) {
            plantWhatYouGetWaveButton.setText("ALL WAVES SENT");
        } else {
            plantWhatYouGetWaveButton.setText("START WAVE " + nextWave);
        }

        boolean announcementActive = pendingAnnouncementWaveNumber != 0
                || gameAnnouncementLabel != null
                        && gameAnnouncementLabel.isVisible();
        boolean ready = !game.haveZombieWavesStarted()
                || game.isNextWaveReadyForGui();
        boolean disabled = gamePaused || pauseModal != null
                || plantSelectionModal != null || announcementActive
                || nextWave <= 0 || !ready;
        plantWhatYouGetWaveButton.setDisabled(disabled);
        plantWhatYouGetWaveButton.setTouchable(disabled
                ? Touchable.disabled : Touchable.enabled);
    }

    private void installSeedTray() {
        seedTray = new Table();
        seedTray.top();
        seedTray.setBounds(SEED_TRAY_X, SEED_TRAY_Y,
                SEED_TRAY_WIDTH, SEED_TRAY_HEIGHT);
        stage.addActor(seedTray);
    }

    private void installConveyorBelt() {
        Game game = activeGame();
        if (game == null || !game.hasConveyorBelt()
                || conveyorBelt != null || plantingOverlayPixel == null) {
            return;
        }

        conveyorBelt = new Group();
        conveyorBelt.setBounds(SEED_TRAY_X, SEED_TRAY_Y,
                SEED_TRAY_WIDTH, SEED_TRAY_HEIGHT);

        Actor track = new Actor() {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                Color previous = new Color(batch.getColor());
                batch.setColor(0f, 0f, 0f, 0.46f * parentAlpha);
                batch.draw(plantingOverlayPixel,
                        getX(), getY(), getWidth(), getHeight());
                batch.setColor(previous);
            }
        };
        track.setBounds(0f, 0f, SEED_TRAY_WIDTH, SEED_TRAY_HEIGHT);
        track.setTouchable(Touchable.disabled);
        conveyorBelt.addActor(track);
        stage.addActor(conveyorBelt);
        refreshConveyorBelt();
    }

    private void refreshConveyorBelt() {
        Game game = activeGame();
        if (conveyorBelt == null || game == null
                || !game.hasConveyorBelt()) {
            return;
        }

        List<ConveyorPlantPacket> packets = game.getConveyorPackets();
        Iterator<Map.Entry<Long, ConveyorPacketActor>> iterator =
                conveyorPacketActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ConveyorPacketActor> entry = iterator.next();
            if (findConveyorPacket(packets, entry.getKey()) == null) {
                entry.getValue().remove();
                iterator.remove();
            }
        }

        if (selectedConveyorPacketSequence != null
                && findConveyorPacket(packets,
                        selectedConveyorPacketSequence) == null) {
            selectedConveyorPacketSequence = null;
            selectedConveyorPlantName = null;
            rebuildCursorPlantActor();
        }

        for (int index = 0; index < packets.size(); index++) {
            ConveyorPlantPacket packet = packets.get(index);
            ConveyorPacketActor actor = conveyorPacketActors.get(
                    packet.getSequenceNumber());
            if (actor == null) {
                actor = new ConveyorPacketActor(packet);
                actor.setPosition(CONVEYOR_CARD_INSET, 0f);
                conveyorPacketActors.put(packet.getSequenceNumber(), actor);
                conveyorBelt.addActor(actor);
            }
            actor.setTargetY(conveyorTargetY(index));
            actor.setSelected(selectedConveyorPacketSequence != null
                    && selectedConveyorPacketSequence.longValue()
                            == packet.getSequenceNumber());
        }
    }

    private float conveyorTargetY(int zeroBasedIndex) {
        float top = SEED_TRAY_HEIGHT - CONVEYOR_CARD_HEIGHT
                - CONVEYOR_CARD_INSET;
        return Math.max(CONVEYOR_CARD_INSET,
                top - zeroBasedIndex
                        * (CONVEYOR_CARD_HEIGHT + CONVEYOR_CARD_GAP));
    }

    private static ConveyorPlantPacket findConveyorPacket(
            List<ConveyorPlantPacket> packets, long sequenceNumber) {
        if (packets == null) {
            return null;
        }
        for (ConveyorPlantPacket packet : packets) {
            if (packet.getSequenceNumber() == sequenceNumber) {
                return packet;
            }
        }
        return null;
    }

    private int conveyorPacketIndex(long sequenceNumber) {
        Game game = activeGame();
        if (game == null || !game.hasConveyorBelt()) {
            return -1;
        }
        List<ConveyorPlantPacket> packets = game.getConveyorPackets();
        for (int index = 0; index < packets.size(); index++) {
            if (packets.get(index).getSequenceNumber() == sequenceNumber) {
                return index + 1;
            }
        }
        return -1;
    }

    private void installVaseBreakerSeedTray() {
        Game game = activeGame();
        if (!(game instanceof VaseBreaker) || vaseSeedTray != null
                || plantingOverlayPixel == null) {
            return;
        }

        vaseSeedTray = new Group();
        vaseSeedTray.setBounds(SEED_TRAY_X, SEED_TRAY_Y,
                SEED_TRAY_WIDTH, SEED_TRAY_HEIGHT);

        Actor track = new Actor() {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                Color previous = new Color(batch.getColor());
                batch.setColor(0f, 0f, 0f, 0.46f * parentAlpha);
                batch.draw(plantingOverlayPixel,
                        getX(), getY(), getWidth(), getHeight());
                batch.setColor(previous);
            }
        };
        track.setBounds(0f, 0f, SEED_TRAY_WIDTH, SEED_TRAY_HEIGHT);
        track.setTouchable(Touchable.disabled);
        vaseSeedTray.addActor(track);

        Label header = new Label("VASE PLANTS", skin, "medium_outline");
        header.setFontScale(0.55f);
        header.setAlignment(Align.center);
        header.setBounds(2f, SEED_TRAY_HEIGHT - VASE_SEED_HEADER_HEIGHT,
                SEED_TRAY_WIDTH - 4f, VASE_SEED_HEADER_HEIGHT);
        header.setTouchable(Touchable.disabled);
        vaseSeedTray.addActor(header);

        stage.addActor(vaseSeedTray);
        refreshVaseBreakerSeedTray();
    }

    private void refreshVaseBreakerSeedTray() {
        Game game = activeGame();
        if (vaseSeedTray == null || !(game instanceof VaseBreaker)) {
            return;
        }

        List<VaseSeedPacket> packets =
                ((VaseBreaker) game).getAvailableSeedPackets();
        Iterator<Map.Entry<VaseSeedPacket, VaseSeedPacketActor>> iterator =
                vaseSeedPacketActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<VaseSeedPacket, VaseSeedPacketActor> entry =
                    iterator.next();
            if (!packets.contains(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }

        if (selectedVaseSeedPacket != null
                && !packets.contains(selectedVaseSeedPacket)) {
            selectedVaseSeedPacket = null;
            rebuildCursorPlantActor();
        }

        float top = SEED_TRAY_HEIGHT - VASE_SEED_HEADER_HEIGHT
                - VASE_SEED_CARD_HEIGHT - VASE_SEED_CARD_INSET;
        for (int index = 0; index < packets.size(); index++) {
            VaseSeedPacket packet = packets.get(index);
            VaseSeedPacketActor actor = vaseSeedPacketActors.get(packet);
            if (actor == null) {
                actor = new VaseSeedPacketActor(packet);
                vaseSeedPacketActors.put(packet, actor);
                vaseSeedTray.addActor(actor);
            }
            float y = Math.max(VASE_SEED_CARD_INSET,
                    top - index
                            * (VASE_SEED_CARD_HEIGHT + VASE_SEED_CARD_GAP));
            actor.setBounds(VASE_SEED_CARD_INSET, y,
                    VASE_SEED_CARD_WIDTH, VASE_SEED_CARD_HEIGHT);
            actor.setSelected(packet == selectedVaseSeedPacket);
            actor.refreshTimer();
        }
    }

    private void refreshVaseSeedSelectionOutlines() {
        for (Map.Entry<VaseSeedPacket, VaseSeedPacketActor> entry
                : vaseSeedPacketActors.entrySet()) {
            entry.getValue().setSelected(
                    entry.getKey() == selectedVaseSeedPacket);
        }
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
        if (enabled && hasPlantPlacementSelection()) {
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

    private BoardLayout currentBoardLayout() {
        BoardLayout layout = layoutForChapter(seedTrayChapter());
        if (layout != null) {
            return layout;
        }
        Game game = activeGame();
        if (game == null) {
            return null;
        }
        GameMenu menu = currentGameMenu();
        // installChapterBoard() uses the Egypt lawn for every minigame that
        // has no adventure chapter. Coordinate conversion must use the same
        // fallback, otherwise minigame entities can exist in the model but
        // cannot be positioned, hovered, or clicked on the graphical board.
        return menu != null && menu.isMinigame()
                ? EGYPT_BOARD : null;
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

        Game protectedSeedGame = activeGame();
        if (protectedSeedGame != null && protectedSeedGame.hasSaveOurSeeds()) {
            Actor protectedSeedHighlights = new Actor() {
                @Override
                public void draw(Batch batch, float parentAlpha) {
                    float previousR = batch.getColor().r;
                    float previousG = batch.getColor().g;
                    float previousB = batch.getColor().b;
                    float previousA = batch.getColor().a;
                    batch.setColor(1f, 0.05f, 0.05f,
                            0.34f * parentAlpha);
                    for (io.github.Plants_Vs_Zombies_2.model.game.special.ProtectedPlantStatus status
                            : protectedSeedGame.getProtectedPlantStatuses()) {
                        CellBounds bounds = screenBoundsForCell(
                                status.getOriginalPosition());
                        if (bounds == null) {
                            continue;
                        }
                        batch.draw(plantingOverlayPixel,
                                bounds.x, bounds.y,
                                bounds.width, bounds.height);
                    }
                    batch.setColor(previousR, previousG,
                            previousB, previousA);
                }
            };
            protectedSeedHighlights.setTouchable(Touchable.disabled);
            protectedSeedHighlights.setBounds(
                    0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            addBackgroundOverlay(protectedSeedHighlights);
        }

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
                    if (hasBoardPlacementSelection()) {
                        clearBoardPlacementSelection();
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
                if (tryBreakVaseAt(boardPosition)) {
                    event.stop();
                    return true;
                }
                if (!canInteractWithBoard()
                        || !hasBoardPlacementSelection()) {
                    return false;
                }

                if (selectedIZombieCard != null) {
                    placeSelectedIZombieAt(boardPosition);
                } else {
                    plantSelectedPlantAt(boardPosition);
                }
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
                && (game.allowsDirectPlanting() || game.hasConveyorBelt()
                        || game instanceof VaseBreaker
                        || game instanceof IZombie);
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
        selectedConveyorPacketSequence = null;
        selectedConveyorPlantName = null;
        selectedVaseSeedPacket = null;
        selectedIZombieCard = null;
        rebuildCursorPlantActor();
        rebuildSeedTray();
        refreshConveyorBelt();
        refreshVaseSeedSelectionOutlines();
    }

    private void selectConveyorPacket(ConveyorPlantPacket packet) {
        if (packet == null || !canInteractWithBoard()) {
            return;
        }
        if (shovelMode) {
            setShovelMode(false);
        }
        selectedPlantForPlacement = null;
        selectedConveyorPacketSequence = packet.getSequenceNumber();
        selectedConveyorPlantName = packet.getPlantType();
        selectedVaseSeedPacket = null;
        selectedIZombieCard = null;
        rebuildCursorPlantActor();
        rebuildSeedTray();
        refreshConveyorBelt();
        refreshVaseSeedSelectionOutlines();
    }

    private void selectVaseSeedPacket(VaseSeedPacket packet) {
        if (packet == null || packet.isRemoved()
                || !canInteractWithBoard()) {
            return;
        }
        if (shovelMode) {
            setShovelMode(false);
        }
        selectedPlantForPlacement = null;
        selectedConveyorPacketSequence = null;
        selectedConveyorPlantName = null;
        selectedVaseSeedPacket = packet;
        selectedIZombieCard = null;
        rebuildCursorPlantActor();
        rebuildSeedTray();
        refreshConveyorBelt();
        refreshVaseSeedSelectionOutlines();
    }

    private boolean hasPlantPlacementSelection() {
        return selectedPlantForPlacement != null
                || selectedConveyorPacketSequence != null
                || selectedVaseSeedPacket != null;
    }

    private boolean hasBoardPlacementSelection() {
        return hasPlantPlacementSelection() || selectedIZombieCard != null;
    }

    private String selectedPlacementPlantName() {
        if (selectedVaseSeedPacket != null) {
            return selectedVaseSeedPacket.getPlantType();
        }
        if (selectedConveyorPacketSequence != null) {
            return selectedConveyorPlantName;
        }
        return selectedPlantForPlacement == null
                ? null : selectedPlantForPlacement.getName();
    }

    private void clearSelectedPlantForPlacement() {
        selectedPlantForPlacement = null;
        selectedConveyorPacketSequence = null;
        selectedConveyorPlantName = null;
        selectedVaseSeedPacket = null;
        selectedIZombieCard = null;
        if (cursorPlantActor != null) {
            cursorPlantActor.remove();
            cursorPlantActor = null;
        }
        if (hoveredBoardCell != null) {
            hoveredBoardCell.setVisible(false);
        }
        rebuildSeedTray();
        refreshConveyorBelt();
        refreshVaseSeedSelectionOutlines();
    }

    private void clearBoardPlacementSelection() {
        clearSelectedPlantForPlacement();
        rebuildIZombieTray();
    }

    private void selectIZombieCard(IZombieCard card) {
        if (card == null || !(activeGame() instanceof IZombie)
                || !canInteractWithBoard()) {
            return;
        }
        IZombie iZombie = (IZombie) activeGame();
        double remaining = iZombie.getCardCooldownRemainingSeconds(card);
        if (remaining > 0.001) {
            showGameNotice(card.getType().getAlias()
                    + " is recharging ("
                    + String.format(java.util.Locale.ROOT, "%.1fs", remaining)
                    + ")!", Color.RED);
            return;
        }
        if (iZombie.getSunCount() < card.getCost()) {
            showGameNotice("Not enough sun for "
                    + card.getType().getAlias() + "!", Color.RED);
            return;
        }
        if (shovelMode) {
            setShovelMode(false);
        }
        selectedPlantForPlacement = null;
        selectedConveyorPacketSequence = null;
        selectedConveyorPlantName = null;
        selectedVaseSeedPacket = null;
        selectedIZombieCard = card;
        rebuildCursorPlantActor();
        rebuildIZombieTray();
    }

    private void placeSelectedIZombieAt(EntityPosition position) {
        Game game = activeGame();
        IZombieCard card = selectedIZombieCard;
        if (!(game instanceof IZombie) || card == null || position == null) {
            return;
        }
        IZombiePlacementResult result = ((IZombie) game).placeZombie(
                card.getType().name(), position);
        switch (result) {
            case SUCCESS:
                selectedIZombieCard = null;
                rebuildIZombieTray();
                refreshSunHud();
                refreshZombieRendering();
                break;
            case NOT_ENOUGH_SUN:
                showGameNotice("Not enough sun for that zombie!", Color.RED);
                break;
            case RECHARGING:
                showGameNotice("That zombie card is still recharging!",
                        Color.RED);
                break;
            case LEFT_OF_RED_LINE:
                showGameNotice("Place zombies on the right side of the red line!",
                        Color.RED);
                break;
            case POSITION_OCCUPIED:
                showGameNotice("That tile already has a zombie!", Color.RED);
                break;
            case GAME_NOT_ACTIVE:
            case UNKNOWN_ZOMBIE:
            case BOSS_NOT_ALLOWED:
            case INVALID_POSITION:
            default:
                break;
        }
    }

    private void installIZombieTray() {
        if (!(activeGame() instanceof IZombie) || iZombieTray != null) {
            return;
        }
        iZombieTray = new Table();
        iZombieTray.top();
        iZombieTray.setBounds(SEED_TRAY_X, SEED_TRAY_Y,
                SEED_TRAY_WIDTH, SEED_TRAY_HEIGHT);
        stage.addActor(iZombieTray);
        rebuildIZombieTray();
    }

    private void installIZombieBoardOverlay() {
        if (!(activeGame() instanceof IZombie)
                || iZombieBoardOverlay != null
                || plantingOverlayPixel == null) {
            return;
        }

        iZombieBoardOverlay = new Group();
        iZombieBoardOverlay.setTouchable(Touchable.disabled);

        Actor redLine = new Actor() {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                Color previous = new Color(batch.getColor());
                batch.setColor(1f, 0.05f, 0.05f,
                        0.82f * parentAlpha);
                batch.draw(plantingOverlayPixel,
                        getX(), getY(), getWidth(), getHeight());
                batch.setColor(previous);
            }
        };
        redLine.setName("i-zombie-red-line");
        redLine.setTouchable(Touchable.disabled);
        iZombieBoardOverlay.addActor(redLine);

        for (int row = 0; row < BOARD_ROWS; row++) {
            Actor brain;
            try {
                brain = new PamAnimationActor(
                        navigator.getPamPlayer(), I_ZOMBIE_BRAIN_PAM,
                        "animation");
            } catch (RuntimeException exception) {
                Image fallback = createAssetImage(
                        I_ZOMBIE_BRAIN_FALLBACK_ASSET);
                fallback.setScaling(Scaling.fit);
                brain = fallback;
            }
            brain.setTouchable(Touchable.disabled);
            iZombieBrainActors.add(brain);
            iZombieBoardOverlay.addActor(brain);
        }

        addBackgroundOverlay(iZombieBoardOverlay);
        refreshIZombieBoardOverlay();
    }

    private void refreshIZombieBoardOverlay() {
        if (iZombieBoardOverlay == null
                || !(activeGame() instanceof IZombie)) {
            return;
        }
        IZombie game = (IZombie) activeGame();

        CellBounds topLeft = screenBoundsForCell(
                new EntityPosition(0, 0));
        CellBounds bottomLeft = screenBoundsForCell(
                new EntityPosition(BOARD_ROWS - 1, 0));
        CellBounds redLineCell = screenBoundsForCell(
                new EntityPosition(0, game.getRedLineColumn()));
        if (topLeft == null || bottomLeft == null || redLineCell == null) {
            iZombieBoardOverlay.setVisible(false);
            return;
        }
        iZombieBoardOverlay.setVisible(true);
        iZombieBoardOverlay.setBounds(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        Actor redLine = iZombieBoardOverlay.findActor("i-zombie-red-line");
        if (redLine != null) {
            float bottom = bottomLeft.y;
            float top = topLeft.y + topLeft.height;
            redLine.setBounds(
                    redLineCell.x + redLineCell.width - 3.5f,
                    bottom, 7f, top - bottom);
        }

        for (int row = 0; row < iZombieBrainActors.size(); row++) {
            Actor brain = iZombieBrainActors.get(row);
            CellBounds rowCell = screenBoundsForCell(
                    new EntityPosition(row, 0));
            if (rowCell == null) {
                brain.setVisible(false);
                continue;
            }
            float size = Math.min(rowCell.width, rowCell.height) * 0.72f;
            brain.setBounds(
                    rowCell.x - size * 0.88f,
                    rowCell.y + (rowCell.height - size) * 0.5f,
                    size, size);
            brain.setVisible(game.isBrainAvailable(row));
        }
    }

    private void rebuildIZombieTray() {
        if (iZombieTray == null || !(activeGame() instanceof IZombie)) {
            return;
        }
        IZombie game = (IZombie) activeGame();
        iZombieTray.clearChildren();

        Label heading = new Label("ZOMBIES", skin, "medium_outline");
        heading.setFontScale(0.62f);
        heading.setAlignment(Align.center);
        iZombieTray.add(heading).width(SEED_TRAY_WIDTH)
                .height(I_ZOMBIE_HEADER_HEIGHT).row();

        for (IZombieCard card : game.getLevel().getZombieCards()) {
            iZombieTray.add(new IZombieCardActor(card))
                    .width(I_ZOMBIE_CARD_WIDTH)
                    .height(I_ZOMBIE_CARD_HEIGHT)
                    .padBottom(I_ZOMBIE_CARD_GAP).row();
        }
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
        if (game == null || !hasPlantPlacementSelection()
                || position == null) {
            return;
        }

        if (selectedVaseSeedPacket != null) {
            plantSelectedVaseSeedAt(position);
            return;
        }
        if (selectedConveyorPacketSequence != null) {
            plantSelectedConveyorPacketAt(position);
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

        clearSelectedPlantForPlacement();
        refreshSunHud();
        rebuildPlantedPlantLayer();
    }

    private boolean tryBreakVaseAt(EntityPosition position) {
        Game game = activeGame();
        if (!(game instanceof VaseBreaker) || position == null
                || gamePaused || pauseModal != null
                || plantSelectionModal != null) {
            return false;
        }
        BaseStructure structure = game.getBoard().getStructureAt(position);
        if (!(structure instanceof Vase) || structure.isRemoved()) {
            return false;
        }

        VaseBreakResult result = ((VaseBreaker) game).breakVase(position);
        switch (result) {
            case SUCCESS_EMPTY:
                showGameNotice("The vase was empty.", Color.WHITE);
                break;
            case SUCCESS_SEED_PACKET:
                showGameNotice("A plant seed packet was revealed!",
                        Color.WHITE);
                break;
            case SUCCESS_ZOMBIE:
                showGameNotice("A zombie was released!", Color.WHITE);
                break;
            case GAME_NOT_ACTIVE:
            case INVALID_POSITION:
            case NO_VASE:
            default:
                break;
        }
        refreshStructureRendering();
        refreshVaseBreakerSeedTray();
        refreshZombieRendering();
        return true;
    }

    private void plantSelectedVaseSeedAt(EntityPosition position) {
        Game game = activeGame();
        VaseSeedPacket packet = selectedVaseSeedPacket;
        if (!(game instanceof VaseBreaker) || packet == null) {
            return;
        }

        VaseSeedPlantingResult result = ((VaseBreaker) game).plantFromSeed(
                packet.getEntityPosition(), position);
        switch (result) {
            case SUCCESS:
                clearSelectedPlantForPlacement();
                refreshVaseBreakerSeedTray();
                rebuildPlantedPlantLayer();
                break;
            case DESTINATION_BLOCKED:
                showGameNotice("That tile is blocked or occupied!",
                        Color.RED);
                break;
            case INVALID_DESTINATION:
                showGameNotice("Choose a tile on the lawn!", Color.RED);
                break;
            case NO_SEED_PACKET:
            case INVALID_SOURCE:
                clearSelectedPlantForPlacement();
                refreshVaseBreakerSeedTray();
                showGameNotice(
                        "That vase seed packet is no longer available!",
                        Color.RED);
                break;
            case UNKNOWN_PLANT:
                showGameNotice("Unknown plant in vase seed packet!",
                        Color.RED);
                break;
            case GAME_NOT_ACTIVE:
                clearSelectedPlantForPlacement();
                break;
            default:
                break;
        }
    }

    private void plantSelectedConveyorPacketAt(EntityPosition position) {
        Game game = activeGame();
        if (game == null || selectedConveyorPacketSequence == null) {
            return;
        }

        int index = conveyorPacketIndex(selectedConveyorPacketSequence);
        if (index < 1) {
            clearSelectedPlantForPlacement();
            return;
        }

        ConveyorPlacementResult result = game.plantFromConveyor(
                index, position);
        if (result != ConveyorPlacementResult.SUCCESS) {
            showConveyorPlacementError(result);
            return;
        }

        clearSelectedPlantForPlacement();
        refreshConveyorBelt();
        rebuildPlantedPlantLayer();
    }

    private void showConveyorPlacementError(ConveyorPlacementResult result) {
        if (result == null) {
            return;
        }
        switch (result) {
            case OUTSIDE_BOWLING_ZONE:
                showGameNotice(
                        "Launch the Wall-nut on the house side of the red line!",
                        Color.RED);
                break;
            case POSITION_OCCUPIED:
                showGameNotice("That tile is already occupied!", Color.RED);
                break;
            case INVALID_PACKET:
                clearSelectedPlantForPlacement();
                showGameNotice("That conveyor card is no longer available!",
                        Color.RED);
                break;
            case INVALID_POSITION:
                showGameNotice("Choose a tile on the lawn!", Color.RED);
                break;
            case UNKNOWN_PLANT:
                showGameNotice("Unknown plant on the conveyor!", Color.RED);
                break;
            case NOT_CONVEYOR_LEVEL:
                clearSelectedPlantForPlacement();
                break;
            case SUCCESS:
                break;
            default:
                break;
        }
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
        BoardLayout layout = currentBoardLayout();
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
        BoardLayout layout = currentBoardLayout();
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

        boolean planting = hasBoardPlacementSelection()
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
        if (selectedIZombieCard != null
                && activeGame() instanceof IZombie
                && position.getColumn()
                        <= ((IZombie) activeGame()).getRedLineColumn()) {
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
        if (selectedIZombieCard != null) {
            cursorPlantActor = createIZombiePreviewActor(selectedIZombieCard);
            if (cursorPlantActor == null) {
                return;
            }
            cursorPlantActor.setTouchable(Touchable.disabled);
            cursorPlantActor.setColor(1f, 1f, 1f, 0.86f);
            addBackgroundOverlay(cursorPlantActor);
            refreshCursorPlantPosition();
            return;
        }
        String plantName = selectedPlacementPlantName();
        if (plantName == null) {
            return;
        }
        cursorPlantActor = createPlantIdleActor(plantName);
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
        if (!canInteractWithBoard() || !hasBoardPlacementSelection()) {
            cursorPlantActor.setVisible(false);
            return;
        }
        BoardLayout layout = currentBoardLayout();
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
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (selectedIZombieCard != null) {
            float width = cellWidth * 1.04f;
            float height = cellHeight * 1.56f;
            cursorPlantActor.setBounds(
                    mouseX - width * 0.5f,
                    mouseY - height * 0.22f,
                    width, height);
            cursorPlantActor.setVisible(true);
            return;
        }
        if (activeGame() instanceof WallnutBowling
                && selectedConveyorPacketSequence != null) {
            // Bowling packets used to fall through to their seed-packet
            // artwork and were then stretched to almost a whole board cell,
            // producing the oversized pickup image. Use a compact square
            // preview matching the rolling Wall-nut actor instead.
            float size = Math.min(cellWidth, cellHeight) * 0.82f;
            cursorPlantActor.setBounds(
                    mouseX - size * 0.5f,
                    mouseY - size * 0.5f,
                    size, size);
            cursorPlantActor.setVisible(true);
            return;
        }
        float width = cellWidth * 0.92f;
        float height = cellHeight * 1.18f;
        cursorPlantActor.setBounds(
                mouseX - width * 0.5f,
                mouseY - height * 0.42f,
                width, height);
        cursorPlantActor.setVisible(true);
    }

    private Actor createIZombiePreviewActor(IZombieCard card) {
        ZombieVisualCatalog.Visual visual = card == null
                ? null : ZombieVisualCatalog.find(card.getType());
        if (visual == null) {
            return null;
        }
        try {
            return new PamAnimationActor(
                    navigator.getPamPlayer(), visual.getPamPath(),
                    visual.getIdleClip());
        } catch (RuntimeException exception) {
            Image fallback = createAssetImage(visual.getPacketAsset());
            fallback.setScaling(Scaling.fit);
            return fallback;
        }
    }

    private Actor createPlantIdleActor(String plantName) {
        if (activeGame() instanceof WallnutBowling) {
            BowlingWallnutType bowlingType = BowlingWallnutType.find(
                    plantName);
            if (bowlingType != null) {
                try {
                    return new PamAnimationActor(
                            navigator.getPamPlayer(),
                            bowlingWallnutPamPath(bowlingType), "idle");
                } catch (RuntimeException ignored) {
                    // Fall through to the normal plant/packet lookup.
                }
            }
        }
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
        Image fallback = new HurtFlashImage(requireAssetRegion(
                packetArtworkAssetFor(plantName)));
        fallback.setScaling(Scaling.fit);
        return fallback;
    }

    private Actor createPlantIdleActor(BasePlant plant) {
        if (plant == null) {
            return createPlantIdleActor((String) null);
        }
        if (activeGame() instanceof WallnutBowling) {
            BowlingWallnutType bowlingType = BowlingWallnutType.find(
                    plant.getName());
            if (bowlingType != null) {
                try {
                    return new PamAnimationActor(
                            navigator.getPamPlayer(),
                            bowlingWallnutPamPath(bowlingType), "idle");
                } catch (RuntimeException ignored) {
                    // Fall through to the normal plant/packet lookup.
                }
            }
        }
        PlantAnimationCatalog.Preview preview =
                PlantAnimationCatalog.find(plant);
        if (preview != null) {
            try {
                return new PamAnimationActor(
                        navigator.getPamPlayer(),
                        preview.getPath(), preview.getClip());
            } catch (RuntimeException ignored) {
                // Fall through to packet artwork when an optional PAM is absent.
            }
        }
        return createPlantIdleActor(plant.getName());
    }

    private static String bowlingWallnutPamPath(BowlingWallnutType type) {
        if (type == BowlingWallnutType.EXPLOSIVE) {
            return "768/INITIAL/PLANT/EXPLODEONUT/EXPLODEONUT.PAM";
        }
        if (type == BowlingWallnutType.LARGE) {
            return "768/FULL/PLANT/PRIMAL_WALLNUT/PRIMAL_WALLNUT.PAM";
        }
        return "768/INITIAL/PLANT/WALLNUT/WALLNUT.PAM";
    }

    private void rebuildPlantedPlantLayer() {
        Game game = activeGame();
        if (plantedPlantLayer == null || game == null) {
            return;
        }
        IdentityHashMap<BasePlant, Integer> previousHealth =
                new IdentityHashMap<>(plantedPlantHealth);
        IdentityHashMap<SunProducer, Integer> previousProductionSequences =
                new IdentityHashMap<>(plantedSunProductionSequences);
        IdentityHashMap<Shooter, Integer> previousAttackSequences =
                new IdentityHashMap<>(plantedShooterAttackSequences);
        IdentityHashMap<Lobber, Integer> previousLobberAttackSequences =
                new IdentityHashMap<>(plantedLobberAttackSequences);
        plantedPlantLayer.clearChildren();
        plantedPlantActors.clear();
        plantedPlantHealth.clear();
        plantedSunProductionSequences.clear();
        plantedShooterAttackSequences.clear();
        plantedLobberAttackSequences.clear();
        for (BasePlant plant : game.getBoard().getPlants()) {
            Actor actor = createPlantIdleActor(plant);
            actor.setTouchable(Touchable.disabled);
            positionPlantActor(actor, plant.getEntityPosition());
            plantedPlantLayer.addActor(actor);
            plantedPlantActors.put(plant, actor);

            int currentHealth = plant.getCurrentHP();
            Integer oldHealth = previousHealth.get(plant);
            if (oldHealth != null && currentHealth < oldHealth) {
                flashHurt(actor);
            }
            plantedPlantHealth.put(plant, currentHealth);
            if (plant instanceof SunProducer) {
                SunProducer producer = (SunProducer) plant;
                configureSunProductionAnimation(producer, actor);
                int currentSequence = producer.getProductionSequence();
                Integer oldSequence = previousProductionSequences.get(producer);
                if (producer.isProductionAnimationPending()
                        || oldSequence != null && currentSequence > oldSequence) {
                    playSunProductionAnimation(producer, actor);
                }
                plantedSunProductionSequences.put(producer, currentSequence);
            }
            if (plant instanceof Shooter) {
                Shooter shooter = (Shooter) plant;
                configureShooterAttackAnimation(shooter, actor);
                int currentSequence = shooter.getAttackSequence();
                Integer oldSequence = previousAttackSequences.get(shooter);
                if (shooter.isAttackAnimationPending()
                        || oldSequence != null && currentSequence > oldSequence) {
                    playShooterAttackAnimation(shooter, actor);
                }
                plantedShooterAttackSequences.put(shooter, currentSequence);
            }
            if (plant instanceof Lobber) {
                Lobber lobber = (Lobber) plant;
                configureLobberAttackAnimation(lobber, actor);
                int currentSequence = lobber.getAttackSequence();
                Integer oldSequence = previousLobberAttackSequences.get(lobber);
                if (lobber.isAttackAnimationPending()
                        || oldSequence != null && currentSequence > oldSequence) {
                    playLobberAttackAnimation(lobber, actor);
                }
                plantedLobberAttackSequences.put(lobber, currentSequence);
            }
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
                    .append('#').append(plantVisualStateSignature(plant))
                    .append(';');
        }
        return signature.toString();
    }

    private String plantVisualStateSignature(BasePlant plant) {
        if (plant instanceof Melee) {
            Melee melee = (Melee) plant;
            if (melee.getType() == MeleePlantType.KIWIBEAST) {
                return "kiwi" + Math.max(1, Math.min(3, melee.getGrowthStage()));
            }
            if (melee.getType() == MeleePlantType.CHOMPER) {
                return melee.isDigesting() ? "digest" : "ready";
            }
        }
        if (plant instanceof Wallnut) {
            Wallnut wallnut = (Wallnut) plant;
            if (wallnut.getType() == WallnutPlantType.SWEET_POTATO) {
                return "sweet" + sweetPotatoDamageBucket(wallnut);
            }
        }
        return "default";
    }

    private int sweetPotatoDamageBucket(Wallnut wallnut) {
        if (wallnut == null || wallnut.getBaseHP() <= 0) {
            return 0;
        }
        float ratio = wallnut.getCurrentHP() / (float) wallnut.getBaseHP();
        if (ratio > 0.66f) {
            return 0;
        }
        if (ratio > 0.33f) {
            return 1;
        }
        if (ratio > 0.15f) {
            return 2;
        }
        return 3;
    }

    private void refreshPlantedPlantLayerIfNeeded() {
        Game game = activeGame();
        if (game == null || plantedPlantLayer == null) {
            return;
        }
        String signature = createPlantRenderSignature(game);
        if (!signature.equals(plantedPlantRenderSignature)) {
            rebuildPlantedPlantLayer();
            return;
        }
        for (BasePlant plant : game.getBoard().getPlants()) {
            int currentHealth = plant.getCurrentHP();
            Integer oldHealth = plantedPlantHealth.put(
                    plant, currentHealth);
            if (oldHealth != null && currentHealth < oldHealth) {
                flashHurt(plantedPlantActors.get(plant));
            }
            if (plant instanceof SunProducer) {
                SunProducer producer = (SunProducer) plant;
                Actor actor = plantedPlantActors.get(plant);
                configureSunProductionAnimation(producer, actor);
                int currentSequence = producer.getProductionSequence();
                Integer oldSequence = plantedSunProductionSequences.put(
                        producer, currentSequence);
                if (oldSequence != null && currentSequence > oldSequence) {
                    playSunProductionAnimation(producer, actor);
                }
            }
            if (plant instanceof Shooter) {
                Shooter shooter = (Shooter) plant;
                Actor actor = plantedPlantActors.get(plant);
                configureShooterAttackAnimation(shooter, actor);
                int currentSequence = shooter.getAttackSequence();
                Integer oldSequence = plantedShooterAttackSequences.put(
                        shooter, currentSequence);
                if (oldSequence != null && currentSequence > oldSequence) {
                    playShooterAttackAnimation(shooter, actor);
                }
            }
            if (plant instanceof Lobber) {
                Lobber lobber = (Lobber) plant;
                Actor actor = plantedPlantActors.get(plant);
                configureLobberAttackAnimation(lobber, actor);
                int currentSequence = lobber.getAttackSequence();
                Integer oldSequence = plantedLobberAttackSequences.put(
                        lobber, currentSequence);
                if (oldSequence != null && currentSequence > oldSequence) {
                    playLobberAttackAnimation(lobber, actor);
                }
            }
        }
    }

    private void configureShooterAttackAnimation(
            Shooter shooter, Actor actor) {
        boolean canAnimate = actor instanceof PamAnimationActor
                && PlantAnimationCatalog.shooterAttackAnimation(shooter) != null;
        shooter.setDeferProjectilesForAttackAnimation(canAnimate);
    }

    private void playShooterAttackAnimation(
            Shooter shooter, Actor actor) {
        if (!(actor instanceof PamAnimationActor)) {
            shooter.completeAttackAnimation();
            return;
        }
        PlantAnimationCatalog.AttackAnimation animation =
                PlantAnimationCatalog.shooterAttackAnimation(shooter);
        if (animation == null) {
            shooter.completeAttackAnimation();
            return;
        }
        boolean started = ((PamAnimationActor) actor).playOnce(
                animation.getAttackClip(), animation.getIdleClip(),
                animation.getProjectileReleaseFraction(),
                shooter::releaseAttackAnimationProjectiles,
                shooter::completeAttackAnimation);
        if (!started) {
            shooter.completeAttackAnimation();
        }
    }

    private void configureLobberAttackAnimation(
            Lobber lobber, Actor actor) {
        boolean canAnimate = actor instanceof PamAnimationActor
                && PlantAnimationCatalog.lobberAttackAnimation(lobber) != null;
        lobber.setDeferProjectilesForAttackAnimation(canAnimate);
    }

    private void playLobberAttackAnimation(
            Lobber lobber, Actor actor) {
        if (!(actor instanceof PamAnimationActor)) {
            lobber.completeAttackAnimation();
            return;
        }
        PlantAnimationCatalog.AttackAnimation animation =
                PlantAnimationCatalog.lobberAttackAnimation(lobber);
        if (animation == null) {
            lobber.completeAttackAnimation();
            return;
        }
        boolean started = ((PamAnimationActor) actor).playOnce(
                animation.getAttackClip(), animation.getIdleClip(),
                animation.getProjectileReleaseFraction(),
                lobber::releaseAttackAnimationProjectiles,
                lobber::completeAttackAnimation);
        if (!started) {
            lobber.completeAttackAnimation();
        }
    }

    private void configureSunProductionAnimation(
            SunProducer producer, Actor actor) {
        boolean canAnimate = actor instanceof PamAnimationActor
                && PlantAnimationCatalog.sunProductionAnimation(producer) != null;
        producer.setDeferProducedSunsForAnimation(canAnimate);
    }

    private void playSunProductionAnimation(
            SunProducer producer, Actor actor) {
        if (!(actor instanceof PamAnimationActor)) {
            producer.completeProductionAnimation();
            return;
        }
        PlantAnimationCatalog.SunProductionAnimation animation =
                PlantAnimationCatalog.sunProductionAnimation(producer);
        if (animation == null) {
            producer.completeProductionAnimation();
            return;
        }
        boolean started = ((PamAnimationActor) actor).playOnce(
                animation.getProductionClip(), animation.getIdleClip(),
                producer::completeProductionAnimation);
        if (!started) {
            producer.completeProductionAnimation();
        }
    }

    private void flashHurt(Actor actor) {
        if (actor instanceof PamAnimationActor) {
            ((PamAnimationActor) actor).flashHurt();
        } else if (actor instanceof HurtFlashImage) {
            ((HurtFlashImage) actor).flashHurt();
        }
    }

    private void installFrostbitePlantIceRendering() {
        if (!isModelBackedGame() || !isFrostbiteGame()
                || frostbitePlantIceLayer != null) {
            return;
        }
        frostbitePlantIceLayer = new Group();
        frostbitePlantIceLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(frostbitePlantIceLayer);
        refreshFrostbitePlantIceRendering();
    }

    private void refreshFrostbitePlantIceRendering() {
        Game game = activeGame();
        if (!isFrostbiteGame() || game == null
                || frostbitePlantIceLayer == null) {
            return;
        }
        IdentityHashMap<BasePlant, Boolean> present = new IdentityHashMap<>();
        for (BasePlant plant : game.getBoard().getPlants()) {
            if (plant == null || plant.isRemoved() || plant.isDestroyed()
                    || !plant.isFrozen()) {
                continue;
            }
            Actor plantActor = plantedPlantActors.get(plant);
            if (plantActor == null) {
                continue;
            }
            present.put(plant, Boolean.TRUE);
            String asset = frostbitePlantIceAsset(plant);
            Image ice = frostbitePlantIceActors.get(plant);
            if (ice == null || !asset.equals(frostbitePlantIceKeys.get(plant))) {
                if (ice != null) {
                    ice.remove();
                }
                ice = createAssetImage(asset);
                ice.setScaling(Scaling.fit);
                ice.setTouchable(Touchable.disabled);
                frostbitePlantIceActors.put(plant, ice);
                frostbitePlantIceKeys.put(plant, asset);
                frostbitePlantIceLayer.addActor(ice);
            }
            positionFrostbitePlantIceActor(ice, plantActor);
        }

        Iterator<Map.Entry<BasePlant, Image>> iterator =
                frostbitePlantIceActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BasePlant, Image> entry = iterator.next();
            if (!present.containsKey(entry.getKey())) {
                entry.getValue().remove();
                frostbitePlantIceKeys.remove(entry.getKey());
                iterator.remove();
            }
        }
    }

    private void positionFrostbitePlantIceActor(Actor ice, Actor plantActor) {
        if (ice == null || plantActor == null) {
            return;
        }
        float width = plantActor.getWidth() * 1.10f;
        float height = plantActor.getHeight() * 1.08f;
        ice.setBounds(plantActor.getX() - (width - plantActor.getWidth()) * 0.5f,
                plantActor.getY() - plantActor.getHeight() * 0.01f,
                width, height);
        ice.setVisible(plantActor.isVisible());
    }

    private String frostbitePlantIceAsset(BasePlant plant) {
        return chooseFrostbiteIceAsset(FROSTBITE_PLANT_ICE_ASSETS,
                plant.getIceShellHitPoints(),
                plant.getIceShellMaximumHitPoints());
    }

    /**
     * Renders model structures that can intercept projectiles. Ancient Egypt
     * begins with several graves, so leaving structures invisible made peas
     * appear to vanish in empty board cells when they correctly hit a grave.
     */
    private void installStructureRendering() {
        if (!isModelBackedGame() || structureLayer != null) {
            return;
        }
        structureLayer = new Group();
        structureLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(structureLayer);
        refreshStructureRendering();
    }

    private void refreshStructureRendering() {
        Game game = activeGame();
        if (game == null || structureLayer == null) {
            return;
        }

        IdentityHashMap<Grave, Boolean> present = new IdentityHashMap<>();
        for (BaseStructure structure : game.getBoard().getStructures()) {
            if (!(structure instanceof Grave) || structure.isRemoved()) {
                continue;
            }
            Grave grave = (Grave) structure;
            present.put(grave, Boolean.TRUE);
            int currentHitPoints = grave.getHitPoints();
            Integer previousHitPoints = graveHitPoints.put(
                    grave, currentHitPoints);
            boolean wasDamaged = previousHitPoints != null
                    && currentHitPoints < previousHitPoints;

            String pamPath = gravePamPath(grave);
            String clip = graveDamageClip(grave);
            String visualKey = pamPath + '|' + clip;
            PamAnimationActor actor = graveActors.get(grave);
            if (actor == null || !visualKey.equals(graveVisualKeys.get(grave))) {
                if (actor != null) {
                    actor.remove();
                }
                try {
                    actor = new PamAnimationActor(
                            navigator.getPamPlayer(), pamPath, clip);
                } catch (RuntimeException ignored) {
                    actor = null;
                }
                if (actor == null) {
                    graveActors.remove(grave);
                    graveVisualKeys.remove(grave);
                    continue;
                }
                actor.setTouchable(Touchable.disabled);
                graveActors.put(grave, actor);
                graveVisualKeys.put(grave, visualKey);
                structureLayer.addActor(actor);
            }
            if (wasDamaged) {
                actor.flashHurt();
            }
            positionGraveActor(actor, grave);
        }

        Iterator<Map.Entry<Grave, PamAnimationActor>> iterator =
                graveActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Grave, PamAnimationActor> entry = iterator.next();
            if (!present.containsKey(entry.getKey())) {
                entry.getValue().remove();
                graveVisualKeys.remove(entry.getKey());
                graveHitPoints.remove(entry.getKey());
                iterator.remove();
            }
        }

        IdentityHashMap<Vase, Boolean> presentVases = new IdentityHashMap<>();
        for (BaseStructure structure : game.getBoard().getStructures()) {
            if (!(structure instanceof Vase) || structure.isRemoved()) {
                continue;
            }
            Vase vase = (Vase) structure;
            presentVases.put(vase, Boolean.TRUE);
            Image actor = vaseActors.get(vase);
            if (actor == null) {
                actor = createAssetImage(vaseAsset(vase.getType()));
                actor.setScaling(Scaling.fit);
                actor.setTouchable(Touchable.disabled);
                vaseActors.put(vase, actor);
                structureLayer.addActor(actor);
            }
            positionVaseActor(actor, vase);
        }

        Iterator<Map.Entry<Vase, Image>> vaseIterator =
                vaseActors.entrySet().iterator();
        while (vaseIterator.hasNext()) {
            Map.Entry<Vase, Image> entry = vaseIterator.next();
            if (!presentVases.containsKey(entry.getKey())) {
                entry.getValue().remove();
                vaseIterator.remove();
            }
        }
    }

    private static String vaseAsset(VaseType type) {
        if (type == VaseType.PLANT) {
            return VASE_GREEN_ASSET;
        }
        if (type == VaseType.GIANT) {
            return VASE_GARGANTUAR_ASSET;
        }
        return VASE_BROWN_ASSET;
    }

    private void positionVaseActor(Actor actor, Vase vase) {
        CellBounds cell = screenBoundsForCell(vase.getPosition());
        if (cell == null) {
            actor.setVisible(false);
            return;
        }
        float height = cell.height * 0.96f;
        float width = height * 115f / 150f;
        actor.setBounds(
                cell.x + (cell.width - width) * 0.5f,
                cell.y + cell.height * 0.02f,
                width, height);
        actor.setVisible(true);
    }

    private String gravePamPath(Grave grave) {
        Chapter chapter = seedTrayChapter();
        if (chapter != null && "dark-ages".equals(chapter.getId())) {
            if (grave.getReward() == GraveReward.SUN) {
                return DARK_GRAVE_SUN_PAM;
            }
            if (grave.getReward() == GraveReward.PLANT_FOOD) {
                return DARK_GRAVE_PLANT_FOOD_PAM;
            }
            return DARK_GRAVE_EMPTY_PAM;
        }
        return EGYPT_GRAVE_PAM;
    }

    private String graveDamageClip(Grave grave) {
        float healthRatio = Math.max(0f, Math.min(1f,
                grave.getHitPoints() / (float) Grave.DEFAULT_HIT_POINTS));
        if (healthRatio > 0.80f) {
            return "undamaged";
        }
        if (healthRatio > 0.60f) {
            return "damage1";
        }
        if (healthRatio > 0.40f) {
            return "damage2";
        }
        if (healthRatio > 0.20f) {
            return "damage3";
        }
        return "damage4";
    }

    private void positionGraveActor(Actor actor, Grave grave) {
        if (actor == null || grave == null || grave.getPosition() == null) {
            return;
        }
        CellBounds cell = screenBoundsForCell(grave.getPosition());
        if (cell == null) {
            actor.setVisible(false);
            return;
        }
        float width = cell.width * 0.90f;
        float height = cell.height * 1.28f;
        actor.setBounds(
                cell.x + (cell.width - width) * 0.5f,
                cell.y + cell.height * 0.02f,
                width, height);
        actor.setVisible(true);
    }

    private void installZombieRendering() {
        if (!isModelBackedGame() || zombieLayer != null) {
            return;
        }
        if (isAncientEgyptGame()) {
            egyptSandstormRearLayer = new Group();
            egyptSandstormRearLayer.setTouchable(Touchable.disabled);
            addBackgroundOverlay(egyptSandstormRearLayer);
        }

        zombieLayer = new Group();
        zombieLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(zombieLayer);

        if (isAncientEgyptGame()) {
            egyptSandstormTopLayer = new Group();
            egyptSandstormTopLayer.setTouchable(Touchable.disabled);
            addBackgroundOverlay(egyptSandstormTopLayer);
        }
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
            int currentDurability = zombie.getCurrentDurability();
            Integer previousDurability = zombieDurability.put(
                    zombie, currentDurability);
            boolean wasDamaged = previousDurability != null
                    && currentDurability < previousDurability;
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
                startEgyptSandstormIfNeeded(game, zombie);
            }
            if (wasDamaged) {
                actor.flashHurt();
            }
            actor.setEating(isZombieEatingPlant(game, zombie));
            positionZombieActor(actor, zombie);
            positionEgyptSandstormEffect(egyptSandstorms.get(zombie), zombie);
        }

        Iterator<Map.Entry<Zombie, ZombiePamActor>> iterator =
                zombieActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Zombie, ZombiePamActor> entry = iterator.next();
            if (!present.containsKey(entry.getKey())) {
                entry.getValue().remove();
                zombieDurability.remove(entry.getKey());
                removeEgyptSandstorm(entry.getKey());
                iterator.remove();
            }
        }
    }

    private boolean isAncientEgyptGame() {
        Chapter chapter = chapterForCurrentGame();
        return chapter != null && "ancient-egypt".equals(chapter.getId());
    }

    private void startEgyptSandstormIfNeeded(Game game, Zombie zombie) {
        if (!isAncientEgyptGame() || game == null || zombie == null
                || zombie.getTornadoAdvanceColumns() <= 0
                || egyptSandstormRearLayer == null
                || egyptSandstormTopLayer == null
                || egyptSandstorms.containsKey(zombie)) {
            return;
        }

        // A saved game can contain an old tornado-spawned zombie that has
        // already walked far away from its arrival point. Only replay the
        // arrival effect while the zombie is still close to the exact
        // advanced spawn column chosen by the Phase-1 tornado logic.
        double expectedSpawnColumn = game.getBoard().getNumberOfColumns()
                - 0.001 - zombie.getTornadoAdvanceColumns();
        if (Math.abs(zombie.getColumnPosition() - expectedSpawnColumn) > 0.40) {
            return;
        }

        try {
            EgyptSandstormEffect effect = new EgyptSandstormEffect(zombie);
            egyptSandstorms.put(zombie, effect);
            egyptSandstormRearLayer.addActor(effect.rear);
            egyptSandstormTopLayer.addActor(effect.top);
            positionEgyptSandstormEffect(effect, zombie);
        } catch (RuntimeException ignored) {
            // The model already advanced the zombie. Missing optional vortex
            // artwork must never stop the Ancient Egypt level from running.
        }
    }

    private void refreshEgyptSandstorms(float deltaSeconds) {
        if (egyptSandstorms.isEmpty()) {
            return;
        }
        for (EgyptSandstormEffect effect :
                new ArrayList<>(egyptSandstorms.values())) {
            if (effect == null) {
                continue;
            }
            positionEgyptSandstormEffect(effect, effect.zombie);
            effect.update(Math.max(0f, deltaSeconds));
        }
    }

    private void removeEgyptSandstorm(Zombie zombie) {
        EgyptSandstormEffect effect = egyptSandstorms.remove(zombie);
        if (effect != null) {
            effect.remove();
        }
    }

    private void finishEgyptSandstorm(Zombie zombie,
            EgyptSandstormEffect expected) {
        if (egyptSandstorms.get(zombie) != expected) {
            return;
        }
        egyptSandstorms.remove(zombie);
        expected.remove();
    }

    private void positionEgyptSandstormEffect(EgyptSandstormEffect effect,
            Zombie zombie) {
        BoardLayout layout = currentBoardLayout();
        if (effect == null || zombie == null || layout == null
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

        float height = cellHeight * 2.25f;
        float width = Math.max(cellWidth * 1.75f, height * 275f / 320f);
        float x = centerX - width * 0.5f;
        float y = laneBottom - cellHeight * 0.18f;
        effect.rear.setBounds(x, y, width, height);
        effect.top.setBounds(x, y, width, height);
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
        BoardLayout layout = currentBoardLayout();
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
        if (zombie.getType().isBoss()) {
            widthScale = zombie.getType() == io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType.ZOMBOSS_BEACH
                    ? 2.65f : 2.35f;
            heightScale = 2.75f;
        }
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

    private void installFrostbiteZombieIceRendering() {
        if (!isModelBackedGame() || !isFrostbiteGame()
                || frostbiteZombieIceLayer != null) {
            return;
        }
        frostbiteZombieIceLayer = new Group();
        frostbiteZombieIceLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(frostbiteZombieIceLayer);
        refreshFrostbiteZombieIceRendering();
    }

    private void refreshFrostbiteZombieIceRendering() {
        Game game = activeGame();
        if (!isFrostbiteGame() || game == null
                || frostbiteZombieIceLayer == null) {
            return;
        }
        IdentityHashMap<Zombie, Boolean> present = new IdentityHashMap<>();
        for (Zombie zombie : game.getBoard().getZombies()) {
            if (zombie == null || zombie.isDead() || zombie.isRemoved()
                    || !zombie.isEncasedInIce()) {
                continue;
            }
            Actor zombieActor = zombieActors.get(zombie);
            if (zombieActor == null) {
                continue;
            }
            present.put(zombie, Boolean.TRUE);
            String asset = chooseFrostbiteIceAsset(
                    FROSTBITE_ZOMBIE_ICE_ASSETS,
                    zombie.getFrozenShellHitPoints(),
                    zombie.getFrozenShellMaximumHitPoints());
            Image ice = frostbiteZombieIceActors.get(zombie);
            if (ice == null || !asset.equals(frostbiteZombieIceKeys.get(zombie))) {
                if (ice != null) {
                    ice.remove();
                }
                ice = createAssetImage(asset);
                ice.setScaling(Scaling.fit);
                ice.setTouchable(Touchable.disabled);
                frostbiteZombieIceActors.put(zombie, ice);
                frostbiteZombieIceKeys.put(zombie, asset);
                frostbiteZombieIceLayer.addActor(ice);
            }
            positionFrostbiteZombieIceActor(ice, zombieActor);
        }

        Iterator<Map.Entry<Zombie, Image>> iterator =
                frostbiteZombieIceActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Zombie, Image> entry = iterator.next();
            if (!present.containsKey(entry.getKey())) {
                entry.getValue().remove();
                frostbiteZombieIceKeys.remove(entry.getKey());
                iterator.remove();
            }
        }
    }

    private void positionFrostbiteZombieIceActor(Actor ice,
            Actor zombieActor) {
        if (ice == null || zombieActor == null) {
            return;
        }
        float width = zombieActor.getWidth() * 1.06f;
        float height = zombieActor.getHeight() * 1.05f;
        ice.setBounds(zombieActor.getX() - (width - zombieActor.getWidth()) * 0.5f,
                zombieActor.getY() - zombieActor.getHeight() * 0.01f,
                width, height);
        ice.setVisible(zombieActor.isVisible());
    }

    private static String chooseFrostbiteIceAsset(String[] assets,
            int currentHitPoints, int maximumHitPoints) {
        if (maximumHitPoints <= 0) {
            return assets[assets.length - 1];
        }
        float health = Math.max(0f, Math.min(1f,
                currentHitPoints / (float) maximumHitPoints));
        int index = Math.min(assets.length - 1,
                (int) Math.floor((1f - health) * assets.length));
        return assets[index];
    }

    private void installFrostbiteWindRendering() {
        if (!isModelBackedGame() || !isFrostbiteGame()
                || frostbiteWindLayer != null) {
            return;
        }
        frostbiteWindLayer = new Group();
        frostbiteWindLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(frostbiteWindLayer);
        refreshFrostbiteWindRendering();
    }

    private void refreshFrostbiteWindRendering() {
        Game game = activeGame();
        if (!isFrostbiteGame() || game == null || frostbiteWindLayer == null) {
            return;
        }
        double eventTime = game.getLastFrostbiteIcyWindAtSeconds();
        if (eventTime < 0.0
                || eventTime <= renderedFrostbiteWindAtSeconds + 0.000001) {
            return;
        }
        renderedFrostbiteWindAtSeconds = eventTime;
        if (game.getElapsedSeconds() - eventTime
                > FROSTBITE_WIND_DURATION_SECONDS) {
            return;
        }
        for (Integer lane : game.getLastFrostbiteIcyWindLanes()) {
            if (lane == null || lane < 0 || lane >= BOARD_ROWS) {
                continue;
            }
            try {
                FrostbiteWindLaneActor effect =
                        new FrostbiteWindLaneActor(lane);
                positionFrostbiteWindLaneActor(effect, lane);
                frostbiteWindLayer.addActor(effect);
            } catch (RuntimeException ignored) {
                // Missing optional wind artwork must not stop level logic.
            }
        }
    }

    private void positionFrostbiteWindLaneActor(Actor actor, int lane) {
        CellBounds left = screenBoundsForCell(new EntityPosition(lane, 0));
        CellBounds right = screenBoundsForCell(
                new EntityPosition(lane, BOARD_COLUMNS - 1));
        if (actor == null || left == null || right == null) {
            return;
        }
        float width = right.x + right.width - left.x;
        float height = left.height * 1.42f;
        actor.setBounds(left.x, left.y - left.height * 0.16f,
                width, height);
    }

    private void installBowlingWallnutRendering() {
        if (!(activeGame() instanceof WallnutBowling)
                || bowlingWallnutLayer != null) {
            return;
        }
        bowlingWallnutLayer = new Group();
        bowlingWallnutLayer.setTouchable(Touchable.disabled);
        // Keep rolling Wall-nuts above zombies so an impact is always visible.
        addBackgroundOverlay(bowlingWallnutLayer);
        refreshBowlingWallnutRendering();
    }

    private void refreshBowlingWallnutRendering() {
        if (!(activeGame() instanceof WallnutBowling)
                || bowlingWallnutLayer == null) {
            return;
        }

        WallnutBowling bowling = (WallnutBowling) activeGame();
        List<BowlingWallnut> current = bowling.getRollingWallnuts();
        IdentityHashMap<BowlingWallnut, Boolean> present =
                new IdentityHashMap<>();
        for (BowlingWallnut wallnut : current) {
            if (wallnut == null || wallnut.isRemoved()) {
                continue;
            }
            present.put(wallnut, Boolean.TRUE);
            BowlingWallnutActor actor = bowlingWallnutActors.get(wallnut);
            if (actor == null) {
                try {
                    actor = new BowlingWallnutActor(wallnut);
                } catch (RuntimeException ignored) {
                    continue;
                }
                bowlingWallnutActors.put(wallnut, actor);
                bowlingWallnutLayer.addActor(actor);
            }
            actor.refreshImpact();
            positionBowlingWallnutActor(actor, wallnut);
        }

        Iterator<Map.Entry<BowlingWallnut, BowlingWallnutActor>> iterator =
                bowlingWallnutActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BowlingWallnut, BowlingWallnutActor> entry =
                    iterator.next();
            if (!present.containsKey(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }
    }

    private void positionBowlingWallnutActor(BowlingWallnutActor actor,
            BowlingWallnut wallnut) {
        BoardLayout layout = currentBoardLayout();
        if (actor == null || wallnut == null || layout == null
                || Gdx.graphics.getWidth() <= 0
                || Gdx.graphics.getHeight() <= 0) {
            return;
        }

        float windowWidth = Gdx.graphics.getWidth();
        float windowHeight = Gdx.graphics.getHeight();
        float boardX = windowWidth * layout.left / layout.sourceWidth;
        float boardY = windowHeight
                * (layout.sourceHeight - layout.bottom) / layout.sourceHeight;
        float boardWidth = windowWidth
                * (layout.right - layout.left) / layout.sourceWidth;
        float boardHeight = windowHeight
                * (layout.bottom - layout.top) / layout.sourceHeight;
        float cellWidth = boardWidth / BOARD_COLUMNS;
        float cellHeight = boardHeight / BOARD_ROWS;

        float centerX = boardX
                + (float) (wallnut.getColumnPosition() + 0.5) * cellWidth;
        float centerY = boardY
                + (float) (BOARD_ROWS - wallnut.getRowPosition() - 0.5)
                        * cellHeight;
        float size = Math.min(cellWidth, cellHeight) * 0.82f;
        actor.setBounds(centerX - size * 0.5f, centerY - size * 0.5f,
                size, size);
        actor.setOrigin(size * 0.5f, size * 0.5f);
        actor.setVisible(centerX > boardX - cellWidth
                && centerX < boardX + boardWidth + cellWidth
                && centerY > boardY - cellHeight
                && centerY < boardY + boardHeight + cellHeight);
    }

    private void installProjectileRendering() {
        if (!isModelBackedGame() || projectileLayer != null) {
            return;
        }
        projectileLayer = new Group();
        projectileLayer.setTouchable(Touchable.disabled);
        addBackgroundOverlay(projectileLayer);
        refreshProjectileRendering();
    }

    private void refreshProjectileRendering() {
        Game game = activeGame();
        if (game == null || projectileLayer == null) {
            return;
        }

        List<Projectile> currentProjectiles = game.getBoard().getProjectiles();
        IdentityHashMap<Projectile, Boolean> present = new IdentityHashMap<>();
        for (Projectile projectile : currentProjectiles) {
            if (projectile == null || projectile.isRemoved()) {
                continue;
            }
            present.put(projectile, Boolean.TRUE);
            ProjectileActor actor = projectileActors.get(projectile);
            boolean firePeaVisual = projectile.hasFireEffect()
                    && ProjectileVisualCatalog.isPeaFamilyProjectile(projectile);
            if (actor == null || actor.hasFirePeaVisual() != firePeaVisual) {
                if (actor != null) {
                    actor.remove();
                }
                actor = createProjectileActor(projectile);
                if (actor == null) {
                    continue;
                }
                projectileActors.put(projectile, actor);
                projectileLayer.addActor(actor);
            }
            positionProjectileActor(actor, projectile);
        }

        Iterator<Map.Entry<Projectile, ProjectileActor>> iterator =
                projectileActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Projectile, ProjectileActor> entry = iterator.next();
            if (!present.containsKey(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }

        refreshBouncingGrapeRendering(game);
    }

    private ProjectileActor createProjectileActor(Projectile projectile) {
        ProjectileVisualCatalog.Preview preview =
                ProjectileVisualCatalog.find(projectile);
        if (preview == null) {
            return null;
        }
        try {
            return new ProjectileActor(preview,
                    projectile.hasFireEffect()
                            && ProjectileVisualCatalog.isPeaFamilyProjectile(
                                    projectile));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void positionProjectileActor(ProjectileActor actor,
            Projectile projectile) {
        BoardLayout layout = currentBoardLayout();
        if (actor == null || projectile == null || layout == null
                || Gdx.graphics.getWidth() <= 0
                || Gdx.graphics.getHeight() <= 0) {
            return;
        }
        float windowWidth = Gdx.graphics.getWidth();
        float windowHeight = Gdx.graphics.getHeight();
        float boardX = windowWidth * layout.left / layout.sourceWidth;
        float boardY = windowHeight
                * (layout.sourceHeight - layout.bottom) / layout.sourceHeight;
        float boardWidth = windowWidth
                * (layout.right - layout.left) / layout.sourceWidth;
        float boardHeight = windowHeight
                * (layout.bottom - layout.top) / layout.sourceHeight;
        float cellWidth = boardWidth / BOARD_COLUMNS;
        float cellHeight = boardHeight / BOARD_ROWS;

        float centerX = boardX
                + (float) (projectile.getColumnPosition() + 0.5) * cellWidth;
        float centerY = boardY
                + (float) (BOARD_ROWS - projectile.getRowPosition() - 0.5)
                        * cellHeight;
        if (projectile instanceof LobbedProjectile) {
            // The Phase-1 lobber already calculates a true parabolic altitude.
            // Convert that tile-space height into pixels for the Phase-2 view.
            centerY += (float) ((LobbedProjectile) projectile).getAltitude()
                    * cellHeight;
        }

        float size = Math.min(cellWidth, cellHeight)
                * actor.getProjectileSizeTiles();
        actor.setBounds(centerX - size * 0.5f, centerY - size * 0.5f,
                size, size);
        actor.layoutAnimations();
        actor.setVisible(centerX > boardX - cellWidth
                && centerX < boardX + boardWidth + cellWidth
                && centerY > boardY - cellHeight * 2f
                && centerY < boardY + boardHeight + cellHeight * 2f);
    }

    private void refreshBouncingGrapeRendering(Game game) {
        List<BouncingGrape> grapes = game.getBoard().getBouncingGrapes();
        IdentityHashMap<BouncingGrape, Boolean> present = new IdentityHashMap<>();
        ProjectileVisualCatalog.Preview preview = ProjectileVisualCatalog.grape();
        for (BouncingGrape grape : grapes) {
            if (grape == null || grape.isRemoved()) {
                continue;
            }
            present.put(grape, Boolean.TRUE);
            PamAnimationActor actor = grapeActors.get(grape);
            if (actor == null) {
                try {
                    actor = new PamAnimationActor(navigator.getPamPlayer(),
                            preview.getPath(), preview.getClip());
                } catch (RuntimeException ignored) {
                    continue;
                }
                actor.setTouchable(Touchable.disabled);
                grapeActors.put(grape, actor);
                projectileLayer.addActor(actor);
            }
            positionBouncingGrapeActor(actor, grape, preview.getSizeTiles());
        }

        Iterator<Map.Entry<BouncingGrape, PamAnimationActor>> iterator =
                grapeActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BouncingGrape, PamAnimationActor> entry = iterator.next();
            if (!present.containsKey(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }
    }

    private void positionBouncingGrapeActor(Actor actor, BouncingGrape grape,
            float sizeTiles) {
        BoardLayout layout = currentBoardLayout();
        if (actor == null || grape == null || layout == null) {
            return;
        }
        float windowWidth = Gdx.graphics.getWidth();
        float windowHeight = Gdx.graphics.getHeight();
        float boardX = windowWidth * layout.left / layout.sourceWidth;
        float boardY = windowHeight
                * (layout.sourceHeight - layout.bottom) / layout.sourceHeight;
        float boardWidth = windowWidth
                * (layout.right - layout.left) / layout.sourceWidth;
        float boardHeight = windowHeight
                * (layout.bottom - layout.top) / layout.sourceHeight;
        float cellWidth = boardWidth / BOARD_COLUMNS;
        float cellHeight = boardHeight / BOARD_ROWS;
        float centerX = boardX
                + (float) (grape.getColumnPosition() + 0.5) * cellWidth;
        float centerY = boardY
                + (float) (BOARD_ROWS - grape.getRowPosition() - 0.5)
                        * cellHeight;
        float size = Math.min(cellWidth, cellHeight) * sizeTiles;
        actor.setBounds(centerX - size * 0.5f, centerY - size * 0.5f,
                size, size);
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

    private static String packetArtworkAssetFor(String plantName) {
        if (plantName != null) {
            if (plantName.equalsIgnoreCase("Bowling Wall-nut")) {
                return "IMAGE_UI_PACKETS_WALLNUT";
            }
            if (plantName.equalsIgnoreCase("Explode-o-nut")) {
                return "IMAGE_UI_PACKETS_EXPLODEONUT";
            }
            if (plantName.equalsIgnoreCase("Giant Wall-nut")) {
                return "IMAGE_UI_PACKETS_PRIMALWALLNUT";
            }
        }
        return PlantPacketCard.packetAssetFor(plantName);
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

        refreshBigWaveBeachTerrainRendering();
        refreshFrostbiteTerrainRendering();
        refreshDarkAgesTerrainRendering();
        refreshStructureRendering();
        refreshZombieRendering();
        refreshFrostbiteZombieIceRendering();
        refreshFrostbiteWindRendering();
        float sceneDelta = gamePaused ? 0f
                : Math.min(delta, 1f / 15f) * currentGameSpeed();
        refreshEgyptSandstorms(sceneDelta);
        refreshBowlingWallnutRendering();
        refreshProjectileRendering();
        refreshSunRendering();
        refreshCollectibleDrops();
        refreshSunHud();
        refreshPlantFoodHud();
        refreshPlantWhatYouGetWaveButton();
        refreshTimedWarObjectivesHud();
        refreshLoveYourPlantsHud();
        refreshVaseBreakerSeedTray();
        refreshConveyorBelt();
        refreshIZombieBoardOverlay();
        refreshBoardHover();
        refreshCursorPlantPosition();
        refreshFallbackShovelCursorPosition();
        refreshPlantedPlantLayerIfNeeded();
        refreshFrostbitePlantIceRendering();
        if (previewLevel == null) {
            currentGameMenu().synchronizeProgress();
            showFinishedGameMenuIfNeeded();
        }
        super.render(delta);
    }

    private String buildGameResultDescription(Game game) {
        if (game == null) {
            return null;
        }
        GameMenu menu = currentGameMenu();
        if (menu != null && menu.isMinigame()) {
            return buildMinigameResultDescription(game);
        }
        if (isBossLevel()) {
            if (game.getStatus()
                    == io.github.Plants_Vs_Zombies_2.model.game.GameStatus.WON) {
                return "Zomboss was defeated and all remaining reinforcements were cleared.";
            }
            return "The Zomboss battle was lost before the boss could be defeated.";
        }
        if (game.getStatus()
                != io.github.Plants_Vs_Zombies_2.model.game.GameStatus.LOST) {
            return null;
        }

        if (game.hasLoveYourPlants()
                && game.getLostPlantCount() > game.getMaximumLostPlants()) {
            return "Love Your Plants failed because "
                    + game.getLostPlantCount()
                    + " plants were lost. You may lose at most "
                    + game.getMaximumLostPlants() + ".";
        }

        if (!game.hasTimedWar()
                || !game.didTimedWarFailAfterWavesCleared()) {
            return null;
        }
        String unmet = game.getTimedWarUnmetRequirements();
        if (unmet == null || unmet.isBlank()) {
            return "Timed War failed before all objectives were completed.";
        }
        return "Timed War failed because " + unmet + ".";
    }

    private String buildMinigameResultDescription(Game game) {
        boolean won = game.getStatus()
                == io.github.Plants_Vs_Zombies_2.model.game.GameStatus.WON;
        if (game instanceof VaseBreaker) {
            return won
                    ? "All vases are broken and every hostile zombie was defeated."
                    : "A released zombie reached the house before you cleared the minigame.";
        }
        if (game instanceof WallnutBowling) {
            return won
                    ? "Every bowling wave was cleared."
                    : "A zombie reached the house before all bowling waves were cleared.";
        }
        if (game instanceof IZombie) {
            IZombie iZombie = (IZombie) game;
            return won
                    ? "All five brains were eaten."
                    : "I, Zombie ended with " + iZombie.getEatenBrainCount()
                            + " of 5 brains eaten. All sun producers are gone, "
                            + "no attacking zombie remains, and the remaining "
                            + iZombie.getSunCount() + " sun cannot buy another "
                            + "zombie.";
        }
        return won ? "Minigame complete!" : "Minigame failed.";
    }

    private void showFinishedGameMenuIfNeeded() {
        Game game = activeGame();
        if (game == null
                || game.getStatus()
                        == io.github.Plants_Vs_Zombies_2.model.game.GameStatus.ACTIVE
                || stage.getRoot().findActor("game-result-overlay") != null) {
            return;
        }

        if (shovelMode) {
            setShovelMode(false);
        }
        gamePaused = true;
        stage.addActor(new GameResultOverlay(
                skin, game.getStatus(), buildGameResultDescription(game),
                VIRTUAL_WIDTH, VIRTUAL_HEIGHT,
                this::restartLevel,
                () -> {
                    gamePaused = false;
                    GameMenu menu = currentGameMenu();
                    if (menu != null && menu.isMinigame()) {
                        navigator.exitMinigameToTravelLog();
                    } else {
                        navigator.exitGameToAdventure();
                    }
                }));
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (isModelBackedGame()) {
            rebuildPlantedPlantLayer();
            bigWaveBeachTerrainSignature = "";
            rebuildBigWaveBeachTerrainRendering();
            rebuildDarkAgesTerrainRendering();
            rebuildFrostbiteTerrainRendering();
            refreshStructureRendering();
            refreshZombieRendering();
            refreshFrostbiteZombieIceRendering();
            refreshFrostbitePlantIceRendering();
            refreshEgyptSandstorms(0f);
            refreshBowlingWallnutRendering();
            refreshProjectileRendering();
            refreshSunRendering();
            refreshCollectibleDrops();
            refreshIZombieBoardOverlay();
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
        if (deadlineLineActor != null) {
            deadlineLineActor.dispose();
            deadlineLineActor = null;
        }
        if (wallnutBowlingLineActor != null) {
            wallnutBowlingLineActor.dispose();
            wallnutBowlingLineActor = null;
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
        if (bossHealthActor != null) {
            bossHealthActor.dispose();
            bossHealthActor = null;
        }
        sunLayer = null;
        plantedPlantActors.clear();
        plantedPlantHealth.clear();
        graveActors.clear();
        graveVisualKeys.clear();
        graveHitPoints.clear();
        vaseActors.clear();
        vaseSeedPacketActors.clear();
        vaseSeedTray = null;
        selectedVaseSeedPacket = null;
        structureLayer = null;
        zombieActors.clear();
        zombieDurability.clear();
        for (EgyptSandstormEffect effect : egyptSandstorms.values()) {
            if (effect != null) {
                effect.remove();
            }
        }
        egyptSandstorms.clear();
        egyptSandstormRearLayer = null;
        egyptSandstormTopLayer = null;
        zombieLayer = null;
        bowlingWallnutActors.clear();
        bowlingWallnutLayer = null;
        projectileActors.clear();
        grapeActors.clear();
        projectileLayer = null;
        queuedGameAnnouncements.clear();
        gameAnnouncementLabel = null;
        if (shovelCursor != null) {
            shovelCursor.dispose();
            shovelCursor = null;
        }
        super.dispose();
    }

    private final class EgyptSandstormEffect {
        private final Zombie zombie;
        private final PamAnimationActor rear;
        private final PamAnimationActor top;
        private final float outroStartSeconds;
        private float elapsedSeconds;
        private boolean outroStarted;

        private EgyptSandstormEffect(Zombie zombie) {
            this.zombie = zombie;
            rear = new PamAnimationActor(navigator.getPamPlayer(),
                    EGYPT_SANDSTORM_REAR_PAM, "loop");
            top = new PamAnimationActor(navigator.getPamPlayer(),
                    EGYPT_SANDSTORM_TOP_PAM, "loop");
            rear.setTouchable(Touchable.disabled);
            top.setTouchable(Touchable.disabled);
            rear.playOnce("intro", "loop");
            top.playOnce("intro", "loop");

            float introSeconds = Math.max(
                    navigator.getPamPlayer().clipDurationSeconds(
                            EGYPT_SANDSTORM_REAR_PAM, "intro"),
                    navigator.getPamPlayer().clipDurationSeconds(
                            EGYPT_SANDSTORM_TOP_PAM, "intro"));
            float loopSeconds = Math.max(
                    navigator.getPamPlayer().clipDurationSeconds(
                            EGYPT_SANDSTORM_REAR_PAM, "loop"),
                    navigator.getPamPlayer().clipDurationSeconds(
                            EGYPT_SANDSTORM_TOP_PAM, "loop"));
            outroStartSeconds = Math.max(0.01f, introSeconds)
                    + Math.max(0.01f, loopSeconds);
        }

        private void update(float deltaSeconds) {
            if (outroStarted) {
                return;
            }
            elapsedSeconds += deltaSeconds;
            if (elapsedSeconds + 0.0001f < outroStartSeconds) {
                return;
            }
            outroStarted = true;
            rear.playOnce("outro", "outro");
            boolean topStarted = top.playOnce("outro", "outro",
                    () -> finishEgyptSandstorm(zombie, this));
            if (!topStarted) {
                finishEgyptSandstorm(zombie, this);
            }
        }

        private void remove() {
            rear.remove();
            top.remove();
        }
    }

    private final class BowlingWallnutActor extends Stack {
        private final BowlingWallnut wallnut;
        private final PamAnimationActor animation;
        private int lastImpactCount;

        private BowlingWallnutActor(BowlingWallnut wallnut) {
            if (wallnut == null) {
                throw new IllegalArgumentException(
                        "bowling Wall-nut cannot be null");
            }
            this.wallnut = wallnut;
            animation = new PamAnimationActor(
                    navigator.getPamPlayer(),
                    bowlingWallnutPamPath(wallnut.getType()), "idle");
            animation.setTouchable(Touchable.disabled);
            add(animation);
            setTouchable(Touchable.disabled);
            lastImpactCount = wallnut.getImpactCount();
        }

        @Override
        public void layout() {
            animation.setBounds(0f, 0f, getWidth(), getHeight());
        }

        private void refreshImpact() {
            int impactCount = wallnut.getImpactCount();
            if (impactCount <= lastImpactCount) {
                return;
            }
            lastImpactCount = impactCount;
            clearActions();
            setScale(1f);
            addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(
                            1.14f, 0.88f, 0.08f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(
                            1f, 1f, 0.12f)));
        }
    }

    private final class ProjectileActor extends Stack {
        private final ProjectileVisualCatalog.Preview preview;
        private final boolean firePeaVisual;
        private final PamAnimationActor animation;
        private final Image staticImage;

        private ProjectileActor(ProjectileVisualCatalog.Preview preview,
                boolean firePeaVisual) {
            this.preview = preview;
            this.firePeaVisual = firePeaVisual;
            setTouchable(Touchable.disabled);

            if (preview.isStaticImage()) {
                animation = null;
                staticImage = new Image(requireAssetRegion(preview.getImageId()));
                staticImage.setScaling(Scaling.fit);
                add(staticImage);
            } else {
                staticImage = null;
                animation = new PamAnimationActor(navigator.getPamPlayer(),
                        preview.getPath(), preview.getClip());
                add(animation);
            }
        }

        private boolean hasFirePeaVisual() {
            return firePeaVisual;
        }

        private float getProjectileSizeTiles() {
            return preview.getSizeTiles();
        }

        private void layoutAnimations() {
            if (animation != null) {
                animation.setBounds(0f, 0f, getWidth(), getHeight());
            }
            if (staticImage != null) {
                staticImage.setBounds(0f, 0f, getWidth(), getHeight());
            }
        }
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

    private final class VaseSeedPacketActor extends Stack {
        private final VaseSeedPacket packet;
        private final Label timerLabel;
        private SelectionOutlineActor selectionOutline;

        private VaseSeedPacketActor(VaseSeedPacket packet) {
            if (packet == null) {
                throw new IllegalArgumentException(
                        "vase seed packet cannot be null");
            }
            this.packet = packet;
            setSize(VASE_SEED_CARD_WIDTH, VASE_SEED_CARD_HEIGHT);
            setTouchable(Touchable.enabled);

            Image background = createAssetImage(
                    packetAssetForChapter(null));
            background.setScaling(Scaling.stretch);
            add(background);

            Table artworkLayer = new Table();
            Image artwork = createAssetImage(
                    packetArtworkAssetFor(packet.getPlantType()));
            artwork.setScaling(Scaling.fit);
            artworkLayer.add(artwork).width(84f).height(52f);
            add(artworkLayer);

            Table timerLayer = new Table();
            timerLayer.bottom().right();
            timerLabel = new Label("", skin, "medium_outline");
            timerLabel.setFontScale(0.46f);
            timerLayer.add(timerLabel).padRight(4f).padBottom(2f);
            add(timerLayer);

            addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y,
                        int pointer, int button) {
                    if (button != Input.Buttons.LEFT) {
                        return false;
                    }
                    selectVaseSeedPacket(VaseSeedPacketActor.this.packet);
                    event.stop();
                    return true;
                }
            });
            addListener(new TextTooltip(
                    packet.getPlantType()
                            + "\nOne-use Vase Breaker plant",
                    skin));
            refreshTimer();
        }

        private void refreshTimer() {
            int seconds = Math.max(0, (int) Math.ceil(
                    packet.getRemainingSeconds()));
            timerLabel.setText(seconds + "s");
            timerLabel.setColor(seconds <= 3 ? Color.RED : Color.WHITE);
        }

        private void setSelected(boolean selected) {
            if (selected && selectionOutline == null) {
                selectionOutline = new SelectionOutlineActor();
                selectionOutline.setTouchable(Touchable.disabled);
                add(selectionOutline);
            } else if (!selected && selectionOutline != null) {
                selectionOutline.remove();
                selectionOutline = null;
            }
        }
    }

    private final class IZombieCardActor extends Stack {
        private final IZombieCard card;
        private final Label cooldownLabel;
        private final Label costLabel;
        private final IZombieCooldownShadeActor cooldownShade;

        private IZombieCardActor(IZombieCard card) {
            if (card == null) {
                throw new IllegalArgumentException(
                        "I, Zombie card cannot be null");
            }
            this.card = card;
            setSize(I_ZOMBIE_CARD_WIDTH, I_ZOMBIE_CARD_HEIGHT);
            setTouchable(Touchable.enabled);

            TextButton frame = new TextButton("", skin,
                    card == selectedIZombieCard ? "purple" : "green");
            frame.setTouchable(Touchable.disabled);
            add(frame);

            Table content = new Table();
            content.top();

            ZombieVisualCatalog.Visual visual =
                    ZombieVisualCatalog.find(card.getType());
            Image portrait = createAssetImage(visual == null
                    ? "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_GUIDE"
                    : visual.getPacketAsset());
            portrait.setScaling(Scaling.fit);
            content.add(portrait).width(92f).height(60f)
                    .padTop(2f).row();

            Label name = new Label(
                    card.getType().name().replace('_', ' '),
                    skin, "medium_outline");
            name.setFontScale(0.38f);
            name.setAlignment(Align.center);
            content.add(name).width(102f).height(17f).row();

            Table costRow = new Table();
            Image sun = createAssetImage(GAME_SUN_ICON);
            sun.setScaling(Scaling.fit);
            costRow.add(sun).size(17f).padRight(1f);
            costLabel = new Label(Integer.toString(card.getCost()),
                    skin, "medium_outline");
            costLabel.setFontScale(0.42f);
            costRow.add(costLabel);
            content.add(costRow).height(18f);
            add(content);

            cooldownShade = new IZombieCooldownShadeActor();
            cooldownShade.setTouchable(Touchable.disabled);
            add(cooldownShade);

            Table cooldownLayer = new Table();
            cooldownLabel = new Label("", skin, "medium_outline");
            cooldownLabel.setFontScale(0.48f);
            cooldownLabel.setAlignment(Align.center);
            cooldownLayer.add(cooldownLabel).grow();
            add(cooldownLayer);

            if (card == selectedIZombieCard) {
                SelectionOutlineActor outline = new SelectionOutlineActor();
                outline.setTouchable(Touchable.disabled);
                add(outline);
            }

            addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y,
                        int pointer, int button) {
                    if (button != Input.Buttons.LEFT) {
                        return false;
                    }
                    selectIZombieCard(IZombieCardActor.this.card);
                    event.stop();
                    return true;
                }
            });
            addListener(new TextTooltip(
                    card.getType().getAlias() + "\nCost: "
                            + card.getCost() + " sun\nRecharge: "
                            + String.format(java.util.Locale.ROOT, "%.1fs",
                                    card.getRechargeSeconds()),
                    skin));
            refreshState();
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            refreshState();
        }

        private void refreshState() {
            if (!(activeGame() instanceof IZombie)) {
                return;
            }
            IZombie game = (IZombie) activeGame();
            double remaining = game.getCardCooldownRemainingSeconds(card);
            float fraction = card.getRechargeSeconds() <= 0.0
                    ? 0f
                    : Math.max(0f, Math.min(1f,
                            (float) (remaining / card.getRechargeSeconds())));
            cooldownShade.setFraction(fraction);
            if (remaining > 0.001) {
                cooldownLabel.setText(String.format(
                        java.util.Locale.ROOT, "%.1fs", remaining));
                cooldownLabel.setVisible(true);
            } else {
                cooldownLabel.setText("");
                cooldownLabel.setVisible(false);
            }
            costLabel.setColor(game.getSunCount() >= card.getCost()
                    ? Color.WHITE : Color.RED);
        }
    }

    private final class IZombieCooldownShadeActor extends Actor {
        private float fraction;

        private void setFraction(float fraction) {
            this.fraction = Math.max(0f, Math.min(1f, fraction));
            setVisible(this.fraction > 0.001f);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (plantingOverlayPixel == null || fraction <= 0f) {
                return;
            }
            Color previous = new Color(batch.getColor());
            batch.setColor(0f, 0f, 0f, 0.62f * parentAlpha);
            float shadeHeight = getHeight() * fraction;
            batch.draw(plantingOverlayPixel,
                    getX(), getY(), getWidth(), shadeHeight);
            batch.setColor(previous);
        }
    }


    private Actor createConveyorPacketArtwork(ConveyorPlantPacket packet) {
        Image artwork = createAssetImage(
                packetArtworkAssetFor(packet == null
                        ? null : packet.getPlantType()));
        artwork.setScaling(Scaling.fit);
        return artwork;
    }

    private final class ConveyorPacketActor extends Stack {
        private final ConveyorPlantPacket packet;
        private SelectionOutlineActor selectionOutline;
        private float targetY;

        private ConveyorPacketActor(ConveyorPlantPacket packet) {
            if (packet == null) {
                throw new IllegalArgumentException(
                        "conveyor packet cannot be null");
            }
            this.packet = packet;
            setSize(CONVEYOR_CARD_WIDTH, CONVEYOR_CARD_HEIGHT);
            setTouchable(Touchable.enabled);

            Image background = createAssetImage(
                    packetAssetForChapter(seedTrayChapter()));
            background.setScaling(Scaling.stretch);
            add(background);

            Table artworkLayer = new Table();
            Actor artwork = createConveyorPacketArtwork(packet);
            artwork.setTouchable(Touchable.disabled);
            artworkLayer.add(artwork).width(84f).height(54f);
            add(artworkLayer);

            addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y,
                        int pointer, int button) {
                    if (button != Input.Buttons.LEFT) {
                        return false;
                    }
                    // Select on press instead of waiting for ClickListener's
                    // release-time hit test. The packet can move several
                    // pixels between touchDown and touchUp while travelling
                    // up the belt, which would otherwise cancel the click.
                    selectConveyorPacket(ConveyorPacketActor.this.packet);
                    event.stop();
                    return true;
                }
            });
            addListener(new TextTooltip(
                    packet.getPlantType() + "\nConveyor Belt", skin));
        }

        private void setTargetY(float targetY) {
            this.targetY = targetY;
        }

        private void setSelected(boolean selected) {
            if (selected && selectionOutline == null) {
                selectionOutline = new SelectionOutlineActor();
                selectionOutline.setTouchable(Touchable.disabled);
                add(selectionOutline);
            } else if (!selected && selectionOutline != null) {
                selectionOutline.remove();
                selectionOutline = null;
            }
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            float difference = targetY - getY();
            if (Math.abs(difference) <= 0.5f) {
                setY(targetY);
                return;
            }
            float travel = CONVEYOR_CARD_TRAVEL_SPEED
                    * Math.max(0f, delta);
            setY(getY() + Math.signum(difference)
                    * Math.min(Math.abs(difference), travel));
        }
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

    private final class FrostbiteWindLaneActor extends Group {
        private final int lane;
        private float remainingSeconds = FROSTBITE_WIND_DURATION_SECONDS;

        private FrostbiteWindLaneActor(int lane) {
            this.lane = lane;
            float[] centers = { 0.17f, 0.50f, 0.83f };
            for (float center : centers) {
                PamAnimationActor wind = new PamAnimationActor(
                        navigator.getPamPlayer(), FROSTBITE_WIND_PAM,
                        "animation");
                wind.setTouchable(Touchable.disabled);
                wind.setColor(1f, 1f, 1f, 0.90f);
                wind.setUserObject(Float.valueOf(center));
                addActor(wind);
            }
            setTouchable(Touchable.disabled);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            remainingSeconds -= Math.max(0f, delta);
            if (remainingSeconds <= 0f) {
                remove();
                return;
            }
            positionFrostbiteWindLaneActor(this, lane);
            layoutWindSegments();
        }

        @Override
        protected void sizeChanged() {
            super.sizeChanged();
            layoutWindSegments();
        }

        private void layoutWindSegments() {
            float segmentWidth = getWidth() * 0.38f;
            for (Actor child : getChildren()) {
                Object marker = child.getUserObject();
                float center = marker instanceof Float
                        ? ((Float) marker).floatValue() : 0.5f;
                child.setBounds(getWidth() * center - segmentWidth * 0.5f,
                        0f, segmentWidth, getHeight());
            }
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
    private final class BossHealthActor extends Actor {
        private static final float BAR_LEFT = 22f;
        private static final float BAR_RIGHT_MARGIN = 22f;
        private static final float BAR_Y = 19f;
        private static final float BAR_HEIGHT = 22f;
        private static final float FRAME = 5f;
        private static final float DIVIDER = 5f;
        private final Game game;
        private final Texture pixel;

        private BossHealthActor(Game game) {
            this.game = game;
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixel = new Texture(pixmap);
            pixmap.dispose();
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Zombie boss = findBoss();
            float ratio = boss == null ? 1f : Math.max(0f,
                    Math.min(1f, boss.getHitPoints()
                            / (float) Math.max(1, boss.getMaximumHitPoints())));
            float x = getX() + BAR_LEFT;
            float y = getY() + BAR_Y;
            float width = getWidth() - BAR_LEFT - BAR_RIGHT_MARGIN;
            Color previous = new Color(batch.getColor());

            batch.setColor(0.10f, 0.06f, 0.04f, parentAlpha);
            batch.draw(pixel, x - FRAME, y - FRAME,
                    width + FRAME * 2f, BAR_HEIGHT + FRAME * 2f);
            batch.setColor(0.20f, 0.08f, 0.06f, parentAlpha);
            batch.draw(pixel, x, y, width, BAR_HEIGHT);
            if (ratio > 0f) {
                batch.setColor(0.86f, 0.12f, 0.08f, parentAlpha);
                batch.draw(pixel, x, y, width * ratio, BAR_HEIGHT);
                batch.setColor(1f, 0.34f, 0.22f, 0.78f * parentAlpha);
                batch.draw(pixel, x, y + BAR_HEIGHT * 0.58f,
                        width * ratio, BAR_HEIGHT * 0.22f);
            }
            batch.setColor(0.08f, 0.05f, 0.03f, parentAlpha);
            for (int section = 1; section <= 2; section++) {
                float dividerX = x + width * section / 3f - DIVIDER * 0.5f;
                batch.draw(pixel, dividerX, y - 1f,
                        DIVIDER, BAR_HEIGHT + 2f);
            }
            batch.setColor(previous);
        }

        private Zombie findBoss() {
            if (game == null) {
                return null;
            }
            for (Zombie zombie : game.getBoard().getZombies()) {
                if (zombie.getType().isBoss() && !zombie.isRemoved()) {
                    return zombie;
                }
            }
            return null;
        }

        private void dispose() {
            pixel.dispose();
        }
    }

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
     * Permanent visual marker for Dead Line special levels. The model stores
     * the losing threshold as a zombie column position, whose rendered center
     * is column + 0.5 cells; draw the line at that exact screen position.
     */
    private static final class DeadlineLineActor extends Actor {
        private static final float OUTLINE_THICKNESS = 14f;
        private static final float CORE_THICKNESS = 9f;
        private static final float HIGHLIGHT_THICKNESS = 3f;

        private final BoardLayout layout;
        private final double lineColumn;
        private final Texture pixel;

        private DeadlineLineActor(BoardLayout layout, double lineColumn) {
            this.layout = layout;
            this.lineColumn = lineColumn;
            setTouchable(Touchable.disabled);

            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixel = new Texture(pixmap);
            pixmap.dispose();
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

            double clampedColumn = Math.max(-0.5,
                    Math.min(BOARD_COLUMNS - 0.5, lineColumn));
            float x = boardX + boardWidth
                    * (float) ((clampedColumn + 0.5) / BOARD_COLUMNS);

            // Scale the stroke with the framebuffer so it remains visibly
            // bolder than the normal 2px grid even on high-DPI windows.
            float scale = Math.max(0.85f,
                    Math.min(2.5f, worldWidth / VIRTUAL_WIDTH));
            float outline = OUTLINE_THICKNESS * scale;
            float core = CORE_THICKNESS * scale;
            float highlight = HIGHLIGHT_THICKNESS * scale;

            Color previous = new Color(batch.getColor());
            batch.setColor(0.32f, 0.01f, 0.01f, 0.98f * parentAlpha);
            batch.draw(pixel, x - outline * 0.5f, boardY,
                    outline, boardHeight);

            batch.setColor(1f, 0.02f, 0.02f, parentAlpha);
            batch.draw(pixel, x - core * 0.5f, boardY,
                    core, boardHeight);

            batch.setColor(1f, 0.42f, 0.18f, 0.92f * parentAlpha);
            batch.draw(pixel, x - highlight * 0.5f, boardY,
                    highlight, boardHeight);
            batch.setColor(previous);
        }

        private void dispose() {
            pixel.dispose();
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
