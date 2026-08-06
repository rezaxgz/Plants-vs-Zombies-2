package io.github.some_example_name.model.validation;

import java.util.ArrayList;
import java.util.List;

import io.github.some_example_name.model.game.DifficultyRules;
import io.github.some_example_name.model.game.ZombieWave;
import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.other.Sun;
import io.github.some_example_name.model.game.entities.other.SunType;
import io.github.some_example_name.model.game.entities.zombies.Zombie;
import io.github.some_example_name.model.game.entities.zombies.ZombieType;
import io.github.some_example_name.model.game.scored.DailyScoredGameFactory;
import io.github.some_example_name.model.game.scored.ScoredGame;
import io.github.some_example_name.model.game.special.TimedWarObjective;
import io.github.some_example_name.model.game.special.TimedWarState;
import io.github.some_example_name.model.game.special.TimedWarSystem;
import io.github.some_example_name.model.roadmap.Chapter;
import io.github.some_example_name.model.roadmap.ChapterCatalog;
import io.github.some_example_name.model.roadmap.Level;
import io.github.some_example_name.model.roadmap.SpecialLevelType;

/**
 * Dependency-free deterministic checks for critical project rules.
 */
public final class ProjectSelfTest {
    private static final double EPSILON = 0.000001;

    private final List<String> results = new ArrayList<>();
    private int passedCount;
    private int totalCount;

    private ProjectSelfTest() {
    }

    public static ProjectSelfTestReport runAll() {
        ProjectSelfTest test = new ProjectSelfTest();
        test.run("difficulty multipliers",
                test::checkDifficultyMultipliers);
        test.run("wave progression and exact budgets",
                test::checkWaveRules);
        test.run("catalog contains no Zomboss",
                test::checkNoBossLevels);
        test.run("daily Scored Game is deterministic",
                test::checkDailyScoredGame);
        test.run("Scored Game disables cheats",
                test::checkScoredGameCheats);
        test.run("Timed War objectives count correctly",
                test::checkTimedWarSystems);
        test.run("Timed War level variants are playable",
                test::checkTimedWarVariants);
        return new ProjectSelfTestReport(
                test.passedCount,
                test.totalCount,
                test.results);
    }

