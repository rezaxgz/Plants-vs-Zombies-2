package model.game.entities.plants;

import model.game.entities.EntityPosition;
import model.game.entities.plants.sunProducer.SunProducer;
import model.game.entities.plants.sunProducer.SunProducerPlantType;

public final class PlantFactory {
    private PlantFactory() {
    }

    public static SunProducer createSunProducer(SunProducerPlantType type, EntityPosition position) {
        return new SunProducer(type, position);
    }
}
