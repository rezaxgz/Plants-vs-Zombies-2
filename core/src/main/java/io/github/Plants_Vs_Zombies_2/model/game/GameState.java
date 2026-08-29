package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.defense.LawnMowerSystem;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFamily;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.modifier.Modifier;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.gameTypes.GameType;
import io.github.Plants_Vs_Zombies_2.model.game.special.ConveyorBeltSystem;
import io.github.Plants_Vs_Zombies_2.model.game.special.DeadLineSystem;
import io.github.Plants_Vs_Zombies_2.model.game.special.LockedPlantsSystem;
import io.github.Plants_Vs_Zombies_2.model.game.special.LoveYourPlantsSystem;
import io.github.Plants_Vs_Zombies_2.model.game.special.PlantWhatYouGetSystem;
import io.github.Plants_Vs_Zombies_2.model.game.special.ProtectedPlantStatus;
import io.github.Plants_Vs_Zombies_2.model.game.special.SaveOurSeedsSystem;
import io.github.Plants_Vs_Zombies_2.model.game.special.TimedWarSystem;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestRunTracker;

abstract class GameState implements java.io.Serializable {
    static final double TIME_EPSILON = 0.000001;
    static final double TORNADO_SPAWN_CHANCE = 0.50;
    static final double ICY_WIND_LANE_CHANCE = 0.50;
    static final int MAX_TORNADO_ADVANCE_COLUMNS = 4;
    static final int DARK_AGES_GRAVES_PER_WAVE = 2;
    static final double DARK_AGES_SUN_GRAVE_CHANCE = 0.20;
    static final double DARK_AGES_PLANT_FOOD_GRAVE_CHANCE = 0.20;
    static final int MAX_PLANT_FOOD = 3;

    final Board board;
    final GameType gameType;
    final ChapterRuleset chapterRuleset;
    final LawnMowerSystem lawnMowerSystem;
    ConveyorBeltSystem conveyorBeltSystem;
    LockedPlantsSystem lockedPlantsSystem;
    SaveOurSeedsSystem saveOurSeedsSystem;
    TimedWarSystem timedWarSystem;
    boolean timedWarCompletionReported;
    boolean timedWarFailedAfterWavesCleared;
    DeadLineSystem deadLineSystem;
    LoveYourPlantsSystem loveYourPlantsSystem;
    PlantWhatYouGetSystem plantWhatYouGetSystem;
    boolean skySunsDisabled;
    String skySunDisabledReason = "";
    boolean zombieWavesStarted;
    transient boolean guiWaveAdvanceHeld;
    double lastFrostbiteIcyWindAtSeconds = -1.0;
    final List<Integer> lastFrostbiteIcyWindLanes = new ArrayList<>();
    final List<ZombieWave> zombieWaves;
    final List<List<Zombie>> spawnedZombiesByWave;
    // Primary wave zombies are deployed gradually instead of all being added
    // to the board on the wave-start frame. These arrays are deliberately not
    // final so older serialized saves (where the fields deserialize as null)
    // can lazily rebuild the scheduling state without invalidating the save.
    int[] primaryWaveSpawnCounts;
    double[] nextPrimaryWaveSpawnAtSeconds;
    final List<String> pendingResults = new ArrayList<>();
    final Map<String, Double> plantCooldowns = new HashMap<>();
    final Map<String, PlantFamily> plantCooldownFamilies = new HashMap<>();
    final Map<String, Integer> plantLoadoutLevels = new LinkedHashMap<>();
    final Map<String, String> plantLoadoutNames = new LinkedHashMap<>();
    final Set<String> boostedPlantTypes = new LinkedHashSet<>();
    final Set<String> greenhouseBoostTypes = new LinkedHashSet<>();
    final List<String> consumedGreenhouseBoosts = new ArrayList<>();
    boolean plantLoadoutConfigured;
    final Random random;
    final DifficultyRules difficultyRules;
    final QuestRunTracker questRunTracker = new QuestRunTracker();

    int sunCount;
    int plantFoodCount;
    int zombieWaveNumber;
    int nextWaveIndex;
    double elapsedSeconds;
    double nextSkySunDropAtSeconds;
    GameStatus status = GameStatus.ACTIVE;

