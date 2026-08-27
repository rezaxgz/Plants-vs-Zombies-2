package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.ZombossProfile.Action;
import io.github.Plants_Vs_Zombies_2.model.game.tile.Tile;
import io.github.Plants_Vs_Zombies_2.model.game.tile.TileType;

/** Shared three-phase behavior for the chapter Zomboss machines. */
public class ZombossAbility extends ZombieAbility {
    private static final double PHASE_STUN_SECONDS = 4.0;
    private static final double BURNING_TILE_SECONDS = 4.0;
    // These durations match the official PAM clips listed in animations.json.
    // Keeping the model action window aligned with the animation lets the view
    // telegraph dangerous attacks before their gameplay effect is applied.
    private static final double EGYPT_RUSH_SECONDS = 2.5333;
    private static final double EGYPT_RUSH_CRUSH_FRACTION = 0.55;
    private static final double STANDARD_ROCKET_WINDUP_SECONDS = 1.8333;
    private static final double ICEAGE_ROCKET_WINDUP_SECONDS = 3.5;

    private final ZombossProfile profile;
    private final Random random;
    private final List<Zombie> lastSpawnedZombies;
    private final List<Zombie> lastDestroyedZombies;
    private final List<BasePlant> lastDestroyedPlants;
    private final List<Integer> lastAffectedLanes;

    private Action lastAction;
    private Action pendingAction;
    private double pendingActionElapsedSeconds;
    private boolean pendingActionResolved;
    private int pendingRushBottomLane = -1;
    private double pendingRushStartColumn;
    private double pendingRushTargetColumn;
    private EntityPosition pendingRocketTarget;
    private EntityPosition lastRocketImpactTarget;
    private int rocketTargetSequence;
    private int rocketImpactSequence;
    private int currentPhase = 1;
    private boolean phaseChangedThisUse;
    private boolean performedActionThisUse;
    private String lastActionDescription = "";
    private int actionSequence;

    public ZombossAbility(String worldName) {
        this(ZombossProfile.parse(worldName), new Random());
    }

    private ZombossAbility(ZombossProfile profile, Random random) {
        super(5.0);
        if (profile == null || random == null) {
            throw new IllegalArgumentException(
                    "Zomboss profile and random cannot be null");
        }
        this.profile = profile;
        this.random = random;
        this.lastSpawnedZombies = new ArrayList<>();
        this.lastDestroyedZombies = new ArrayList<>();
        this.lastDestroyedPlants = new ArrayList<>();
        this.lastAffectedLanes = new ArrayList<>();
        // ZombieAbility starts ready by default. Zomboss should wait a few
        // seconds before its first random move, matching the phase-two
        // requirement and giving the conveyor fight a fair opening.
        this.elapsedSinceLastUse = 0.0;
    }

    @Override
    public void update(double deltaSeconds) {
        super.update(deltaSeconds);
        if (pendingAction != null) {
            pendingActionElapsedSeconds += Math.max(0.0, deltaSeconds);
        }
    }

    @Override
    public boolean tryUse(Zombie zomboss, Board board) {
        resetUseState();
        if (zomboss == null || board == null
                || !zomboss.getType().isBoss() || zomboss.isDead()) {
            return false;
        }

        int observedPhase = profile.phaseFor(zomboss);
        if (observedPhase > currentPhase && !zomboss.isStunned()) {
            // Advance only one health segment at a time. If a very large hit
            // crosses two thirds at once, each crossed segment still earns
            // its own stun instead of silently skipping a phase.
            currentPhase++;
            phaseChangedThisUse = true;
            cancelPendingAction();
            zomboss.applyStun(PHASE_STUN_SECONDS);
        } else if (observedPhase < currentPhase) {
            // Defensive support for a restored/loaded boss state. Normal
            // Zomboss gameplay never heals back into an earlier phase.
            currentPhase = observedPhase;
        }
        cooldown = profile.cooldownFor(currentPhase);

        if (phaseChangedThisUse) {
            return true;
        }
        if (pendingAction != null) {
            return advancePendingAction(zomboss, board);
        }
        if (!canUse() || zomboss.isHypnotized()
                || zomboss.isFrozen() || zomboss.isStunned()) {
            return false;
        }

        Action action = chooseAction();
        executeAction(action, zomboss, board);
        lastAction = action;
        // The action sequence changes at the START of the move so the GUI can
        // begin the corresponding PAM animation immediately. Delayed attacks
        // (Egypt rush and missiles) report their gameplay result later.
        actionSequence++;
        performedActionThisUse = pendingAction == null;
        resetCooldown();
        return true;
    }

