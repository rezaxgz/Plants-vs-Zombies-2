package io.github.Plants_Vs_Zombies_2.model.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.Game;
import io.github.Plants_Vs_Zombies_2.model.game.GameStatus;
import io.github.Plants_Vs_Zombies_2.model.game.defense.LawnMower;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantCategory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFamily;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantTag;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

/** Collects quest telemetry without coupling individual quests to game logic. */
public final class QuestRunTracker implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final Set<BasePlant> trackedPlants = new HashSet<>();
    private final Set<String> offensivePlantNames = new LinkedHashSet<>();
    private final Set<String> offensivePlantFamilies = new LinkedHashSet<>();
    private final Set<String> allPlantFamilies = new LinkedHashSet<>();
    private final Map<String, String> familyByPlantName = new HashMap<>();
    private final Set<Integer> plantedRows = new LinkedHashSet<>();
    private final Set<Integer> plantedColumns = new LinkedHashSet<>();
    private int collectedSun;
    private int zombieKills;
    private int plantAttributedKills;
    private int killsWithinThirtySeconds;
    private int firstColumnKillsWithoutMower;
    private int lostPlants;
    private int explosivePlantsUsed;
    private int placedPlantCount;
    private int sunProducerPlantCount;
    private int nonShroomPlantCount;
    private double firstWaveStartedAt = Double.NaN;

    public void recordSunCollected(int amount) {
        if (amount > 0) {
            collectedSun += amount;
        }
    }

    public void recordZombieSpawn(double elapsedSeconds) {
        if (Double.isNaN(firstWaveStartedAt)) {
            firstWaveStartedAt = elapsedSeconds;
        }
    }

    public void recordZombieDeaths(List<Zombie> zombies,
            double elapsedSeconds, List<LawnMower> mowers) {
        if (zombies == null) {
            return;
        }
        for (Zombie zombie : zombies) {
            if (zombie == null) {
                continue;
            }
            zombieKills++;
            String sourcePlant = QuestRunSummary.normalize(
                    zombie.getLastDamageSourcePlantName());
            if (!sourcePlant.isEmpty()) {
                plantAttributedKills++;
                offensivePlantNames.add(sourcePlant);
                String family = familyByPlantName.get(sourcePlant);
                if (family != null && !family.isEmpty()) {
                    offensivePlantFamilies.add(family);
                }
            }
            if (!Double.isNaN(firstWaveStartedAt)
                    && elapsedSeconds - firstWaveStartedAt <= 30.0) {
                killsWithinThirtySeconds++;
            }
            if (zombie.getColumnPosition() < 1.0
                    && isMowerUsed(mowers, zombie.getLane())) {
                firstColumnKillsWithoutMower++;
            }
        }
    }

    private static boolean isMowerUsed(List<LawnMower> mowers, int row) {
        if (mowers == null) {
            return false;
        }
        for (LawnMower mower : mowers) {
            if (mower.getRow() == row) {
                return mower.isUsed();
            }
        }
        return false;
    }

    public void recordPlantPlaced(BasePlant plant) {
        if (plant == null || plant.getEntityPosition() == null) {
            return;
        }
        trackedPlants.add(plant);
        placedPlantCount++;
        EntityPosition position = plant.getEntityPosition();
        plantedRows.add(position.getRow());
        plantedColumns.add(position.getColumn());
        PlantFamily family = PlantFamily.findForPlant(plant);
        if (family != null) {
            String normalizedFamily = QuestRunSummary.normalize(family.name());
            allPlantFamilies.add(normalizedFamily);
            familyByPlantName.put(QuestRunSummary.normalize(plant.getName()),
                    normalizedFamily);
        }
        if (plant.getCategory() == PlantCategory.SUN_PRODUCER) {
            sunProducerPlantCount++;
        }
        if (plant.getCategory() == PlantCategory.EXPLOSIVE) {
            explosivePlantsUsed++;
        }
        if (!plant.hasTag(PlantTag.SHROOM)) {
            nonShroomPlantCount++;
        }
    }

    public void forgetPluckedPlant(BasePlant plant) {
        trackedPlants.remove(plant);
    }

    public void capturePlantLosses(Board board) {
        if (board == null || trackedPlants.isEmpty()) {
            return;
        }
        List<BasePlant> remaining = board.getPlants();
        for (BasePlant plant : new ArrayList<>(trackedPlants)) {
            if (!remaining.contains(plant)) {
                trackedPlants.remove(plant);
                lostPlants++;
            }
        }
    }

    public QuestRunSummary createSummary(Game game, String chapterId) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        capturePlantLosses(game.getBoard());
        return new QuestRunSummary(
                game.getStatus() == GameStatus.WON,
                chapterId, game.getChapterRuleset(),
                game.getDifficultyLevel(), game.getSunCount(),
                collectedSun, zombieKills, plantAttributedKills,
                killsWithinThirtySeconds,
                firstColumnKillsWithoutMower, lostPlants,
                explosivePlantsUsed, placedPlantCount,
                sunProducerPlantCount,
                placedPlantCount > 0 && nonShroomPlantCount == 0,
                isGardenSymmetrical(game.getBoard()),
                offensivePlantNames, offensivePlantFamilies,
                allPlantFamilies, plantedRows, plantedColumns);
    }

    private static boolean isGardenSymmetrical(Board board) {
        if (board.getPlants().isEmpty()) {
            return false;
        }
        Map<String, String> plantsByPosition = new HashMap<>();
        for (BasePlant plant : board.getPlants()) {
            EntityPosition position = plant.getEntityPosition();
            if (position != null) {
                plantsByPosition.put(key(position.getRow(), position.getColumn()),
                        QuestRunSummary.normalize(plant.getName()));
            }
        }
        int rows = board.getNumberOfRows();
        for (int row = 0; row < rows / 2; row++) {
            int mirror = rows - 1 - row;
            for (int column = 0; column < board.getNumberOfColumns(); column++) {
                String first = plantsByPosition.get(key(row, column));
                String second = plantsByPosition.get(key(mirror, column));
                if (first == null ? second != null : !first.equals(second)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String key(int row, int column) {
        return row + ":" + column;
    }
}
