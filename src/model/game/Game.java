package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import model.Constants;
import model.game.entities.EntityPosition;
import model.game.entities.other.Sun;
import model.game.entities.other.SunType;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantFamily;
import model.game.entities.plants.modifier.Modifier;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.ZombieType;
import model.game.entities.zombies.abilities.FishingHookAbility;
import model.game.entities.zombies.abilities.ImpThrowAbility;
import model.game.entities.zombies.abilities.KingBuffAbility;
import model.game.entities.zombies.abilities.LaserBeamAbility;
import model.game.entities.zombies.abilities.OctopusThrowAbility;
import model.game.entities.zombies.abilities.SnowballThrowAbility;
import model.game.entities.zombies.abilities.SunStealAbility;
import model.game.entities.zombies.abilities.TombSummonAbility;
import model.game.entities.zombies.abilities.WeaselReleaseAbility;
import model.game.entities.zombies.abilities.WizardSpellAbility;
import model.game.entities.zombies.abilities.ZombieAbility;
import model.game.gameTypes.GameType;

public class Game {
    private static final double TIME_EPSILON = 0.000001;
    private static final int MAX_PLANT_FOOD = 3;

    private final Board board;
    private final GameType gameType;
    private final List<ZombieWave> zombieWaves;
    private final List<List<Zombie>> spawnedZombiesByWave;
    private final List<String> pendingResults = new ArrayList<>();
    private final Map<String, Double> plantCooldowns = new HashMap<>();
    private final Map<String, PlantFamily> plantCooldownFamilies = new HashMap<>();
    private final Random random;

    private int sunCount;
    private int plantFoodCount;
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
                ? new ArrayList<>()
                : new ArrayList<>(zombieWaves);
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

        updatePlantCooldowns(deltaSeconds);
        List<Zombie> zombieSnapshot = new ArrayList<>(board.getZombies());
        board.update(deltaSeconds);
        trackBoardSpawnedZombies();
        activateAutomaticZombieAbilities(zombieSnapshot);
        returnStolenSunFromDeadZombies(zombieSnapshot);
        returnCrystalSkullSunFromDeadZombies(zombieSnapshot);
        restoreWizardSheepFromDeadZombies(zombieSnapshot);
        applyPlantCooldownResetRequests();
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