    private void resetUseState() {
        phaseChangedThisUse = false;
        performedActionThisUse = false;
        lastActionDescription = "";
        lastSpawnedZombies.clear();
        lastDestroyedZombies.clear();
        lastDestroyedPlants.clear();
        lastAffectedLanes.clear();
    }

    private Action chooseAction() {
        List<Action> choices = new ArrayList<>(profile.actionsFor(currentPhase));
        if (choices.size() > 1 && lastAction != null) {
            choices.remove(lastAction);
        }
        return choices.get(random.nextInt(choices.size()));
    }

    private void executeAction(Action action, Zombie zomboss, Board board) {
        switch (action) {
            case MOVE:
                moveBoss(zomboss, board);
                break;
            case SPAWN:
                spawnMinions(zomboss, board);
                break;
            case RUSH:
                rushRows(zomboss, board);
                break;
            case ROCKET:
                fireRocket(zomboss, board);
                break;
            case IMP_CANNON:
                fireImpCannon(zomboss, board);
                break;
            case FIRE_BREATH:
                breatheFire(zomboss, board);
                break;
            case FIREBALLS:
                rainFireballs(zomboss, board);
                break;
            case ICY_WIND:
                blowIcyWind(board);
                break;
            case FREEZE_COLUMN:
                freezeColumn(zomboss, board);
                break;
            case BABY_SHARK:
                sendBabySharks(board);
                break;
            case TURBINE:
                useTurbine(zomboss, board);
                break;
            default:
                throw new IllegalStateException("Unhandled Zomboss action");
        }
    }

    private void moveBoss(Zombie zomboss, Board board) {
        if (!profile.canMoveBetweenLanes()) {
            lastActionDescription = "held its position.";
            return;
        }
        int lane = randomBossLane(board);
        zomboss.moveToLane(lane);
        zomboss.moveTo(ZombossProfile.homeColumn(board));
        lastAffectedLanes.add(lane - 1);
        lastAffectedLanes.add(lane);
        lastActionDescription = "moved to rows " + lane
                + " and " + (lane + 1) + ".";
    }

    private void spawnMinions(Zombie zomboss, Board board) {
        if (!profile.canSummonNormalZombies()) {
            lastActionDescription = "cannot summon normal zombies in this fight.";
            return;
        }
        List<ZombieType> pool = profile.minionsFor(currentPhase);
        if (pool.isEmpty()) {
            return;
        }
        int spawnCount = currentPhase >= 3 ? 2 : 1;
        int bottomLane = ensureBossLane(zomboss, board);
        double spawnColumn = profile == ZombossProfile.EGYPT
                ? mouthSpawnColumn(zomboss, board)
                : board.getNumberOfColumns() - 0.001;
        for (int index = 0; index < spawnCount; index++) {
            ZombieType type = pool.get(random.nextInt(pool.size()));
            int spawnLane = profile == ZombossProfile.EGYPT
                    ? bottomLane
                    : random.nextInt(board.getNumberOfRows());
            Zombie spawned = spawnZombie(type, zomboss.getWaveNumber(),
                    spawnLane, spawnColumn, board);
            lastSpawnedZombies.add(spawned);
        }
        lastActionDescription = "summoned " + lastSpawnedZombies.size()
                + " reinforcement(s).";
    }

    private void rushRows(Zombie zomboss, Board board) {
        if (profile != ZombossProfile.EGYPT) {
            crushRushRows(zomboss, board);
            return;
        }

        pendingAction = Action.RUSH;
        pendingActionElapsedSeconds = 0.0;
        pendingActionResolved = false;
        pendingRushBottomLane = ensureBossLane(zomboss, board);
        pendingRushStartColumn = zomboss.getColumnPosition();
        pendingRushTargetColumn = profile.rushMinimumColumn(board);
        lastActionDescription = "";
    }

    private void crushRushRows(Zombie zomboss, Board board) {
        int bottomLane = pendingRushBottomLane >= 0
                ? pendingRushBottomLane : ensureBossLane(zomboss, board);
        List<Integer> lanes = occupiedBossLanes(bottomLane);
        lastAffectedLanes.addAll(lanes);
        for (BasePlant plant : new ArrayList<>(board.getPlants())) {
            if (plant.getEntityPosition() != null
                    && lanes.contains(plant.getEntityPosition().getRow())) {
                destroyPlant(plant);
            }
        }
        lastActionDescription = "rushed across two rows, crushed "
                + lastDestroyedPlants.size() + " plant(s), and retreated.";
    }

