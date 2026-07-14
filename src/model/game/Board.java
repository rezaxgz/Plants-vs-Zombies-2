package model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Constants;
import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.other.Sun;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.shooter.Shooter;
import model.game.entities.plants.shooter.ShooterPlantType;
import model.game.entities.plants.sunProducer.SunProducer;
import model.game.entities.plants.wallnut.Wallnut;
import model.game.entities.projectile.Projectile;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.abilities.SmashAbility;
import model.game.entities.zombies.abilities.ZombieAbility;
import model.game.structure.BaseStructure;
import model.game.tile.Tile;

public class Board {
    private static final double POSITION_EPSILON = 0.000001;
    private static final double SWEET_POTATO_ATTRACTION_RANGE = 1.0;
    private static final double PROJECTILE_COLLISION_RADIUS = 0.35;
    private static final double PROJECTILE_BOARD_MARGIN = 0.5;

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
        resolveProjectileImpacts(updateSnapshot);
        reportDeadZombies(updateSnapshot);
        activateReadyShooters(updateSnapshot, entitiesToAdd);
        applyPendingShooterBoardEffects(updateSnapshot, entitiesToAdd);
        applyPendingSunProducerBoardEffects(updateSnapshot, entitiesToAdd);
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

    private void activateReadyShooters(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Shooter) || entity.isRemoved()) {
                continue;
            }
            Shooter shooter = (Shooter) entity;
            if (shooter.isReadyToShoot() && hasTarget(shooter)) {
                entitiesToAdd.addAll(shooter.shoot(numberOfRows));
            }
        }
    }

    private boolean hasTarget(Shooter shooter) {
        for (Zombie zombie : getZombies()) {
            if (shooter.canTarget(zombie, numberOfRows)) {
                return true;
            }
        }
        return false;
    }

    private void applyPendingShooterBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Shooter) || entity.isRemoved()) {
                continue;
            }
            Shooter mint = (Shooter) entity;
            if (mint.drainFamilyBoostPending()) {
                applyShooterFamilyBoost(mint, entitiesToAdd);
            }
        }
    }

    private void applyShooterFamilyBoost(Shooter mint, List<Entity> entitiesToAdd) {
        for (BasePlant plant : getPlants()) {
            if (!(plant instanceof Shooter) || plant == mint) {
                continue;
            }
            Shooter shooter = (Shooter) plant;
            shooter.usePlantFood(numberOfRows);
            entitiesToAdd.addAll(shooter.drainProjectiles());
        }
        mint.markForRemoval();
        pendingResults.add("Appease-mint applied plant food to every Shooter plant.");
    }

    private void resolveProjectileImpacts(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof Projectile) || entity.isRemoved()) {
                continue;
            }
            Projectile projectile = (Projectile) entity;
            Zombie target = findFirstZombieHit(projectile);
            if (target != null) {
                projectile.hit(target);
                if (target.isDead()) {
                    reportZombieDeath(target);
                }
            } else if (projectile.hasExpired() || isProjectileOutsideBoard(projectile)) {
                projectile.markForRemoval();
            }
        }
    }

    private Zombie findFirstZombieHit(Projectile projectile) {
        Zombie firstTarget = null;
        double firstParameter = Double.POSITIVE_INFINITY;
        for (Zombie zombie : getZombies()) {
            if (zombie.isDead() || zombie.isSubmerged()) {
                continue;
            }
            double parameter = projectile.getIntersectionParameter(
                    zombie.getLane(), zombie.getColumnPosition(), PROJECTILE_COLLISION_RADIUS);
            if (!Double.isNaN(parameter) && parameter < firstParameter) {
                firstParameter = parameter;
                firstTarget = zombie;
            }
        }
        return firstTarget;
    }

    private boolean isProjectileOutsideBoard(Projectile projectile) {
        return projectile.getRowPosition() < -PROJECTILE_BOARD_MARGIN
                || projectile.getRowPosition() > numberOfRows - 1 + PROJECTILE_BOARD_MARGIN
                || projectile.getColumnPosition() < -PROJECTILE_BOARD_MARGIN
                || projectile.getColumnPosition() > numberOfColumns - 1 + PROJECTILE_BOARD_MARGIN;
    }

    private void reportDeadZombies(List<Entity> updateSnapshot) {
        for (Entity entity : updateSnapshot) {
            if (entity instanceof Zombie && ((Zombie) entity).isDead()) {
                reportZombieDeath((Zombie) entity);
            }
        }
    }


    private void applyPendingSunProducerBoardEffects(List<Entity> updateSnapshot,
            List<Entity> entitiesToAdd) {
        for (Entity entity : updateSnapshot) {
            if (!(entity instanceof SunProducer) || entity.isRemoved()) {
                continue;
            }
            SunProducer mint = (SunProducer) entity;
            if (!mint.drainFamilyBoostPending()) {
                continue;
            }
            applySunProducerFamilyBoost(mint, entitiesToAdd);
        }
    }

    private void applySunProducerFamilyBoost(SunProducer mint,
            List<Entity> entitiesToAdd) {
        for (BasePlant plant : getPlants()) {
            if (!(plant instanceof SunProducer) || plant == mint) {
                continue;
            }
            SunProducer producer = (SunProducer) plant;
            producer.usePlantFood();
            collectProducedSuns(producer, entitiesToAdd);
        }
        mint.markForRemoval();
        pendingResults.add("Enlighten-mint applied plant food to every Sun Producer plant.");
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
            attackPlant(zombie, blockingPlant, deltaSeconds);
            handleWallnutAfterAttack(zombie, blockingPlant, deltaSeconds);
            reportDestroyedPlant(blockingPlant);
        } else {
            zombie.move(deltaSeconds, attackColumn);
        }
    }

    private void attackPlant(Zombie zombie, BasePlant plant, float deltaSeconds) {
        if (!trySmashPlant(zombie, plant)) {
            zombie.eat(plant, deltaSeconds);
        }
    }

    private boolean trySmashPlant(Zombie zombie, BasePlant plant) {
        for (ZombieAbility ability : zombie.getAbilities()) {
            if (ability instanceof SmashAbility && ability.tryUse(zombie, this)) {
                plant.takeDamage(Integer.MAX_VALUE);
                return true;
            }
        }
        return false;
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
        addEntity(Sun.createPlantSun(sunAmount, wallnut.getEntityPosition()));
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
        if (isPeaPod(plant) && plantsAtPosition.size() < 5) {
            return plantsAtPosition.stream().allMatch(Board::isPeaPod);
        }
        return isCover(plant) && plantsAtPosition.size() == 1
                && !isCover(plantsAtPosition.get(0));
    }

    private static boolean isPeaPod(BasePlant plant) {
        return plant instanceof Shooter
                && ((Shooter) plant).getType() == ShooterPlantType.PEA_POD;
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

    public List<Projectile> getProjectiles() {
        List<Projectile> projectiles = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof Projectile && !entity.isRemoved()) {
                projectiles.add((Projectile) entity);
            }
        }
        return Collections.unmodifiableList(projectiles);
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
