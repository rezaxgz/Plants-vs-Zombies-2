package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Constants;
import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.other.Sun;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.sunProducer.SunProducer;
import model.game.entities.plants.wallnut.Wallnut;
import model.game.entities.zombies.Zombie;
import model.game.structure.BaseStructure;
import model.game.tile.Tile;

public class Board {
    private static final double POSITION_EPSILON = 0.000001;
    private static final double SWEET_POTATO_ATTRACTION_RANGE = 1.0;

    private final int numberOfRows;
    private final int numberOfColumns;
    private final List<Tile> tiles;
    private final List<Entity> allEntities;
    private final List<BaseStructure> structures;
    private final List<String> pendingResults;

    public Board() {
        this(Constants.DEFAULT_BOARD_ROWS, Constants.DEFAULT_BOARD_COLUMNS);
    }

    public Board(int numberOfRows, int numberOfColumns) {
        if (numberOfRows <= 0 || numberOfColumns <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }
        this.numberOfRows = numberOfRows;
        this.numberOfColumns = numberOfColumns;
        this.tiles = new ArrayList<>();
        this.allEntities = new ArrayList<>();
        this.structures = new ArrayList<>();
        this.pendingResults = new ArrayList<>();
    }

    public void update(float deltaSeconds) {
        validateDeltaSeconds(deltaSeconds);

        List<Entity> entitiesToAdd = new ArrayList<>();
        List<Entity> updateSnapshot = new ArrayList<>(allEntities);
        updateEntities(updateSnapshot, entitiesToAdd, deltaSeconds);
        applyPendingWallnutBoardEffects(updateSnapshot);
        applyPendingWallnutPassiveEffects(updateSnapshot);
        updateZombies(updateSnapshot, deltaSeconds);

        allEntities.removeIf(Entity::isRemoved);
        for (Entity entity : entitiesToAdd) {
            addEntity(entity);
        }
    }

    private void updateEntities(List<Entity> updateSnapshot, List<Entity> entitiesToAdd,
            float deltaSeconds) {
        for (Entity entity : updateSnapshot) {
            if (entity.isRemoved()) {
                continue;
            }
            if (entity instanceof Zombie && ((Zombie) entity).isDead()) {
                reportZombieDeath((Zombie) entity);
                continue;
            }

            boolean sunWasDropping = entity instanceof Sun && ((Sun) entity).isDropping();
            entity.update(deltaSeconds);
            reportSunLanding(entity, sunWasDropping);
            collectProducedSuns(entity, entitiesToAdd);
        }
    }

    private void reportSunLanding(Entity entity, boolean sunWasDropping) {
        if (sunWasDropping && entity instanceof Sun && !((Sun) entity).isDropping()) {
            pendingResults.add("Sun reached the ground at position " + entity.getEntityPosition());
        }
    }

    private void collectProducedSuns(Entity entity, List<Entity> entitiesToAdd) {
        if (!(entity instanceof SunProducer)) {
            return;
        }
        SunProducer producer = (SunProducer) entity;
        List<Sun> producedSuns = producer.drainProducedSuns();
        entitiesToAdd.addAll(producedSuns);
        for (int i = 0; i < producedSuns.size(); i++) {
            pendingResults.add(buildSunProductionResult(producer));
        }
    }