    private void run(String name, Runnable check) {
        totalCount++;
        try {
            check.run();
            passedCount++;
            results.add("PASS - " + name);
        } catch (RuntimeException | AssertionError exception) {
            results.add("FAIL - " + name + ": "
                    + safeMessage(exception));
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName()
                : message;
    }

    private void checkDifficultyMultipliers() {
        DifficultyRules normal = DifficultyRules.forLevel(3);
        DifficultyRules hard = DifficultyRules.forLevel(5);
        requireClose(1.0,
                normal.getZombieHealthMultiplier());
        requireClose(5.0 / 3.0,
                hard.getZombieHealthMultiplier());
        requireClose(5.0 / 3.0,
                hard.getZombieDamageMultiplier());
        requireClose(5.0 / 3.0,
                hard.getGameSpeedMultiplier());
        requireClose(3.0 / 5.0,
                hard.getZombieWaveCostMultiplier());
        requireClose(25.0 / 9.0,
                hard.getSkySunIntervalMultiplier());
    }

    private void checkWaveRules() {
        for (Chapter chapter : ChapterCatalog.getChapters()) {
            for (Level level : chapter.getLevels()) {
                List<ZombieWave> waves = level.getZombieWaves();
                require(waves.size() == 3,
                        "every level must have three waves");
                checkWaveProgression(waves);
                checkDefaultWaveBudgets(level, waves);
            }
        }
    }

    private static void checkWaveProgression(
            List<ZombieWave> waves) {
        ZombieWave first = waves.get(0);
        ZombieWave second = waves.get(1);
        ZombieWave last = waves.get(2);
        require(second.getDifficulty() * 4 == first.getDifficulty() * 5,
                "wave two must be 25 percent harder");
        require(last.getDifficulty() == second.getDifficulty() * 2,
                "final wave must be double wave two");
        require(!first.isFinalWave()
                && !second.isFinalWave()
                && last.isFinalWave(),
                "only wave three must be final");
        require(last.getZombieTypes()
                .contains(ZombieType.FLAG),
                "final wave must contain Flag Zombie");
    }

    private static void checkDefaultWaveBudgets(
            Level level, List<ZombieWave> waves) {
        for (int index = 0; index < waves.size(); index++) {
            ZombieWave wave = waves.get(index);
            int actual = wave.getEffectiveWaveCost(3);
            int expected = wave.getDifficulty();
            require(actual == expected,
                    level.getName()
                            + " wave " + (index + 1)
                            + " expected " + expected
                            + " but generated " + actual);
        }
    }

    private void checkNoBossLevels() {
        for (Chapter chapter : ChapterCatalog.getChapters()) {
            for (Level level : chapter.getLevels()) {
                for (ZombieWave wave : level.getZombieWaves()) {
                    for (ZombieType type : wave.getZombieTypes()) {
                        require(!type.isBoss(),
                                "boss found in "
                                        + level.getName());
                    }
                }
            }
        }
    }

    private void checkDailyScoredGame() {
        ScoredGame first = DailyScoredGameFactory.create();
        ScoredGame second = DailyScoredGameFactory.create();
        require(first.getChallengeDate()
                .equals(second.getChallengeDate()),
                "daily dates differ");
        require(first.getDailySeed() == second.getDailySeed(),
                "daily seeds differ");
        require(zombieSignature(first)
                .equals(zombieSignature(second)),
                "daily waves or lanes differ");
    }

    private static List<String> zombieSignature(
            ScoredGame game) {
        List<String> signature = new ArrayList<>();
        for (Zombie zombie : game.getBoard().getZombies()) {
            signature.add(zombie.getType().name()
                    + ":" + zombie.getLane()
                    + ":" + zombie.getColumnPosition());
        }
        return signature;
    }

    private void checkScoredGameCheats() {
        ScoredGame game = DailyScoredGameFactory.create();
        int zombieCount = game.getBoard().getZombies().size();
        require(!game.allowsCheats(),
                "Scored Game allows cheats");
        game.releaseNuke();
        require(game.getBoard().getZombies().size() == zombieCount,
                "nuke changed Scored Game");
        require(game.spawnZombie(
                "Basic", 8, 2) == null,
                "spawn cheat changed Scored Game");
    }

    private void checkTimedWarSystems() {
        Zombie zombie = new Zombie(
                ZombieType.BASIC, 1, 0, 8.0, false);
        zombie.kill();
        TimedWarSystem killSystem = TimedWarSystem.forZombieKills(30.0, 1);
        require(killSystem.update(
                0.0, List.of(zombie), List.of()) == TimedWarState.SUCCEEDED,
                "kill objective did not succeed");

        TimedWarSystem sunSystem = TimedWarSystem.forSunProduction(60.0, 50);
        Sun skySun = Sun.createSkySun(
                SunType.NORMAL,
                new EntityPosition(0, 0));
        sunSystem.update(
                0.0, List.of(), List.of(skySun));
        require(sunSystem.getProgress() == 0,
                "sky sun was counted");
        checkPlantSunProgress(sunSystem);
    }

    private static void checkPlantSunProgress(
            TimedWarSystem sunSystem) {
        Sun plantSun = Sun.createPlantSun(
                50, new EntityPosition(0, 0));
        require(sunSystem.update(
                0.0, List.of(), List.of(plantSun)) == TimedWarState.SUCCEEDED,
                "plant sun objective did not succeed");
        require(sunSystem.getProgress() == 50,
                "plant sun amount was counted incorrectly");
    }

    private void checkTimedWarVariants() {
        Chapter chapter = ChapterCatalog.findById("frostbite-caves");
        require(chapter != null,
                "Frostbite Caves is missing");
        Level base = chapter.getLevel(3);
        require(base != null
                && base.getSpecialLevelType() == SpecialLevelType.TIMED_WAR,
                "Timed War level is missing");

        Level kills = base.withTimedWarObjective(
                TimedWarObjective.KILL_ZOMBIES);
        Level sun = base.withTimedWarObjective(
                TimedWarObjective.PRODUCE_SUN);
        require(kills.getSpecialConfig().getTarget() == 10,
                "kill target is not 10");
        require(sun.getSpecialConfig().getTarget() == 200,
                "sun target is not 200");
        requireClose(30.0,
                kills.getSpecialConfig()
                        .getDurationSeconds());
        requireClose(60.0,
                sun.getSpecialConfig()
                        .getDurationSeconds());
    }

    private static void require(
            boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireClose(
            double expected, double actual) {
        if (Math.abs(expected - actual) > EPSILON) {
            throw new AssertionError(
                    "expected " + expected
                            + " but was " + actual);
        }
    }
}
