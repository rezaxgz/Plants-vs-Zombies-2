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
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.ZombieType;
import model.game.gameTypes.GameType;

public class Game {
    private static final double TIME_EPSILON = 0.000001;

    private final Board board;
    private final GameType gameType;
    private final List<ZombieWave> zombieWaves;
    private final List<List<Zombie>> spawnedZombiesByWave;
    private final List<String> pendingResults = new ArrayList<>();
    private final Random random;

    private int sunCount;
    private int zombieWaveNumber;
    private int nextWaveIndex;
    private double elapsedSeconds;
    private double nextSkySunDropAtSeconds;
    private GameStatus status = GameStatus.ACTIVE;

    public Game() {
        this(new Board(), null, 0, Collections.emptyList());
    }

    public Game(Board board, int initialSunCount) {
        this(board, null, initialSunCount, Collections.emptyList());
    }

    public Game(Board board, GameType gameType, int initialSunCount, List<ZombieWave> zombieWaves) {
        this(board, gameType, initialSunCount, zombieWaves, new Random());
    }

    Game(Board board, GameType gameType, int initialSunCount,
            List<ZombieWave> zombieWaves, Random random) {
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
        this.zombieWaves = zombieWaves == null
                ? new ArrayList<>() : new ArrayList<>(zombieWaves);
        this.spawnedZombiesByWave = createWaveTracking(this.zombieWaves.size());
        this.random = random;
        this.nextSkySunDropAtSeconds = getSkySunDropIntervalSeconds(0.0);
        startNextWaveIfPossible();
    }

    private static List<List<Zombie>> createWaveTracking(int waveCount) {
        List<List<Zombie>> tracking = new ArrayList<>();
        for (int i = 0; i < waveCount; i++) {
            tracking.add(new ArrayList<>());
        }
        return tracking;
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

        board.update(deltaSeconds);
        pendingResults.addAll(board.drainResults());
        elapsedSeconds += deltaSeconds;

        if (hasZombieReachedHouse()) {
            loseGame();
            return;
        }

        startNextWaveIfPossible();
        checkForWin();
        if (status == GameStatus.ACTIVE) {
            updateSkySuns();
        }
    }

    private void updateSkySuns() {
        if (gameType != null && !gameType.spawnsSuns()) {
            return;
        }
        while (elapsedSeconds + TIME_EPSILON >= nextSkySunDropAtSeconds) {
            dropSkySun();
            nextSkySunDropAtSeconds += getSkySunDropIntervalSeconds(nextSkySunDropAtSeconds);
        }
    }

    private void startNextWaveIfPossible() {
        while (status == GameStatus.ACTIVE && nextWaveIndex < zombieWaves.size()
                && isPreviousWaveDamagedEnough()) {
            spawnWave(nextWaveIndex);
            nextWaveIndex++;
        }
    }

    private boolean isPreviousWaveDamagedEnough() {
        if (nextWaveIndex == 0) {
            return true;
        }
        List<Zombie> previousWave = spawnedZombiesByWave.get(nextWaveIndex - 1);
        long maximumHealth = 0;
        long remainingHealth = 0;
        for (Zombie zombie : previousWave) {
            maximumHealth += zombie.getMaximumHitPoints();
            remainingHealth += zombie.getHitPoints();
        }
        return maximumHealth > 0 && remainingHealth * 4 <= maximumHealth;
    }

    private void spawnWave(int waveIndex) {
        int waveNumber = waveIndex + 1;
        ZombieWave wave = zombieWaves.get(waveIndex);
        if (waveIndex == zombieWaves.size() - 1) {
            pendingResults.add("The final wave has come.");
        } else {
            pendingResults.add("Wave " + waveNumber + " started.");
        }

        double spawnColumn = board.getNumberOfColumns() - 0.001;
        List<Zombie> spawnedZombies = spawnedZombiesByWave.get(waveIndex);
        for (ZombieType zombieType : wave.getZombieTypes()) {
            int lane = random.nextInt(board.getNumberOfRows());
            Zombie zombie = new Zombie(zombieType, waveNumber, lane, spawnColumn);
            spawnedZombies.add(zombie);
            board.addZombie(zombie);
            pendingResults.add(buildSpawnMessage(zombie));
        }
        zombieWaveNumber = waveNumber;
    }

    private static String buildSpawnMessage(Zombie zombie) {
        return "Zombie " + zombie.getName() + " spawned at wave "
                + zombie.getWaveNumber() + " in lane " + zombie.getLane()
                + " which costed " + zombie.getWavePointCost() + ".";
    }

    private boolean hasZombieReachedHouse() {
        for (Zombie zombie : board.getZombies()) {
            if (zombie.hasReachedHouse()) {
                return true;
            }
        }
        return false;
    }

    private void checkForWin() {
        if (zombieWaves.isEmpty() || nextWaveIndex < zombieWaves.size()) {
            return;
        }
        for (List<Zombie> waveZombies : spawnedZombiesByWave) {
            for (Zombie zombie : waveZombies) {
                if (!zombie.isDead()) {
                    return;
                }
            }
        }
        status = GameStatus.WON;
        pendingResults.add("Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
    }

    private void loseGame() {
        status = GameStatus.LOST;
        pendingResults.add("The zombie ate your brain; LOSER!!!");
    }

    public void releaseNuke() {
        if (status != GameStatus.ACTIVE) {
            return;
        }
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            zombie.kill();
        }
        board.update(0.0f);
        pendingResults.addAll(board.drainResults());
        startNextWaveIfPossible();
        checkForWin();
    }

    private void dropSkySun() {
        SunType type = random.nextDouble() < Constants.SPECIAL_SKY_SUN_CHANCE
                ? SunType.SPECIAL : SunType.NORMAL;
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
        return status != GameStatus.ACTIVE
                || gameType != null && gameType.checkForSpecialGameEnd();
    }

    private static void validateDeltaSeconds(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
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

    public List<ZombieWave> getZombieWaves() {
        return Collections.unmodifiableList(zombieWaves);
    }

    public GameStatus getStatus() {
        return status;
    }
}