    private void applyPendingWallnutPassiveEffects(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (entity instanceof Wallnut) {
                Wallnut wallnut = (Wallnut) entity;
                releaseSunBeanSun(wallnut);
                applyWallnutExplosion(wallnut);
            }
        }
    }

    private void applyPendingWallnutBoardEffects(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Wallnut) || entity.isRemoved()) {
                continue;
            }
            Wallnut wallnut = (Wallnut) entity;
            applyFamilyBoost(wallnut);
            applyGarlicPlantFood(wallnut);
            applySweetPotatoPlantFood(wallnut);
        }
    }

    private void applyFamilyBoost(Wallnut mint) {
        if (!mint.drainFamilyBoostPending()) {
            return;
        }
        for (BasePlant plant : getPlants()) {
            if (plant instanceof Wallnut && plant != mint) {
                ((Wallnut) plant).usePlantFood();
            }
        }
        mint.markForRemoval();
        pendingResults.add("Reinforce-mint applied plant food to every Wall-nut family plant.");
    }

    private void applyGarlicPlantFood(Wallnut garlic) {
        if (!garlic.drainDivertAllPending() || garlic.getEntityPosition() == null) {
            return;
        }
        int sourceLane = garlic.getEntityPosition().getRow();
        for (Zombie zombie : getZombies()) {
            if (zombie.getLane() == sourceLane) {
                int targetLane = garlic.chooseAdjacentLane(sourceLane, numberOfRows);
                zombie.moveToLane(targetLane);
            }
        }
        pendingResults.add("Garlic diverted every zombie in lane " + sourceLane + ".");
    }

    private void applySweetPotatoPlantFood(Wallnut sweetPotato) {
        if (!sweetPotato.drainAttractAllPending() || sweetPotato.getEntityPosition() == null) {
            return;
        }
        int targetLane = sweetPotato.getEntityPosition().getRow();
        for (Zombie zombie : getZombies()) {
            if (Math.abs(zombie.getLane() - targetLane) == 1) {
                zombie.moveToLane(targetLane);
            }
        }
        pendingResults.add("Sweet Potato pulled adjacent-lane zombies into lane " + targetLane + ".");
    }

    private void updateZombies(List<Entity> updateSnapshot, float deltaSeconds) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Zombie) || entity.isRemoved()) {
                continue;
            }
            Zombie zombie = (Zombie) entity;
            if (zombie.isDead()) {
                reportZombieDeath(zombie);
                continue;
            }
            attractZombieToSweetPotato(zombie);
            updateZombie(zombie, deltaSeconds);
        }
    }

    private void attractZombieToSweetPotato(Zombie zombie) {
        for (BasePlant plant : getPlants()) {
            if (!(plant instanceof Wallnut)) {
                continue;
            }
            Wallnut wallnut = (Wallnut) plant;
            if (!wallnut.attractsAdjacentLanes() || wallnut.getEntityPosition() == null) {
                continue;
            }
            int targetLane = wallnut.getEntityPosition().getRow();
            double distance = Math.abs(zombie.getColumnPosition()
                    - wallnut.getEntityPosition().getColumn());
            if (Math.abs(zombie.getLane() - targetLane) == 1
                    && distance <= SWEET_POTATO_ATTRACTION_RANGE) {
                zombie.moveToLane(targetLane);
                return;
            }
        }
    }

    private void updateZombie(Zombie zombie, float deltaSeconds) {
        BasePlant blockingPlant = findNearestPlantAhead(zombie);
        if (blockingPlant == null) {
            zombie.move(deltaSeconds, 0.0);
            if (zombie.getColumnPosition() <= POSITION_EPSILON) {
                zombie.markReachedHouse();
            }
            return;
        }

        double attackColumn = blockingPlant.getEntityPosition().getColumn() + Zombie.ATTACK_REACH;
        if (zombie.getColumnPosition() <= attackColumn + POSITION_EPSILON) {
            zombie.eat(blockingPlant, deltaSeconds);
            handleWallnutAfterAttack(zombie, blockingPlant, deltaSeconds);
            reportDestroyedPlant(blockingPlant);
        } else {
            zombie.move(deltaSeconds, attackColumn);
        }
    }

    private void handleWallnutAfterAttack(Zombie zombie, BasePlant plant, float deltaSeconds) {
        if (!(plant instanceof Wallnut)) {
            return;
        }
        Wallnut wallnut = (Wallnut) plant;
        reflectEndurianDamage(zombie, wallnut, deltaSeconds);
        divertZombieAfterGarlicBite(zombie, wallnut);
        releaseSunBeanSun(wallnut);
        applyWallnutExplosion(wallnut);
        if (zombie.isDead()) {
            reportZombieDeath(zombie);
        }
    }

    private void reflectEndurianDamage(Zombie zombie, Wallnut wallnut, float deltaSeconds) {
        int reflectedDamage = wallnut.calculateReflectedDamage(deltaSeconds);
        if (reflectedDamage > 0) {
            zombie.takeDamage(reflectedDamage);
        }
    }

    private void divertZombieAfterGarlicBite(Zombie zombie, Wallnut wallnut) {
        if (!wallnut.drainDivertLanePending()) {
            return;
        }
        int targetLane = wallnut.chooseAdjacentLane(zombie.getLane(), numberOfRows);
        zombie.moveToLane(targetLane);
        pendingResults.add("Garlic diverted " + zombie.getName() + " into lane " + targetLane + ".");
    }

    private void releaseSunBeanSun(Wallnut wallnut) {
        int sunAmount = wallnut.drainPendingSunAmount();
        if (sunAmount <= 0) {
            return;
        }
        addEntity(new Sun(sunAmount, wallnut.getEntityPosition()));
        pendingResults.add("plant Sun Bean produced " + sunAmount + " sun at "
                + wallnut.getEntityPosition());
    }

    private void applyWallnutExplosion(Wallnut wallnut) {
        int explosionDamage = wallnut.drainPendingExplosionDamage();
        if (explosionDamage <= 0 || wallnut.getEntityPosition() == null) {
            return;
        }
        EntityPosition center = wallnut.getEntityPosition();
        for (Zombie target : getZombies()) {
            if (isInsideThreeByThree(target, center)) {
                target.takeDamage(explosionDamage);
            }
        }
        pendingResults.add(wallnut.getName() + " exploded for " + explosionDamage
                + " damage around " + center + ".");
    }

    private static boolean isInsideThreeByThree(Zombie zombie, EntityPosition center) {
        return Math.abs(zombie.getLane() - center.getRow()) <= 1
                && Math.abs(zombie.getColumnPosition() - center.getColumn()) <= 1.0;
    }

    private BasePlant findNearestPlantAhead(Zombie zombie) {
        BasePlant nearestPlant = null;
        int nearestColumn = -1;
        for (BasePlant plant : getPlants()) {
            if (plant.isRemoved() || plant.getEntityPosition().getRow() != zombie.getLane()) {
                continue;
            }
            int plantColumn = plant.getEntityPosition().getColumn();
            if (plantColumn <= zombie.getColumnPosition() + POSITION_EPSILON
                    && isBetterBlocker(plant, nearestPlant, plantColumn, nearestColumn)) {
                nearestPlant = plant;
                nearestColumn = plantColumn;
            }
        }
        return nearestPlant;
    }

    private static boolean isBetterBlocker(BasePlant candidate, BasePlant current,
            int candidateColumn, int currentColumn) {
        if (candidateColumn > currentColumn) {
            return true;
        }
        return candidateColumn == currentColumn && isCover(candidate) && !isCover(current);
    }

    private static boolean isCover(BasePlant plant) {
        return plant instanceof Wallnut && ((Wallnut) plant).isCoverPlant();
    }

    private void reportDestroyedPlant(BasePlant plant) {
        if (plant.isDestroyed()) {
            pendingResults.add("Plant " + plant.getName() + " at "
                    + plant.getEntityPosition() + " is destroyed.");
        }
    }

    private void reportZombieDeath(Zombie zombie) {
        if (zombie.isDeathReported()) {
            return;
        }
        zombie.markDeathReported();
        zombie.markForRemoval();
        pendingResults.add("Zombie of type " + zombie.getName() + " is dead at ("
                + formatColumn(zombie.getColumnPosition()) + ", " + zombie.getLane() + ")");
    }

    private static String formatColumn(double column) {
        return String.format(java.util.Locale.ROOT, "%.2f", column);
    }

    public List<String> drainResults() {
        if (pendingResults.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>(pendingResults);
        pendingResults.clear();
        return Collections.unmodifiableList(results);
    }

    private static String buildSunProductionResult(SunProducer producer) {
        return "plant " + producer.getType().getDisplayName()
                + " produced a sun at " + producer.getEntityPosition();
    }

    public void addEntity(Entity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null");
        }
        validatePosition(entity.getEntityPosition());
        allEntities.add(entity);
    }

    public void addZombie(Zombie zombie) {
        addEntity(zombie);
    }

    public boolean canAddPlant(BasePlant plant) {
        if (plant == null || !isPositionInsideBoard(plant.getEntityPosition())) {
            return false;
        }
        List<BasePlant> plantsAtPosition = getPlantsAt(plant.getEntityPosition());
        if (plantsAtPosition.isEmpty()) {
            return !isCover(plant);
        }
        return isCover(plant) && plantsAtPosition.size() == 1
                && !isCover(plantsAtPosition.get(0));
    }

    public boolean addPlant(BasePlant plant) {
        if (!canAddPlant(plant)) {
            return false;
        }
        addEntity(plant);
        return true;
    }

    public boolean removeEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        entity.markForRemoval();
        return allEntities.remove(entity);
    }

    public boolean containsEntity(Entity entity) {
        return entity != null && allEntities.contains(entity) && !entity.isRemoved();
    }

    public List<BasePlant> getPlants() {
        List<BasePlant> plants = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof BasePlant && !entity.isRemoved()) {
                plants.add((BasePlant) entity);
            }
        }
        return Collections.unmodifiableList(plants);
    }

    public List<Zombie> getZombies() {
        List<Zombie> zombies = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof Zombie && !entity.isRemoved()) {
                zombies.add((Zombie) entity);
            }
        }
        return Collections.unmodifiableList(zombies);
    }

    public List<Sun> getSuns() {
        List<Sun> suns = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof Sun && !entity.isRemoved()) {
                suns.add((Sun) entity);
            }
        }
        return Collections.unmodifiableList(suns);
    }

    public List<BasePlant> getPlantsAt(EntityPosition position) {
        if (position == null) {
            return Collections.emptyList();
        }
        List<BasePlant> plants = new ArrayList<>();
        for (BasePlant plant : getPlants()) {
            if (position.equals(plant.getEntityPosition())) {
                plants.add(plant);
            }
        }
        return Collections.unmodifiableList(plants);
    }

    public BasePlant getPlantAt(EntityPosition position) {
        BasePlant fallback = null;
        for (BasePlant plant : getPlantsAt(position)) {
            if (isCover(plant)) {
                return plant;
            }
            fallback = plant;
        }
        return fallback;
    }

    public BasePlant removePlantAt(EntityPosition position) {
        BasePlant plant = getPlantAt(position);
        if (plant != null) {
            removeEntity(plant);
        }
        return plant;
    }

    public boolean isPositionInsideBoard(EntityPosition position) {
        return position != null
                && position.getRow() < numberOfRows
                && position.getColumn() < numberOfColumns;
    }

    public List<Sun> getSunsAt(EntityPosition position) {
        if (position == null) {
            return Collections.emptyList();
        }
        List<Sun> suns = new ArrayList<>();
        for (Sun sun : getSuns()) {
            if (position.equals(sun.getEntityPosition())) {
                suns.add(sun);
            }
        }
        return Collections.unmodifiableList(suns);
    }

    private void validatePosition(EntityPosition position) {
        if (position == null) {
            return;
        }
        if (position.getRow() >= numberOfRows || position.getColumn() >= numberOfColumns) {
            throw new IllegalArgumentException("Entity position is outside the board: " + position);
        }
    }

    private static void validateDeltaSeconds(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }

    public int getNumberOfColumns() {
        return numberOfColumns;
    }

    public List<Tile> getTiles() {
        return Collections.unmodifiableList(tiles);
    }

    public List<Entity> getAllEntities() {
        return Collections.unmodifiableList(new ArrayList<>(allEntities));
    }

    public List<BaseStructure> getStructures() {
        return Collections.unmodifiableList(structures);
    }
}
