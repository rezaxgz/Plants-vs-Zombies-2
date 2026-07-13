package model.game.entities.plants.modifier;

import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;

public class Modifier extends BasePlant {
    public Modifier() {
        super(PlantCategory.MODIFIER);
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        // Modifier behavior will be implemented with the modifier plant data.
    }
}
