package model.game.entities.plants.strikeThrough;

import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantCategory;

public class StrikeThrough extends BasePlant {
    public StrikeThrough() {
        super(PlantCategory.STRIKE_THROUGH);
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        // Strike-through behavior will be implemented with its plant data.
    }
}
