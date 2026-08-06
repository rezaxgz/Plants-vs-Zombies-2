package io.github.some_example_name.model.game.entities.zombies.abilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.zombies.Zombie;
import io.github.some_example_name.model.game.entities.zombies.ZombieType;
import io.github.some_example_name.model.game.entities.zombies.abilities.ZombossProfile.Action;

/**
 * Shared three-phase behavior for the four chapter Zomboss machines.
 */
public class ZombossAbility extends ZombieAbility {
    private final ZombossProfile profile;
    private final Random random;
    private final List<Zombie> lastSpawnedZombies;
    private final List<BasePlant> lastDestroyedPlants;

    private Action lastAction;
    private int currentPhase = 1;
    private boolean phaseChangedThisUse;
    private boolean performedActionThisUse;
    private String lastActionDescription = "";

    public ZombossAbility(String worldName) {
        this(ZombossProfile.parse(worldName), new Random());
    }

    private ZombossAbility(
            ZombossProfile profile, Random random) {
        super(5.0);
        if (profile == null || random == null) {
            throw new IllegalArgumentException(
                    "Zomboss profile and random cannot be null");
        }
        this.profile = profile;
        this.random = random;
        this.lastSpawnedZombies = new ArrayList<>();
        this.lastDestroyedPlants = new ArrayList<>();
    }

    @Override
    public boolean tryUse(Zombie zomboss, Board board) {
        resetUseState();
        if (zomboss == null || board == null
                || !zomboss.getType().isBoss()
                || zomboss.isDead()) {
            return false;
        }

        int observedPhase = profile.phaseFor(zomboss.getHitPoints());
        if (observedPhase != currentPhase) {
            currentPhase = observedPhase;
            phaseChangedThisUse = true;
        }
        cooldown = profile.cooldownFor(currentPhase);

        if (!canUse() || zomboss.isHypnotized()
                || zomboss.isFrozen()
                || zomboss.isStunned()) {
            return phaseChangedThisUse;
        }

        Action action = chooseAction();
        executeAction(action, zomboss, board);
        lastAction = action;
        performedActionThisUse = true;
        resetCooldown();
        return true;
    }

    private void resetUseState() {
        phaseChangedThisUse = false;
        performedActionThisUse = false;
        lastActionDescription = "";
        lastSpawnedZombies.clear();
        lastDestroyedPlants.clear();
    }

    private Action chooseAction() {
        List<Action> choices = new ArrayList<>(
                profile.actionsFor(currentPhase));
        if (choices.size() > 1 && lastAction != null) {
            choices.remove(lastAction);
        }
        return choices.get(random.nextInt(choices.size()));
    }

    private void executeAction(Action action,
            Zombie zomboss, Board board) {
        switch (action) {
            case MOVE:
                moveBoss(zomboss, board);
                break;
            case SPAWN:
                spawnMinions(zomboss, board);
                break;
            case RUSH:
                rushLane(zomboss, board);
                break;
            case ROCKET:
                fireRocket(board);
                break;
            case IMP_CANNON:
                fireImpCannon(zomboss, board);
                break;
            case FIRE_BREATH:
                breatheFire(zomboss, board);
                break;
            case FIREBALLS:
                rainFireballs(board);
                break;
            default:
                throw new IllegalStateException(
                        "Unhandled Zomboss action");
        }
    }

    private void moveBoss(Zombie zomboss, Board board) {
        int lane = randomLane(board);
        double minimum = profile.minimumColumn(board);
        double maximum = ZombossProfile.maximumColumn(board);
        int firstColumn = (int) Math.ceil(minimum);
        int lastColumn = (int) Math.floor(maximum);
        int column = firstColumn + random.nextInt(
                lastColumn - firstColumn + 1);

        zomboss.moveToLane(lane);
        zomboss.moveTo(column);
        lastActionDescription = "relocated to lane "
                + lane + " at column " + column + ".";
    }

    private void spawnMinions(
            Zombie zomboss, Board board) {
        List<ZombieType> pool = profile.minionsFor(currentPhase);
        for (int index = 0; index < currentPhase; index++) {
            ZombieType type = pool.get(
                    random.nextInt(pool.size()));
            Zombie spawned = spawnZombie(
                    type,
                    zomboss.getWaveNumber(),
                    randomLane(board),
                    board.getNumberOfColumns() - 0.001,
                    board);
            lastSpawnedZombies.add(spawned);
        }
        lastActionDescription = "summoned "
                + lastSpawnedZombies.size()
                + " reinforcement(s).";
    }

    private void rushLane(Zombie zomboss, Board board) {
        int lane = randomLane(board);
        zomboss.moveToLane(lane);
        destroyPlantsInLane(lane, board);
        zomboss.moveTo(profile.minimumColumn(board));
        zomboss.moveTo(
                ZombossProfile.maximumColumn(board));
        lastActionDescription = "rushed through lane "
                + lane + ", crushed "
                + lastDestroyedPlants.size()
                + " plant(s), and retreated.";
    }

