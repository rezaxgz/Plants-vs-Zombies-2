package io.github.some_example_name.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.some_example_name.model.App;
import io.github.some_example_name.model.game.entities.Entity;
import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.other.CollectibleDrop;
import io.github.some_example_name.model.game.entities.other.IceBlock;
import io.github.some_example_name.model.game.entities.other.PushedObstacle;
import io.github.some_example_name.model.game.entities.other.Sun;
import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.plants.PlantTag;
import io.github.some_example_name.model.game.entities.plants.explosive.Explosive;
import io.github.some_example_name.model.game.entities.plants.explosive.ExplosivePlantType;
import io.github.some_example_name.model.game.entities.plants.modifier.Modifier;
import io.github.some_example_name.model.game.entities.plants.shooter.Shooter;
import io.github.some_example_name.model.game.entities.plants.shooter.ShooterPlantType;
import io.github.some_example_name.model.game.entities.projectile.BouncingGrape;
import io.github.some_example_name.model.game.entities.projectile.Projectile;
import io.github.some_example_name.model.game.entities.zombies.Zombie;
import io.github.some_example_name.model.game.tile.Tile;
import io.github.some_example_name.model.game.tile.TileType;

abstract class BoardEntityLogic extends BoardZombieCombatLogic {
    protected BoardEntityLogic() {
        super();
    }

    protected BoardEntityLogic(int numberOfRows, int numberOfColumns) {
        super(numberOfRows, numberOfColumns);
    }

    public List<String> drainResults() {
        if (pendingResults.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>(pendingResults);
        pendingResults.clear();
        return Collections.unmodifiableList(results);
    }

    public void addEntity(Entity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null");
        }
        validatePosition(entity.getEntityPosition());
        allEntities.add(entity);
    }

    public void addZombie(Zombie zombie) {
        if (zombie != null && frostbiteCavesRules) {
            zombie.setChapterColdImmune(true);
        }
        addEntity(zombie);
        if (App.getInstance().getLoggedInUser() != null) {
            App.getInstance().getLoggedInUser().unlockZombie(zombie.getType().getAlias());
        }
    }

    public boolean canAddPlant(BasePlant requestedPlant) {
        BasePlant plant = resolvePlacedPlant(requestedPlant);
        if (plant == null
                || !isPositionInsideBoard(plant.getEntityPosition())
                || !canPlantOnTerrain(plant)) {
            return false;
        }
        EntityPosition position = plant.getEntityPosition();
        List<BasePlant> plantsAtPosition = getPlantsAt(position);
        Tile tile = getTileAt(position);
        if (tile != null
                && tile.getTileType() == TileType.WATER) {
            return canAddPlantInWater(plant, plantsAtPosition);
        }
        return canAddPlantOnLand(plant, plantsAtPosition);
    }

    boolean canAddPlantInWater(BasePlant plant,
            List<BasePlant> plantsAtPosition) {
        if (isLilyPad(plant)) {
            return plantsAtPosition.isEmpty();
        }
        boolean hasLilyPad = plantsAtPosition.stream()
                .anyMatch(Board::isLilyPad);
        List<BasePlant> plantsAbovePad = new ArrayList<>();
        for (BasePlant existing : plantsAtPosition) {
            if (!isLilyPad(existing)) {
                plantsAbovePad.add(existing);
            }
        }
        if (plant.hasTag(PlantTag.WATER)) {
            return plantsAtPosition.isEmpty();
        }
        if (!hasLilyPad) {
            return false;
        }
        if (plantsAbovePad.isEmpty()) {
            return !isCover(plant);
        }
        if (isPeaPod(plant) && plantsAbovePad.size() < 5) {
            return plantsAbovePad.stream()
                    .allMatch(Board::isPeaPod);
        }
        return isCover(plant)
                && plantsAbovePad.size() == 1
                && !isCover(plantsAbovePad.get(0));
    }

    BasePlant resolvePlacedPlant(BasePlant plant) {
        if (!(plant instanceof Modifier) || !((Modifier) plant).isImitater()) {
            return plant;
        }
        Modifier imitater = (Modifier) plant;
        return imitater.hasValidImitatedPlant()
                ? imitater.getImitatedPlant()
                : null;
    }

    boolean canPlantOnTerrain(BasePlant plant) {
        Tile tile = getTileAt(plant.getEntityPosition());
        if (tile == null) {
            return false;
        }
        if (plant instanceof Explosive) {
            ExplosivePlantType type = ((Explosive) plant).getType();
            if (type == ExplosivePlantType.HOT_POTATO) {
                return tile.getTileType() == TileType.FROZEN;
            }
            if (type == ExplosivePlantType.GRAVE_BUSTER) {
                return hasGraveAt(plant.getEntityPosition());
            }
            if (type == ExplosivePlantType.TANGLE_KELP) {
                return tile.getTileType() == TileType.WATER;
            }
        }
        if (tile.getTileType() == TileType.WATER) {
            return plant.getTags().contains(PlantTag.WATER)
                    || getPlantsAt(plant.getEntityPosition()).stream()
                            .anyMatch(Board::isLilyPad);
        }
        return tile.isPlantableTerrain();
    }

