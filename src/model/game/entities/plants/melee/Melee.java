package model.game.entities.plants.melee;

import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;

public class Melee extends BasePlant {
    public Melee() {
        super(PlantCategory.MELEE);
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        // Melee behavior will be implemented with the melee plant data.
    }
}
