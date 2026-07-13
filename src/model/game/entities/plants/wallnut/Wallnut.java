package model.game.entities.plants.wallnut;

import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;

public class Wallnut extends BasePlant {
    public Wallnut() {
        super(PlantCategory.WALL_NUT);
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        // Wall-nut behavior will be implemented with the wall-nut plant data.
    }
}
