package model.game.entities.plants.explosive;

import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;

public class Explosive extends BasePlant {
    public Explosive() {
        super(PlantCategory.EXPLOSIVE);
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        // Explosive behavior will be implemented with the explosive plant data.
    }
}
