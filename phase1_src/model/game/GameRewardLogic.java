package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import model.Constants;
import model.game.entities.EntityPosition;
import model.game.entities.other.Coin;
import model.game.entities.other.CollectibleDrop;
import model.game.entities.other.Diamond;
import model.game.entities.other.PlantFoodDrop;
import model.game.entities.other.PotDrop;
import model.game.entities.other.Sun;
import model.game.entities.other.SunType;
import model.game.entities.plants.BasePlant;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.ZombieType;
import model.game.gameTypes.GameType;
import model.user.User;

abstract class GameRewardLogic extends GameWaveLogic {
    protected GameRewardLogic(Board board, GameType gameType,
            int initialSunCount, List<ZombieWave> zombieWaves,
            Random random, boolean startWavesImmediately,
            ChapterRuleset chapterRuleset, int difficultyLevel) {
        super(board, gameType, initialSunCount, zombieWaves, random,
                startWavesImmediately, chapterRuleset, difficultyLevel);
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
        processZombieDeathDrops(zombieSnapshot);
        returnStolenSunFromDeadZombies(zombieSnapshot);
        returnCrystalSkullSunFromDeadZombies(zombieSnapshot);
        restoreWizardSheepFromDeadZombies(zombieSnapshot);
        pendingResults.addAll(board.drainResults());
        if (resolveTimedWar(0.0f)) {
            return;
        }
        startNextWaveIfPossible();
        checkForWin();
    }

    void dropSkySun() {
        double roll = random.nextDouble();
        SunType type;
        if (roll < Constants.RADIOACTIVE_SKY_SUN_CHANCE) {
            type = SunType.RADIOACTIVE;
        } else if (roll < Constants.RADIOACTIVE_SKY_SUN_CHANCE
                + Constants.SPECIAL_SKY_SUN_CHANCE) {
            type = SunType.SPECIAL;
        } else {
            type = SunType.NORMAL;
        }
        EntityPosition position = new EntityPosition(random.nextInt(board.getNumberOfRows()),
                random.nextInt(board.getNumberOfColumns()));
        board.addEntity(Sun.createSkySun(type, position));
        pendingResults.add("New " + type.getDisplayName() + " sun is dropping at position " + position);
    }

    void reportSunLandings() {
        for (Sun sun : board.getSuns()) {
            if (sun.consumeLandedEvent()) {
                pendingResults.add("Sun reached the ground at position "
                        + sun.getEntityPosition());
            }
        }
    }

    void processZombieDeathDrops(List<Zombie> zombies) {
        List<Zombie> newlyDead = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (zombie != null && zombie.isDead()
                    && !zombie.areDeathDropsProcessed()) {
                newlyDead.add(zombie);
            }
        }
        if (!newlyDead.isEmpty()) {
            onZombieDeaths(Collections.unmodifiableList(
                    new ArrayList<>(newlyDead)));
        }