    private void fireRocket(Zombie zomboss, Board board) {
        pendingAction = Action.ROCKET;
        pendingActionElapsedSeconds = 0.0;
        pendingActionResolved = false;
        pendingRocketTarget = chooseRocketTarget(board);
        lastRocketImpactTarget = null;
        rocketTargetSequence++;
        lastActionDescription = "";
    }

    private EntityPosition chooseRocketTarget(Board board) {
        BasePlant targetPlant = chooseRandomPlant(board);
        if (targetPlant != null && targetPlant.getEntityPosition() != null) {
            return targetPlant.getEntityPosition();
        }
        return randomBoardPosition(board);
    }

    private void resolveRocketImpact(Board board) {
        EntityPosition target = pendingRocketTarget;
        if (target == null) {
            return;
        }

        BasePlant targetPlant = board.getPlantAt(target);
        if (targetPlant != null) {
            destroyPlant(targetPlant);
        }
        if (profile == ZombossProfile.EGYPT) {
            int graves = addRandomGraves(board, 2);
            lastActionDescription = "launched a missile at " + target
                    + ", destroyed " + lastDestroyedPlants.size()
                    + " plant(s), and raised " + graves + " grave(s).";
        } else if (profile == ZombossProfile.ICEAGE) {
            lastActionDescription = "launched an ice missile at " + target
                    + " and destroyed " + lastDestroyedPlants.size()
                    + " plant(s).";
        } else {
            // Legacy Cowboy behavior also damages the surrounding tiles. The
            // centre plant is already removed above; destroyPlant() ignores it
            // if the area pass encounters it again.
            destroyPlantsInArea(target, 1, board);
            lastActionDescription = "launched a missile at " + target
                    + " and destroyed " + lastDestroyedPlants.size()
                    + " plant(s).";
        }
        lastRocketImpactTarget = target;
        rocketImpactSequence++;
    }

    private boolean advancePendingAction(Zombie zomboss, Board board) {
        if (pendingAction == Action.RUSH) {
            boolean changed = false;
            double crushAt = EGYPT_RUSH_SECONDS * EGYPT_RUSH_CRUSH_FRACTION;
            if (!pendingActionResolved
                    && pendingActionElapsedSeconds >= crushAt) {
                pendingActionResolved = true;
                crushRushRows(zomboss, board);
                performedActionThisUse = true;
                changed = true;
            }
            if (pendingActionElapsedSeconds >= EGYPT_RUSH_SECONDS) {
                clearPendingAction();
            }
            return changed;
        }
        if (pendingAction == Action.ROCKET) {
            if (!pendingActionResolved
                    && pendingActionElapsedSeconds >= rocketWindupSeconds()) {
                pendingActionResolved = true;
                resolveRocketImpact(board);
                performedActionThisUse = true;
                clearPendingAction();
                return true;
            }
            return false;
        }
        clearPendingAction();
        return false;
    }

    private double rocketWindupSeconds() {
        return profile == ZombossProfile.ICEAGE
                ? ICEAGE_ROCKET_WINDUP_SECONDS
                : STANDARD_ROCKET_WINDUP_SECONDS;
    }

    private void cancelPendingAction() {
        clearPendingAction();
        pendingRocketTarget = null;
    }

    private void clearPendingAction() {
        pendingAction = null;
        pendingActionElapsedSeconds = 0.0;
        pendingActionResolved = false;
        pendingRushBottomLane = -1;
        pendingRushStartColumn = 0.0;
        pendingRushTargetColumn = 0.0;
        pendingRocketTarget = null;
    }

    private int addRandomGraves(Board board, int count) {
        List<EntityPosition> candidates = new ArrayList<>();
        for (int row = 0; row < board.getNumberOfRows(); row++) {
            for (int column = 0; column < board.getNumberOfColumns(); column++) {
                EntityPosition position = new EntityPosition(row, column);
                if (board.canAddGraveAt(position)) {
                    candidates.add(position);
                }
            }
        }
        Collections.shuffle(candidates, random);
        int added = 0;
        for (EntityPosition position : candidates) {
            if (added >= count) {
                break;
            }
            if (board.addGrave(position)) {
                added++;
            }
        }
        return added;
    }

