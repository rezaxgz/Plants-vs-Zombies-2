package io.github.Plants_Vs_Zombies_2.model.game.scored;

import java.time.LocalDate;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.ChapterRuleset;
import io.github.Plants_Vs_Zombies_2.model.game.Game;
import io.github.Plants_Vs_Zombies_2.model.game.GameStatus;
import io.github.Plants_Vs_Zombies_2.model.game.ZombieWave;
import io.github.Plants_Vs_Zombies_2.model.game.defense.LawnMower;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.armor.ArmorType;
import io.github.Plants_Vs_Zombies_2.view.game.ScoredGameView;

/**
 * Daily deterministic score challenge. MowPoint is recorded at game end.
 */
public final class ScoredGame extends Game {
    private static final int QUICK_KILL_SECONDS = 8;
    private static final int QUICK_KILL_BONUS = 150;
    private static final int LONG_RANGE_BONUS = 75;
    private static final int ARMORED_KILL_BONUS = 100;
    private static final int MULTI_KILL_BONUS = 125;
    private static final int UNUSED_MOWER_BONUS = 250;

    private final LocalDate challengeDate;
    private final long dailySeed;
    private final ScoreBreakdown scoreBreakdown = new ScoreBreakdown();
    private final Map<Zombie, Double> spawnTimes = new IdentityHashMap<>();
    private boolean scoreFinalized;

    ScoredGame(LocalDate challengeDate, long dailySeed,
            List<ZombieWave> waves, Random gameRandom) {
        super(new Board(), null, 150, waves, gameRandom,
                false, ChapterRuleset.NONE, 3);
        if (challengeDate == null || gameRandom == null) {
            throw new IllegalArgumentException(
                    "challenge date and random are required");
        }
        this.challengeDate = challengeDate;
        this.dailySeed = dailySeed;
        addPendingResult("Daily Scored Game started for "
                + challengeDate + " UTC.");
        addPendingResult("Fair-play difficulty is fixed at 3.");
        beginZombieWaves();
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        finalizeScoreIfNeeded();
    }

    @Override
    protected void onZombieSpawned(Zombie zombie) {
        if (zombie != null) {
            spawnTimes.putIfAbsent(
                    zombie, getElapsedSeconds());
        }
    }

    @Override
    protected void onZombieDeaths(List<Zombie> zombies) {
        if (zombies == null || zombies.isEmpty()) {
            return;
        }
        int gained = 0;
        for (Zombie zombie : zombies) {
            gained += scoreZombieDeath(zombie);
        }
        if (zombies.size() >= 2) {
            int bonus = MULTI_KILL_BONUS
                    * (zombies.size() - 1);
            scoreBreakdown.addMultiKillPoints(bonus);
            gained += bonus;
        }
        addPendingResult("Scored " + gained
                + " MowPoint from " + zombies.size()
                + " zombie death(s); total: " + getScore() + ".");
    }

    private int scoreZombieDeath(Zombie zombie) {
        if (zombie == null) {
            return 0;
        }
        ZombieType type = zombie.getType();
        int base = Math.max(25, type.getWavePointCost());
        scoreBreakdown.addBaseKillPoints(base);
        int gained = base;

        double spawnTime = spawnTimes.getOrDefault(
                zombie, getElapsedSeconds());
        if (getElapsedSeconds() - spawnTime <= QUICK_KILL_SECONDS) {
            scoreBreakdown.addQuickKillPoints(
                    QUICK_KILL_BONUS);
            gained += QUICK_KILL_BONUS;
        }
        if (zombie.getColumnPosition() >= getBoard().getNumberOfColumns() / 2.0) {
            scoreBreakdown.addLongRangePoints(
                    LONG_RANGE_BONUS);
            gained += LONG_RANGE_BONUS;
        }
        ArmorType armor = type.getDefaultArmor();
        if (armor != null && armor != ArmorType.NONE) {
            scoreBreakdown.addArmoredKillPoints(
                    ARMORED_KILL_BONUS);
            gained += ARMORED_KILL_BONUS;
        }
        return gained;
    }

    private void finalizeScoreIfNeeded() {
        if (scoreFinalized
                || getStatus() == GameStatus.ACTIVE) {
            return;
        }
        scoreFinalized = true;
        if (getStatus() == GameStatus.WON) {
            int readyMowers = 0;
            for (LawnMower mower : getLawnMowers()) {
                if (mower.isAvailable()) {
                    readyMowers++;
                }
            }
            int bonus = readyMowers * UNUSED_MOWER_BONUS;
            scoreBreakdown.addFinishBonusPoints(bonus);
            addPendingResult("Unused lawn-mower finish bonus: "
                    + bonus + " MowPoint.");
        }
        addPendingResult("Daily Scored Game ended with "
                + getScore() + " MowPoint.");
    }

    @Override
    public void releaseNuke() {
        addPendingResult(
                "The nuke cheat is disabled in Scored Game.");
    }

    @Override
    public Zombie spawnZombie(
            String requestedType, int column, int row) {
        addPendingResult(
                "Zombie spawning cheats are disabled in Scored Game.");
        return null;
    }

    @Override
    public boolean allowsCheats() {
        return false;
    }

    public int getScore() {
        return scoreBreakdown.getTotalPoints();
    }

    public ScoreBreakdown getScoreBreakdown() {
        return scoreBreakdown;
    }

    public LocalDate getChallengeDate() {
        return challengeDate;
    }

    public long getDailySeed() {
        return dailySeed;
    }

    public boolean isScoreFinalized() {
        return scoreFinalized;
    }

    public static String getRulesDescription() {
        return ScoredGameView.formatRules();
    }
}
