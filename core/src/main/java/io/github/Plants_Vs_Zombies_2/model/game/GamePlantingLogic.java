package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.github.Plants_Vs_Zombies_2.model.game.defense.LawnMower;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.gameTypes.GameType;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestRunSummary;

abstract class GamePlantingLogic extends GameSpecialLevelLogic {
    protected GamePlantingLogic(Board board, GameType gameType,
            int initialSunCount, List<ZombieWave> zombieWaves,
            Random random, boolean startWavesImmediately,
            ChapterRuleset chapterRuleset, int difficultyLevel) {
        super(board, gameType, initialSunCount, zombieWaves, random,
                startWavesImmediately, chapterRuleset, difficultyLevel);
    }

    boolean isPlantAllowedByPlantWhatYouGet(
            BasePlant plant) {
        return plantWhatYouGetSystem == null
                || plantWhatYouGetSystem
                        .isPlantAllowed(plant);
    }

    boolean ignoresPlantCooldown() {
        return plantWhatYouGetSystem != null;
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
                && getPlantCooldownRemaining(plant) > TIME_EPSILON) {
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
        questRunTracker.recordPlantPlaced(plant);
        return PlantPlacementResult.SUCCESS;
    }

    public BasePlant pluckPlantAt(EntityPosition position) {
        if (!board.isPositionInsideBoard(position)
                || isProtectedSeedAt(position)) {
            return null;
        }
        prepareLoveYourPlants();
        BasePlant removed = board.removePlantAt(position);
        if (removed != null) {
            questRunTracker.forgetPluckedPlant(removed);
            resolveLoveYourPlantsFailure();
        }
        return removed;
    }

    public double getPlantCooldownRemainingSeconds(BasePlant plant) {
        if (plant == null || ignoresPlantCooldown()) {
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

    protected void onZombieSpawned(Zombie zombie) {
        questRunTracker.recordZombieSpawn(elapsedSeconds);
    }

    protected void onZombieDeaths(List<Zombie> zombies) {
        questRunTracker.recordZombieDeaths(zombies, elapsedSeconds,
                lawnMowerSystem.getMowers());
    }

    public boolean allowsCheats() {
        return true;
    }

    public boolean allowsDirectPlanting() {
        return true;
    }

    public String getDirectPlantingDisabledMessage() {
        return "ordinary planting is disabled in this minigame!";
    }

    protected boolean shouldProcessZombieDeathDrops() {
        return true;
    }

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

    public QuestRunSummary createQuestRunSummary(String chapterId) {
        return questRunTracker.createSummary((Game) this, chapterId);
    }
}
