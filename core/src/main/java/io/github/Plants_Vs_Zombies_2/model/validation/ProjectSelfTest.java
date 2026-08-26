package io.github.Plants_Vs_Zombies_2.model.validation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.auth.QuestPersistenceCheck;
import io.github.Plants_Vs_Zombies_2.model.game.DifficultyRules;
import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.Game;
import io.github.Plants_Vs_Zombies_2.model.game.PlantPlacementResult;
import io.github.Plants_Vs_Zombies_2.model.game.ZombieWave;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.Sun;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.SunType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFactory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.scored.DailyScoredGameFactory;
import io.github.Plants_Vs_Zombies_2.model.game.scored.ScoredGame;
import io.github.Plants_Vs_Zombies_2.model.game.special.TimedWarObjective;
import io.github.Plants_Vs_Zombies_2.model.game.special.TimedWarState;
import io.github.Plants_Vs_Zombies_2.model.game.special.TimedWarSystem;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Chapter;
import io.github.Plants_Vs_Zombies_2.model.roadmap.ChapterCatalog;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Level;
import io.github.Plants_Vs_Zombies_2.model.roadmap.LevelKind;
import io.github.Plants_Vs_Zombies_2.model.roadmap.SpecialLevelType;
import io.github.Plants_Vs_Zombies_2.model.quest.Quest;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestCondition;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestPriority;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestReward;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestRewardType;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestRunSummary;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestType;
import io.github.Plants_Vs_Zombies_2.model.user.User;

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

    public static void main(String[] args) {
        ProjectSelfTestReport report = runAll();
        System.out.println(report.format());
        if (!report.isSuccessful()) {
            throw new IllegalStateException("project self-test failed");
        }
    }

    public static ProjectSelfTestReport runAll() {
        ProjectSelfTest test = new ProjectSelfTest();
        test.run("difficulty multipliers",
                test::checkDifficultyMultipliers);
        test.run("wave progression and exact budgets",
                test::checkWaveRules);
        test.run("boss levels are internally consistent",
                test::checkBossLevelConsistency);
        test.run("daily Scored Game is deterministic",
                test::checkDailyScoredGame);
        test.run("Scored Game disables cheats",
                test::checkScoredGameCheats);
        test.run("Timed War objectives count correctly",
                test::checkTimedWarSystems);
        test.run("Timed War level variants are playable",
                test::checkTimedWarVariants);
        test.run("phase-one quest catalog and daily rotation",
                test::checkQuestCatalogAndRotation);
        test.run("quest rewards are idempotent",
                test::checkQuestRewardIdempotency);
        test.run("quest JSON persistence round-trip",
                QuestPersistenceCheck::run);
        test.run("quest telemetry follows game events",
                test::checkQuestTelemetry);
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
                if (level.getKind() == LevelKind.BOSS) {
                    require(waves.size() == 1,
                            "every boss level must have exactly one boss wave");
                    require(waves.get(0).containsBoss(),
                            "boss wave must contain Zomboss");
                    continue;
                }
                require(waves.size() == 3,
                        "every non-boss level must have three waves");
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

    private void checkBossLevelConsistency() {
        for (Chapter chapter : ChapterCatalog.getChapters()) {
            int bossLevelCount = 0;
            for (Level level : chapter.getLevels()) {
                boolean isBossLevel = level.getKind() == LevelKind.BOSS;
                if (isBossLevel) {
                    bossLevelCount++;
                }
                for (ZombieWave wave : level.getZombieWaves()) {
                    for (ZombieType type : wave.getZombieTypes()) {
                        require(type.isBoss() == isBossLevel,
                                "boss zombies must only appear in boss levels: "
                                        + level.getName());
                    }
                }
            }
            require(bossLevelCount == 1,
                    chapter.getDisplayName()
                            + " must contain exactly one Zomboss finale");
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

        TimedWarSystem rollingSystem = TimedWarSystem.forZombieKills(30.0, 2);
        Zombie first = killedZombie(0);
        rollingSystem.recordZombieDeaths(List.of(first));
        rollingSystem.update(31.0, List.of(first), List.of());
        require(rollingSystem.getState() == TimedWarState.ACTIVE
                        && rollingSystem.getRecentZombieKills() == 0,
                "rolling kill window expired the whole objective");
        Zombie second = killedZombie(1);
        rollingSystem.recordZombieDeaths(List.of(second));
        require(rollingSystem.getState() == TimedWarState.ACTIVE,
                "rolling kill objective completed too early");
        Zombie third = killedZombie(2);
        rollingSystem.recordZombieDeaths(List.of(third));
        require(rollingSystem.getState() == TimedWarState.SUCCEEDED,
                "rolling kill objective did not complete later in the game");

        TimedWarSystem combined =
                TimedWarSystem.forZombieKillsAndCollectedSun(30.0, 1, 400);
        combined.recordZombieDeaths(List.of(killedZombie(3)));
        combined.recordCollectedSun(275);
        require(combined.getState() == TimedWarState.ACTIVE
                        && combined.getCollectedSunProgress() == 275,
                "combined Timed War ignored the sun requirement");
        combined.recordCollectedSun(125);
        require(combined.getState() == TimedWarState.SUCCEEDED,
                "combined Timed War did not accept collected sun");
        combined.update(31.0, List.of(), List.of());
        require(combined.getState() == TimedWarState.SUCCEEDED
                        && combined.getRecentZombieKills() == 0,
                "completed Timed War stopped updating its rolling HUD count");

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

    private static Zombie killedZombie(int lane) {
        Zombie zombie = new Zombie(
                ZombieType.BASIC, 1, lane, 8.0, false);
        zombie.kill();
        return zombie;
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
        require(base.getSpecialConfig().getTarget() == 5,
                "Frostbite Timed War kill target is not 5");
        require(base.getSpecialConfig().getMinimumCollectedSun() == 400,
                "Frostbite Timed War sun collection target is not 400");
        require(base.createGame(3, false).getBoard().getZombies().isEmpty(),
                "Frostbite Timed War starts with fixed frozen zombies");
        require(kills.getSpecialConfig().getTarget() == 5,
                "kill target is not 5");
        require(sun.getSpecialConfig().getTarget() == 200,
                "sun target is not 200");
        requireClose(30.0,
                kills.getSpecialConfig()
                        .getDurationSeconds());
        requireClose(60.0,
                sun.getSpecialConfig()
                        .getDurationSeconds());
    }

    private void checkQuestCatalogAndRotation() {
        User user = new User("quest-self-test", "Password1!",
                "Quest Tester", "quest@test.local", Gender.MALE);
        LocalDate firstDate = LocalDate.of(2030, 1, 1);
        user.getQuestProgress().ensureInitialized(user, firstDate);
        List<Quest> first = List.copyOf(
                user.getQuestProgress().getActiveQuests());
        require(first.size() == 19,
                "phase-one catalog must contain nineteen quests");
        require(first.stream().filter(
                quest -> quest.getType() == QuestType.DAILY).count() == 14,
                "daily quest count must be fourteen");
        require(first.stream().filter(
                quest -> quest.getType() == QuestType.MAIN).count() == 3,
                "main quest count must be three");
        require(first.stream().filter(
                quest -> quest.getType() == QuestType.EPIC).count() == 2,
                "epic quest count must be two");
        Set<String> ids = new HashSet<>();
        for (Quest quest : first) {
            require(ids.add(quest.getId()),
                    "quest ids must be unique");
        }

        Set<String> permanentIds = new HashSet<>();
        Set<String> firstDailyIds = new HashSet<>();
        for (Quest quest : first) {
            if (quest.getType() == QuestType.DAILY) {
                firstDailyIds.add(quest.getId());
            } else {
                permanentIds.add(quest.getId());
            }
        }
        user.getQuestProgress().ensureInitialized(
                user, firstDate.plusDays(1));
        List<Quest> second = user.getQuestProgress().getActiveQuests();
        require(second.size() == 19,
                "daily refresh changed catalog size");
        require(second.stream().filter(quest -> quest.getType() != QuestType.DAILY)
                .allMatch(quest -> permanentIds.contains(quest.getId())),
                "daily refresh replaced permanent quests");
        require(second.stream().filter(quest -> quest.getType() == QuestType.DAILY)
                .noneMatch(quest -> firstDailyIds.contains(quest.getId())),
                "daily refresh did not replace daily quests");
    }

    private void checkQuestRewardIdempotency() {
        User user = new User("quest-reward-test", "Password1!",
                "Reward Tester", "reward@test.local", Gender.FEMALE);
        Quest quest = Quest.restore("reward-test", "Reward test",
                "Already completed for the self-test.", QuestType.MAIN,
                QuestPriority.HIGH, QuestCondition.COLLECT_SUN, "", 1,
                new QuestReward(QuestRewardType.COINS, 50),
                1, true, false);
        int before = user.getCoins();
        quest.giveReward(user);
        quest.giveReward(user);
        require(user.getCoins() == before + 50,
                "quest reward was granted more than once");
        require(quest.isRewardGranted(),
                "quest did not persist reward-granted state");
    }

    private void checkQuestTelemetry() {
        Game game = new Game(new Board(), 500);
        BasePlant peashooter = PlantFactory.createPlant(
                "Peashooter", new EntityPosition(0, 0));
        require(game.plant(peashooter) == PlantPlacementResult.SUCCESS,
                "quest telemetry setup could not plant Peashooter");

        Sun plantSun = Sun.createPlantSun(
                50, new EntityPosition(1, 1));
        game.getBoard().addEntity(plantSun);
        require(game.collectSun(plantSun),
                "quest telemetry setup could not collect sun");

        Zombie zombie = game.spawnZombie("Basic", 8, 0);
        require(zombie != null,
                "quest telemetry setup could not spawn zombie");
        zombie.recordDamageSourcePlant("Peashooter");
        zombie.kill();
        game.update(0.0f);

        QuestRunSummary summary = game.createQuestRunSummary(
                "ancient-egypt");
        require(summary.getCollectedSun() == 50,
                "collected sun was not tracked");
        require(summary.getZombieKills() == 1,
                "zombie death was not tracked");
        require(summary.usedOnlyOffensivePlant("Peashooter"),
                "offensive plant usage was not tracked");
        require(!summary.wasRowNeverPlanted(1)
                        && !summary.wasColumnNeverPlanted(1),
                "plant position was not tracked");
        require("ancientegypt".equals(summary.getChapterId()),
                "chapter id was not normalized");
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
