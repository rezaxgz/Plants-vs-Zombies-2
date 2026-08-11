package io.github.Plants_Vs_Zombies_2.model.game.scored;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

import io.github.Plants_Vs_Zombies_2.model.game.ZombieWave;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;

/**
 * Builds the same UTC-dated scored challenge for every user.
 */
public final class DailyScoredGameFactory {
    private static final int FIXED_DIFFICULTY = 3;
    private static final long SEED_SALT = 20_260_721L;
    private static final long GAME_RANDOM_SALT = 0x5DEECE66DL;

    private static final List<ZombieType> DAILY_POOL = List.of(
            ZombieType.BASIC,
            ZombieType.CONEHEAD,
            ZombieType.BUCKETHEAD,
            ZombieType.NEWSPAPER,
            ZombieType.IMP,
            ZombieType.MUMMY,
            ZombieType.MUMMY_CONEHEAD,
            ZombieType.MUMMY_BUCKETHEAD,
            ZombieType.RA,
            ZombieType.EXPLORER,
            ZombieType.DARK,
            ZombieType.DARK_CONEHEAD,
            ZombieType.DARK_BUCKETHEAD,
            ZombieType.JUGGLER);

    private DailyScoredGameFactory() {
    }

    public static ScoredGame create() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        long seed = date.toEpochDay()
                * 1_000_003L + SEED_SALT;
        Random waveRandom = new Random(seed);
        List<ZombieWave> waves = List.of(
                ZombieWave.seededWave(
                        DAILY_POOL, 400, false,
                        FIXED_DIFFICULTY, waveRandom),
                ZombieWave.seededWave(
                        DAILY_POOL, 500, false,
                        FIXED_DIFFICULTY, waveRandom),
                ZombieWave.seededWave(
                        DAILY_POOL, 1000, true,
                        FIXED_DIFFICULTY, waveRandom));
        Random gameRandom = new Random(seed ^ GAME_RANDOM_SALT);
        return new ScoredGame(
                date, seed, waves, gameRandom);
    }
}
