package model.game.entities.plants.homing;

import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;

public class Homing extends BasePlant {
    public Homing() {
        super(PlantCategory.HOMING);
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        // Homing behavior will be implemented with the homing plant data.
    }
}