    public boolean movePlant(BasePlant plant,
            EntityPosition destination) {
        if (plant == null || plant.isRemoved()
                || plant.getEntityPosition() == null
                || !isPositionInsideBoard(destination)
                || destination.equals(plant.getEntityPosition())
                || !getPlantsAt(destination).isEmpty()
                || getStructureAt(destination) != null) {
            return false;
        }

        Tile destinationTile = getTileAt(destination);
        if (destinationTile == null
                || !canMovePlantOntoTile(plant, destinationTile)) {
            return false;
        }

        EntityPosition oldPosition = plant.getEntityPosition();
        plant.setEntityPosition(destination);
        refreshTilePlant(oldPosition);
        destinationTile.setPlant(plant);
        return true;
    }

    boolean canMovePlantOntoTile(BasePlant plant, Tile tile) {
        if (tile.getTileType() == TileType.WATER) {
            return plant.getTags().contains(PlantTag.WATER);
        }
        return tile.isPlantableTerrain();
    }

    public boolean addPlant(BasePlant requestedPlant) {
        return addPlantInternal(requestedPlant, true);
    }

    boolean addPlantInternal(BasePlant requestedPlant,
            boolean applyActiveFamilyBoosts) {
        if (!canAddPlant(requestedPlant)) {
            return false;
        }
        BasePlant plant = resolvePlacedPlant(requestedPlant);
        addEntity(plant);
        Tile tile = getTileAt(plant.getEntityPosition());
        if (tile != null) {
            tile.setPlant(plant);
        }
        applyImitaterEntranceEffect(requestedPlant, plant);
        if (applyActiveFamilyBoosts) {
            applyActiveFamilyBoostsToPlant(plant);
        }
        return true;
    }

    void applyImitaterEntranceEffect(BasePlant requestedPlant,
            BasePlant placedPlant) {
        if (!(requestedPlant instanceof Modifier)) {
            return;
        }
        Modifier imitater = (Modifier) requestedPlant;
        if (!imitater.isImitater() || !imitater.appliesPlantFoodOnEntrance()) {
            return;
        }
        List<Entity> spawnedEntities = new ArrayList<>();
        applyPlantFoodToPlant(placedPlant, spawnedEntities);
        for (Entity entity : spawnedEntities) {
            addEntity(entity);
        }
    }

    public PlantFoodResult usePlantFoodAt(EntityPosition position) {
        BasePlant plant = getPlantAt(position);
        if (plant == null) {
            return PlantFoodResult.NO_PLANT;
        }
        List<Entity> spawnedEntities = new ArrayList<>();
        boolean applied = applyPlantFoodAtPosition(plant, position, spawnedEntities);
        if (!applied) {
            return PlantFoodResult.NO_EFFECT;
        }
        for (Entity entity : spawnedEntities) {
            addEntity(entity);
        }
        pendingResults.add(plant.getName() + " received plant food.");
        return PlantFoodResult.SUCCESS;
    }

    boolean applyPlantFoodAtPosition(BasePlant plant, EntityPosition position,
            List<Entity> spawnedEntities) {
        if (!(plant instanceof Shooter)
                || ((Shooter) plant).getType() != ShooterPlantType.PEA_POD) {
            return applyPlantFoodToPlant(plant, spawnedEntities);
        }
        boolean applied = false;
        for (BasePlant stackedPlant : getPlantsAt(position)) {
            if (stackedPlant instanceof Shooter
                    && ((Shooter) stackedPlant).getType() == ShooterPlantType.PEA_POD) {
                applied |= applyPlantFoodToPlant(stackedPlant, spawnedEntities);
            }
        }
        return applied;
    }

    public boolean removeEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        EntityPosition plantPosition = entity instanceof BasePlant
                ? entity.getEntityPosition()
                : null;
        entity.markForRemoval();
        boolean removed = allEntities.remove(entity);
        if (plantPosition != null) {
            refreshTilePlant(plantPosition);
        }
        return removed;
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

    public List<IceBlock> getIceBlocks() {
        List<IceBlock> iceBlocks = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof IceBlock && !entity.isRemoved()) {
                iceBlocks.add((IceBlock) entity);
            }
        }
        return Collections.unmodifiableList(iceBlocks);
    }

    public List<PushedObstacle> getPushedObstacles() {
        List<PushedObstacle> obstacles = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof PushedObstacle
                    && !entity.isRemoved()) {
                obstacles.add((PushedObstacle) entity);
            }
        }
        return Collections.unmodifiableList(obstacles);
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

    public List<CollectibleDrop> getCollectibleDrops() {
        List<CollectibleDrop> drops = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof CollectibleDrop && !entity.isRemoved()) {
                drops.add((CollectibleDrop) entity);
            }
        }
        return Collections.unmodifiableList(drops);
    }

    public List<CollectibleDrop> getCollectibleDropsAt(EntityPosition position) {
        if (position == null) {
            return Collections.emptyList();
        }
        List<CollectibleDrop> drops = new ArrayList<>();
        for (CollectibleDrop drop : getCollectibleDrops()) {
            if (position.equals(drop.getEntityPosition())) {
                drops.add(drop);
            }
        }
        return Collections.unmodifiableList(drops);
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

    public List<BouncingGrape> getBouncingGrapes() {
        List<BouncingGrape> grapes = new ArrayList<>();
        for (Entity entity : allEntities) {
            if (entity instanceof BouncingGrape && !entity.isRemoved()) {
                grapes.add((BouncingGrape) entity);
            }
        }
        return Collections.unmodifiableList(grapes);
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
                && position.getRow() >= 0
                && position.getColumn() >= 0
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

    void validatePosition(EntityPosition position) {
        if (position == null) {
            return;
        }
        if (position.getRow() >= numberOfRows || position.getColumn() >= numberOfColumns) {
            throw new IllegalArgumentException("Entity position is outside the board: " + position);
        }
    }
}
