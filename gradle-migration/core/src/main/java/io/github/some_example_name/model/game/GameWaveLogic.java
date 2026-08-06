package io.github.some_example_name.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.github.some_example_name.model.Constants;
import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.plants.PlantTag;
import io.github.some_example_name.model.game.entities.zombies.Zombie;
import io.github.some_example_name.model.game.entities.zombies.ZombieType;
import io.github.some_example_name.model.game.gameTypes.GameType;
import io.github.some_example_name.model.game.special.ProtectedPlantStatus;
import io.github.some_example_name.model.game.structure.Grave;
import io.github.some_example_name.model.game.structure.GraveReward;
import io.github.some_example_name.model.game.tile.TileType;
import io.github.some_example_name.view.game.ZombieView;

abstract class GameWaveLogic extends GameAbilityLogic {
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
        if (!zombieWavesStarted) {
            return;
        }
        while (status == GameStatus.ACTIVE && nextWaveIndex < zombieWaves.size()
                && isPreviousWaveDamagedEnough()) {
            spawnWave(nextWaveIndex);
            nextWaveIndex++;
        }
    }

    boolean isPreviousWaveDamagedEnough() {
        if (nextWaveIndex == 0) {
            return true;
        }

        List<Zombie> previousWave = spawnedZombiesByWave.get(nextWaveIndex - 1);
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
        applyDarkAgesWave(waveNumber, spawnedZombies);
        applyFrostbiteIcyWind(waveNumber);
        applyBigWaveBeachWaterWave(waveNumber, spawnedZombies);
        double normalSpawnColumn = board.getNumberOfColumns() - 0.001;
        for (ZombieType zombieType : wave.getZombieTypes()) {
            int lane = random.nextInt(board.getNumberOfRows());
            boolean glowing = random.nextDouble() < Constants.GLOWING_ZOMBIE_CHANCE;
            int tornadoAdvance = chooseTornadoAdvance(wave);
            double spawnColumn = normalSpawnColumn - tornadoAdvance;
            Zombie zombie = new Zombie(zombieType, waveNumber, lane,
                    spawnColumn, glowing);
            applyDifficultyToZombie(zombie);
            spawnedZombies.add(zombie);
            board.addZombie(zombie);
            onZombieSpawned(zombie);
            pendingResults.add(ZombieView.buildSpawnMessage(
                    zombie, tornadoAdvance));
        }
        zombieWaveNumber = waveNumber;
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
