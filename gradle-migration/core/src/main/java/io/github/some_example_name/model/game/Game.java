package io.github.some_example_name.model.game;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.github.some_example_name.model.game.gameTypes.GameType;

public class Game extends GamePlantingLogic {
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

    protected Game(Board board, GameType gameType,
            int initialSunCount,
            List<ZombieWave> zombieWaves,
            Random random,
            boolean startWavesImmediately,
            ChapterRuleset chapterRuleset,
            int difficultyLevel) {
        super(board, gameType, initialSunCount, zombieWaves, random,
                startWavesImmediately, chapterRuleset, difficultyLevel);
        startNextWaveIfPossible();
    }
}
