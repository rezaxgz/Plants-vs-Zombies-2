package model.game.entities.plants.shooter;

import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;

public class Shooter extends BasePlant {
    public Shooter() {
        super(PlantCategory.SHOOTER);
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        // Shooter behavior will be implemented with the shooter plant data.
    }
}
