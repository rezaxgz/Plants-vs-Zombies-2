package model.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import model.Constants;
import model.game.defense.LawnMowerResolution;
import model.game.entities.zombies.Zombie;
import model.game.gameTypes.GameType;
import model.game.special.ProtectedPlantStatus;
import model.game.special.TimedWarState;

abstract class GameUpdateLogic extends GameState {
    protected GameUpdateLogic(Board board, GameType gameType,
            int initialSunCount, List<ZombieWave> zombieWaves,
            Random random, boolean startWavesImmediately,
            ChapterRuleset chapterRuleset, int difficultyLevel) {
        super(board, gameType, initialSunCount, zombieWaves, random,
                startWavesImmediately, chapterRuleset, difficultyLevel);
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

    void updateConveyorBelt(
            float deltaSeconds) {
        if (conveyorBeltSystem == null) {
            return;
        }
        conveyorBeltSystem.update(deltaSeconds);
        pendingResults.addAll(
                conveyorBeltSystem.drainMessages());
    }

    boolean resolveDeadLineFailure(
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

    void prepareLoveYourPlants() {
        if (loveYourPlantsSystem != null) {
            loveYourPlantsSystem.observePlants(board);
        }
    }

    boolean resolveSaveOurSeedsFailure() {
        ProtectedPlantStatus failedPlant =
                getFailedProtectedPlant();
        if (failedPlant == null) {
            return false;
        }
        loseSaveOurSeeds(failedPlant);
        return true;
    }

    boolean resolveTimedWar(
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

    boolean resolveLoveYourPlantsFailure() {
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

    List<Zombie> getTrackedZombies() {
        List<Zombie> zombies = new ArrayList<>();
        for (List<Zombie> wave :
                spawnedZombiesByWave) {
            zombies.addAll(wave);
        }
        return zombies;
    }
}
