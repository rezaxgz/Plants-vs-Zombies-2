package io.github.Plants_Vs_Zombies_2.model.quest;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.ChapterRuleset;

/** Immutable facts collected from one completed game. */
public final class QuestRunSummary {
    private final boolean won;
    private final String chapterId;
    private final ChapterRuleset chapterRuleset;
    private final int difficultyLevel;
    private final int finalSun;
    private final int collectedSun;
    private final int zombieKills;
    private final int plantAttributedKills;
    private final int killsWithinThirtySeconds;
    private final int firstColumnKillsWithoutMower;
    private final int lostPlants;
    private final int explosivePlantsUsed;
    private final int placedPlantCount;
    private final int sunProducerPlantCount;
    private final boolean allPlacedPlantsAreShrooms;
    private final boolean symmetricalGarden;
    private final Set<String> offensivePlantNames;
    private final Set<String> offensivePlantFamilies;
    private final Set<String> allPlantFamilies;
    private final Set<Integer> plantedRows;
    private final Set<Integer> plantedColumns;

    QuestRunSummary(boolean won, String chapterId,
            ChapterRuleset chapterRuleset, int difficultyLevel,
            int finalSun, int collectedSun, int zombieKills,
            int plantAttributedKills,
            int killsWithinThirtySeconds,
            int firstColumnKillsWithoutMower, int lostPlants,
            int explosivePlantsUsed, int placedPlantCount,
            int sunProducerPlantCount, boolean allPlacedPlantsAreShrooms,
            boolean symmetricalGarden, Set<String> offensivePlantNames,
            Set<String> offensivePlantFamilies, Set<String> allPlantFamilies,
            Set<Integer> plantedRows, Set<Integer> plantedColumns) {
        this.won = won;
        this.chapterId = normalize(chapterId);
        this.chapterRuleset = chapterRuleset;
        this.difficultyLevel = difficultyLevel;
        this.finalSun = finalSun;
        this.collectedSun = collectedSun;
        this.zombieKills = zombieKills;
        this.plantAttributedKills = plantAttributedKills;
        this.killsWithinThirtySeconds = killsWithinThirtySeconds;
        this.firstColumnKillsWithoutMower = firstColumnKillsWithoutMower;
        this.lostPlants = lostPlants;
        this.explosivePlantsUsed = explosivePlantsUsed;
        this.placedPlantCount = placedPlantCount;
        this.sunProducerPlantCount = sunProducerPlantCount;
        this.allPlacedPlantsAreShrooms = allPlacedPlantsAreShrooms;
        this.symmetricalGarden = symmetricalGarden;
        this.offensivePlantNames = immutableCopy(offensivePlantNames);
        this.offensivePlantFamilies = immutableCopy(offensivePlantFamilies);
        this.allPlantFamilies = immutableCopy(allPlantFamilies);
        this.plantedRows = immutableCopy(plantedRows);
        this.plantedColumns = immutableCopy(plantedColumns);
    }

    private static <T> Set<T> immutableCopy(Set<T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    public boolean isWon() { return won; }
    public String getChapterId() { return chapterId; }
    public ChapterRuleset getChapterRuleset() { return chapterRuleset; }
    public int getDifficultyLevel() { return difficultyLevel; }
    public int getFinalSun() { return finalSun; }
    public int getCollectedSun() { return collectedSun; }
    public int getZombieKills() { return zombieKills; }
    public int getPlantAttributedKills() { return plantAttributedKills; }
    public int getKillsWithinThirtySeconds() { return killsWithinThirtySeconds; }
    public int getFirstColumnKillsWithoutMower() { return firstColumnKillsWithoutMower; }
    public int getLostPlants() { return lostPlants; }
    public int getExplosivePlantsUsed() { return explosivePlantsUsed; }
    public int getPlacedPlantCount() { return placedPlantCount; }
    public int getSunProducerPlantCount() { return sunProducerPlantCount; }
    public boolean areAllPlacedPlantsShrooms() { return allPlacedPlantsAreShrooms; }
    public boolean isSymmetricalGarden() { return symmetricalGarden; }
    public Set<String> getOffensivePlantNames() { return offensivePlantNames; }
    public Set<String> getOffensivePlantFamilies() { return offensivePlantFamilies; }
    public Set<String> getAllPlantFamilies() { return allPlantFamilies; }

    public boolean wasRowNeverPlanted(int oneBasedRow) {
        return !plantedRows.contains(oneBasedRow - 1);
    }

    public boolean wasColumnNeverPlanted(int oneBasedColumn) {
        return !plantedColumns.contains(oneBasedColumn - 1);
    }

    public boolean usedOnlyOffensivePlant(String plantName) {
        return offensivePlantNames.size() == 1
                && offensivePlantNames.contains(normalize(plantName));
    }

    public int getKillsByOnlyPlant(String plantName) {
        return usedOnlyOffensivePlant(plantName) ? plantAttributedKills : 0;
    }

    public boolean usedOnlyOffensiveFamily(String family) {
        return offensivePlantFamilies.size() == 1
                && offensivePlantFamilies.contains(normalize(family));
    }

    public int getKillsByOnlyFamily(String family) {
        return usedOnlyOffensiveFamily(family) ? plantAttributedKills : 0;
    }

    public boolean usedPlantFamily(String family) {
        return allPlantFamilies.contains(normalize(family));
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
