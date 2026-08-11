package io.github.Plants_Vs_Zombies_2.model.game.entities.plants;

import java.util.Locale;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.explosive.Explosive;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.explosive.ExplosivePlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.homing.Homing;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.homing.HomingPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.lobber.Lobber;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.lobber.LobberPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.melee.Melee;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.melee.MeleePlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.modifier.Modifier;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.modifier.ModifierPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.shooter.Shooter;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.shooter.ShooterPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.strikeThrough.StrikeThrough;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.strikeThrough.StrikeThroughPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.sunProducer.SunProducer;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.sunProducer.SunProducerPlantType;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.wallnut.Wallnut;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.wallnut.WallnutPlantType;

public final class PlantFactory {
    private PlantFactory() {
    }

    public static BasePlant createPlant(String typeName, EntityPosition position) {
        return createPlant(typeName, 1, position);
    }

    public static BasePlant createPlant(String typeName, int level,
            EntityPosition position) {
        String imitatedType = parseImitatedType(typeName);
        if (imitatedType != null) {
            return createImitater(imitatedType, level, 1, position);
        }
        return createPlantInternal(typeName, level, position, true);
    }

    private static BasePlant createPlantInternal(String typeName, int level,
            EntityPosition position, boolean allowPlainImitater) {
        BasePlant plant = SunProducerPlantType.findByName(typeName)
                .map(type -> createSunProducer(type, level, position))
                .orElse(null);
        if (plant != null) {
            return plant;
        }
        plant = ShooterPlantType.findByName(typeName)
                .map(type -> createShooter(type, level, position))
                .orElse(null);
        if (plant != null) {
            return plant;
        }
        plant = HomingPlantType.findByName(typeName)
                .map(type -> createHoming(type, level, position))
                .orElse(null);
        if (plant != null) {
            return plant;
        }
        plant = LobberPlantType.findByName(typeName)
                .map(type -> createLobber(type, level, position))
                .orElse(null);
        if (plant != null) {
            return plant;
        }
        plant = StrikeThroughPlantType.findByName(typeName)
                .map(type -> createStrikeThrough(type, level, position))
                .orElse(null);
        if (plant != null) {
            return plant;
        }
        plant = ExplosivePlantType.findByName(typeName)
                .map(type -> createExplosive(type, level, position))
                .orElse(null);
        if (plant != null) {
            return plant;
        }
        plant = MeleePlantType.findByName(typeName)
                .map(type -> createMelee(type, level, position))
                .orElse(null);
        if (plant != null) {
            return plant;
        }
        plant = ModifierPlantType.findByName(typeName)
                .filter(type -> allowPlainImitater
                        || type != ModifierPlantType.IMITATER)
                .map(type -> createModifier(type, level, position))
                .orElse(null);
        if (plant != null) {
            return plant;
        }
        return WallnutPlantType.findByName(typeName)
                .map(type -> createWallnut(type, level, position))
                .orElse(null);
    }

    private static String parseImitatedType(String typeName) {
        if (typeName == null) {
            return null;
        }
        String trimmed = typeName.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("imitater:")) {
            return nonBlankSuffix(trimmed.substring("imitater:".length()));
        }
        if (lower.startsWith("imitater(") && trimmed.endsWith(")")) {
            return nonBlankSuffix(trimmed.substring("imitater(".length(),
                    trimmed.length() - 1));
        }
        if (lower.startsWith("imitater ")) {
            return nonBlankSuffix(trimmed.substring("imitater ".length()));
        }
        return null;
    }

    private static String nonBlankSuffix(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static Modifier createImitater(String copiedTypeName, int imitaterLevel,
            int copiedPlantLevel, EntityPosition position) {
        BasePlant copiedPlant = createPlantInternal(copiedTypeName, copiedPlantLevel,
                position, false);
        if (copiedPlant == null || copiedPlant instanceof Modifier
                && ((Modifier) copiedPlant).isImitater()) {
            return null;
        }
        return new Modifier(ModifierPlantType.IMITATER, imitaterLevel,
                position, copiedPlant);
    }

    public static SunProducer createSunProducer(SunProducerPlantType type,
            EntityPosition position) {
        return createSunProducer(type, 1, position);
    }

    public static SunProducer createSunProducer(SunProducerPlantType type, int level,
            EntityPosition position) {
        return new SunProducer(type, level, position);
    }

    public static Homing createHoming(HomingPlantType type,
            EntityPosition position) {
        return createHoming(type, 1, position);
    }

    public static Homing createHoming(HomingPlantType type, int level,
            EntityPosition position) {
        return new Homing(type, level, position);
    }

    public static Lobber createLobber(LobberPlantType type,
            EntityPosition position) {
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

    public static Melee createMelee(MeleePlantType type,
            EntityPosition position) {
        return createMelee(type, 1, position);
    }

    public static Melee createMelee(MeleePlantType type, int level,
            EntityPosition position) {
        return new Melee(type, level, position);
    }

    public static Modifier createModifier(ModifierPlantType type,
            EntityPosition position) {
        return createModifier(type, 1, position);
    }

    public static Modifier createModifier(ModifierPlantType type, int level,
            EntityPosition position) {
        return new Modifier(type, level, position);
    }

    public static Wallnut createWallnut(WallnutPlantType type,
            EntityPosition position) {
        return createWallnut(type, 1, position);
    }

    public static Shooter createShooter(ShooterPlantType type,
            EntityPosition position) {
        return createShooter(type, 1, position);
    }

    public static Shooter createShooter(ShooterPlantType type, int level,
            EntityPosition position) {
        return new Shooter(type, level, position);
    }

    public static Wallnut createWallnut(WallnutPlantType type, int level,
            EntityPosition position) {
        return new Wallnut(type, level, position);
    }
}
