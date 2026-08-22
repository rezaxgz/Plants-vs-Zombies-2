package io.github.Plants_Vs_Zombies_2.model.game;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.gameTypes.GameType;
import io.github.Plants_Vs_Zombies_2.model.game.special.DeadLineSystem;
import io.github.Plants_Vs_Zombies_2.model.game.special.LoveYourPlantsSystem;
import io.github.Plants_Vs_Zombies_2.model.game.special.PlantWhatYouGetSystem;
import io.github.Plants_Vs_Zombies_2.model.game.special.ProtectedPlantSpec;
import io.github.Plants_Vs_Zombies_2.model.game.special.ProtectedPlantStatus;
import io.github.Plants_Vs_Zombies_2.model.game.special.SaveOurSeedsSystem;
import io.github.Plants_Vs_Zombies_2.model.game.special.TimedWarObjective;
import io.github.Plants_Vs_Zombies_2.model.game.special.TimedWarSystem;

abstract class GameSpecialLevelLogic extends GameLoadoutLogic {
    protected GameSpecialLevelLogic(Board board, GameType gameType,
            int initialSunCount, List<ZombieWave> zombieWaves,
            Random random, boolean startWavesImmediately,
            ChapterRuleset chapterRuleset, int difficultyLevel) {
        super(board, gameType, initialSunCount, zombieWaves, random,
                startWavesImmediately, chapterRuleset, difficultyLevel);
    }

    public void enableSaveOurSeeds(
            List<ProtectedPlantSpec> protectedPlants) {
        if (saveOurSeedsSystem != null) {
            throw new IllegalStateException(
                    "Save Our Seeds is already enabled");
        }
        saveOurSeedsSystem = new SaveOurSeedsSystem(
                board, protectedPlants);
        pendingResults.addAll(
                saveOurSeedsSystem.getStartMessages());
    }

    public boolean hasSaveOurSeeds() {
        return saveOurSeedsSystem != null;
    }

    public List<ProtectedPlantStatus> getProtectedPlantStatuses() {
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

    ProtectedPlantStatus getFailedProtectedPlant() {
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
        timedWarCompletionReported = false;
        timedWarFailedAfterWavesCleared = false;
        pendingResults.add(
                "Timed War started: "
                        + timedWarSystem.describeObjective()
                        + ".");
    }

    public void enableTimedWarZombieKillsAndSunCollection(
            double killWindowSeconds,
            int requiredKills,
            int requiredCollectedSun) {
        timedWarSystem = TimedWarSystem.forZombieKillsAndCollectedSun(
                killWindowSeconds, requiredKills, requiredCollectedSun);
        timedWarCompletionReported = false;
        timedWarFailedAfterWavesCleared = false;
        pendingResults.add(
                "Timed War started: "
                        + timedWarSystem.describeObjective()
                        + ".");
    }

    public void enableTimedWarSunProduction(
            double durationSeconds,
            int requiredSun) {
        timedWarSystem = TimedWarSystem.forSunProduction(
                durationSeconds, requiredSun);
        timedWarCompletionReported = false;
        timedWarFailedAfterWavesCleared = false;
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
                ? null
                : timedWarSystem.getObjective();
    }

    public int getTimedWarProgress() {
        return timedWarSystem == null
                ? 0
                : timedWarSystem.getProgress();
    }

    public int getTimedWarTarget() {
        return timedWarSystem == null
                ? 0
                : timedWarSystem.getTarget();
    }

    public double getTimedWarRemainingSeconds() {
        return timedWarSystem == null
                ? 0.0
                : timedWarSystem.getRemainingSeconds();
    }

    public int getTimedWarRecentZombieKills() {
        return timedWarSystem == null
                ? 0
                : timedWarSystem.getRecentZombieKills();
    }

    public boolean isTimedWarZombieKillRequirementMet() {
        return timedWarSystem != null
                && timedWarSystem.isZombieKillRequirementMet();
    }

    public double getTimedWarKillWindowSeconds() {
        return timedWarSystem == null
                ? 0.0
                : timedWarSystem.getWindowSeconds();
    }

    public int getTimedWarCollectedSun() {
        return timedWarSystem == null
                ? 0
                : timedWarSystem.getCollectedSunProgress();
    }

    public int getTimedWarCollectedSunTarget() {
        return timedWarSystem == null
                ? 0
                : timedWarSystem.getCollectedSunTarget();
    }

    public int getTimedWarSunLeftToCollect() {
        if (timedWarSystem == null) {
            return 0;
        }
        return Math.max(0,
                timedWarSystem.getCollectedSunTarget()
                        - timedWarSystem.getCollectedSunProgress());
    }

    public boolean isTimedWarCollectedSunRequirementMet() {
        return timedWarSystem != null
                && timedWarSystem.isCollectedSunRequirementMet();
    }

    public String getTimedWarUnmetRequirements() {
        return timedWarSystem == null
                ? ""
                : timedWarSystem.describeUnmetRequirements();
    }

    public boolean didTimedWarFailAfterWavesCleared() {
        return timedWarFailedAfterWavesCleared;
    }

    public void enableNightOps() {
        disableSkySuns("Night Ops");
        pendingResults.add(
                "Night Ops started: no sun will fall from the sky.");
    }

    public void disableSkySuns(String reason) {
        skySunsDisabled = true;
        skySunDisabledReason = reason == null ? "" : reason;
    }

    public boolean areSkySunsDisabled() {
        return skySunsDisabled;
    }

    public String getSkySunDisabledReason() {
        return skySunDisabledReason;
    }

    public void enableDeadLine(double lineColumn) {
        deadLineSystem = new DeadLineSystem(lineColumn);
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
        loveYourPlantsSystem = new LoveYourPlantsSystem(
                maximumLostPlants);
        loveYourPlantsSystem.observePlants(board);
        pendingResults.add(
                "Love Your Plants started: do not lose more than "
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

    public int getRemainingPlantLossAllowance() {
        return loveYourPlantsSystem == null
                ? 0
                : loveYourPlantsSystem
                        .getRemainingPlantLossAllowance();
    }

    public void enablePlantWhatYouGet() {
        plantWhatYouGetSystem = new PlantWhatYouGetSystem();
        disableSkySuns("Plant What You Get");
        pendingResults.add(
                "Plant What You Get setup started: "
                        + "sun-producing plants are locked, "
                        + "plant recharge cooldowns are disabled for the "
                        + "entire level, and zombie waves wait for you to "
                        + "start them.");
    }

    public boolean hasPlantWhatYouGet() {
        return plantWhatYouGetSystem != null;
    }

    public void setGuiWaveAdvanceHeld(boolean held) {
        guiWaveAdvanceHeld = held;
    }

    public boolean isGuiWaveAdvanceHeld() {
        return guiWaveAdvanceHeld;
    }

    public boolean startZombieWavesFromGui() {
        if (zombieWavesStarted) {
            return true;
        }
        if (plantWhatYouGetSystem != null
                && !plantWhatYouGetSystem.startZombieWaves()) {
            return false;
        }
        zombieWavesStarted = true;
        pendingResults.add("Zombie waves are ready to start.");
        return true;
    }

    protected final void beginZombieWaves() {
        if (zombieWavesStarted) {
            return;
        }
        zombieWavesStarted = true;
        startNextWaveIfPossible();
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
                "Zombie waves started; plant recharge cooldowns remain "
                        + "disabled for Plant What You Get.");
        return true;
    }

    public boolean haveZombieWavesStarted() {
        return zombieWavesStarted;
    }
}
