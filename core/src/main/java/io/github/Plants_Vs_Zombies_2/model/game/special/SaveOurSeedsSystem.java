package io.github.Plants_Vs_Zombies_2.model.game.special;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFactory;

/**
 * Places and tracks the plants that must survive Save Our Seeds.
 */
public final class SaveOurSeedsSystem implements java.io.Serializable {
    private static final class ProtectedPlantEntry implements java.io.Serializable {
        private final BasePlant plant;
        private final EntityPosition originalPosition;

        private ProtectedPlantEntry(
                BasePlant plant,
                EntityPosition originalPosition) {
            this.plant = plant;
            this.originalPosition = originalPosition;
        }
    }

    private final List<ProtectedPlantEntry> entries;
    private final List<String> startMessages;

    public SaveOurSeedsSystem(
            Board board,
            List<ProtectedPlantSpec> specs) {
        if (board == null || specs == null
                || specs.isEmpty()) {
            throw new IllegalArgumentException(
                    "board and protected plants are required");
        }

        List<BasePlant> plants = createAndValidatePlants(board, specs);
        entries = new ArrayList<>();
        startMessages = new ArrayList<>();

        for (int index = 0; index < specs.size(); index++) {
            ProtectedPlantSpec spec = specs.get(index);
            BasePlant plant = plants.get(index);
            if (!board.addPlant(plant)) {
                throw new IllegalStateException(
                        "could not place protected plant at "
                                + spec.getPosition());
            }
            entries.add(new ProtectedPlantEntry(
                    plant, spec.getPosition()));
            startMessages.add(
                    "WARNING - RED DEFENSE LINE: protect "
                            + plant.getName() + " at "
                            + spec.getPosition()
                            + " in row "
                            + spec.getPosition().getRow()
                            + ".");
        }
    }

    private static List<BasePlant> createAndValidatePlants(
            Board board,
            List<ProtectedPlantSpec> specs) {
        Set<EntityPosition> positions = new HashSet<>();
        List<BasePlant> plants = new ArrayList<>();

        for (ProtectedPlantSpec spec : specs) {
            if (spec == null
                    || !board.isPositionInsideBoard(
                            spec.getPosition())) {
                throw new IllegalArgumentException(
                        "protected plant position is outside the board");
            }
            if (!positions.add(spec.getPosition())) {
                throw new IllegalArgumentException(
                        "protected plant positions must be unique");
            }

            BasePlant plant = PlantFactory.createPlant(
                    spec.getPlantType(),
                    spec.getPosition());
            if (plant == null) {
                throw new IllegalArgumentException(
                        "unknown protected plant type: "
                                + spec.getPlantType());
            }
            if (!board.canAddPlant(plant)) {
                throw new IllegalArgumentException(
                        "protected plant cannot be placed at "
                                + spec.getPosition());
            }
            plants.add(plant);
        }
        return plants;
    }

    public ProtectedPlantStatus findFailedPlant(
            Board board) {
        for (ProtectedPlantEntry entry : entries) {
            if (!isAlive(board, entry.plant)) {
                return createStatus(
                        board, entry);
            }
        }
        return null;
    }

    public boolean isProtectedPlantAt(
            EntityPosition position) {
        if (position == null) {
            return false;
        }
        for (ProtectedPlantEntry entry : entries) {
            if (position.equals(
                    entry.plant.getEntityPosition())
                    && !entry.plant.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    public List<ProtectedPlantStatus> getStatuses(
            Board board) {
        List<ProtectedPlantStatus> statuses = new ArrayList<>();
        for (ProtectedPlantEntry entry : entries) {
            statuses.add(createStatus(
                    board, entry));
        }
        return Collections.unmodifiableList(statuses);
    }

    private static ProtectedPlantStatus createStatus(
            Board board,
            ProtectedPlantEntry entry) {
        BasePlant plant = entry.plant;
        return new ProtectedPlantStatus(
                plant.getName(),
                entry.originalPosition,
                plant.getEntityPosition(),
                plant.getCurrentHP(),
                plant.getBaseHP(),
                isAlive(board, plant));
    }

    private static boolean isAlive(
            Board board, BasePlant plant) {
        return plant != null
                && !plant.isDestroyed()
                && !plant.isRemoved()
                && board.containsEntity(plant);
    }

    public List<String> getStartMessages() {
        return Collections.unmodifiableList(
                new ArrayList<>(startMessages));
    }
}
