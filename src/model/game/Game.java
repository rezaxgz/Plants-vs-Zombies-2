package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import model.Constants;
import model.game.entities.EntityPosition;
import model.game.entities.other.Sun;
import model.game.entities.other.SunType;
import model.game.entities.plants.BasePlant;
import model.game.gameTypes.GameType;

public class Game {
    private final Board board;
    private final GameType gameType;
    private int sunCount;
    private int zombieWaveNumber;
    private final List<ZombieWave> zombieWaves;
    private final List<String> pendingResults = new ArrayList<>();
    private final Random random;
    private double elapsedSeconds;
    private double nextSkySunDropAtSeconds;

    public Game() {
        this(new Board(), null, 0, Collections.emptyList());
    }

    public Game(Board board, int initialSunCount) {
        this(board, null, initialSunCount, Collections.emptyList());
    }

    public Game(Board board, GameType gameType, int initialSunCount, List<ZombieWave> zombieWaves) {
        this(board, gameType, initialSunCount, zombieWaves, new Random());
    }

    Game(Board board, GameType gameType, int initialSunCount, List<ZombieWave> zombieWaves, Random random) {
        if (board == null) {
            throw new IllegalArgumentException("board cannot be null");
        }
        if (initialSunCount < 0) {
            throw new IllegalArgumentException("initialSunCount cannot be negative");
        }
        if (random == null) {
            throw new IllegalArgumentException("random cannot be null");
        }

        this.board = board;
        this.gameType = gameType;
        this.sunCount = initialSunCount;
        this.zombieWaves = zombieWaves == null ? new ArrayList<>() : new ArrayList<>(zombieWaves);
        this.random = random;
        this.nextSkySunDropAtSeconds = getSkySunDropIntervalSeconds(0.0);
    }

    /**
     * Terminal/game-loop entry point. A tick is converted to seconds here and every
     * model update below this point works only with seconds.
     */
    public final void tick() {
        update(Constants.ONE_TICK_IN_SECONDS);
    }

    public void advanceTicks(int tickCount) {
        if (tickCount < 0) {
            throw new IllegalArgumentException("tickCount cannot be negative");
        }
        for (int i = 0; i < tickCount; i++) {
            tick();
        }
    }

    public void update(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }

        board.update(deltaSeconds);
        pendingResults.addAll(board.drainResults());
        elapsedSeconds += deltaSeconds;

        while (elapsedSeconds + 0.000001 >= nextSkySunDropAtSeconds) {
            dropSkySun();
            nextSkySunDropAtSeconds += getSkySunDropIntervalSeconds(nextSkySunDropAtSeconds);
        }
    }

    private void dropSkySun() {
        SunType type = random.nextDouble() < Constants.SPECIAL_SKY_SUN_CHANCE ? SunType.SPECIAL : SunType.NORMAL;
        EntityPosition position = new EntityPosition(random.nextInt(board.getNumberOfRows()),
                random.nextInt(board.getNumberOfColumns()));
        board.addEntity(Sun.createSkySun(type, position));
        pendingResults.add("New " + type.getDisplayName() + " sun is dropping at position " + position);
    }

    public static double getSkySunDropIntervalSeconds(double timePassedSeconds) {
        if (!Double.isFinite(timePassedSeconds) || timePassedSeconds < 0.0) {
            throw new IllegalArgumentException("timePassedSeconds must be finite and non-negative");
        }
        return Math.max(6.0 + 0.05 * timePassedSeconds, 12.0);
    }

    public List<String> drainResults() {
        if (pendingResults.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>(pendingResults);
        pendingResults.clear();
        return Collections.unmodifiableList(results);
    }

    public boolean collectSun(Sun sun) {
        if (!board.containsEntity(sun)) {
            return false;
        }

        int collectedAmount = sun.collect();
        if (collectedAmount <= 0) {
            return false;
        }

        sunCount += collectedAmount;
        board.removeEntity(sun);
        return true;
    }

    public int collectSunAt(int row, int column) {
        return collectSunAt(new EntityPosition(row, column));
    }

    public int collectSunAt(EntityPosition position) {
        int collectedAmount = 0;
        List<Sun> sunsAtPosition = new ArrayList<>(board.getSunsAt(position));
        for (Sun sun : sunsAtPosition) {
            int amount = sun.getSunAmount();
            if (collectSun(sun)) {
                collectedAmount += amount;
            }
        }
        return collectedAmount;
    }

    public void addSun(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (sunCount > Integer.MAX_VALUE - amount) {
            throw new IllegalArgumentException("sun total is too large");
        }
        sunCount += amount;
    }

    public PlantPlacementResult plant(BasePlant plant) {
        if (plant == null || !board.isPositionInsideBoard(plant.getEntityPosition())) {
            return PlantPlacementResult.INVALID_POSITION;
        }
        if (board.getPlantAt(plant.getEntityPosition()) != null) {
            return PlantPlacementResult.POSITION_OCCUPIED;
        }
        if (sunCount < plant.getCost()) {
            return PlantPlacementResult.NOT_ENOUGH_SUN;
        }
        if (!board.addPlant(plant)) {
            return PlantPlacementResult.POSITION_OCCUPIED;
        }

        sunCount -= plant.getCost();
        return PlantPlacementResult.SUCCESS;
    }

    public BasePlant pluckPlantAt(EntityPosition position) {
        if (!board.isPositionInsideBoard(position)) {
            return null;
        }
        return board.removePlantAt(position);
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
        return gameType != null && gameType.checkForSpecialGameEnd();
    }

    public Board getBoard() {
        return board;
    }

    public GameType getGameType() {
        return gameType;
    }

    public int getSunCount() {
        return sunCount;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getZombieWaveNumber() {
        return zombieWaveNumber;
    }

    public void setZombieWaveNumber(int zombieWaveNumber) {
        if (zombieWaveNumber < 0) {
            throw new IllegalArgumentException("zombieWaveNumber cannot be negative");
        }
        this.zombieWaveNumber = zombieWaveNumber;
    }

    public List<ZombieWave> getZombieWaves() {
        return Collections.unmodifiableList(zombieWaves);
    }
}
