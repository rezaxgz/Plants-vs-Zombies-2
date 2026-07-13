package model.game.entities.plants.lobber;

import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;

public class Lobber extends BasePlant {
    public Lobber() {
        super(PlantCategory.LOBBER);
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        // Lobber behavior will be implemented with the lobber plant data.
    }
}
