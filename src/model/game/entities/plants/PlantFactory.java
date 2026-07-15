package model.game.entities.plants;

import model.game.entities.EntityPosition;
import model.game.entities.plants.explosive.Explosive;
import model.game.entities.plants.explosive.ExplosivePlantType;
import model.game.entities.plants.homing.Homing;
import model.game.entities.plants.homing.HomingPlantType;
import model.game.entities.plants.lobber.Lobber;
import model.game.entities.plants.lobber.LobberPlantType;
import model.game.entities.plants.melee.Melee;
import model.game.entities.plants.melee.MeleePlantType;
import model.game.entities.plants.shooter.Shooter;
import model.game.entities.plants.shooter.ShooterPlantType;
import model.game.entities.plants.strikeThrough.StrikeThrough;
import model.game.entities.plants.strikeThrough.StrikeThroughPlantType;
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
        BasePlant homing = HomingPlantType.findByName(typeName)
                .map(type -> createHoming(type, level, position))
                .orElse(null);
        if (homing != null) {
            return homing;
        }
        BasePlant lobber = LobberPlantType.findByName(typeName)
                .map(type -> createLobber(type, level, position))
                .orElse(null);
        if (lobber != null) {
            return lobber;
        }
        BasePlant strikeThrough = StrikeThroughPlantType.findByName(typeName)
                .map(type -> createStrikeThrough(type, level, position))
                .orElse(null);
        if (strikeThrough != null) {
            return strikeThrough;
        }
        BasePlant explosive = ExplosivePlantType.findByName(typeName)
                .map(type -> createExplosive(type, level, position))
                .orElse(null);
        if (explosive != null) {
            return explosive;
        }
        BasePlant melee = MeleePlantType.findByName(typeName)
                .map(type -> createMelee(type, level, position))
                .orElse(null);
        if (melee != null) {
            return melee;
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

    public static Homing createHoming(HomingPlantType type, EntityPosition position) {
        return createHoming(type, 1, position);
    }

    public static Homing createHoming(HomingPlantType type, int level,
            EntityPosition position) {
        return new Homing(type, level, position);
    }

    public static Lobber createLobber(LobberPlantType type, EntityPosition position) {
        return createLobber(type, 1, position);
    }

    public static Lobber createLobber(LobberPlantType type, int level,
            EntityPosition position) {
        return new Lobber(type, level, position);
    }

    public static StrikeThrough createStrikeThrough(
            StrikeThroughPlantType type, EntityPosition position) {
        return createStrikeThrough(type, 1, position);
    }

    public static StrikeThrough createStrikeThrough(
            StrikeThroughPlantType type, int level, EntityPosition position) {
        return new StrikeThrough(type, level, position);
    }

    public static Explosive createExplosive(ExplosivePlantType type,
            EntityPosition position) {
        return createExplosive(type, 1, position);
    }

    public static Explosive createExplosive(ExplosivePlantType type, int level,
            EntityPosition position) {
        return new Explosive(type, level, position);
    }

    public static Melee createMelee(MeleePlantType type, EntityPosition position) {
        return createMelee(type, 1, position);
    }

    public static Melee createMelee(MeleePlantType type, int level,
            EntityPosition position) {
        return new Melee(type, level, position);
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
