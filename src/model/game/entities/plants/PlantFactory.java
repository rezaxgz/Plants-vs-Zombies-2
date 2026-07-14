package model.game.entities.plants;

import model.game.entities.EntityPosition;
import model.game.entities.plants.sunProducer.SunProducer;
import model.game.entities.plants.sunProducer.SunProducerPlantType;
import model.game.entities.plants.wallnut.Wallnut;
import model.game.entities.plants.wallnut.WallnutPlantType;

public final class PlantFactory {
    private PlantFactory() {
    }

    public static BasePlant createPlant(String typeName, EntityPosition position) {
        return createPlant(typeName, 1, position);
    }

    public static BasePlant createPlant(String typeName, int level, EntityPosition position) {
        BasePlant sunProducer = SunProducerPlantType.findByName(typeName)
                .map(type -> createSunProducer(type, position))
                .orElse(null);
        if (sunProducer != null) {
            return sunProducer;
        }
        return WallnutPlantType.findByName(typeName)
                .map(type -> createWallnut(type, level, position))
                .orElse(null);
    }

    public static SunProducer createSunProducer(SunProducerPlantType type, EntityPosition position) {
        return new SunProducer(type, position);
    }

    public static Wallnut createWallnut(WallnutPlantType type, EntityPosition position) {
        return createWallnut(type, 1, position);
    }

    public static Wallnut createWallnut(WallnutPlantType type, int level, EntityPosition position) {
        return new Wallnut(type, level, position);
    }
}
