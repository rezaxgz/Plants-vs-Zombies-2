package io.github.some_example_name.model.game.entities.plants;

import io.github.some_example_name.model.game.entities.plants.explosive.Explosive;
import io.github.some_example_name.model.game.entities.plants.explosive.ExplosivePlantType;
import io.github.some_example_name.model.game.entities.plants.homing.Homing;
import io.github.some_example_name.model.game.entities.plants.homing.HomingPlantType;
import io.github.some_example_name.model.game.entities.plants.lobber.Lobber;
import io.github.some_example_name.model.game.entities.plants.lobber.LobberPlantType;
import io.github.some_example_name.model.game.entities.plants.melee.Melee;
import io.github.some_example_name.model.game.entities.plants.melee.MeleePlantType;
import io.github.some_example_name.model.game.entities.plants.modifier.Modifier;
import io.github.some_example_name.model.game.entities.plants.modifier.ModifierPlantType;
import io.github.some_example_name.model.game.entities.plants.shooter.Shooter;
import io.github.some_example_name.model.game.entities.plants.shooter.ShooterPlantType;
import io.github.some_example_name.model.game.entities.plants.strikeThrough.StrikeThrough;
import io.github.some_example_name.model.game.entities.plants.strikeThrough.StrikeThroughPlantType;
import io.github.some_example_name.model.game.entities.plants.sunProducer.SunProducer;
import io.github.some_example_name.model.game.entities.plants.sunProducer.SunProducerPlantType;
import io.github.some_example_name.model.game.entities.plants.wallnut.Wallnut;
import io.github.some_example_name.model.game.entities.plants.wallnut.WallnutPlantType;

public enum PlantFamily {
    SUN_PRODUCER(PlantCategory.SUN_PRODUCER),
    SHOOTER(PlantCategory.SHOOTER),
    LOBBER(PlantCategory.LOBBER),
    EXPLOSIVE(PlantCategory.EXPLOSIVE),
    MELEE(PlantCategory.MELEE),
    WALL_NUT(PlantCategory.WALL_NUT),
    MODIFIER(PlantCategory.MODIFIER),
    STRIKE_THROUGH(PlantCategory.STRIKE_THROUGH),
    HOMING(PlantCategory.HOMING);

    private final PlantCategory category;

    PlantFamily(PlantCategory category) {
        this.category = category;
    }

    public boolean contains(BasePlant plant) {
        return plant != null && plant.getCategory() == category && !isMint(plant);
    }

    public static PlantFamily findForPlant(BasePlant plant) {
        for (PlantFamily family : values()) {
            if (family.contains(plant)) {
                return family;
            }
        }
        return null;
    }

    public static boolean isMint(BasePlant plant) {
        if (plant instanceof SunProducer) {
            return ((SunProducer) plant).getType() == SunProducerPlantType.ENLIGHTEN_MINT;
        }
        if (plant instanceof Shooter) {
            return ((Shooter) plant).getType() == ShooterPlantType.APPEASE_MINT;
        }
        if (plant instanceof Lobber) {
            return ((Lobber) plant).getType() == LobberPlantType.ARMA_MINT;
        }
        if (plant instanceof Explosive) {
            return ((Explosive) plant).getType() == ExplosivePlantType.BOMBARD_MINT;
        }
        if (plant instanceof Melee) {
            return ((Melee) plant).getType() == MeleePlantType.ENFORCE_MINT;
        }
        if (plant instanceof Wallnut) {
            return ((Wallnut) plant).getType() == WallnutPlantType.REINFORCE_MINT;
        }
        if (plant instanceof Modifier) {
            return ((Modifier) plant).getType() == ModifierPlantType.ENCHANT_MINT;
        }
        if (plant instanceof StrikeThrough) {
            return ((StrikeThrough) plant).getType() == StrikeThroughPlantType.PIERCE_MINT;
        }
        if (plant instanceof Homing) {
            return ((Homing) plant).getType() == HomingPlantType.CAT_TAIL_MINT;
        }
        return false;
    }
}
