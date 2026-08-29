package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.github.Plants_Vs_Zombies_2.model.Constants;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.gameTypes.GameType;
import io.github.Plants_Vs_Zombies_2.model.game.special.ProtectedPlantStatus;
import io.github.Plants_Vs_Zombies_2.model.game.special.TimedWarState;
import io.github.Plants_Vs_Zombies_2.model.game.structure.Grave;
import io.github.Plants_Vs_Zombies_2.model.game.structure.GraveReward;
import io.github.Plants_Vs_Zombies_2.model.game.tile.TileType;
import io.github.Plants_Vs_Zombies_2.view.game.ZombieView;

abstract class GameWaveLogic extends GameAbilityLogic {
    private static final int MAX_INITIAL_WAVE_ZOMBIES = 2;
    private static final int MIN_NON_BOSS_WAVE_ZOMBIES = 4;
    private static final double MIN_WAVE_SPAWN_SPACING_SECONDS = 3.0;
    private static final double MAX_WAVE_SPAWN_SPACING_SECONDS = 6.0;
    private static final double TARGET_WAVE_DEPLOYMENT_SECONDS = 24.0;

    protected GameWaveLogic(Board board, GameType gameType,
            int initialSunCount, List<ZombieWave> zombieWaves,
            Random random, boolean startWavesImmediately,
            ChapterRuleset chapterRuleset, int difficultyLevel) {
        super(board, gameType, initialSunCount, zombieWaves, random,
                startWavesImmediately, chapterRuleset, difficultyLevel);
    }

    void updateSkySuns() {
        if (hasConveyorBelt()
                || skySunsDisabled) {
            return;
        }
        if (gameType != null && !gameType.spawnsSuns()) {
            return;
        }
        while (elapsedSeconds + TIME_EPSILON >= nextSkySunDropAtSeconds) {
            dropSkySun();
            nextSkySunDropAtSeconds += getAdjustedSkySunDropIntervalSeconds(
                    nextSkySunDropAtSeconds);
        }
    }

    void startNextWaveIfPossible() {
        if (!zombieWavesStarted || guiWaveAdvanceHeld) {
            return;
        }
        while (status == GameStatus.ACTIVE && nextWaveIndex < zombieWaves.size()
                && isPreviousWaveDamagedEnough()) {
            spawnWave(nextWaveIndex);
            nextWaveIndex++;
        }
    }

    public boolean isNextWaveReadyForGui() {
        return zombieWavesStarted
                && nextWaveIndex < zombieWaves.size()
                && isPreviousWaveDamagedEnough();
    }

    public int getNextWaveNumberForGui() {
        return nextWaveIndex < zombieWaves.size()
                ? nextWaveIndex + 1
                : 0;
    }

    public boolean spawnNextWaveForGui() {
        if (!isNextWaveReadyForGui()) {
            return false;
        }
        spawnWave(nextWaveIndex);
        nextWaveIndex++;
        return true;
    }