    private void fireImpCannon(Zombie zomboss, Board board) {
        BasePlant target = chooseRandomPlant(board);
        int lane = target == null ? random.nextInt(board.getNumberOfRows())
                : target.getEntityPosition().getRow();
        double column = target == null
                ? Math.max(1.0, board.getNumberOfColumns() / 2.0)
                : target.getEntityPosition().getColumn() + 1.0;
        column = Math.max(1.0,
                Math.min(board.getNumberOfColumns() - 0.001, column));
        Zombie imp = spawnZombie(ZombieType.IMP, zomboss.getWaveNumber(),
                lane, column, board);
        lastSpawnedZombies.add(imp);
        lastActionDescription = "fired an Imp into lane " + lane
                + " at column " + String.format(Locale.ROOT, "%.1f", column)
                + ".";
    }

    private void breatheFire(Zombie zomboss, Board board) {
        int bottomLane = ensureBossLane(zomboss, board);
        List<Integer> lanes = occupiedBossLanes(bottomLane);
        lastAffectedLanes.addAll(lanes);
        for (int lane : lanes) {
            for (int column = 0; column < board.getNumberOfColumns(); column++) {
                EntityPosition position = new EntityPosition(lane, column);
                BasePlant plant = board.getPlantAt(position);
                if (plant != null) {
                    destroyPlant(plant);
                }
                board.igniteTile(position, BURNING_TILE_SECONDS);
            }
        }
        lastActionDescription = "burned two rows, destroyed "
                + lastDestroyedPlants.size()
                + " plant(s), and left the ground burning for 4 seconds.";
    }

    private void rainFireballs(Zombie zomboss, Board board) {
        int targetCount = Math.min(currentPhase >= 3 ? 3 : 2,
                board.getNumberOfRows() * board.getNumberOfColumns());
        List<EntityPosition> candidates = new ArrayList<>();
        for (int row = 0; row < board.getNumberOfRows(); row++) {
            for (int column = 0; column < board.getNumberOfColumns(); column++) {
                EntityPosition position = new EntityPosition(row, column);
                if (!board.hasZombieAt(position)) {
                    candidates.add(position);
                }
            }
        }
        Collections.shuffle(candidates, random);
        Set<EntityPosition> targets = new LinkedHashSet<>(
                candidates.subList(0, Math.min(targetCount, candidates.size())));
        for (EntityPosition position : targets) {
            BasePlant plant = board.getPlantAt(position);
            if (plant != null) {
                destroyPlant(plant);
            }
            board.igniteTile(position, BURNING_TILE_SECONDS);
            if (!board.hasZombieAt(position)) {
                Zombie imp = spawnZombie(ZombieType.DRAGON_IMP,
                        zomboss.getWaveNumber(), position.getRow(),
                        position.getColumn(), board);
                lastSpawnedZombies.add(imp);
            }
        }
        lastActionDescription = "rained fireballs on " + targets.size()
                + " tile(s), burned the ground, and released "
                + lastSpawnedZombies.size() + " Dragon Imp(s).";
    }

    private void blowIcyWind(Board board) {
        List<Integer> lanes = new ArrayList<>();
        for (int lane = 0; lane < board.getNumberOfRows(); lane++) {
            lanes.add(lane);
        }
        Collections.shuffle(lanes, random);
        int count = Math.min(2, lanes.size());
        List<Integer> selected = new ArrayList<>(lanes.subList(0, count));
        board.applyIcyWind(selected);
        lastAffectedLanes.addAll(selected);
        lastActionDescription = "blew icy wind across rows " + selected + ".";
    }

    private void freezeColumn(Zombie zomboss, Board board) {
        int column = random.nextInt(Math.max(1, board.getNumberOfColumns() - 2));
        int frozen = 0;
        for (int row = 0; row < board.getNumberOfRows(); row++) {
            EntityPosition position = new EntityPosition(row, column);
            Tile tile = board.getTileAt(position);
            if (tile == null || tile.hasPlant() || board.hasZombieAt(position)
                    || tile.getTileType() == TileType.WATER
                    || tile.getTileType() == TileType.GRAVESTONE) {
                continue;
            }
            ZombieType type = row % 2 == 0
                    ? ZombieType.ICEAGE : ZombieType.ICEAGE_CONEHEAD;
            Zombie frozenZombie = new Zombie(type, zomboss.getWaveNumber(),
                    row, column, false);
            frozenZombie.encaseInIce();
            board.addZombie(frozenZombie);
            board.setTileType(position, TileType.FROZEN);
            lastSpawnedZombies.add(frozenZombie);
            frozen++;
        }
        lastActionDescription = "froze column " + (column + 1)
                + " and encased " + frozen + " zombie(s) in ice.";
    }

