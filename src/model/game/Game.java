package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import model.Constants;
import model.game.defense.LawnMower;
import model.game.defense.LawnMowerResolution;
import model.game.defense.LawnMowerSystem;
import model.game.entities.EntityPosition;
import model.game.entities.other.Coin;
import model.game.entities.other.CollectibleDrop;
import model.game.entities.other.Diamond;
import model.game.entities.other.PlantFoodDrop;
import model.game.entities.other.PotDrop;
import model.game.entities.other.Sun;
import model.game.entities.other.SunType;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantFactory;
import model.game.entities.plants.PlantFamily;
import model.game.entities.plants.PlantTag;
import model.game.entities.plants.modifier.Modifier;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.ZombieType;
import model.game.entities.zombies.abilities.FishingHookAbility;
import model.game.entities.zombies.abilities.ImpThrowAbility;
import model.game.entities.zombies.abilities.KingBuffAbility;
import model.game.entities.zombies.abilities.LaserBeamAbility;
import model.game.entities.zombies.abilities.OctopusThrowAbility;
import model.game.entities.zombies.abilities.SnowballThrowAbility;
import model.game.entities.zombies.abilities.SunStealAbility;
import model.game.entities.zombies.abilities.TombSummonAbility;
import model.game.entities.zombies.abilities.WeaselReleaseAbility;
import model.game.entities.zombies.abilities.WizardSpellAbility;
import model.game.entities.zombies.abilities.ZombossAbility;
import model.game.entities.zombies.abilities.ZombieAbility;
import model.game.gameTypes.GameType;
import model.game.structure.Grave;
import model.game.structure.GraveReward;
import model.game.tile.TileType;
import model.game.special.ConveyorBeltSystem;
import model.game.special.ConveyorPlacementResult;
import model.game.special.ConveyorPlantPacket;
import model.game.special.DeadLineSystem;
import model.game.special.LockedPlantsMode;
import model.game.special.LockedPlantsSystem;
import model.game.special.LoveYourPlantsSystem;
import model.game.special.PlantWhatYouGetSystem;
import model.game.special.ProtectedPlantSpec;
import model.game.special.ProtectedPlantStatus;
import model.game.special.SaveOurSeedsSystem;
import model.game.special.TimedWarObjective;
import model.game.special.TimedWarState;
import model.game.special.TimedWarSystem;
import model.user.User;

public class Game {
    private static final double TIME_EPSILON = 0.000001;
    private static final double TORNADO_SPAWN_CHANCE = 0.50;
    private static final double ICY_WIND_LANE_CHANCE = 0.50;
    private static final int MAX_TORNADO_ADVANCE_COLUMNS = 4;
    private static final int DARK_AGES_GRAVES_PER_WAVE = 2;
    private static final double DARK_AGES_SUN_GRAVE_CHANCE = 0.20;
    private static final double DARK_AGES_PLANT_FOOD_GRAVE_CHANCE = 0.20;
    private static final int MAX_PLANT_FOOD = 3;

    private final Board board;
    private final GameType gameType;
    private final ChapterRuleset chapterRuleset;
    private final LawnMowerSystem lawnMowerSystem;
    private ConveyorBeltSystem conveyorBeltSystem;
    private LockedPlantsSystem lockedPlantsSystem;
    private SaveOurSeedsSystem saveOurSeedsSystem;
    private TimedWarSystem timedWarSystem;
    private DeadLineSystem deadLineSystem;
    private LoveYourPlantsSystem loveYourPlantsSystem;
    private PlantWhatYouGetSystem plantWhatYouGetSystem;
    private boolean skySunsDisabled;
    private String skySunDisabledReason = "";
    private boolean zombieWavesStarted;
    private final List<ZombieWave> zombieWaves;
    private final List<List<Zombie>> spawnedZombiesByWave;
    private final List<String> pendingResults = new ArrayList<>();
    private final Map<String, Double> plantCooldowns = new HashMap<>();
    private final Map<String, PlantFamily> plantCooldownFamilies = new HashMap<>();
    private final Map<String, Integer> plantLoadoutLevels = new LinkedHashMap<>();
    private final Map<String, String> plantLoadoutNames = new LinkedHashMap<>();
    private final Set<String> boostedPlantTypes = new LinkedHashSet<>();
    private final Set<String> greenhouseBoostTypes = new LinkedHashSet<>();
    private final List<String> consumedGreenhouseBoosts = new ArrayList<>();
    private boolean plantLoadoutConfigured;
    private final Random random;
    private final DifficultyRules difficultyRules;

    private int sunCount;
    private int plantFoodCount;
    private int zombieWaveNumber;
    private int nextWaveIndex;
    private double elapsedSeconds;
    private double nextSkySunDropAtSeconds;
    private GameStatus status = GameStatus.ACTIVE;

    public Game() {
        this(new Board(), null, 0, Collections.emptyList());
    }

    public Game(Board board, int initialSunCount) {
        this(board, null, initialSunCount, Collections.emptyList());
    }

    public Game(Board board, GameType gameType,
            int initialSunCount,
            List<ZombieWave> zombieWaves) {
        this(board, gameType, initialSunCount,
                zombieWaves, true);
    }

    public Game(Board board, GameType gameType,
                int initialSunCount,
                List<ZombieWave> zombieWaves,
                boolean startWavesImmediately) {
        this(board, gameType, initialSunCount,
                zombieWaves, new Random(),
                startWavesImmediately, ChapterRuleset.NONE, 3);
    }

    public Game(Board board, GameType gameType,
            int initialSunCount,
            List<ZombieWave> zombieWaves,
            boolean startWavesImmediately,
            ChapterRuleset chapterRuleset) {
        this(board, gameType, initialSunCount,
                zombieWaves, startWavesImmediately,
                chapterRuleset, 3);
    }

    public Game(Board board, GameType gameType,
            int initialSunCount,
            List<ZombieWave> zombieWaves,
            boolean startWavesImmediately,
            ChapterRuleset chapterRuleset,
            int difficultyLevel) {
        this(board, gameType, initialSunCount,
                zombieWaves, new Random(),
                startWavesImmediately, chapterRuleset,
                difficultyLevel);
    }

    Game(Board board, GameType gameType,
            int initialSunCount,
            List<ZombieWave> zombieWaves,
            Random random) {
        this(board, gameType, initialSunCount,
                zombieWaves, random, true,
                ChapterRuleset.NONE, 3);
    }

    Game(Board board, GameType gameType,
            int initialSunCount,
            List<ZombieWave> zombieWaves,
            Random random,
            boolean startWavesImmediately) {
        this(board, gameType, initialSunCount, zombieWaves,
                random, startWavesImmediately,
                ChapterRuleset.NONE, 3);
    }

    Game(Board board, GameType gameType,
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
        this.gameType = gameType;
        this.chapterRuleset = chapterRuleset;
        this.difficultyRules =
                DifficultyRules.forLevel(difficultyLevel);
        if (chapterRuleset == ChapterRuleset.FROSTBITE_CAVES) {
            board.enableFrostbiteCavesRules();
        } else if (chapterRuleset == ChapterRuleset.DARK_AGES) {
            disableSkySuns("Dark Ages night");
            pendingResults.add("Dark Ages is at night: no sun will fall "
                    + "from the sky; use sun-producing plants.");
        }
        this.lawnMowerSystem =
                new LawnMowerSystem(board.getNumberOfRows());
        this.sunCount = initialSunCount;
        this.zombieWaves = zombieWaves == null
                ? new ArrayList<>()
                : new ArrayList<>(zombieWaves);
        validateNoBossWaves(this.zombieWaves);
        this.spawnedZombiesByWave = createWaveTracking(this.zombieWaves.size());
        this.random = random;
        for (Zombie zombie : board.getZombies()) {
            applyDifficultyToZombie(zombie);
        }
        this.zombieWavesStarted =
                startWavesImmediately;
        this.nextSkySunDropAtSeconds =
                getAdjustedSkySunDropIntervalSeconds(0.0);
        startNextWaveIfPossible();
    }

    private static void validateNoBossWaves(
            List<ZombieWave> waves) {
        for (ZombieWave wave : waves) {
            for (ZombieType type : wave.getZombieTypes()) {
                if (type.isBoss()) {
                    throw new IllegalArgumentException(
                            "Zomboss enemies are not enabled in levels yet");
                }
            }
        }
    }

    private static List<List<Zombie>> createWaveTracking(int waveCount) {
        List<List<Zombie>> tracking = new ArrayList<>();
        for (int i = 0; i < waveCount; i++) {
            tracking.add(new ArrayList<>());
        }
        return tracking;
    }

