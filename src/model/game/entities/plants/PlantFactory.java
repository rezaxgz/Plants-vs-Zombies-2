package model.game.entities.plants;

import model.game.entities.EntityPosition;
import model.game.entities.plants.sunProducer.SunProducer;
import model.game.entities.plants.sunProducer.SunProducerPlantType;

public final class PlantFactory {
    private PlantFactory() {
    }

    public static BasePlant createPlant(String typeName, EntityPosition position) {
        return SunProducerPlantType.findByName(typeName)
                .map(type -> createSunProducer(type, position))
                .orElse(null);
    }

    public static SunProducer createSunProducer(SunProducerPlantType type, EntityPosition position) {
        return new SunProducer(type, position);
    }
}