    private void sendBabySharks(Board board) {
        List<BasePlant> candidates = new ArrayList<>();
        for (BasePlant plant : board.getPlants()) {
            if (plant.getEntityPosition() == null || plant.isDestroyed()) {
                continue;
            }
            Tile tile = board.getTileAt(plant.getEntityPosition());
            if (tile != null && tile.getTileType() == TileType.WATER) {
                candidates.add(plant);
            }
        }
        Collections.shuffle(candidates, random);
        int targetCount = Math.min(currentPhase == 1 ? 1 : 2,
                candidates.size());
        for (int index = 0; index < targetCount; index++) {
            destroyPlant(candidates.get(index));
        }
        lastActionDescription = candidates.isEmpty()
                ? "sent baby sharks, but no plant was standing in water."
                : "sent baby sharks that swallowed "
                        + lastDestroyedPlants.size() + " water plant(s).";
    }

    private void useTurbine(Zombie zomboss, Board board) {
        int bottomLane = ensureBossLane(zomboss, board);
        List<Integer> lanes = occupiedBossLanes(bottomLane);
        lastAffectedLanes.addAll(lanes);

        int mouthColumn = Math.max(1,
                (int) Math.floor(ZombossProfile.homeColumn(board) - 1.0));
        int pulledPlantStacks = 0;
        Set<EntityPosition> handledPlantPositions = new LinkedHashSet<>();
        List<BasePlant> plants = new ArrayList<>(board.getPlants());
        plants.sort((first, second) -> Integer.compare(
                second.getEntityPosition() == null ? -1
                        : second.getEntityPosition().getColumn(),
                first.getEntityPosition() == null ? -1
                        : first.getEntityPosition().getColumn()));
        for (BasePlant plant : plants) {
            EntityPosition source = plant.getEntityPosition();
            if (source == null || !lanes.contains(source.getRow())
                    || !handledPlantPositions.add(source)) {
                continue;
            }
            if (source.getColumn() >= mouthColumn) {
                for (BasePlant stacked : new ArrayList<>(board.getPlantsAt(source))) {
                    destroyPlant(stacked);
                }
                continue;
            }
            int destinationColumn = Math.min(mouthColumn,
                    source.getColumn() + 2);
            if (destinationColumn > source.getColumn()
                    && board.movePlantStack(source,
                            new EntityPosition(source.getRow(),
                                    destinationColumn))) {
                pulledPlantStacks++;
            }
        }

        int pulledZombies = 0;
        double mouthPosition = mouthColumn + 0.25;
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            if (zombie == zomboss || zombie.isDead()
                    || !lanes.contains(zombie.getLane())) {
                continue;
            }
            if (zombie.getColumnPosition() >= mouthPosition) {
                zombie.kill();
                lastDestroyedZombies.add(zombie);
                continue;
            }
            zombie.moveTo(Math.min(mouthPosition,
                    zombie.getColumnPosition() + 2.0));
            pulledZombies++;
        }
        lastActionDescription = "used its turbine on two rows, pulled "
                + pulledPlantStacks + " plant stack(s) and "
                + pulledZombies + " zombie(s) toward its mouth, and swallowed "
                + lastDestroyedPlants.size() + " plant(s) and "
                + lastDestroyedZombies.size() + " zombie(s) already close enough.";
    }

    private void destroyPlantsInArea(EntityPosition center, int radius,
            Board board) {
        for (BasePlant plant : new ArrayList<>(board.getPlants())) {
            EntityPosition position = plant.getEntityPosition();
            if (position == null) {
                continue;
            }
            if (Math.abs(position.getRow() - center.getRow()) <= radius
                    && Math.abs(position.getColumn()
                            - center.getColumn()) <= radius) {
                destroyPlant(plant);
            }
        }
    }

    private void destroyPlant(BasePlant plant) {
        if (plant == null || plant.isDestroyed() || plant.isRemoved()) {
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
        return plants.get(random.nextInt(plants.size()));
    }

    private EntityPosition randomBoardPosition(Board board) {
        return new EntityPosition(random.nextInt(board.getNumberOfRows()),
                random.nextInt(board.getNumberOfColumns()));
    }

    private double mouthSpawnColumn(Zombie zomboss, Board board) {
        double mouthColumn = Math.min(zomboss.getColumnPosition(),
                ZombossProfile.homeColumn(board)) - 1.0;
        return Math.max(0.35,
                Math.min(board.getNumberOfColumns() - 0.001, mouthColumn));
    }

    private static Zombie spawnZombie(ZombieType type, int waveNumber,
            int lane, double column, Board board) {
        Zombie zombie = new Zombie(type, waveNumber, lane, column);
        board.addZombie(zombie);
        return zombie;
    }

    private int randomBossLane(Board board) {
        if (board.getNumberOfRows() <= 1) {
            return 0;
        }
        return 1 + random.nextInt(board.getNumberOfRows() - 1);
    }

    private int ensureBossLane(Zombie zomboss, Board board) {
        int lane = zomboss.getLane();
        if (board.getNumberOfRows() > 1) {
            lane = Math.max(1, Math.min(board.getNumberOfRows() - 1, lane));
            if (lane != zomboss.getLane()) {
                zomboss.moveToLane(lane);
            }
        }
        return lane;
    }

    private static List<Integer> occupiedBossLanes(int bottomLane) {
        if (bottomLane <= 0) {
            return List.of(0);
        }
        return List.of(bottomLane - 1, bottomLane);
    }

    /**
     * Column used only for rendering the Egypt rush. Gameplay position stays at
     * the boss's home column while the stomp clip carries it forward and back.
     */
    public double getPresentationColumn(double fallbackColumn) {
        if (profile != ZombossProfile.EGYPT || pendingAction != Action.RUSH) {
            return fallbackColumn;
        }
        double progress = Math.max(0.0, Math.min(1.0,
                pendingActionElapsedSeconds / EGYPT_RUSH_SECONDS));
        // Keep advancing until the crush moment so the boss visibly crosses
        // the entire board, then hold briefly at the front before retreating.
        double forwardEnd = EGYPT_RUSH_CRUSH_FRACTION;
        double retreatStart = 0.72;
        if (progress < forwardEnd) {
            return lerp(pendingRushStartColumn, pendingRushTargetColumn,
                    progress / forwardEnd);
        }
        if (progress <= retreatStart) {
            return pendingRushTargetColumn;
        }
        return lerp(pendingRushTargetColumn, pendingRushStartColumn,
                (progress - retreatStart) / (1.0 - retreatStart));
    }

    private static double lerp(double start, double end, double amount) {
        return start + (end - start) * Math.max(0.0, Math.min(1.0, amount));
    }

    public boolean isRocketTargeting() {
        return pendingAction == Action.ROCKET && pendingRocketTarget != null;
    }

    public EntityPosition getRocketTarget() {
        return pendingRocketTarget;
    }

    public EntityPosition getLastRocketImpactTarget() {
        return lastRocketImpactTarget;
    }

    public int getRocketTargetSequence() {
        return rocketTargetSequence;
    }

    public int getRocketImpactSequence() {
        return rocketImpactSequence;
    }


    /**
     * Presentation progress for the currently telegraphed missile attack.
     * Zero is the start of the firing animation and one is the impact frame.
     */
    public double getRocketTargetingProgress() {
        if (pendingAction != Action.ROCKET || pendingRocketTarget == null) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0,
                pendingActionElapsedSeconds / rocketWindupSeconds()));
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

    public int getActionSequence() {
        return actionSequence;
    }

    public String getLastActionName() {
        return lastAction == null ? "" : lastAction.name();
    }

    public List<Integer> getLastAffectedLanes() {
        return Collections.unmodifiableList(new ArrayList<>(lastAffectedLanes));
    }

    public List<Zombie> getLastSpawnedZombies() {
        return Collections.unmodifiableList(new ArrayList<>(lastSpawnedZombies));
    }

    public List<Zombie> getLastDestroyedZombies() {
        return Collections.unmodifiableList(new ArrayList<>(lastDestroyedZombies));
    }

    public List<BasePlant> getLastDestroyedPlants() {
        return Collections.unmodifiableList(new ArrayList<>(lastDestroyedPlants));
    }
}