    private void activateAutomaticZombieAbilities(List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            for (ZombieAbility ability : zombie.getAbilities()) {
                activateWeaselRelease(zombie, ability);
            }
            if (zombie.isDead() || zombie.isHypnotized()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                activateImpThrow(zombie, ability);
                activateSunSteal(zombie, ability);
                activateTombSummon(zombie, ability);
                activateSnowballThrow(zombie, ability);
                activateFishingHook(zombie, ability);
                activateOctopusThrow(zombie, ability);
                activateWizardSpell(zombie, ability);
                activateKingBuff(zombie, ability);
                activateCrystalSkull(zombie, ability);
            }
        }
    }

    private void activateCrystalSkull(Zombie crystalSkull,
            ZombieAbility ability) {
        if (!(ability instanceof LaserBeamAbility)) {
            return;
        }

        LaserBeamAbility laser = (LaserBeamAbility) ability;
        boolean stateChanged = laser.tryUse(crystalSkull, board);
        int requestedSun = laser.drainPendingSunRequest();
        int stolenSun = Math.min(sunCount, requestedSun);
        if (stolenSun > 0) {
            spendSun(stolenSun);
            laser.recordStolenSun(stolenSun);
            pendingResults.add(crystalSkull.getName() + " stole "
                    + stolenSun + " stored sun while charging.");
        }

        if (stateChanged && laser.didStartChargingThisUse()) {
            pendingResults.add(crystalSkull.getName()
                    + " started charging its skull laser.");
        }
        if (stateChanged && laser.didFireThisUse()) {
            pendingResults.add(crystalSkull.getName()
                    + " fired its laser and destroyed "
                    + laser.getLastDestroyedPlantCount()
                    + " plant(s).");
        }
    }

    private void activateWizardSpell(Zombie wizard,
            ZombieAbility ability) {
        if (!(ability instanceof WizardSpellAbility)
                || !ability.tryUse(wizard, board)) {
            return;
        }
        BasePlant target =
                ((WizardSpellAbility) ability).getLastTarget();
        if (target != null) {
            pendingResults.add(wizard.getName() + " transformed "
                    + target.getName() + " into a sheep at "
                    + target.getEntityPosition() + ".");
        }
    }

    private void activateKingBuff(Zombie king,
            ZombieAbility ability) {
        if (!(ability instanceof KingBuffAbility)
                || !ability.tryUse(king, board)) {
            return;
        }
        Zombie target =
                ((KingBuffAbility) ability).getLastKnightedZombie();
        if (target != null) {
            pendingResults.add(king.getName() + " knighted "
                    + target.getName() + " with crown armor.");
        }
    }

    private void activateFishingHook(Zombie fisherman,
            ZombieAbility ability) {
        if (!(ability instanceof FishingHookAbility)
                || !ability.tryUse(fisherman, board)) {
            return;
        }
        FishingHookAbility hook = (FishingHookAbility) ability;
        BasePlant target = hook.getLastTarget();
        if (target == null) {
            return;
        }
        if (hook.wasLastTargetDestroyed()) {
            pendingResults.add(fisherman.getName() + " hooked and destroyed "
                    + target.getName() + " beside the right edge.");
        } else {
            pendingResults.add(fisherman.getName() + " pulled "
                    + target.getName() + " from " + hook.getLastFromPosition()
                    + " to " + hook.getLastToPosition() + ".");
        }
    }

    private void activateOctopusThrow(Zombie octopusZombie,
            ZombieAbility ability) {
        if (!(ability instanceof OctopusThrowAbility)
                || !ability.tryUse(octopusZombie, board)) {
            return;
        }
        BasePlant target =
                ((OctopusThrowAbility) ability).getLastTarget();
        if (target != null) {
            pendingResults.add(octopusZombie.getName()
                    + " covered " + target.getName()
                    + " with an octopus at "
                    + target.getEntityPosition() + ".");
        }
    }

    private void activateSnowballThrow(Zombie hunter, ZombieAbility ability) {
        if (!(ability instanceof SnowballThrowAbility)
                || !ability.tryUse(hunter, board)) {
            return;
        }
        SnowballThrowAbility snowball = (SnowballThrowAbility) ability;
        BasePlant target = snowball.getLastTarget();
        if (target == null) {
            return;
        }
        pendingResults.add(hunter.getName() + " hit " + target.getName()
                + " with " + snowball.getLastSnowballCount() + " snowball(s)."
                + (snowball.didLastBarrageFreezeTarget()
                        ? " The plant is now frozen." : ""));
    }

    private void activateWeaselRelease(Zombie hoarder, ZombieAbility ability) {
        if (!(ability instanceof WeaselReleaseAbility)
                || !ability.tryUse(hoarder, board)) {
            return;
        }
        WeaselReleaseAbility release = (WeaselReleaseAbility) ability;
        for (Zombie weasel : release.getLastSpawnedWeasels()) {
            trackSpawnedZombie(weasel);
        }
        pendingResults.add(hoarder.getName() + " released "
                + release.getLastSpawnedWeasels().size() + " weasel(s).");
    }

    private void activateImpThrow(Zombie gargantuar, ZombieAbility ability) {
        if (!(ability instanceof ImpThrowAbility)
                || !ability.tryUse(gargantuar, board)) {
            return;
        }

        ImpThrowAbility impThrow = (ImpThrowAbility) ability;
        Zombie imp = impThrow.getSpawnedImp();
        if (imp == null) {
            return;
        }

        trackSpawnedZombie(imp);
        pendingResults.add(gargantuar.getName() + " threw "
                + imp.getName() + " into lane " + imp.getLane()
                + " at column "
                + String.format(Locale.ROOT, "%.0f", imp.getColumnPosition())
                + ".");
    }

    private void activateSunSteal(Zombie raZombie, ZombieAbility ability) {
        if (!(ability instanceof SunStealAbility)
                || !ability.tryUse(raZombie, board)) {
            return;
        }
        SunStealAbility sunSteal = (SunStealAbility) ability;
        pendingResults.add(raZombie.getName() + " pulled and stole "
                + sunSteal.getLastStolenAmount() + " sun.");
    }

    private void activateTombSummon(Zombie tombRaiser, ZombieAbility ability) {
        if (!(ability instanceof TombSummonAbility)
                || !ability.tryUse(tombRaiser, board)) {
            return;
        }
        TombSummonAbility tombSummon = (TombSummonAbility) ability;
        pendingResults.add(tombRaiser.getName() + " raised "
                + tombSummon.getLastSpawnedCount() + " grave(s) at "
                + tombSummon.getLastSpawnedPositions() + ".");
    }

    private void returnStolenSunFromDeadZombies(List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                if (!(ability instanceof SunStealAbility)) {
                    continue;
                }
                int returnedSun = ((SunStealAbility) ability).releaseStolenSun();
                if (returnedSun > 0) {
                    addSun(returnedSun);
                    pendingResults.add(returnedSun + " stolen sun returned after "
                            + zombie.getName() + " died.");
                }
            }
        }
    }

    private void returnCrystalSkullSunFromDeadZombies(
            List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                if (!(ability instanceof LaserBeamAbility)) {
                    continue;
                }
                int droppedSun =
                        ((LaserBeamAbility) ability)
                                .releaseHalfStolenSun();
                if (droppedSun <= 0) {
                    continue;
                }
                EntityPosition position = new EntityPosition(
                        zombie.getLane(),
                        Math.max(0, Math.min(
                                board.getNumberOfColumns() - 1,
                                (int) Math.floor(
                                        zombie.getColumnPosition()))));
                board.addEntity(Sun.createPlantSun(
                        droppedSun, position));
                pendingResults.add(zombie.getName() + " dropped "
                        + droppedSun + " stolen sun on death.");
            }
        }
    }

    private void restoreWizardSheepFromDeadZombies(
            List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                continue;
            }
            for (ZombieAbility ability : zombie.getAbilities()) {
                if (!(ability instanceof WizardSpellAbility)) {
                    continue;
                }
                int restored =
                        ((WizardSpellAbility) ability)
                                .restoreTransformedPlants();
                if (restored > 0) {
                    pendingResults.add(restored
                            + " sheep plant(s) returned to normal after "
                            + zombie.getName() + " died.");
                }
            }
        }
    }

    private void trackBoardSpawnedZombies() {
        for (Zombie zombie : board.drainSpawnedZombies()) {
            trackSpawnedZombie(zombie);
        }
    }

    private void trackSpawnedZombie(Zombie zombie) {
        int waveIndex = zombie.getWaveNumber() - 1;
        if (waveIndex < 0 || waveIndex >= spawnedZombiesByWave.size()) {
            return;
        }
        List<Zombie> waveZombies = spawnedZombiesByWave.get(waveIndex);
        if (!waveZombies.contains(zombie)) {
            waveZombies.add(zombie);
        }
    }

    private void updatePlantCooldowns(float deltaSeconds) {
        plantCooldowns.replaceAll((name, remaining) ->
                Math.max(0.0, remaining - deltaSeconds));
        List<String> expiredKeys = new ArrayList<>();
        for (Map.Entry<String, Double> entry : plantCooldowns.entrySet()) {
            if (entry.getValue() <= TIME_EPSILON) {
                expiredKeys.add(entry.getKey());
            }
        }
        for (String key : expiredKeys) {
            plantCooldowns.remove(key);
            plantCooldownFamilies.remove(key);
        }
    }

    private void applyPlantCooldownResetRequests() {
        for (PlantFamily family : board.drainPlantCooldownResetRequests()) {
            List<String> resetKeys = new ArrayList<>();
            for (Map.Entry<String, PlantFamily> entry : plantCooldownFamilies.entrySet()) {
                if (entry.getValue() == family) {
                    resetKeys.add(entry.getKey());
                }
            }
            for (String key : resetKeys) {
                plantCooldowns.remove(key);
                plantCooldownFamilies.remove(key);
            }
        }
    }

    private static String getCooldownKey(BasePlant plant) {
        if (plant instanceof Modifier && ((Modifier) plant).isImitater()) {
            return "imitater";
        }
        return plant.getName().trim().toLowerCase(Locale.ROOT);
    }

    private double getPlantCooldownRemaining(BasePlant plant) {
        return plantCooldowns.getOrDefault(getCooldownKey(plant), 0.0);
    }

    private void startPlantCooldown(BasePlant plant) {
        double rechargeSeconds = Math.max(0.0, plant.getRechargeSeconds());
        if (rechargeSeconds > TIME_EPSILON) {
            String key = getCooldownKey(plant);
            plantCooldowns.put(key, rechargeSeconds);
            PlantFamily family = PlantFamily.findForPlant(plant);
            if (family == null) {
                plantCooldownFamilies.remove(key);
            } else {
                plantCooldownFamilies.put(key, family);
            }
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
            if (!zombie.isHypnotized()) {
                remainingHealth += zombie.getHitPoints();
            }
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
                + " which costed " + zombie.getType().getWavePointCost() + ".";
    }

    private boolean hasZombieReachedHouse() {
        for (Zombie zombie : board.getZombies()) {
            if (!zombie.isHypnotized() && zombie.hasReachedHouse()) {
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
                if (!zombie.isDead() && !zombie.isHypnotized()) {
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
        List<Zombie> zombieSnapshot = new ArrayList<>(board.getZombies());
        for (Zombie zombie : zombieSnapshot) {
            zombie.kill();
        }
        board.update(0.0f);
        returnStolenSunFromDeadZombies(zombieSnapshot);
        returnCrystalSkullSunFromDeadZombies(zombieSnapshot);
        restoreWizardSheepFromDeadZombies(zombieSnapshot);
        pendingResults.addAll(board.drainResults());
        startNextWaveIfPossible();
        checkForWin();
    }

    private void dropSkySun() {
        SunType type = random.nextDouble() < Constants.SPECIAL_SKY_SUN_CHANCE
                ? SunType.SPECIAL
                : SunType.NORMAL;
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

    public boolean addPlantFood() {
        if (plantFoodCount >= MAX_PLANT_FOOD) {
            return false;
        }
        plantFoodCount++;
        return true;
    }

    public PlantFoodResult feedPlantAt(EntityPosition position) {
        if (plantFoodCount <= 0) {
            return PlantFoodResult.NO_PLANT_FOOD;
        }
        PlantFoodResult result = board.usePlantFoodAt(position);
        if (result == PlantFoodResult.SUCCESS) {
            plantFoodCount--;
        }
        return result;
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
        if (!board.canAddPlant(plant)) {
            return PlantPlacementResult.POSITION_OCCUPIED;
        }
        if (sunCount < plant.getCost()) {
            return PlantPlacementResult.NOT_ENOUGH_SUN;
        }
        if (getPlantCooldownRemaining(plant) > TIME_EPSILON) {
            return PlantPlacementResult.COOLDOWN_ACTIVE;
        }
        if (!board.addPlant(plant)) {
            return PlantPlacementResult.POSITION_OCCUPIED;
        }

        sunCount -= plant.getCost();
        startPlantCooldown(plant);
        return PlantPlacementResult.SUCCESS;
    }

    public BasePlant pluckPlantAt(EntityPosition position) {
        if (!board.isPositionInsideBoard(position)) {
            return null;
        }
        return board.removePlantAt(position);
    }

    public double getPlantCooldownRemainingSeconds(BasePlant plant) {
        if (plant == null) {
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
}