        if (!shouldProcessZombieDeathDrops()) {
            for (Zombie zombie : newlyDead) {
                zombie.markDeathDropsProcessed();
            }
            return;
        }
        for (Zombie zombie : newlyDead) {
            zombie.markDeathDropsProcessed();
            EntityPosition position = getDropPosition(zombie);
            if (zombie.isGlowing()) {
                board.addEntity(new PlantFoodDrop(position));
                pendingResults.add("The glowing zombie dropped a plant food at "
                        + position + ".");
            }
            if (random.nextDouble()
                    < Constants.ZOMBIE_REWARD_DROP_CHANCE) {
                dropZombieReward(position);
            }
        }
    }

    EntityPosition getDropPosition(Zombie zombie) {
        int row = Math.max(0, Math.min(board.getNumberOfRows() - 1,
                zombie.getLane()));
        int column = Math.max(0, Math.min(board.getNumberOfColumns() - 1,
                (int) Math.floor(zombie.getColumnPosition())));
        return new EntityPosition(row, column);
    }

    void dropZombieReward(EntityPosition position) {
        int rewardType = random.nextInt(3);
        String rewardName;
        if (rewardType == 0) {
            board.addEntity(new Diamond(position));
            rewardName = "diamond";
        } else if (rewardType == 1) {
            board.addEntity(new Coin(position));
            rewardName = "coin";
        } else {
            board.addEntity(new PotDrop(position));
            rewardName = "pot";
        }
        pendingResults.add("A zombie dropped a " + rewardName + " at "
                + position + ".");
    }

    double getAdjustedSkySunDropIntervalSeconds(
            double timePassedSeconds) {
        return difficultyRules.scaleSkySunInterval(
                getSkySunDropIntervalSeconds(timePassedSeconds));
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

        if (sun.isRadioactive() && sun.isDropping()) {
            if (!sun.collectRadioactiveWhileDropping()) {
                return false;
            }
            board.removeEntity(sun);
            explodeRadioactiveSun(sun.getEntityPosition());
            return true;
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
        return collectSunsAt(position).getCollectedSunAmount();
    }

    public SunCollectionResult collectSunsAt(EntityPosition position) {
        int collectedAmount = 0;
        int collectedCount = 0;
        int explosionCount = 0;
        List<Sun> sunsAtPosition = new ArrayList<>(board.getSunsAt(position));
        for (Sun sun : sunsAtPosition) {
            int amount = sun.getSunAmount();
            boolean radioactiveExplosion = sun.isRadioactive() && sun.isDropping();
            if (collectSun(sun)) {
                collectedCount++;
                collectedAmount += amount;
                if (radioactiveExplosion) {
                    explosionCount++;
                }
            }
        }
        return new SunCollectionResult(collectedCount, collectedAmount,
                explosionCount);
    }

    void explodeRadioactiveSun(EntityPosition position) {
        List<Zombie> zombieSnapshot = new ArrayList<>(board.getZombies());
        int damagedZombies = 0;
        int damagedPlants = 0;
        for (Zombie zombie : zombieSnapshot) {
            if (isWithinArea(position, zombie.getLane(),
                    (int) Math.floor(zombie.getColumnPosition()), 2)) {
                zombie.takeDamage(Constants.RADIOACTIVE_SUN_ZOMBIE_DAMAGE);
                damagedZombies++;
            }
        }
        for (BasePlant plant : new ArrayList<>(board.getPlants())) {
            EntityPosition plantPosition = plant.getEntityPosition();
            if (plantPosition != null && isWithinArea(position,
                    plantPosition.getRow(), plantPosition.getColumn(), 1)) {
                plant.takeDamage(Constants.RADIOACTIVE_SUN_PLANT_DAMAGE);
                damagedPlants++;
            }
        }
        board.update(0.0f);
        processZombieDeathDrops(zombieSnapshot);
        returnStolenSunFromDeadZombies(zombieSnapshot);
        returnCrystalSkullSunFromDeadZombies(zombieSnapshot);
        restoreWizardSheepFromDeadZombies(zombieSnapshot);
        pendingResults.addAll(board.drainResults());
        pendingResults.add("Radioactive sun exploded at " + position
                + ", damaging " + damagedZombies + " zombies and "
                + damagedPlants + " plants.");
    }

    public int collectPlantFoodDropsAt(EntityPosition position) {
        int collected = 0;
        for (CollectibleDrop drop : new ArrayList<>(
                board.getCollectibleDropsAt(position))) {
            if (!(drop instanceof PlantFoodDrop)
                    || plantFoodCount >= MAX_PLANT_FOOD) {
                continue;
            }
            if (drop.collect()) {
                board.removeEntity(drop);
                plantFoodCount++;
                collected++;
            }
        }
        return collected;
    }

    public RewardCollectionResult collectRewardDropsAt(EntityPosition position,
            User user) {
        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }
        int dropCount = 0;
        int coins = 0;
        int diamonds = 0;
        int pots = 0;
        for (CollectibleDrop drop : new ArrayList<>(
                board.getCollectibleDropsAt(position))) {
            boolean isReward = drop instanceof Coin
                    || drop instanceof Diamond
                    || drop instanceof PotDrop;
            if (!isReward || !drop.collect()) {
                continue;
            }
            board.removeEntity(drop);
            dropCount++;
            if (drop instanceof Coin) {
                coins += Coin.AMOUNT;
                user.addCoins(Coin.AMOUNT);
            } else if (drop instanceof Diamond) {
                diamonds += Diamond.AMOUNT;
                user.addDiamonds(Diamond.AMOUNT);
            } else if (drop instanceof PotDrop) {
                pots++;
                user.addPots(1);
            }
        }
        return new RewardCollectionResult(dropCount, coins, diamonds, pots);
    }

    public boolean addPlantFood() {
        if (plantFoodCount >= MAX_PLANT_FOOD) {
            return false;
        }
        plantFoodCount++;
        return true;
    }

    public void loadStartingPlantFood(int count) {
        if (count < 0 || count > MAX_PLANT_FOOD) {
            throw new IllegalArgumentException(
                    "starting plant food must be between 0 and 3");
        }
        plantFoodCount = count;
        if (count > 0) {
            pendingResults.add("Started the level with " + count
                    + " stored plant food" + (count == 1 ? "." : "s."));
        }
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

    public Zombie spawnZombie(String requestedType, int column, int row) {
        ZombieType type = ZombieType.findByName(requestedType);
        if (type == null || type.isBoss()) {
            return null;
        }
        if (row < 0 || row >= board.getNumberOfRows()
                || column < 0 || column >= board.getNumberOfColumns()) {
            throw new IllegalArgumentException("zombie position is outside the board");
        }
        boolean glowing = random.nextDouble()
                < Constants.GLOWING_ZOMBIE_CHANCE;
        Zombie zombie = new Zombie(type, Math.max(1, zombieWaveNumber),
                row, column, glowing);
        applyDifficultyToZombie(zombie);
        board.addZombie(zombie);
        onZombieSpawned(zombie);
        pendingResults.add("Zombie " + zombie.getName()
                + " spawned by cheat at (" + column + ", " + row + ").");
        return zombie;
    }

    void applyDifficultyToZombie(Zombie zombie) {
        if (zombie != null) {
            zombie.applyDifficulty(difficultyRules.getLevel());
        }
    }

    public int getDifficultyLevel() {
        return difficultyRules.getLevel();
    }
}
