package io.github.Plants_Vs_Zombies_2.model.game.minigame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.Game;
import io.github.Plants_Vs_Zombies_2.model.game.GameStatus;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFactory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/**
 * Reverse-defense minigame: the player buys zombies and eats five brains.
 */
public final class IZombie extends Game {
    public static final int INITIAL_SUN = 150;
    public static final int PRODUCED_SUN_AMOUNT = 25;

    private static final double FIRST_PRODUCTION_SECONDS = 8.0;
    private static final double PRODUCTION_ROW_DELAY_SECONDS = 2.0;
    private static final double INITIAL_PRODUCTION_INTERVAL_SECONDS = 16.0;
    private static final double MINIMUM_PRODUCTION_INTERVAL_SECONDS = 4.0;
    private static final double ACCELERATION_SECONDS = 30.0;
    private static final double OCCUPANCY_RADIUS = 0.45;

    private final IZombieLevel level;
    private final Random random;
    private final List<IZombieSunProducer> sunProducers;
    private final double[] nextSunProductionAt;
    private final boolean[] brainsAvailable;

    public IZombie(IZombieLevel level) {
        this(level, new Random());
    }

    IZombie(IZombieLevel level, Random random) {
        super(new Board(), null, INITIAL_SUN,
                Collections.emptyList(), false);
        if (level == null || random == null) {
            throw new IllegalArgumentException(
                    "I, Zombie level and random source are required");
        }
        this.level = level;
        this.random = random;
        this.sunProducers = new ArrayList<>();
        this.nextSunProductionAt = new double[getBoard().getNumberOfRows()];
        this.brainsAvailable = new boolean[getBoard().getNumberOfRows()];
        disableSkySuns("I, Zombie uses zombie-produced sun");
        initializeBrains();
        placeRandomPlants();
        placeSunProducers();
        addPendingResult("I, Zombie level " + level.getNumber()
                + " started: " + level.getName() + ".");
        addPendingResult("Spend the initial 150 sun to place zombies on "
                + "the right side of the red line and eat all five brains.");
    }

    @Override
    public void update(float deltaSeconds) {
        if (getStatus() != GameStatus.ACTIVE) {
            return;
        }
        super.update(deltaSeconds);
        if (getStatus() != GameStatus.ACTIVE) {
            return;
        }
        produceZombieSun();
        consumeReachedBrains();
        evaluateEndConditions();
    }

    @Override
    public void releaseNuke() {
        super.releaseNuke();
        if (getStatus() == GameStatus.ACTIVE) {
            evaluateEndConditions();
        }
    }

    private void initializeBrains() {
        for (int row = 0; row < brainsAvailable.length; row++) {
            brainsAvailable[row] = true;
        }
    }

    private void placeRandomPlants() {
        List<EntityPosition> selected = selectPlantPositions();
        List<String> pool = level.getPlantPool();
        for (EntityPosition position : selected) {
            String type = pool.get(random.nextInt(pool.size()));
            BasePlant plant = PlantFactory.createPlant(type, position);
            if (plant == null || !getBoard().addPlant(plant)) {
                throw new IllegalStateException(
                        "could not place I, Zombie plant at " + position);
            }
        }
    }

    private List<EntityPosition> selectPlantPositions() {
        List<EntityPosition> allPositions = new ArrayList<>();
        for (int row = 0; row < getBoard().getNumberOfRows(); row++) {
            for (int column = 0; column <= level.getRedLineColumn(); column++) {
                allPositions.add(new EntityPosition(row, column));
            }
        }
        Collections.shuffle(allPositions, random);
        if (level.getPlantCount() > allPositions.size()) {
            throw new IllegalStateException(
                    "not enough left-side cells for I, Zombie plants");
        }
        return new ArrayList<>(
                allPositions.subList(0, level.getPlantCount()));
    }

    private void placeSunProducers() {
        double column = getBoard().getNumberOfColumns() - 0.25;
        for (int row = 0; row < getBoard().getNumberOfRows(); row++) {
            IZombieSunProducer producer = new IZombieSunProducer(row, column);
            sunProducers.add(producer);
            getBoard().addZombie(producer);
            nextSunProductionAt[row] = FIRST_PRODUCTION_SECONDS
                    + row * PRODUCTION_ROW_DELAY_SECONDS;
        }
        addPendingResult("One irreplaceable sun-producer zombie was placed "
                + "in every row. Each has buckethead durability.");
    }

    private void produceZombieSun() {
        double now = getElapsedSeconds();
        for (int row = 0; row < sunProducers.size(); row++) {
            IZombieSunProducer producer = sunProducers.get(row);
            if (!isActive(producer)) {
                continue;
            }
            int produced = 0;
            while (now >= nextSunProductionAt[row]) {
                produced += PRODUCED_SUN_AMOUNT;
                nextSunProductionAt[row] += getCurrentProductionIntervalSeconds();
            }
            if (produced > 0) {
                addSun(produced);
                addPendingResult("Sun producer in row " + row
                        + " generated " + produced + " zombie sun.");
            }
        }
    }