    public final void tick() {
        update(Constants.ONE_TICK_IN_SECONDS);
    }

    public void advanceTicks(int tickCount) {
        if (tickCount < 0) {
            throw new IllegalArgumentException("tickCount cannot be negative");
        }
        for (int i = 0; i < tickCount && status == GameStatus.ACTIVE; i++) {
            tick();
        }
    }

    public void update(float deltaSeconds) {
        validateDeltaSeconds(deltaSeconds);
        if (status != GameStatus.ACTIVE) {
            return;
        }

        deltaSeconds = difficultyRules.scaleGameDelta(deltaSeconds);
        updateConveyorBelt(deltaSeconds);
        updatePlantCooldowns(deltaSeconds);
        prepareLoveYourPlants();
        List<Zombie> zombieSnapshot =
                new ArrayList<>(board.getZombies());
        board.update(deltaSeconds);
        reportSunLandings();
        processZombieDeathDrops(zombieSnapshot);

        if (resolveDeadLineFailure(deltaSeconds)) {
            return;
        }

        if (usesLawnMowers()) {
            LawnMowerResolution mowerResolution =
                    lawnMowerSystem.resolve(board);
            pendingResults.addAll(mowerResolution.getMessages());
            processZombieDeathDrops(
                    mowerResolution.getKilledZombies());
            if (mowerResolution.isBrainEaten()) {
                pendingResults.addAll(board.drainResults());
                elapsedSeconds += deltaSeconds;
                loseGame();
                return;
            }
        }
        trackBoardSpawnedZombies();

        activateAutomaticZombieAbilities(zombieSnapshot);
        returnStolenSunFromDeadZombies(zombieSnapshot);
        returnCrystalSkullSunFromDeadZombies(zombieSnapshot);
        restoreWizardSheepFromDeadZombies(zombieSnapshot);
        applyPlantCooldownResetRequests();
        pendingResults.addAll(board.drainResults());
        elapsedSeconds += deltaSeconds;

        if (resolveSaveOurSeedsFailure()
                || resolveTimedWar(deltaSeconds)
                || resolveLoveYourPlantsFailure()) {
            return;
        }

        if (usesLawnMowers()
                && hasZombieReachedHouse()) {
            loseGame();
            return;
        }

        startNextWaveIfPossible();
        checkForWin();
        if (status == GameStatus.ACTIVE) {
            updateSkySuns();
        }
    }

    private void updateConveyorBelt(
            float deltaSeconds) {
        if (conveyorBeltSystem == null) {
            return;
        }
        conveyorBeltSystem.update(deltaSeconds);
        pendingResults.addAll(
                conveyorBeltSystem.drainMessages());
    }

    private boolean resolveDeadLineFailure(
            float deltaSeconds) {
        if (deadLineSystem == null) {
            return false;
        }
        Zombie breacher = deadLineSystem.findBreacher(
                board.getZombies());
        if (breacher == null) {
            return false;
        }

        pendingResults.addAll(board.drainResults());
        elapsedSeconds += deltaSeconds;
        status = GameStatus.LOST;
        pendingResults.add(
                breacher.getName()
                        + " crossed the Dead Line at column "
                        + String.format(
                                Locale.ROOT, "%.1f",
                                deadLineSystem.getLineColumn())
                        + "; game lost!");
        return true;
    }

    private void prepareLoveYourPlants() {
        if (loveYourPlantsSystem != null) {
            loveYourPlantsSystem.observePlants(board);
        }
    }

    private boolean resolveSaveOurSeedsFailure() {
        ProtectedPlantStatus failedPlant =
                getFailedProtectedPlant();
        if (failedPlant == null) {
            return false;
        }
        loseSaveOurSeeds(failedPlant);
        return true;
    }

    private boolean resolveTimedWar(
            float deltaSeconds) {
        if (timedWarSystem == null) {
            return false;
        }

        TimedWarState timedState =
                timedWarSystem.update(
                        deltaSeconds,
                        getTrackedZombies(),
                        board.getSuns());
        if (timedState == TimedWarState.SUCCEEDED) {
            status = GameStatus.WON;
            pendingResults.add(
                    "Timed War objective completed: "
                            + timedWarSystem.describeProgress()
                            + ".");
            return true;
        }
        if (timedState == TimedWarState.FAILED) {
            status = GameStatus.LOST;
            pendingResults.add(
                    "Timed War timer expired: "
                            + timedWarSystem.describeProgress()
                            + ".");
            return true;
        }
        return false;
    }

    private boolean resolveLoveYourPlantsFailure() {
        if (loveYourPlantsSystem == null) {
            return false;
        }
        loveYourPlantsSystem.updateLosses(board);
        if (!loveYourPlantsSystem.hasFailed()) {
            return false;
        }

        status = GameStatus.LOST;
        pendingResults.add(
                "Love Your Plants failed: "
                        + loveYourPlantsSystem.getLostPlantCount()
                        + " plants were lost; limit was "
                        + loveYourPlantsSystem
                                .getMaximumLostPlants()
                        + ".");
        return true;
    }

    private List<Zombie> getTrackedZombies() {
        List<Zombie> zombies = new ArrayList<>();
        for (List<Zombie> wave :
                spawnedZombiesByWave) {
            zombies.addAll(wave);
        }
        return zombies;
    }