    public boolean willNextWaveTriggerNecromancy() {
        if (chapterRuleset != ChapterRuleset.DARK_AGES
                || nextWaveIndex >= zombieWaves.size()) {
            return false;
        }
        for (Grave grave : board.getGraves()) {
            if (grave.isNecromancyGrave()
                    && !board.hasZombieAt(grave.getPosition())) {
                return true;
            }
        }
        for (int row = 0; row < board.getNumberOfRows(); row++) {
            for (int column = 0; column < board.getNumberOfColumns(); column++) {
                EntityPosition position = new EntityPosition(row, column);
                if (board.canAddGraveAt(position)
                        && board.getTileAt(position) != null
                        && board.getTileAt(position).getTileType()
                                == TileType.NECROMANCY) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean willNextWaveTriggerLowBeachEmergence() {
        int waveNumber = getNextWaveNumberForGui();
        if (chapterRuleset != ChapterRuleset.BIG_WAVE_BEACH
                || waveNumber <= 1
                || !board.isBigWaveBeachRulesEnabled()
                || board.getWaterColumnCount()
                        >= board.getMaximumWaterColumnCount()) {
            return false;
        }
        int newlyFloodedColumn = board.getNumberOfColumns()
                - (board.getWaterColumnCount() + 1);
        if (newlyFloodedColumn < 0) {
            return false;
        }
        for (int row = 0; row < board.getNumberOfRows(); row++) {
            if (board.isLowBeachTile(
                    new EntityPosition(row, newlyFloodedColumn))) {
                return true;
            }
        }
        return false;
    }

    boolean isPreviousWaveDamagedEnough() {
        if (nextWaveIndex == 0) {
            return true;
        }

        int previousWaveIndex = nextWaveIndex - 1;
        // The old implementation put every member of a wave on the lawn
        // immediately, so the 25% health threshold could never be evaluated
        // before those zombies existed. Preserve that exact readiness model:
        // delayed members are still part of the current wave and must enter
        // the lawn before the normal next-wave health check can succeed.
        if (hasPendingPrimaryWaveSpawns(previousWaveIndex)) {
            return false;
        }

        List<Zombie> previousWave = spawnedZombiesByWave.get(previousWaveIndex);
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
                remainingHealth += Math.max(0, zombie.getHitPoints());
            }
        }

        return remainingHealth == 0
                || maximumHealth > 0
                        && remainingHealth * 4 <= maximumHealth;
    }

    void spawnWave(int waveIndex) {
        int waveNumber = waveIndex + 1;
        ZombieWave wave = zombieWaves.get(waveIndex);
        if (waveIndex == zombieWaves.size() - 1) {
            pendingResults.add("The final wave has come.");
        } else {
            pendingResults.add("Wave " + waveNumber + " started.");
        }
        List<Zombie> spawnedZombies = spawnedZombiesByWave.get(waveIndex);
        boolean bossWave = isBossWave(wave);
        if (!bossWave) {
            // Chapter-specific wave events still occur at the instant the wave
            // begins. Only the ordinary zombies belonging to the wave budget
            // are staggered; changing these hooks would alter necromancy,
            // Frostbite wind, or Big Wave Beach tide timing.
            applyDarkAgesWave(waveNumber, spawnedZombies);
            applyFrostbiteIcyWind(waveNumber);
            applyBigWaveBeachWaterWave(waveNumber, spawnedZombies);
        }

        ensureWaveSpawnSchedulingState();
        List<ZombieType> types = scheduledWaveZombieTypes(wave);
        primaryWaveSpawnCounts[waveIndex] = 0;
        nextPrimaryWaveSpawnAtSeconds[waveIndex] = -1.0;

        if (bossWave) {
            // Zomboss levels remain a single immediate boss spawn. Normal
            // one- and two-zombie waves are expanded below so their staggered
            // deployment is visible instead of ending on the first frame.
            for (ZombieType zombieType : types) {
                spawnPrimaryWaveZombie(waveIndex, wave, zombieType);
                primaryWaveSpawnCounts[waveIndex]++;
            }
        } else {
            int initialCount = initialWaveSpawnCount(types.size());
            for (int index = 0; index < initialCount; index++) {
                spawnPrimaryWaveZombie(waveIndex, wave, types.get(index));
                primaryWaveSpawnCounts[waveIndex]++;
            }
            if (primaryWaveSpawnCounts[waveIndex] < types.size()) {
                nextPrimaryWaveSpawnAtSeconds[waveIndex] = elapsedSeconds
                        + waveSpawnSpacingSeconds(types.size(), initialCount);
            }
        }
        zombieWaveNumber = waveNumber;
    }

    /**
     * Adds delayed members of all waves that have already started. Each wave
     * releases at most one scheduled zombie per update, so even a long frame
     * cannot collapse the remaining deployment into a single burst.
     */
    @Override
    void updatePendingWaveSpawns() {
        ensureWaveSpawnSchedulingState();
        int startedWaveCount = Math.min(nextWaveIndex, zombieWaves.size());
        for (int waveIndex = 0; waveIndex < startedWaveCount; waveIndex++) {
            ZombieWave wave = zombieWaves.get(waveIndex);
            List<ZombieType> types = scheduledWaveZombieTypes(wave);
            int count = Math.max(0, Math.min(types.size(),
                    primaryWaveSpawnCounts[waveIndex]));
            if (count >= types.size()) {
                nextPrimaryWaveSpawnAtSeconds[waveIndex] = -1.0;
                continue;
            }

            double nextAt = nextPrimaryWaveSpawnAtSeconds[waveIndex];
            if (nextAt < 0.0) {
                // Compatibility for old saves made before staggered waves:
                // existing tracked primary zombies are treated as already
                // deployed and any missing members resume from now.
                count = inferPrimaryWaveSpawnCount(waveIndex, types);
                primaryWaveSpawnCounts[waveIndex] = count;
                if (count >= types.size()) {
                    continue;
                }
                nextAt = elapsedSeconds
                        + waveSpawnSpacingSeconds(types.size(),
                                initialWaveSpawnCount(types.size()));
                nextPrimaryWaveSpawnAtSeconds[waveIndex] = nextAt;
            }

            double spacing = waveSpawnSpacingSeconds(types.size(),
                    initialWaveSpawnCount(types.size()));
            if (count < types.size()
                    && elapsedSeconds + TIME_EPSILON >= nextAt) {
                // Never release multiple scheduled members on one update.
                // Even after a long pause or lag spike the remaining zombies
                // still enter one at a time rather than forming a new burst.
                spawnPrimaryWaveZombie(waveIndex, wave, types.get(count));
                count++;
                primaryWaveSpawnCounts[waveIndex] = count;
                if (count < types.size()) {
                    nextAt = Math.max(nextAt + spacing,
                            elapsedSeconds + spacing);
                    nextPrimaryWaveSpawnAtSeconds[waveIndex] = nextAt;
                } else {
                    nextPrimaryWaveSpawnAtSeconds[waveIndex] = -1.0;
                }
            }
        }
    }

    private List<ZombieType> scheduledWaveZombieTypes(ZombieWave wave) {
        if (wave == null) {
            return Collections.emptyList();
        }
        List<ZombieType> original = wave.getZombieTypes();
        if (original.isEmpty() || isBossWave(wave)
                || original.size() >= MIN_NON_BOSS_WAVE_ZOMBIES) {
            return original;
        }

        // Very small normal waves used to finish before the stagger was even
        // noticeable. Repeat their existing composition until the wave has at
        // least four primary zombies; no new zombie type is introduced and
        // the level's original type mix is preserved (A,B becomes A,B,A,B).
        List<ZombieType> expanded = new ArrayList<>(
                MIN_NON_BOSS_WAVE_ZOMBIES);
        for (int index = 0; index < MIN_NON_BOSS_WAVE_ZOMBIES; index++) {
            expanded.add(original.get(index % original.size()));
        }
        return expanded;
    }

    private int initialWaveSpawnCount(int waveSize) {
        if (waveSize <= 0) {
            return 0;
        }
        // Only a small opening group enters immediately. The rest arrive one
        // at a time across a much longer deployment window so each wave reads
        // as a sustained attack rather than a burst at the flag marker.
        return Math.min(waveSize, Math.min(MAX_INITIAL_WAVE_ZOMBIES,
                Math.max(1, (waveSize + 4) / 5)));
    }

    private double waveSpawnSpacingSeconds(int waveSize, int initialCount) {
        int delayedCount = Math.max(0, waveSize - initialCount);
        if (delayedCount <= 0) {
            return 0.0;
        }
        return Math.max(MIN_WAVE_SPAWN_SPACING_SECONDS,
                Math.min(MAX_WAVE_SPAWN_SPACING_SECONDS,
                        TARGET_WAVE_DEPLOYMENT_SECONDS / delayedCount));
    }

    private boolean hasPendingPrimaryWaveSpawns(int waveIndex) {
        if (waveIndex < 0 || waveIndex >= zombieWaves.size()) {
            return false;
        }
        ensureWaveSpawnSchedulingState();
        List<ZombieType> scheduledTypes = scheduledWaveZombieTypes(
                zombieWaves.get(waveIndex));
        int waveSize = scheduledTypes.size();
        int count = Math.max(0, Math.min(waveSize,
                primaryWaveSpawnCounts[waveIndex]));
        if (count == 0 && !spawnedZombiesByWave.get(waveIndex).isEmpty()) {
            count = inferPrimaryWaveSpawnCount(waveIndex, scheduledTypes);
            primaryWaveSpawnCounts[waveIndex] = count;
        }
        return count < waveSize;
    }

    private boolean hasAnyPendingPrimaryWaveSpawns() {
        int startedWaveCount = Math.min(nextWaveIndex, zombieWaves.size());
        for (int waveIndex = 0; waveIndex < startedWaveCount; waveIndex++) {
            if (hasPendingPrimaryWaveSpawns(waveIndex)) {
                return true;
            }
        }
        return false;
    }

    private int inferPrimaryWaveSpawnCount(int waveIndex,
            List<ZombieType> waveTypes) {
        if (waveTypes == null || waveTypes.isEmpty()) {
            return 0;
        }
        int[] neededByType = new int[ZombieType.values().length];
        for (ZombieType type : waveTypes) {
            neededByType[type.ordinal()]++;
        }
        int matched = 0;
        for (Zombie zombie : spawnedZombiesByWave.get(waveIndex)) {
            if (zombie == null) {
                continue;
            }
            int ordinal = zombie.getType().ordinal();
            if (neededByType[ordinal] > 0) {
                neededByType[ordinal]--;
                matched++;
            }
        }
        return Math.min(waveTypes.size(), matched);
    }

    private void ensureWaveSpawnSchedulingState() {
        int waveCount = zombieWaves.size();
        if (primaryWaveSpawnCounts == null
                || primaryWaveSpawnCounts.length != waveCount) {
            primaryWaveSpawnCounts = new int[waveCount];
            for (int waveIndex = 0;
                    waveIndex < Math.min(nextWaveIndex, waveCount);
                    waveIndex++) {
                primaryWaveSpawnCounts[waveIndex] = inferPrimaryWaveSpawnCount(
                        waveIndex, scheduledWaveZombieTypes(
                                zombieWaves.get(waveIndex)));
            }
        }
        if (nextPrimaryWaveSpawnAtSeconds == null
                || nextPrimaryWaveSpawnAtSeconds.length != waveCount) {
            nextPrimaryWaveSpawnAtSeconds = new double[waveCount];
            java.util.Arrays.fill(nextPrimaryWaveSpawnAtSeconds, -1.0);
        }
    }

    /** Total number of primary wave zombies that will be deployed. */
    public int getScheduledPrimaryWaveZombieCount() {
        int total = 0;
        for (ZombieWave wave : zombieWaves) {
            total += scheduledWaveZombieTypes(wave).size();
        }
        return total;
    }

    /** Number of primary wave zombies that have actually entered the lawn. */
    public int getDeployedPrimaryWaveZombieCount() {
        ensureWaveSpawnSchedulingState();
        int total = 0;
        for (int waveIndex = 0; waveIndex < zombieWaves.size(); waveIndex++) {
            int scheduled = scheduledWaveZombieTypes(
                    zombieWaves.get(waveIndex)).size();
            total += Math.max(0, Math.min(scheduled,
                    primaryWaveSpawnCounts[waveIndex]));
        }
        return total;
    }

    /** Scheduled primary-zombie count for one zero-based wave index. */
    public int getScheduledPrimaryWaveZombieCount(int waveIndex) {
        if (waveIndex < 0 || waveIndex >= zombieWaves.size()) {
            return 0;
        }
        return scheduledWaveZombieTypes(zombieWaves.get(waveIndex)).size();
    }

    private void spawnPrimaryWaveZombie(int waveIndex, ZombieWave wave,
            ZombieType zombieType) {
        int waveNumber = waveIndex + 1;
        int lane = zombieType.isBoss()
                ? initialBossLane(zombieType)
                : random.nextInt(board.getNumberOfRows());
        boolean glowing = !zombieType.isBoss()
                && random.nextDouble() < Constants.GLOWING_ZOMBIE_CHANCE;
        int tornadoAdvance = zombieType.isBoss()
                ? 0 : chooseTornadoAdvance(wave);
        double spawnColumn = board.getNumberOfColumns() - 0.001
                - tornadoAdvance;
        if (zombieType == ZombieType.ZOMBOSS_EGYPT) {
            // Egypt Zomboss is wider than a normal zombie. Starting it at
            // the ordinary right-edge spawn column leaves most of the
            // machine clipped off-screen; use the same home column its
            // movement ability returns to after repositioning.
            spawnColumn = Math.max(1.0,
                    board.getNumberOfColumns() - 2.0);
        } else if (zombieType == ZombieType.ZOMBOSS_ICEAGE) {
            // The Ice Age machine is also wider than a normal zombie, but
            // only needs a small visual step onto the lawn to fit fully.
            spawnColumn = Math.max(1.0,
                    board.getNumberOfColumns() - 1.75);
        } else if (zombieType == ZombieType.ZOMBOSS_BEACH) {
            // Big Wave Beach Zomboss is even wider than the regular shark
            // zombies. Spawn it on the same in-bounds home column the boss
            // returns to after lane changes so the intro starts fully on
            // screen instead of clipped beyond the right edge.
            spawnColumn = Math.max(1.0,
                    board.getNumberOfColumns() - 2.0);
        } else if (zombieType == ZombieType.ZOMBOSS_DARK) {
            // Dark Ages Zomboss also has a large 390px PAM and a long
            // entrance animation. Start it on the normal Zomboss home
            // column so the whole intro is visible on the lawn.
            spawnColumn = Math.max(1.0,
                    board.getNumberOfColumns() - 2.0);
        }
        Zombie zombie = new Zombie(zombieType, waveNumber, lane,
                spawnColumn, glowing);
        zombie.setTornadoAdvanceColumns(tornadoAdvance);
        applyDifficultyToZombie(zombie);
        spawnedZombiesByWave.get(waveIndex).add(zombie);
        board.addZombie(zombie);
        onZombieSpawned(zombie);
        pendingResults.add(ZombieView.buildSpawnMessage(
                zombie, tornadoAdvance));
    }

    boolean isBossWave(ZombieWave wave) {
        return wave != null && wave.getZombieTypes().size() == 1
                && wave.getZombieTypes().get(0).isBoss();
    }

    int initialBossLane(ZombieType type) {
        if (board.getNumberOfRows() <= 1) {
            return 0;
        }
        if (type == ZombieType.ZOMBOSS_ICEAGE) {
            return Math.min(2, board.getNumberOfRows() - 1);
        }
        return 1 + random.nextInt(board.getNumberOfRows() - 1);
    }

    void applyDarkAgesWave(int waveNumber,
            List<Zombie> spawnedZombies) {
        if (chapterRuleset != ChapterRuleset.DARK_AGES) {
            return;
        }
        spawnDarkAgesGraves(waveNumber);
        spawnNecromancyZombies(waveNumber, spawnedZombies);
    }

    void spawnDarkAgesGraves(int waveNumber) {
        List<EntityPosition> candidates = findDarkAgesGraveCandidates();
        if (candidates.isEmpty()) {
            pendingResults.add("No empty tile was available for a new "
                    + "Dark Ages grave at wave " + waveNumber + ".");
            return;
        }

        List<EntityPosition> selected = new ArrayList<>();
        List<EntityPosition> necromancyCandidates = new ArrayList<>();
        for (EntityPosition position : candidates) {
            if (board.getTileAt(position).getTileType() == TileType.NECROMANCY) {
                necromancyCandidates.add(position);
            }
        }
        if (!necromancyCandidates.isEmpty()) {
            Collections.shuffle(necromancyCandidates, random);
            EntityPosition necromancyPosition = necromancyCandidates.get(0);
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
            boolean necromancy = board.getTileAt(position).getTileType() == TileType.NECROMANCY;
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

    List<EntityPosition> findDarkAgesGraveCandidates() {
        List<EntityPosition> candidates = new ArrayList<>();
        for (int row = 0; row < board.getNumberOfRows(); row++) {
            for (int column = 0; column < board.getNumberOfColumns(); column++) {
                EntityPosition position = new EntityPosition(row, column);
                if (board.canAddGraveAt(position)) {
                    candidates.add(position);
                }
            }
        }
        return candidates;
    }

    GraveReward chooseDarkAgesGraveReward() {
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

    void spawnNecromancyZombies(int waveNumber,
            List<Zombie> spawnedZombies) {
        for (Grave grave : board.getGraves()) {
            if (!grave.isNecromancyGrave()
                    || board.hasZombieAt(grave.getPosition())) {
                continue;
            }
            EntityPosition position = grave.getPosition();
            boolean glowing = random.nextDouble() < Constants.GLOWING_ZOMBIE_CHANCE;
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

    void applyFrostbiteIcyWind(int waveNumber) {
        if (chapterRuleset != ChapterRuleset.FROSTBITE_CAVES) {
            return;
        }
        // Phase-1 defines the wind in terms of rows first: on each wave, any
        // number of rows may be struck, then every non-fire plant in those
        // rows gains one freeze level. Choose rows independently of what is
        // planted in them so the wind itself is not biased by board contents.
        List<Integer> windLanes = new ArrayList<>();
        for (int lane = 0; lane < board.getNumberOfRows(); lane++) {
            if (random.nextDouble() < ICY_WIND_LANE_CHANCE) {
                windLanes.add(lane);
            }
        }
        if (windLanes.isEmpty()) {
            return;
        }
        int affectedPlants = board.applyIcyWind(windLanes);
        pendingResults.addAll(board.drainResults());
        lastFrostbiteIcyWindLanes.clear();
        lastFrostbiteIcyWindLanes.addAll(windLanes);
        lastFrostbiteIcyWindAtSeconds = elapsedSeconds;
        pendingResults.add("Icy wind struck lane(s) "
                + windLanes + " at wave " + waveNumber
                + " and chilled " + affectedPlants
                + " plant(s).");
    }

    public double getLastFrostbiteIcyWindAtSeconds() {
        return lastFrostbiteIcyWindAtSeconds;
    }

    public List<Integer> getLastFrostbiteIcyWindLanes() {
        return Collections.unmodifiableList(
                new ArrayList<>(lastFrostbiteIcyWindLanes));
    }

    void applyBigWaveBeachWaterWave(int waveNumber,
            List<Zombie> spawnedZombies) {
        if (chapterRuleset != ChapterRuleset.BIG_WAVE_BEACH
                || waveNumber <= 1
                || !board.isBigWaveBeachRulesEnabled()) {
            return;
        }
        int previousWaterColumns = board.getWaterColumnCount();
        List<EntityPosition> floodedLowBeachTiles = board.raiseBigWaveBeachTide();
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

    void spawnLowBeachZombie(EntityPosition position,
            int waveNumber, List<Zombie> spawnedZombies) {
        boolean glowing = random.nextDouble() < Constants.GLOWING_ZOMBIE_CHANCE;
        Zombie zombie = new Zombie(ZombieType.BEACH, waveNumber,
                position.getRow(), position.getColumn(), glowing);
        spawnedZombies.add(zombie);
        board.addZombie(zombie);
        pendingResults.add("Zombie " + zombie.getName()
                + " emerged from the flooded low-beach tile at "
                + position + " during wave " + waveNumber + ".");
    }

    int chooseTornadoAdvance(ZombieWave wave) {
        if (chapterRuleset != ChapterRuleset.ANCIENT_EGYPT
                || !wave.isFinalWave()
                || random.nextDouble() >= TORNADO_SPAWN_CHANCE) {
            return 0;
        }
        return 1 + random.nextInt(MAX_TORNADO_ADVANCE_COLUMNS);
    }

    boolean hasZombieReachedHouse() {
        for (Zombie zombie : board.getZombies()) {
            if (!zombie.isHypnotized()
                    && !zombie.getType().isBoss()
                    && zombie.hasReachedHouse()) {
                return true;
            }
        }
        return false;
    }

    void checkForWin() {
        if (zombieWaves.isEmpty()
                || nextWaveIndex < zombieWaves.size()
                || hasAnyPendingPrimaryWaveSpawns()) {
            return;
        }
        for (Zombie zombie : board.getZombies()) {
            if (!zombie.isDead() && !zombie.isHypnotized()) {
                return;
            }
        }

        if (timedWarSystem != null
                && timedWarSystem.getState() != TimedWarState.SUCCEEDED) {
            status = GameStatus.LOST;
            timedWarFailedAfterWavesCleared = true;
            String unmet = timedWarSystem.describeUnmetRequirements();
            pendingResults.add(
                    "Timed War failed after the final zombie was defeated: "
                            + (unmet.isBlank()
                                    ? timedWarSystem.describeProgress()
                                    : unmet)
                            + ".");
            return;
        }

        status = GameStatus.WON;
        pendingResults.add("Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
    }

    void loseGame() {
        status = GameStatus.LOST;
        pendingResults.add("The zombie ate your brain; LOSER!!!");
    }

    void loseSaveOurSeeds(
            ProtectedPlantStatus failedPlant) {
        status = GameStatus.LOST;
        pendingResults.add(
                "A protected plant was eaten or destroyed at "
                        + failedPlant.getOriginalPosition()
                        + "; Save Our Seeds failed!");
    }
}
