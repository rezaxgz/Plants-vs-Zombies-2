package io.github.Plants_Vs_Zombies_2.model.game.entities.plants;

import java.util.Set;

/**
 * Read-only metadata shared by every plant definition enum.
 */
public interface PlantDefinition {
    int getId();

    String getDisplayName();

    Set<PlantTag> getTags();

    int getCost(int level);

    int getBaseHP(int level);

    default int getDamage(int level) {
        return 0;
    }

    default String getBaseAbility() {
        return "See the plant type implementation.";
    }

    default String getPlantFoodEffect() {
        return "See the plant type implementation.";
    }

    default String getLevelTwoUpgrade() {
        return "Defined by the level-2 plant stats.";
    }

    default String getLevelThreeUpgrade() {
        return "Defined by the level-3 plant stats.";
    }

    default String getLevelFourUpgrade() {
        return "Defined by the level-4 plant stats.";
    }

    default float getActionIntervalSeconds(int level) {
        return Float.NaN;
    }

    float getRechargeSeconds(int level);
}