    private void activateAutomaticZombieAbilities(List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            for (ZombieAbility ability : zombie.getAbilities()) {
                activateWeaselRelease(zombie, ability);
            }
            if (zombie.isDead() || zombie.isHypnotized()
                    || zombie.isEncasedInIce()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                activateImpThrow(zombie, ability);
                activateSunSteal(zombie, ability);
                activateTombSummon(zombie, ability);
                activateSnowballThrow(zombie, ability);
                activateFishingHook(zombie, ability);
                activateOctopusThrow(zombie, ability);
                activateWizardSpell(zombie, ability);
                activateKingBuff(zombie, ability);
                activateCrystalSkull(zombie, ability);
                activateZomboss(zombie, ability);
            }
        }
    }

    private void activateZomboss(Zombie zomboss,
            ZombieAbility ability) {
        if (!(ability instanceof ZombossAbility)) {
            return;
        }

        ZombossAbility bossAbility = (ZombossAbility) ability;
        if (!bossAbility.tryUse(zomboss, board)) {
            return;
        }

        for (Zombie spawned : bossAbility.getLastSpawnedZombies()) {
            trackSpawnedZombie(spawned);
        }
        if (bossAbility.didPhaseChangeThisUse()) {
            pendingResults.add(zomboss.getName()
                    + " entered phase "
                    + bossAbility.getCurrentPhase() + ".");
        }
        for (BasePlant plant :
                bossAbility.getLastDestroyedPlants()) {
            pendingResults.add("Plant " + plant.getName()
                    + " at " + plant.getEntityPosition()
                    + " is destroyed.");
        }
        if (bossAbility.didPerformActionThisUse()) {
            pendingResults.add(zomboss.getName() + " "
                    + bossAbility.getLastActionDescription());
        }
    }

    private void activateCrystalSkull(Zombie crystalSkull,
            ZombieAbility ability) {
        if (!(ability instanceof LaserBeamAbility)) {
            return;
        }

        LaserBeamAbility laser = (LaserBeamAbility) ability;
        boolean stateChanged = laser.tryUse(crystalSkull, board);
        int requestedSun = laser.drainPendingSunRequest();
        int stolenSun = Math.min(sunCount, requestedSun);
        if (stolenSun > 0) {
            spendSun(stolenSun);
            laser.recordStolenSun(stolenSun);
            pendingResults.add(crystalSkull.getName() + " stole "
                    + stolenSun + " stored sun while charging.");
        }

        if (stateChanged && laser.didStartChargingThisUse()) {
            pendingResults.add(crystalSkull.getName()
                    + " started charging its skull laser.");
        }
        if (stateChanged && laser.didFireThisUse()) {
            pendingResults.add(crystalSkull.getName()
                    + " fired its laser and destroyed "
                    + laser.getLastDestroyedPlantCount()
                    + " plant(s).");
        }
    }

    private void activateWizardSpell(Zombie wizard,
            ZombieAbility ability) {
        if (!(ability instanceof WizardSpellAbility)
                || !ability.tryUse(wizard, board)) {
            return;
        }
        BasePlant target =
                ((WizardSpellAbility) ability).getLastTarget();
        if (target != null) {
            pendingResults.add(wizard.getName() + " transformed "
                    + target.getName() + " into a cat at "
                    + target.getEntityPosition() + ".");
        }
    }

    private void activateKingBuff(Zombie king,
            ZombieAbility ability) {
        if (!(ability instanceof KingBuffAbility)
                || !ability.tryUse(king, board)) {
            return;
        }
        Zombie target =
                ((KingBuffAbility) ability).getLastKnightedZombie();
        if (target != null) {
            pendingResults.add(king.getName() + " knighted "
                    + target.getName()
                    + " with helmet and shoulder armor.");
        }
    }

    private void activateFishingHook(Zombie fisherman,
            ZombieAbility ability) {
        if (!(ability instanceof FishingHookAbility)
                || !ability.tryUse(fisherman, board)) {
            return;
        }
        FishingHookAbility hook = (FishingHookAbility) ability;
        BasePlant target = hook.getLastTarget();
        if (target == null) {
            return;
        }
        if (hook.wasLastTargetDestroyed()) {
            pendingResults.add(fisherman.getName() + " hooked and destroyed "
                    + target.getName() + " beside the right edge.");
        } else {
            pendingResults.add(fisherman.getName() + " pulled "
                    + target.getName() + " from " + hook.getLastFromPosition()
                    + " to " + hook.getLastToPosition() + ".");
        }
    }

    private void activateOctopusThrow(Zombie octopusZombie,
            ZombieAbility ability) {
        if (!(ability instanceof OctopusThrowAbility)
                || !ability.tryUse(octopusZombie, board)) {
            return;
        }
        BasePlant target =
                ((OctopusThrowAbility) ability).getLastTarget();
        if (target != null) {
            pendingResults.add(octopusZombie.getName()
                    + " covered " + target.getName()
                    + " with an octopus at "
                    + target.getEntityPosition() + ".");
        }
    }

    private void activateSnowballThrow(Zombie hunter, ZombieAbility ability) {
        if (!(ability instanceof SnowballThrowAbility)
                || !ability.tryUse(hunter, board)) {
            return;
        }
        SnowballThrowAbility snowball = (SnowballThrowAbility) ability;
        BasePlant target = snowball.getLastTarget();
        if (target == null) {
            return;
        }
        pendingResults.add(hunter.getName() + " hit " + target.getName()
                + " with " + snowball.getLastSnowballCount()
                + " snowball(s), raising it to freeze level "
                + target.getFreezeLevel() + "/"
                + BasePlant.MAX_FREEZE_LEVEL + "."
                + (snowball.didLastBarrageFreezeTarget()
                        ? " The plant is frozen inside a "
                                + BasePlant.ICE_SHELL_HIT_POINTS
                                + " HP ice shell."
                        : ""));
    }

    private void activateWeaselRelease(Zombie hoarder, ZombieAbility ability) {
        if (!(ability instanceof WeaselReleaseAbility)
                || !ability.tryUse(hoarder, board)) {
            return;
        }
        WeaselReleaseAbility release = (WeaselReleaseAbility) ability;
        for (Zombie weasel : release.getLastSpawnedWeasels()) {
            trackSpawnedZombie(weasel);
        }
        pendingResults.add(hoarder.getName() + " released "
                + release.getLastSpawnedWeasels().size() + " weasel(s).");
    }

    private void activateImpThrow(Zombie gargantuar, ZombieAbility ability) {
        if (!(ability instanceof ImpThrowAbility)
                || !ability.tryUse(gargantuar, board)) {
            return;
        }

        ImpThrowAbility impThrow = (ImpThrowAbility) ability;
        Zombie imp = impThrow.getSpawnedImp();
        if (imp == null) {
            return;
        }

        trackSpawnedZombie(imp);
        pendingResults.add(gargantuar.getName() + " threw "
                + imp.getName() + " into lane " + imp.getLane()
                + " at column "
                + String.format(Locale.ROOT, "%.0f", imp.getColumnPosition())
                + ".");
    }

    private void activateSunSteal(Zombie raZombie, ZombieAbility ability) {
        if (!(ability instanceof SunStealAbility)
                || !ability.tryUse(raZombie, board)) {
            return;
        }
        SunStealAbility sunSteal = (SunStealAbility) ability;
        pendingResults.add(raZombie.getName() + " pulled and stole "
                + sunSteal.getLastStolenAmount() + " sun.");
    }

    private void activateTombSummon(Zombie tombRaiser, ZombieAbility ability) {
        if (!(ability instanceof TombSummonAbility)
                || !ability.tryUse(tombRaiser, board)) {
            return;
        }
        TombSummonAbility tombSummon = (TombSummonAbility) ability;
        pendingResults.add(tombRaiser.getName() + " raised "
                + tombSummon.getLastSpawnedCount() + " grave(s) at "
                + tombSummon.getLastSpawnedPositions() + ".");
    }

    private void returnStolenSunFromDeadZombies(List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                if (!(ability instanceof SunStealAbility)) {
                    continue;
                }
                int returnedSun = ((SunStealAbility) ability).releaseStolenSun();
                if (returnedSun > 0) {
                    addSun(returnedSun);
                    pendingResults.add(returnedSun + " stolen sun returned after "
                            + zombie.getName() + " died.");
                }
            }
        }
    }

    private void returnCrystalSkullSunFromDeadZombies(
            List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                if (!(ability instanceof LaserBeamAbility)) {
                    continue;
                }
                int droppedSun =
                        ((LaserBeamAbility) ability)
                                .releaseHalfStolenSun();
                if (droppedSun <= 0) {
                    continue;
                }
                EntityPosition position = new EntityPosition(
                        zombie.getLane(),
                        Math.max(0, Math.min(
                                board.getNumberOfColumns() - 1,
                                (int) Math.floor(
                                        zombie.getColumnPosition()))));
                board.addEntity(Sun.createPlantSun(
                        droppedSun, position));
                pendingResults.add(zombie.getName() + " dropped "
                        + droppedSun + " stolen sun on death.");
            }
        }
    }

    private void restoreWizardSheepFromDeadZombies(
            List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                if (!(ability instanceof WizardSpellAbility)) {
                    continue;
                }
                int restored =
                        ((WizardSpellAbility) ability)
                                .restoreTransformedPlants();
                if (restored > 0) {
                    pendingResults.add(restored
                            + " cat-transformed plant(s) returned to normal after "
                            + zombie.getName() + " died.");
                }
            }
        }
    }

    private void trackBoardSpawnedZombies() {
        for (Zombie zombie : board.drainSpawnedZombies()) {
            trackSpawnedZombie(zombie);
        }
    }

    private void trackSpawnedZombie(Zombie zombie) {
        applyDifficultyToZombie(zombie);
        int waveIndex = zombie.getWaveNumber() - 1;
        if (waveIndex < 0 || waveIndex >= spawnedZombiesByWave.size()) {
            return;
        }
        List<Zombie> waveZombies = spawnedZombiesByWave.get(waveIndex);
        if (!waveZombies.contains(zombie)) {
            waveZombies.add(zombie);
        }
    }

    private void updatePlantCooldowns(float deltaSeconds) {
        plantCooldowns.replaceAll((name, remaining) ->
                Math.max(0.0, remaining - deltaSeconds));
        List<String> expiredKeys = new ArrayList<>();
        for (Map.Entry<String, Double> entry : plantCooldowns.entrySet()) {
            if (entry.getValue() <= TIME_EPSILON) {
                expiredKeys.add(entry.getKey());
            }
        }
        for (String key : expiredKeys) {
            plantCooldowns.remove(key);
            plantCooldownFamilies.remove(key);
        }
    }

    private void applyPlantCooldownResetRequests() {
        for (PlantFamily family : board.drainPlantCooldownResetRequests()) {
            List<String> resetKeys = new ArrayList<>();
            for (Map.Entry<String, PlantFamily> entry : plantCooldownFamilies.entrySet()) {
                if (entry.getValue() == family) {
                    resetKeys.add(entry.getKey());
                }
            }
            for (String key : resetKeys) {
                plantCooldowns.remove(key);
                plantCooldownFamilies.remove(key);
            }
        }
    }

    private static String getCooldownKey(BasePlant plant) {
        if (plant instanceof Modifier && ((Modifier) plant).isImitater()) {
            return "imitater";
        }
        return plant.getName().trim().toLowerCase(Locale.ROOT);
    }

    private double getPlantCooldownRemaining(BasePlant plant) {
        return plantCooldowns.getOrDefault(getCooldownKey(plant), 0.0);
    }

    private void startPlantCooldown(BasePlant plant) {
        double rechargeSeconds = Math.max(0.0, plant.getRechargeSeconds());
        if (rechargeSeconds > TIME_EPSILON) {
            String key = getCooldownKey(plant);
            plantCooldowns.put(key, rechargeSeconds);
            PlantFamily family = PlantFamily.findForPlant(plant);
            if (family == null) {
                plantCooldownFamilies.remove(key);
            } else {
                plantCooldownFamilies.put(key, family);
            }
        }
    }

    private void updateSkySuns() {
        if (hasConveyorBelt()
                || skySunsDisabled) {
            return;
        }
        if (gameType != null && !gameType.spawnsSuns()) {
            return;
        }
        while (elapsedSeconds + TIME_EPSILON >= nextSkySunDropAtSeconds) {
            dropSkySun();
            nextSkySunDropAtSeconds +=
                    getAdjustedSkySunDropIntervalSeconds(
                            nextSkySunDropAtSeconds);
        }
    }

    private void startNextWaveIfPossible() {
        if (!zombieWavesStarted) {
            return;
        }
        while (status == GameStatus.ACTIVE && nextWaveIndex < zombieWaves.size()
                && isPreviousWaveDamagedEnough()) {
            spawnWave(nextWaveIndex);
            nextWaveIndex++;
        }
    }

    private boolean isPreviousWaveDamagedEnough() {
        if (nextWaveIndex == 0) {
            return true;
        }

        List<Zombie> previousWave =
                spawnedZombiesByWave.get(nextWaveIndex - 1);
        if (previousWave.isEmpty()) {
            return true;
        }

        long maximumHealth = 0;
        long remainingHealth = 0;
        for (Zombie zombie : previousWave) {
            maximumHealth += zombie.getMaximumHitPoints();
            if (!zombie.isDead()
                    && !zombie.isHypnotized()
                    && board.containsEntity(zombie)) {
                remainingHealth +=
                        Math.max(0, zombie.getHitPoints());
            }
        }

        return remainingHealth == 0
                || maximumHealth > 0
                        && remainingHealth * 4
                                <= maximumHealth;
    }

    private void spawnWave(int waveIndex) {
        int waveNumber = waveIndex + 1;
        ZombieWave wave = zombieWaves.get(waveIndex);
        if (waveIndex == zombieWaves.size() - 1) {
            pendingResults.add("The final wave has come.");
        } else {
            pendingResults.add("Wave " + waveNumber + " started.");
        }
        List<Zombie> spawnedZombies =
                spawnedZombiesByWave.get(waveIndex);
        applyDarkAgesWave(waveNumber, spawnedZombies);
        applyFrostbiteIcyWind(waveNumber);
        applyBigWaveBeachWaterWave(waveNumber, spawnedZombies);
        double normalSpawnColumn = board.getNumberOfColumns() - 0.001;
        for (ZombieType zombieType : wave.getZombieTypes()) {
            int lane = random.nextInt(board.getNumberOfRows());
            boolean glowing = random.nextDouble()
                    < Constants.GLOWING_ZOMBIE_CHANCE;
            int tornadoAdvance = chooseTornadoAdvance(wave);
            double spawnColumn = normalSpawnColumn - tornadoAdvance;
            Zombie zombie = new Zombie(zombieType, waveNumber, lane,
                    spawnColumn, glowing);
            applyDifficultyToZombie(zombie);
            spawnedZombies.add(zombie);
            board.addZombie(zombie);
            pendingResults.add(buildSpawnMessage(zombie, tornadoAdvance));
        }
        zombieWaveNumber = waveNumber;
    }

    private void applyDarkAgesWave(int waveNumber,
            List<Zombie> spawnedZombies) {
        if (chapterRuleset != ChapterRuleset.DARK_AGES) {
            return;
        }
        spawnDarkAgesGraves(waveNumber);
        spawnNecromancyZombies(waveNumber, spawnedZombies);
    }

    private void spawnDarkAgesGraves(int waveNumber) {
        List<EntityPosition> candidates =
                findDarkAgesGraveCandidates();
        if (candidates.isEmpty()) {
            pendingResults.add("No empty tile was available for a new "
                    + "Dark Ages grave at wave " + waveNumber + ".");
            return;
        }

        List<EntityPosition> selected = new ArrayList<>();
        List<EntityPosition> necromancyCandidates = new ArrayList<>();
        for (EntityPosition position : candidates) {
            if (board.getTileAt(position).getTileType()
                    == TileType.NECROMANCY) {
                necromancyCandidates.add(position);
            }
        }
        if (!necromancyCandidates.isEmpty()) {
            Collections.shuffle(necromancyCandidates, random);
            EntityPosition necromancyPosition =
                    necromancyCandidates.get(0);
            selected.add(necromancyPosition);
            candidates.remove(necromancyPosition);
        }

        Collections.shuffle(candidates, random);
        for (EntityPosition position : candidates) {
            if (selected.size() >= DARK_AGES_GRAVES_PER_WAVE) {
                break;
            }
            selected.add(position);
        }

        for (EntityPosition position : selected) {
            boolean necromancy = board.getTileAt(position).getTileType()
                    == TileType.NECROMANCY;
            GraveReward reward = chooseDarkAgesGraveReward();
            if (!board.addGrave(position, reward)) {
                continue;
            }
            pendingResults.add("Dark Ages grave formed at " + position
                    + " during wave " + waveNumber + "; type: "
                    + (necromancy ? "necromancy" : "ordinary")
                    + "; contents: " + reward.getDescription() + ".");
        }
    }

    private List<EntityPosition> findDarkAgesGraveCandidates() {
        List<EntityPosition> candidates = new ArrayList<>();
        for (int row = 0; row < board.getNumberOfRows(); row++) {
            for (int column = 0;
                    column < board.getNumberOfColumns(); column++) {
                EntityPosition position =
                        new EntityPosition(row, column);
                if (board.canAddGraveAt(position)) {
                    candidates.add(position);
                }
            }
        }
        return candidates;
    }

    private GraveReward chooseDarkAgesGraveReward() {
        double rewardRoll = random.nextDouble();
        if (rewardRoll < DARK_AGES_PLANT_FOOD_GRAVE_CHANCE) {
            return GraveReward.PLANT_FOOD;
        }
        if (rewardRoll < DARK_AGES_PLANT_FOOD_GRAVE_CHANCE
                + DARK_AGES_SUN_GRAVE_CHANCE) {
            return GraveReward.SUN;
        }
        return GraveReward.NONE;
    }

    private void spawnNecromancyZombies(int waveNumber,
            List<Zombie> spawnedZombies) {
        for (Grave grave : board.getGraves()) {
            if (!grave.isNecromancyGrave()
                    || board.hasZombieAt(grave.getPosition())) {
                continue;
            }
            EntityPosition position = grave.getPosition();
            boolean glowing = random.nextDouble()
                    < Constants.GLOWING_ZOMBIE_CHANCE;
            Zombie zombie = new Zombie(ZombieType.DARK,
                    waveNumber, position.getRow(),
                    position.getColumn(), glowing);
            spawnedZombies.add(zombie);
            board.addZombie(zombie);
            pendingResults.add("Necromancy awakened "
                    + zombie.getName() + " beneath the grave at "
                    + position + " during wave " + waveNumber + ".");
        }
    }

    private void applyFrostbiteIcyWind(int waveNumber) {
        if (chapterRuleset != ChapterRuleset.FROSTBITE_CAVES
                || board.getPlants().isEmpty()) {
            return;
        }
        List<Integer> candidateLanes = new ArrayList<>();
        for (BasePlant plant : board.getPlants()) {
            EntityPosition position = plant.getEntityPosition();
            if (position != null && !plant.isDestroyed()
                    && !plant.isFrozen()
                    && !plant.hasTag(PlantTag.FIRE)
                    && !candidateLanes.contains(position.getRow())) {
                candidateLanes.add(position.getRow());
            }
        }
        if (candidateLanes.isEmpty()) {
            return;
        }
        List<Integer> windLanes = new ArrayList<>();
        for (int lane : candidateLanes) {
            if (random.nextDouble() < ICY_WIND_LANE_CHANCE) {
                windLanes.add(lane);
            }
        }
        if (windLanes.isEmpty()) {
            return;
        }
        Collections.sort(windLanes);
        int affectedPlants = board.applyIcyWind(windLanes);
        pendingResults.addAll(board.drainResults());
        if (affectedPlants > 0) {
            pendingResults.add("Icy wind struck lane(s) "
                    + windLanes + " at wave " + waveNumber
                    + " and chilled " + affectedPlants
                    + " plant(s).");
        }
    }

    private void applyBigWaveBeachWaterWave(int waveNumber,
            List<Zombie> spawnedZombies) {
        if (chapterRuleset != ChapterRuleset.BIG_WAVE_BEACH
                || waveNumber <= 1
                || !board.isBigWaveBeachRulesEnabled()) {
            return;
        }
        int previousWaterColumns = board.getWaterColumnCount();
        List<EntityPosition> floodedLowBeachTiles =
                board.raiseBigWaveBeachTide();
        int currentWaterColumns = board.getWaterColumnCount();
        pendingResults.addAll(board.drainResults());
        if (currentWaterColumns == previousWaterColumns) {
            return;
        }
        pendingResults.add("A water wave raised the tide from "
                + previousWaterColumns + " to "
                + currentWaterColumns
                + " rightmost columns. The tide limit is "
                + board.getMaximumWaterColumnCount()
                + " columns, beginning at column "
                + board.getWaterBoundaryColumn() + ".");
        for (EntityPosition position : floodedLowBeachTiles) {
            spawnLowBeachZombie(position, waveNumber,
                    spawnedZombies);
        }
    }

    private void spawnLowBeachZombie(EntityPosition position,
            int waveNumber, List<Zombie> spawnedZombies) {
        boolean glowing = random.nextDouble()
                < Constants.GLOWING_ZOMBIE_CHANCE;
        Zombie zombie = new Zombie(ZombieType.BEACH, waveNumber,
                position.getRow(), position.getColumn(), glowing);
        spawnedZombies.add(zombie);
        board.addZombie(zombie);
        pendingResults.add("Zombie " + zombie.getName()
                + " emerged from the flooded low-beach tile at "
                + position + " during wave " + waveNumber + ".");
    }

    private int chooseTornadoAdvance(ZombieWave wave) {
        if (chapterRuleset != ChapterRuleset.ANCIENT_EGYPT
                || !wave.isFinalWave()
                || random.nextDouble() >= TORNADO_SPAWN_CHANCE) {
            return 0;
        }
        return 1 + random.nextInt(MAX_TORNADO_ADVANCE_COLUMNS);
    }

    private static String buildSpawnMessage(
            Zombie zombie, int tornadoAdvance) {
        if (tornadoAdvance > 0) {
            return "Zombie " + zombie.getName()
                    + " arrived by tornado at wave "
                    + zombie.getWaveNumber() + " in lane "
                    + zombie.getLane() + ", " + tornadoAdvance
                    + " columns ahead, which costed "
                    + zombie.getType().getWavePointCost() + ".";
        }
        return "Zombie " + zombie.getName() + " spawned at wave "
                + zombie.getWaveNumber() + " in lane " + zombie.getLane()
                + " which costed " + zombie.getType().getWavePointCost() + ".";
    }

    private boolean hasZombieReachedHouse() {
        for (Zombie zombie : board.getZombies()) {
            if (!zombie.isHypnotized()
                    && !zombie.getType().isBoss()
                    && zombie.hasReachedHouse()) {
                return true;
            }
        }
        return false;
    }

    private void checkForWin() {
        if (timedWarSystem != null
                || zombieWaves.isEmpty()
                || nextWaveIndex < zombieWaves.size()) {
            return;
        }
        for (Zombie zombie : board.getZombies()) {
            if (!zombie.isDead() && !zombie.isHypnotized()) {
                return;
            }
        }
        status = GameStatus.WON;
        pendingResults.add("Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
    }

    private void loseGame() {
        status = GameStatus.LOST;
        pendingResults.add("The zombie ate your brain; LOSER!!!");
    }

    private void loseSaveOurSeeds(
            ProtectedPlantStatus failedPlant) {
        status = GameStatus.LOST;
        pendingResults.add(
                "A protected plant was eaten or destroyed at "
                        + failedPlant.getOriginalPosition()
                        + "; Save Our Seeds failed!");
    }

    public void releaseNuke() {
        if (status != GameStatus.ACTIVE) {
            return;
        }
        List<Zombie> zombieSnapshot = new ArrayList<>(board.getZombies());
        for (Zombie zombie : zombieSnapshot) {
            zombie.kill();
        }
        board.update(0.0f);
        processZombieDeathDrops(zombieSnapshot);
        returnStolenSunFromDeadZombies(zombieSnapshot);
        returnCrystalSkullSunFromDeadZombies(zombieSnapshot);
        restoreWizardSheepFromDeadZombies(zombieSnapshot);
        pendingResults.addAll(board.drainResults());
        if (resolveTimedWar(0.0f)) {
            return;
        }
        startNextWaveIfPossible();
        checkForWin();
    }

    private void dropSkySun() {
        double roll = random.nextDouble();
        SunType type;
        if (roll < Constants.RADIOACTIVE_SKY_SUN_CHANCE) {
            type = SunType.RADIOACTIVE;
        } else if (roll < Constants.RADIOACTIVE_SKY_SUN_CHANCE
                + Constants.SPECIAL_SKY_SUN_CHANCE) {
            type = SunType.SPECIAL;
        } else {
            type = SunType.NORMAL;
        }
        EntityPosition position = new EntityPosition(random.nextInt(board.getNumberOfRows()),
                random.nextInt(board.getNumberOfColumns()));
        board.addEntity(Sun.createSkySun(type, position));
        pendingResults.add("New " + type.getDisplayName() + " sun is dropping at position " + position);
    }

    private void reportSunLandings() {
        for (Sun sun : board.getSuns()) {
            if (sun.consumeLandedEvent()) {
                pendingResults.add("Sun reached the ground at position "
                        + sun.getEntityPosition());
            }
        }
    }

    private void processZombieDeathDrops(List<Zombie> zombies) {
        if (!shouldProcessZombieDeathDrops()) {
            for (Zombie zombie : zombies) {
                if (zombie != null && zombie.isDead()
                        && !zombie.areDeathDropsProcessed()) {
                    zombie.markDeathDropsProcessed();
                }
            }
            return;
        }
        for (Zombie zombie : zombies) {
            if (zombie == null || !zombie.isDead()
                    || zombie.areDeathDropsProcessed()) {
                continue;
            }
            zombie.markDeathDropsProcessed();
            EntityPosition position = getDropPosition(zombie);
            if (zombie.isGlowing()) {
                board.addEntity(new PlantFoodDrop(position));
                pendingResults.add("The glowing zombie dropped a plant food at "
                        + position + ".");
            }
            if (random.nextDouble() < Constants.ZOMBIE_REWARD_DROP_CHANCE) {
                dropZombieReward(position);
            }
        }
    }

    private EntityPosition getDropPosition(Zombie zombie) {
        int row = Math.max(0, Math.min(board.getNumberOfRows() - 1,
                zombie.getLane()));
        int column = Math.max(0, Math.min(board.getNumberOfColumns() - 1,
                (int) Math.floor(zombie.getColumnPosition())));
        return new EntityPosition(row, column);
    }

    private void dropZombieReward(EntityPosition position) {
        int rewardType = random.nextInt(3);
        String rewardName;
        if (rewardType == 0) {
            board.addEntity(new Diamond(position));
            rewardName = "diamond";
        } else if (rewardType == 1) {
            board.addEntity(new Coin(position));
            rewardName = "coin";
        } else {
            board.addEntity(new PotDrop(position));
            rewardName = "pot";
        }
        pendingResults.add("A zombie dropped a " + rewardName + " at "
                + position + ".");
    }

    private double getAdjustedSkySunDropIntervalSeconds(
            double timePassedSeconds) {
        return difficultyRules.scaleSkySunInterval(
                getSkySunDropIntervalSeconds(timePassedSeconds));
    }

    public static double getSkySunDropIntervalSeconds(double timePassedSeconds) {
        if (!Double.isFinite(timePassedSeconds) || timePassedSeconds < 0.0) {
            throw new IllegalArgumentException("timePassedSeconds must be finite and non-negative");
        }
        return Math.max(6.0 + 0.05 * timePassedSeconds, 12.0);
    }

    public List<String> drainResults() {
        if (pendingResults.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>(pendingResults);
        pendingResults.clear();
        return Collections.unmodifiableList(results);
    }

    public boolean collectSun(Sun sun) {
        if (!board.containsEntity(sun)) {
            return false;
        }

        if (sun.isRadioactive() && sun.isDropping()) {
            if (!sun.collectRadioactiveWhileDropping()) {
                return false;
            }
            board.removeEntity(sun);
            explodeRadioactiveSun(sun.getEntityPosition());
            return true;
        }

        int collectedAmount = sun.collect();
        if (collectedAmount <= 0) {
            return false;
        }

        sunCount += collectedAmount;
        board.removeEntity(sun);
        return true;
    }

    public int collectSunAt(int row, int column) {
        return collectSunAt(new EntityPosition(row, column));
    }

    public int collectSunAt(EntityPosition position) {
        return collectSunsAt(position).getCollectedSunAmount();
    }

    public SunCollectionResult collectSunsAt(EntityPosition position) {
        int collectedAmount = 0;
        int collectedCount = 0;
        int explosionCount = 0;
        List<Sun> sunsAtPosition = new ArrayList<>(board.getSunsAt(position));
        for (Sun sun : sunsAtPosition) {
            int amount = sun.getSunAmount();
            boolean radioactiveExplosion = sun.isRadioactive() && sun.isDropping();
            if (collectSun(sun)) {
                collectedCount++;
                collectedAmount += amount;
                if (radioactiveExplosion) {
                    explosionCount++;
                }
            }
        }
        return new SunCollectionResult(collectedCount, collectedAmount,
                explosionCount);
    }

    private void explodeRadioactiveSun(EntityPosition position) {
        List<Zombie> zombieSnapshot = new ArrayList<>(board.getZombies());
        int damagedZombies = 0;
        int damagedPlants = 0;
        for (Zombie zombie : zombieSnapshot) {
            if (isWithinArea(position, zombie.getLane(),
                    (int) Math.floor(zombie.getColumnPosition()), 2)) {
                zombie.takeDamage(Constants.RADIOACTIVE_SUN_ZOMBIE_DAMAGE);
                damagedZombies++;
            }
        }
        for (BasePlant plant : new ArrayList<>(board.getPlants())) {
            EntityPosition plantPosition = plant.getEntityPosition();
            if (plantPosition != null && isWithinArea(position,
                    plantPosition.getRow(), plantPosition.getColumn(), 1)) {
                plant.takeDamage(Constants.RADIOACTIVE_SUN_PLANT_DAMAGE);
                damagedPlants++;
            }
        }
        board.update(0.0f);
        processZombieDeathDrops(zombieSnapshot);
        returnStolenSunFromDeadZombies(zombieSnapshot);
        returnCrystalSkullSunFromDeadZombies(zombieSnapshot);
        restoreWizardSheepFromDeadZombies(zombieSnapshot);
        pendingResults.addAll(board.drainResults());
        pendingResults.add("Radioactive sun exploded at " + position
                + ", damaging " + damagedZombies + " zombies and "
                + damagedPlants + " plants.");
    }

    private static boolean isWithinArea(EntityPosition center, int row,
            int column, int radius) {
        return Math.abs(center.getRow() - row) <= radius
                && Math.abs(center.getColumn() - column) <= radius;
    }

    public int collectPlantFoodDropsAt(EntityPosition position) {
        int collected = 0;
        for (CollectibleDrop drop : new ArrayList<>(
                board.getCollectibleDropsAt(position))) {
            if (!(drop instanceof PlantFoodDrop)
                    || plantFoodCount >= MAX_PLANT_FOOD) {
                continue;
            }
            if (drop.collect()) {
                board.removeEntity(drop);
                plantFoodCount++;
                collected++;
            }
        }
        return collected;
    }

    public RewardCollectionResult collectRewardDropsAt(EntityPosition position,
            User user) {
        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }
        int dropCount = 0;
        int coins = 0;
        int diamonds = 0;
        int pots = 0;
        for (CollectibleDrop drop : new ArrayList<>(
                board.getCollectibleDropsAt(position))) {
            boolean isReward = drop instanceof Coin
                    || drop instanceof Diamond
                    || drop instanceof PotDrop;
            if (!isReward || !drop.collect()) {
                continue;
            }
            board.removeEntity(drop);
            dropCount++;
            if (drop instanceof Coin) {
                coins += Coin.AMOUNT;
                user.addCoins(Coin.AMOUNT);
            } else if (drop instanceof Diamond) {
                diamonds += Diamond.AMOUNT;
                user.addDiamonds(Diamond.AMOUNT);
            } else if (drop instanceof PotDrop) {
                pots++;
                user.addPots(1);
            }
        }
        return new RewardCollectionResult(dropCount, coins, diamonds, pots);
    }

    public boolean addPlantFood() {
        if (plantFoodCount >= MAX_PLANT_FOOD) {
            return false;
        }
        plantFoodCount++;
        return true;
    }

    public void loadStartingPlantFood(int count) {
        if (count < 0 || count > MAX_PLANT_FOOD) {
            throw new IllegalArgumentException(
                    "starting plant food must be between 0 and 3");
        }
        plantFoodCount = count;
        if (count > 0) {
            pendingResults.add("Started the level with " + count
                    + " stored plant food" + (count == 1 ? "." : "s."));
        }
    }

    public PlantFoodResult feedPlantAt(EntityPosition position) {
        if (plantFoodCount <= 0) {
            return PlantFoodResult.NO_PLANT_FOOD;
        }
        PlantFoodResult result = board.usePlantFoodAt(position);
        if (result == PlantFoodResult.SUCCESS) {
            plantFoodCount--;
        }
        return result;
    }

    public void addSun(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (sunCount > Integer.MAX_VALUE - amount) {
            throw new IllegalArgumentException("sun total is too large");
        }
        sunCount += amount;
    }

    public Zombie spawnZombie(String requestedType, int column, int row) {
        ZombieType type = ZombieType.findByName(requestedType);
        if (type == null || type.isBoss()) {
            return null;
        }
        if (row < 0 || row >= board.getNumberOfRows()
                || column < 0 || column >= board.getNumberOfColumns()) {
            throw new IllegalArgumentException("zombie position is outside the board");
        }
        boolean glowing = random.nextDouble()
                < Constants.GLOWING_ZOMBIE_CHANCE;
        Zombie zombie = new Zombie(type, Math.max(1, zombieWaveNumber),
                row, column, glowing);
        applyDifficultyToZombie(zombie);
        board.addZombie(zombie);
        pendingResults.add("Zombie " + zombie.getName()
                + " spawned by cheat at (" + column + ", " + row + ").");
        return zombie;
    }

    private void applyDifficultyToZombie(Zombie zombie) {
        if (zombie != null) {
            zombie.applyDifficulty(difficultyRules.getLevel());
        }
    }

    public int getDifficultyLevel() {
        return difficultyRules.getLevel();
    }

    public void enableConveyorBelt(
            List<String> availablePlantTypes) {
        if (conveyorBeltSystem != null
                || lockedPlantsSystem != null) {
            throw new IllegalStateException(
                    "another plant-selection rule is already enabled");
        }
        conveyorBeltSystem =
                new ConveyorBeltSystem(
                        availablePlantTypes);
        pendingResults.addAll(
                conveyorBeltSystem.drainMessages());
    }

    public void replaceConveyorPlantPool(
            List<String> availablePlantTypes) {
        if (conveyorBeltSystem == null) {
            throw new IllegalStateException(
                    "this game has no Conveyor Belt");
        }
        conveyorBeltSystem =
                new ConveyorBeltSystem(availablePlantTypes);
        pendingResults.add("Conveyor Belt pool updated from the "
                + "player's unlocked plants.");
        pendingResults.addAll(
                conveyorBeltSystem.drainMessages());
    }

    public boolean hasConveyorBelt() {
        return conveyorBeltSystem != null;
    }

    public List<ConveyorPlantPacket>
            getConveyorPackets() {
        if (conveyorBeltSystem == null) {
            return Collections.emptyList();
        }
        return conveyorBeltSystem.getPackets();
    }

    public ConveyorPlantPacket getConveyorPacket(
            int index) {
        if (conveyorBeltSystem == null) {
            return null;
        }
        return conveyorBeltSystem.getPacket(index);
    }

    protected final ConveyorPlantPacket consumeConveyorPacket(
            int index) {
        if (conveyorBeltSystem == null) {
            return null;
        }
        return conveyorBeltSystem.consumePacket(index);
    }

    public double
            getConveyorSecondsUntilNextPacket() {
        if (conveyorBeltSystem == null) {
            return 0.0;
        }
        return conveyorBeltSystem
                .getSecondsUntilNextPacket();
    }

    public ConveyorPlacementResult
            plantFromConveyor(
                    int index,
                    EntityPosition position) {
        if (conveyorBeltSystem == null) {
            return ConveyorPlacementResult
                    .NOT_CONVEYOR_LEVEL;
        }
        ConveyorPlantPacket packet =
                conveyorBeltSystem.getPacket(index);
        if (packet == null) {
            return ConveyorPlacementResult
                    .INVALID_PACKET;
        }
        if (!board.isPositionInsideBoard(position)) {
            return ConveyorPlacementResult
                    .INVALID_POSITION;
        }

        BasePlant plant = PlantFactory.createPlant(
                packet.getPlantType(), position);
        if (plant == null) {
            return ConveyorPlacementResult
                    .UNKNOWN_PLANT;
        }
        if (!board.canAddPlant(plant)
                || !board.addPlant(plant)) {
            return ConveyorPlacementResult
                    .POSITION_OCCUPIED;
        }

        conveyorBeltSystem.consumePacket(index);
        pendingResults.add("Conveyor Belt plant "
                + plant.getName() + " was planted at "
                + position + ".");
        return ConveyorPlacementResult.SUCCESS;
    }

    public void enableLockedPlantsForcedLoadout(
            List<String> forcedPlantTypes) {
        if (lockedPlantsSystem != null
                || conveyorBeltSystem != null) {
            throw new IllegalStateException(
                    "another plant-selection rule is already enabled");
        }
        lockedPlantsSystem =
                LockedPlantsSystem.forcedLoadout(
                        forcedPlantTypes);
        pendingResults.add(
                "Locked Plants level started. "
                        + lockedPlantsSystem.describeRule());
    }

    public void enableLockedPlantFamilyRepresentatives(
            List<String> representativePlantTypes) {
        if (lockedPlantsSystem != null
                || conveyorBeltSystem != null) {
            throw new IllegalStateException(
                    "another plant-selection rule is already enabled");
        }
        lockedPlantsSystem =
                LockedPlantsSystem.familyRepresentatives(
                        representativePlantTypes);
        pendingResults.add(
                "Locked Plants level started. "
                        + lockedPlantsSystem.describeRule());
    }

    public boolean hasLockedPlants() {
        return lockedPlantsSystem != null;
    }

    public boolean isPlantAllowed(BasePlant plant) {
        return lockedPlantsSystem == null
                || lockedPlantsSystem.isAllowed(plant);
    }

    public LockedPlantsMode getLockedPlantsMode() {
        return lockedPlantsSystem == null
                ? null : lockedPlantsSystem.getMode();
    }

    public List<String> getLockedPlantTypes() {
        if (lockedPlantsSystem == null) {
            return Collections.emptyList();
        }
        return lockedPlantsSystem.getConfiguredPlantTypes();
    }

    public List<String> getForcedPlantTypes() {
        if (lockedPlantsSystem == null) {
            return Collections.emptyList();
        }
        return lockedPlantsSystem.getForcedPlantTypes();
    }

    public String getLockedPlantsRuleDescription() {
        if (lockedPlantsSystem == null) {
            return "none";
        }
        return lockedPlantsSystem.describeRule();
    }


    public void configurePlantLoadout(
            Map<String, Integer> selectedPlantLevels,
            List<String> boostedPlantNames) {
        configurePlantLoadout(selectedPlantLevels,
                boostedPlantNames, Collections.emptyList());
    }

    public void configurePlantLoadout(
            Map<String, Integer> selectedPlantLevels,
            List<String> boostedPlantNames,
            List<String> greenhouseBoostNames) {
        if (selectedPlantLevels == null
                || selectedPlantLevels.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one selected plant is required");
        }
        plantLoadoutLevels.clear();
        plantLoadoutNames.clear();
        boostedPlantTypes.clear();
        greenhouseBoostTypes.clear();
        consumedGreenhouseBoosts.clear();
        for (Map.Entry<String, Integer> entry
                : selectedPlantLevels.entrySet()) {
            addPlantToLoadout(entry.getKey(), entry.getValue());
        }
        addConfiguredBoosts(boostedPlantNames,
                boostedPlantTypes);
        addConfiguredBoosts(greenhouseBoostNames,
                greenhouseBoostTypes);
        boostedPlantTypes.addAll(greenhouseBoostTypes);
        plantLoadoutConfigured = true;
    }

    private void addConfiguredBoosts(List<String> plantNames,
            Set<String> destination) {
        if (plantNames == null) {
            return;
        }
        for (String plantName : plantNames) {
            String key = requestedPlantKey(plantName);
            if (plantLoadoutLevels.containsKey(key)) {
                destination.add(key);
            }
        }
    }

    private void addPlantToLoadout(String plantName, int level) {
        BasePlant plant = PlantFactory.createPlant(
                plantName, level, new EntityPosition(0, 0));
        if (plant == null) {
            throw new IllegalArgumentException(
                    "unknown plant in loadout: " + plantName);
        }
        String key = getLoadoutKey(plant);
        plantLoadoutLevels.put(key, Math.max(1, level));
        plantLoadoutNames.put(key, plant.getName());
    }

    public BasePlant createPlantFromLoadout(
            String requestedType, EntityPosition position) {
        if (!plantLoadoutConfigured) {
            return PlantFactory.createPlant(requestedType, position);
        }
        Integer level = plantLoadoutLevels.get(
                requestedPlantKey(requestedType));
        if (level == null) {
            return null;
        }
        return PlantFactory.createPlant(requestedType, level, position);
    }

    public boolean isPlantInLoadout(BasePlant plant) {
        return !plantLoadoutConfigured
                || plantLoadoutLevels.containsKey(getLoadoutKey(plant));
    }

    public boolean hasConfiguredPlantLoadout() {
        return plantLoadoutConfigured;
    }

    public List<BasePlant> getPlantLoadoutPrototypes() {
        if (!plantLoadoutConfigured) {
            return Collections.emptyList();
        }
        List<BasePlant> plants = new ArrayList<>();
        for (Map.Entry<String, String> entry : plantLoadoutNames.entrySet()) {
            BasePlant plant = PlantFactory.createPlant(
                    entry.getValue(), plantLoadoutLevels.get(entry.getKey()),
                    new EntityPosition(0, 0));
            if (plant != null) {
                plants.add(plant);
            }
        }
        return Collections.unmodifiableList(plants);
    }

    private static String requestedPlantKey(String requestedType) {
        String normalized = normalizePlantName(requestedType);
        if (normalized.startsWith("imitater")) {
            return "imitater";
        }
        return normalized;
    }

    private static String getLoadoutKey(BasePlant plant) {
        if (plant instanceof Modifier
                && ((Modifier) plant).isImitater()) {
            return "imitater";
        }
        return normalizePlantName(plant == null ? null : plant.getName());
    }

    private static String normalizePlantName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private void applyLoadoutBoost(BasePlant plant) {
        String key = getLoadoutKey(plant);
        if (!boostedPlantTypes.contains(key)) {
            return;
        }
        board.usePlantFoodAt(plant.getEntityPosition());
        pendingResults.addAll(board.drainResults());
        if (greenhouseBoostTypes.remove(key)) {
            String plantName = plantLoadoutNames.getOrDefault(
                    key, plant.getName());
            consumedGreenhouseBoosts.add(plantName);
            pendingResults.add("Stored greenhouse boost for "
                    + plantName + " was consumed on first use.");
        }
    }

    public List<String> drainConsumedGreenhouseBoosts() {
        List<String> drained = new ArrayList<>(
                consumedGreenhouseBoosts);
        consumedGreenhouseBoosts.clear();
        return drained;
    }

    public void enableSaveOurSeeds(
            List<ProtectedPlantSpec> protectedPlants) {
        if (saveOurSeedsSystem != null) {
            throw new IllegalStateException(
                    "Save Our Seeds is already enabled");
        }
        saveOurSeedsSystem =
                new SaveOurSeedsSystem(
                        board, protectedPlants);
        pendingResults.addAll(
                saveOurSeedsSystem.getStartMessages());
    }

    public boolean hasSaveOurSeeds() {
        return saveOurSeedsSystem != null;
    }

    public List<ProtectedPlantStatus>
            getProtectedPlantStatuses() {
        if (saveOurSeedsSystem == null) {
            return Collections.emptyList();
        }
        return saveOurSeedsSystem.getStatuses(board);
    }

    public boolean isProtectedSeedAt(
            EntityPosition position) {
        return saveOurSeedsSystem != null
                && saveOurSeedsSystem
                        .isProtectedPlantAt(position);
    }

    private ProtectedPlantStatus
            getFailedProtectedPlant() {
        if (saveOurSeedsSystem == null) {
            return null;
        }
        return saveOurSeedsSystem
                .findFailedPlant(board);
    }

    public void enableTimedWarZombieKills(
            double durationSeconds,
            int requiredKills) {
        timedWarSystem = TimedWarSystem.forZombieKills(
                durationSeconds, requiredKills);
        pendingResults.add(
                "Timed War started: "
                        + timedWarSystem.describeObjective()
                        + ".");
    }

    public void enableTimedWarSunProduction(
            double durationSeconds,
            int requiredSun) {
        timedWarSystem =
                TimedWarSystem.forSunProduction(
                        durationSeconds, requiredSun);
        pendingResults.add(
                "Timed War started: "
                        + timedWarSystem.describeObjective()
                        + ".");
    }

    public boolean hasTimedWar() {
        return timedWarSystem != null;
    }

    public TimedWarObjective getTimedWarObjective() {
        return timedWarSystem == null
                ? null : timedWarSystem.getObjective();
    }

    public int getTimedWarProgress() {
        return timedWarSystem == null
                ? 0 : timedWarSystem.getProgress();
    }

    public int getTimedWarTarget() {
        return timedWarSystem == null
                ? 0 : timedWarSystem.getTarget();
    }

    public double getTimedWarRemainingSeconds() {
        return timedWarSystem == null
                ? 0.0
                : timedWarSystem.getRemainingSeconds();
    }

    public void enableNightOps() {
        disableSkySuns("Night Ops");
        pendingResults.add(
                "Night Ops started: no sun will fall from the sky.");
    }

    public void disableSkySuns(String reason) {
        skySunsDisabled = true;
        skySunDisabledReason =
                reason == null ? "" : reason;
    }

    public boolean areSkySunsDisabled() {
        return skySunsDisabled;
    }

    public String getSkySunDisabledReason() {
        return skySunDisabledReason;
    }

    public void enableDeadLine(double lineColumn) {
        deadLineSystem =
                new DeadLineSystem(lineColumn);
        pendingResults.add(
                "Dead Line active at column "
                        + String.format(
                                Locale.ROOT, "%.1f",
                                lineColumn)
                        + ".");
    }

    public boolean hasDeadLine() {
        return deadLineSystem != null;
    }

    public double getDeadLineColumn() {
        return deadLineSystem == null
                ? -1.0
                : deadLineSystem.getLineColumn();
    }

    public void enableLoveYourPlants(
            int maximumLostPlants) {
        loveYourPlantsSystem =
                new LoveYourPlantsSystem(
                        maximumLostPlants);
        loveYourPlantsSystem.observePlants(board);
        pendingResults.add(
                "Love Your Plants started: lose the level "
                        + "after losing "
                        + maximumLostPlants + " plants.");
    }

    public boolean hasLoveYourPlants() {
        return loveYourPlantsSystem != null;
    }

    public int getLostPlantCount() {
        return loveYourPlantsSystem == null
                ? 0
                : loveYourPlantsSystem
                        .getLostPlantCount();
    }

    public int getMaximumLostPlants() {
        return loveYourPlantsSystem == null
                ? 0
                : loveYourPlantsSystem
                        .getMaximumLostPlants();
    }

    public void enablePlantWhatYouGet() {
        plantWhatYouGetSystem =
                new PlantWhatYouGetSystem();
        disableSkySuns("Plant What You Get");
        pendingResults.add(
                "Plant What You Get setup started: "
                        + "sun-producing plants are locked, "
                        + "cooldowns are ignored until "
                        + "start zombie waves.");
    }

    public boolean hasPlantWhatYouGet() {
        return plantWhatYouGetSystem != null;
    }

    public boolean startZombieWaves() {
        if (plantWhatYouGetSystem == null
                || !plantWhatYouGetSystem
                        .startZombieWaves()) {
            return false;
        }
        zombieWavesStarted = true;
        startNextWaveIfPossible();
        pendingResults.add(
                "Zombie waves started; recharge cooldowns "
                        + "are active now.");
        return true;
    }

    public boolean haveZombieWavesStarted() {
        return zombieWavesStarted;
    }

    private boolean isPlantAllowedByPlantWhatYouGet(
            BasePlant plant) {
        return plantWhatYouGetSystem == null
                || plantWhatYouGetSystem
                        .isPlantAllowed(plant);
    }

    private boolean ignoresPlantCooldown() {
        return plantWhatYouGetSystem != null
                && plantWhatYouGetSystem
                        .isSetupActive();
    }

    public PlantPlacementResult plant(BasePlant plant) {
        if (plant == null || !board.isPositionInsideBoard(plant.getEntityPosition())) {
            return PlantPlacementResult.INVALID_POSITION;
        }
        if (!allowsDirectPlanting()) {
            return PlantPlacementResult.PLANT_LOCKED;
        }
        if (!isPlantInLoadout(plant)) {
            return PlantPlacementResult.PLANT_NOT_SELECTED;
        }
        if (!isPlantAllowed(plant)
                || !isPlantAllowedByPlantWhatYouGet(plant)) {
            return PlantPlacementResult.PLANT_LOCKED;
        }
        if (!board.canAddPlant(plant)) {
            return PlantPlacementResult.POSITION_OCCUPIED;
        }
        if (sunCount < plant.getCost()) {
            return PlantPlacementResult.NOT_ENOUGH_SUN;
        }
        if (!ignoresPlantCooldown()
                && getPlantCooldownRemaining(plant)
                        > TIME_EPSILON) {
            return PlantPlacementResult.COOLDOWN_ACTIVE;
        }
        if (!board.addPlant(plant)) {
            return PlantPlacementResult.POSITION_OCCUPIED;
        }

        sunCount -= plant.getCost();
        if (!ignoresPlantCooldown()) {
            startPlantCooldown(plant);
        }
        applyLoadoutBoost(plant);
        if (loveYourPlantsSystem != null) {
            loveYourPlantsSystem.observePlants(board);
        }
        return PlantPlacementResult.SUCCESS;
    }

    public BasePlant pluckPlantAt(EntityPosition position) {
        if (!board.isPositionInsideBoard(position)
                || isProtectedSeedAt(position)) {
            return null;
        }
        prepareLoveYourPlants();
        BasePlant removed =
                board.removePlantAt(position);
        if (removed != null) {
            resolveLoveYourPlantsFailure();
        }
        return removed;
    }

    public double getPlantCooldownRemainingSeconds(BasePlant plant) {
        if (plant == null) {
            return 0.0;
        }
        return getPlantCooldownRemaining(plant);
    }

    public void removePlantCooldowns() {
        plantCooldowns.clear();
        plantCooldownFamilies.clear();
    }

    public boolean spendSun(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        if (sunCount < amount) {
            return false;
        }
        sunCount -= amount;
        return true;
    }

    public boolean isGameOver() {
        return status != GameStatus.ACTIVE
                || gameType != null && gameType.checkForSpecialGameEnd();
    }

    /**
     * Minigames can disable the ordinary sun-and-cooldown planting command.
     */
    public boolean allowsDirectPlanting() {
        return true;
    }

    public String getDirectPlantingDisabledMessage() {
        return "ordinary planting is disabled in this minigame!";
    }

    /**
     * Minigames can suppress glowing-zombie and economy drops.
     */
    protected boolean shouldProcessZombieDeathDrops() {
        return true;
    }

    /**
     * Reverse-defense minigames can replace lawn mowers with their own goal.
     */
    protected boolean usesLawnMowers() {
        return true;
    }

    protected final void addPendingResult(String message) {
        if (message != null && !message.isBlank()) {
            pendingResults.add(message);
        }
    }

    protected final void completeGameAsWon(String message) {
        if (status != GameStatus.ACTIVE) {
            return;
        }
        status = GameStatus.WON;
        addPendingResult(message);
    }

    protected final void completeGameAsLost(String message) {
        if (status != GameStatus.ACTIVE) {
            return;
        }
        status = GameStatus.LOST;
        addPendingResult(message);
    }

    private static void validateDeltaSeconds(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
    }

    public Board getBoard() {
        return board;
    }

    public ChapterRuleset getChapterRuleset() {
        return chapterRuleset;
    }

    public GameType getGameType() {
        return gameType;
    }

    public List<LawnMower> getLawnMowers() {
        return lawnMowerSystem.getMowers();
    }

    public LawnMower getLawnMowerAtRow(int row) {
        return lawnMowerSystem.getMowerAtRow(row);
    }

    public int getSunCount() {
        return sunCount;
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public int getMaximumPlantFoodCount() {
        return MAX_PLANT_FOOD;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getZombieWaveNumber() {
        return zombieWaveNumber;
    }

    public List<ZombieWave> getZombieWaves() {
        return Collections.unmodifiableList(zombieWaves);
    }

    public GameStatus getStatus() {
        return status;
    }
}