    public double getCurrentProductionIntervalSeconds() {
        return Math.max(MINIMUM_PRODUCTION_INTERVAL_SECONDS,
                INITIAL_PRODUCTION_INTERVAL_SECONDS
                        - getElapsedSeconds() / ACCELERATION_SECONDS);
    }

    private void consumeReachedBrains() {
        for (Zombie zombie : new ArrayList<>(getBoard().getZombies())) {
            if (zombie instanceof IZombieSunProducer
                    || !isActive(zombie)
                    || !zombie.hasReachedHouse()) {
                continue;
            }
            int row = zombie.getLane();
            if (row >= 0 && row < brainsAvailable.length
                    && brainsAvailable[row]) {
                brainsAvailable[row] = false;
                addPendingResult(zombie.getName()
                        + " ate the brain in row " + row + ".");
            }
            zombie.markForRemoval();
        }
    }

    private void evaluateEndConditions() {
        if (allBrainsEaten()) {
            completeGameAsWon(
                    "All five brains were eaten. I, Zombie level "
                            + level.getNumber() + " complete!");
            return;
        }
        if (!hasActiveZombie()
                && getSunCount() < level.getMinimumZombieCost()) {
            completeGameAsLost(
                    "No zombie remains on the lawn and the remaining sun "
                            + "cannot buy another zombie. I, Zombie failed!");
        }
    }

    private boolean allBrainsEaten() {
        for (boolean available : brainsAvailable) {
            if (available) {
                return false;
            }
        }
        return true;
    }

    private boolean hasActiveZombie() {
        for (Zombie zombie : getBoard().getZombies()) {
            if (isActive(zombie)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isActive(Zombie zombie) {
        return zombie != null
                && !zombie.isDead()
                && !zombie.isRemoved();
    }

    public IZombiePlacementResult placeZombie(
            String requestedType, EntityPosition position) {
        if (getStatus() != GameStatus.ACTIVE) {
            return IZombiePlacementResult.GAME_NOT_ACTIVE;
        }
        IZombieCard card = level.findCard(requestedType);
        if (card == null) {
            return IZombiePlacementResult.UNKNOWN_ZOMBIE;
        }
        if (card.getType().isBoss()) {
            return IZombiePlacementResult.BOSS_NOT_ALLOWED;
        }
        if (position == null
                || !getBoard().isPositionInsideBoard(position)) {
            return IZombiePlacementResult.INVALID_POSITION;
        }
        if (position.getColumn() <= level.getRedLineColumn()) {
            return IZombiePlacementResult.LEFT_OF_RED_LINE;
        }
        if (hasZombieAt(position)) {
            return IZombiePlacementResult.POSITION_OCCUPIED;
        }
        if (!spendSun(card.getCost())) {
            return IZombiePlacementResult.NOT_ENOUGH_SUN;
        }

        Zombie zombie = new Zombie(card.getType(), 0,
                position.getRow(), position.getColumn(), false);
        getBoard().addZombie(zombie);
        addPendingResult("Placed " + card.getType().getAlias()
                + " at " + position + " for " + card.getCost()
                + " sun.");
        return IZombiePlacementResult.SUCCESS;
    }

    private boolean hasZombieAt(EntityPosition position) {
        for (Zombie zombie : getBoard().getZombies()) {
            if (!isActive(zombie)
                    || zombie.getLane() != position.getRow()) {
                continue;
            }
            if (Math.abs(zombie.getColumnPosition()
                    - position.getColumn()) <= OCCUPANCY_RADIUS) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allowsDirectPlanting() {
        return false;
    }

    @Override
    public String getDirectPlantingDisabledMessage() {
        return "planting is disabled in I, Zombie; place zombies instead!";
    }

    @Override
    protected boolean shouldProcessZombieDeathDrops() {
        return false;
    }

    @Override
    protected boolean usesLawnMowers() {
        return false;
    }

    public IZombieLevel getLevel() {
        return level;
    }

    public int getRedLineColumn() {
        return level.getRedLineColumn();
    }

    public boolean isBrainAvailable(int row) {
        if (row < 0 || row >= brainsAvailable.length) {
            throw new IllegalArgumentException(
                    "brain row is outside the board");
        }
        return brainsAvailable[row];
    }

    public int getEatenBrainCount() {
        int eaten = 0;
        for (boolean available : brainsAvailable) {
            if (!available) {
                eaten++;
            }
        }
        return eaten;
    }

    public int getLivingSunProducerCount() {
        int living = 0;
        for (IZombieSunProducer producer : sunProducers) {
            if (isActive(producer)) {
                living++;
            }
        }
        return living;
    }

    public int getRemainingPlantCount() {
        return getBoard().getPlants().size();
    }
}