    private void fireRocket(Board board) {
        BasePlant target = chooseRandomPlant(board);
        if (target == null) {
            lastActionDescription = "launched a missile, but no plant was available.";
            return;
        }
        EntityPosition center = target.getEntityPosition();
        destroyPlantsInArea(center, 1, board);
        lastActionDescription = "launched a missile at "
                + center + " and destroyed "
                + lastDestroyedPlants.size()
                + " plant(s).";
    }

    private void fireImpCannon(
            Zombie zomboss, Board board) {
        BasePlant target = chooseRandomPlant(board);
        int lane = target == null
                ? randomLane(board)
                : target.getEntityPosition().getRow();
        double column = target == null
                ? Math.max(1.0,
                        board.getNumberOfColumns() / 2.0)
                : target.getEntityPosition()
                        .getColumn() + 1.0;
        column = Math.max(1.0, Math.min(
                board.getNumberOfColumns() - 0.001,
                column));

        Zombie imp = spawnZombie(
                ZombieType.IMP,
                zomboss.getWaveNumber(),
                lane,
                column,
                board);
        lastSpawnedZombies.add(imp);
        lastActionDescription = "fired an Imp into lane "
                + lane + " at column "
                + String.format(
                        Locale.ROOT, "%.1f", column)
                + ".";
    }

    private void breatheFire(
            Zombie zomboss, Board board) {
        int lane = randomLane(board);
        zomboss.moveToLane(lane);
        for (BasePlant plant : new ArrayList<>(board.getPlants())) {
            if (plant.getEntityPosition() == null
                    || plant.getEntityPosition().getRow() != lane) {
                continue;
            }
            double distance = zomboss.getColumnPosition()
                    - plant.getEntityPosition().getColumn();
            if (distance >= 0.0 && distance <= 4.0) {
                destroyPlant(plant);
            }
        }
        lastActionDescription = "breathed fire across lane "
                + lane + " and destroyed "
                + lastDestroyedPlants.size()
                + " plant(s).";
    }

    private void rainFireballs(Board board) {
        List<BasePlant> candidates = new ArrayList<>(board.getPlants());
        Collections.shuffle(candidates, random);
        int targetCount = Math.min(
                currentPhase, candidates.size());
        for (int index = 0; index < targetCount; index++) {
            destroyPlant(candidates.get(index));
        }
        lastActionDescription = "rained fireballs and destroyed "
                + lastDestroyedPlants.size()
                + " plant(s).";
    }

    private void destroyPlantsInLane(
            int lane, Board board) {
        for (BasePlant plant : new ArrayList<>(board.getPlants())) {
            if (plant.getEntityPosition() != null
                    && plant.getEntityPosition().getRow() == lane) {
                destroyPlant(plant);
            }
        }
    }

    private void destroyPlantsInArea(
            EntityPosition center,
            int radius,
            Board board) {
        for (BasePlant plant : new ArrayList<>(board.getPlants())) {
            EntityPosition position = plant.getEntityPosition();
            if (position == null) {
                continue;
            }
            if (Math.abs(position.getRow()
                    - center.getRow()) <= radius
                    && Math.abs(position.getColumn()
                            - center.getColumn()) <= radius) {
                destroyPlant(plant);
            }
        }
    }

    private void destroyPlant(BasePlant plant) {
        if (plant == null || plant.isDestroyed()
                || plant.isRemoved()) {
            return;
        }
        plant.takeDamage(Integer.MAX_VALUE);
        lastDestroyedPlants.add(plant);
    }

    private BasePlant chooseRandomPlant(Board board) {
        List<BasePlant> plants = board.getPlants();
        if (plants.isEmpty()) {
            return null;
        }
        return plants.get(
                random.nextInt(plants.size()));
    }

    private static Zombie spawnZombie(
            ZombieType type,
            int waveNumber,
            int lane,
            double column,
            Board board) {
        Zombie zombie = new Zombie(
                type, waveNumber, lane, column);
        board.addZombie(zombie);
        return zombie;
    }

    private int randomLane(Board board) {
        return random.nextInt(board.getNumberOfRows());
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    public boolean didPhaseChangeThisUse() {
        return phaseChangedThisUse;
    }

    public boolean didPerformActionThisUse() {
        return performedActionThisUse;
    }

    public String getLastActionDescription() {
        return lastActionDescription;
    }

    public List<Zombie> getLastSpawnedZombies() {
        return Collections.unmodifiableList(
                new ArrayList<>(lastSpawnedZombies));
    }

    public List<BasePlant> getLastDestroyedPlants() {
        return Collections.unmodifiableList(
                new ArrayList<>(lastDestroyedPlants));
    }
}