    protected GameState(Board board, GameType gameType,
            int initialSunCount,
            List<ZombieWave> zombieWaves,
            Random random,
            boolean startWavesImmediately,
            ChapterRuleset chapterRuleset,
            int difficultyLevel) {
        if (board == null) {
            throw new IllegalArgumentException("board cannot be null");
        }
        if (initialSunCount < 0) {
            throw new IllegalArgumentException("initialSunCount cannot be negative");
        }
        if (random == null) {
            throw new IllegalArgumentException("random cannot be null");
        }
        if (chapterRuleset == null) {
            throw new IllegalArgumentException(
                    "chapterRuleset cannot be null");
        }

        this.board = board;
        for (BasePlant existingPlant : board.getPlants()) {
            questRunTracker.recordPlantPlaced(existingPlant);
        }
        this.gameType = gameType;
        this.chapterRuleset = chapterRuleset;
        this.difficultyRules = DifficultyRules.forLevel(difficultyLevel);
        if (chapterRuleset == ChapterRuleset.FROSTBITE_CAVES) {
            board.enableFrostbiteCavesRules();
        } else if (chapterRuleset == ChapterRuleset.DARK_AGES) {
            disableSkySuns("Dark Ages night");
            pendingResults.add("Dark Ages is at night: no sun will fall "
                    + "from the sky; use sun-producing plants.");
        }
        this.lawnMowerSystem = new LawnMowerSystem(board.getNumberOfRows());
        this.sunCount = initialSunCount;
        this.zombieWaves = zombieWaves == null
                ? new ArrayList<>()
                : new ArrayList<>(zombieWaves);
        validateBossWaves(this.zombieWaves);
        this.spawnedZombiesByWave = createWaveTracking(this.zombieWaves.size());
        this.primaryWaveSpawnCounts = new int[this.zombieWaves.size()];
        this.nextPrimaryWaveSpawnAtSeconds = new double[this.zombieWaves.size()];
        java.util.Arrays.fill(this.nextPrimaryWaveSpawnAtSeconds, -1.0);
        this.random = random;
        for (Zombie zombie : board.getZombies()) {
            applyDifficultyToZombie(zombie);
        }
        this.zombieWavesStarted = startWavesImmediately;
        this.nextSkySunDropAtSeconds = getAdjustedSkySunDropIntervalSeconds(0.0);
    }

    static void validateBossWaves(List<ZombieWave> waves) {
        int bossCount = 0;
        int nonBossCount = 0;
        for (ZombieWave wave : waves) {
            for (ZombieType type : wave.getZombieTypes()) {
                if (type.isBoss()) {
                    bossCount++;
                } else {
                    nonBossCount++;
                }
            }
        }
        if (bossCount == 0) {
            return;
        }
        if (bossCount != 1 || nonBossCount != 0 || waves.size() != 1) {
            throw new IllegalArgumentException(
                    "a Zomboss level must contain exactly one boss-only wave");
        }
    }

    static List<List<Zombie>> createWaveTracking(int waveCount) {
        List<List<Zombie>> tracking = new ArrayList<>();
        for (int i = 0; i < waveCount; i++) {
            tracking.add(new ArrayList<>());
        }
        return tracking;
    }

    static String getCooldownKey(BasePlant plant) {
        if (plant instanceof Modifier && ((Modifier) plant).isImitater()) {
            return "imitater";
        }
        return plant.getName().trim().toLowerCase(Locale.ROOT);
    }

    public static double getSkySunDropIntervalSeconds(double timePassedSeconds) {
        if (!Double.isFinite(timePassedSeconds) || timePassedSeconds < 0.0) {
            throw new IllegalArgumentException("timePassedSeconds must be finite and non-negative");
        }
        return Math.max(9.0 + 0.075 * timePassedSeconds, 18.0);
    }

    static boolean isWithinArea(EntityPosition center, int row,
            int column, int radius) {
        return Math.abs(center.getRow() - row) <= radius
                && Math.abs(center.getColumn() - column) <= radius;
    }

    static String requestedPlantKey(String requestedType) {
        String normalized = normalizePlantName(requestedType);
        if (normalized.startsWith("imitater")) {
            return "imitater";
        }
        return normalized;
    }

    static String getLoadoutKey(BasePlant plant) {
        if (plant instanceof Modifier
                && ((Modifier) plant).isImitater()) {
            return "imitater";
        }
        return normalizePlantName(plant == null ? null : plant.getName());
    }

    static String normalizePlantName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    static void validateDeltaSeconds(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
    }

    abstract void activateAutomaticZombieAbilities(List<Zombie> zombies);

    abstract void returnStolenSunFromDeadZombies(List<Zombie> zombies);

    abstract void returnCrystalSkullSunFromDeadZombies(
            List<Zombie> zombies);

    abstract void restoreWizardSheepFromDeadZombies(
            List<Zombie> zombies);

    abstract void trackBoardSpawnedZombies();

    abstract void updatePlantCooldowns(float deltaSeconds);

    abstract void applyPlantCooldownResetRequests();

    abstract void updateSkySuns();

    abstract void startNextWaveIfPossible();

    abstract void updatePendingWaveSpawns();

    abstract boolean hasZombieReachedHouse();

    abstract void checkForWin();

    abstract void loseGame();

    abstract void loseSaveOurSeeds(
            ProtectedPlantStatus failedPlant);

    abstract void dropSkySun();

    abstract void reportSunLandings();

    abstract void processZombieDeathDrops(List<Zombie> zombies);

    abstract double getAdjustedSkySunDropIntervalSeconds(
            double timePassedSeconds);

    public abstract void addSun(int amount);

    abstract void applyDifficultyToZombie(Zombie zombie);

    public abstract boolean hasConveyorBelt();

    abstract ProtectedPlantStatus getFailedProtectedPlant();

    public abstract void disableSkySuns(String reason);

    public abstract boolean spendSun(int amount);

    protected abstract void onZombieSpawned(Zombie zombie);

    protected abstract void onZombieDeaths(List<Zombie> zombies);

    protected abstract boolean shouldProcessZombieDeathDrops();

    protected abstract boolean usesLawnMowers();
}
