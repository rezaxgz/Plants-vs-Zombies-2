package model.game.entities.plants;

import model.game.entities.EntityPosition;
import model.game.entities.plants.shooter.Shooter;
import model.game.entities.plants.shooter.ShooterPlantType;
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
                .map(type -> createSunProducer(type, level, position))
                .orElse(null);
        if (sunProducer != null) {
            return sunProducer;
        }
        BasePlant shooter = ShooterPlantType.findByName(typeName)
                .map(type -> createShooter(type, level, position))
                .orElse(null);
        if (shooter != null) {
            return shooter;
        }
        return WallnutPlantType.findByName(typeName)
                .map(type -> createWallnut(type, level, position))
                .orElse(null);
    }

    public static SunProducer createSunProducer(SunProducerPlantType type, EntityPosition position) {
        return createSunProducer(type, 1, position);
    }

    public static SunProducer createSunProducer(SunProducerPlantType type, int level,
            EntityPosition position) {
        return new SunProducer(type, level, position);
    }

    public static Wallnut createWallnut(WallnutPlantType type, EntityPosition position) {
        return createWallnut(type, 1, position);
    }

    public static Shooter createShooter(ShooterPlantType type, EntityPosition position) {
        return createShooter(type, 1, position);
    }

    public static Shooter createShooter(ShooterPlantType type, int level,
            EntityPosition position) {
        return new Shooter(type, level, position);
    }

    public static Wallnut createWallnut(WallnutPlantType type, int level, EntityPosition position) {
        return new Wallnut(type, level, position);
    }
}
