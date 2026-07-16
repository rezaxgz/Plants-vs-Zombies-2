package model.game.special;

import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;

/**
 * Setup phase with fixed sun, no producers, and no recharge usage.
 */
public final class PlantWhatYouGetSystem {
    private boolean setupActive = true;

    public boolean isPlantAllowed(
            BasePlant plant) {
        return plant != null
                && plant.getCategory()
                        != PlantCategory.SUN_PRODUCER;
    }

    public boolean startZombieWaves() {
        if (!setupActive) {
            return false;
        }
        setupActive = false;
        return true;
    }

    public boolean isSetupActive() {
        return setupActive;
    }
}
