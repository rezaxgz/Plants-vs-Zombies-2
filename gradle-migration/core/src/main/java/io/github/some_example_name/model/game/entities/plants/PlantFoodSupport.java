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

/**
 * Canonical plant-food eligibility rules shared by gameplay and the greenhouse.
 */
public final class PlantFoodSupport {
    private PlantFoodSupport() {
    }

    public static boolean supports(BasePlant plant) {
        if (plant == null || plant.isRemoved()
                || plant.isDisabled() || PlantFamily.isMint(plant)) {
            return false;
        }
        if (plant instanceof SunProducer) {
            return ((SunProducer) plant).getType() != SunProducerPlantType.GOLD_BLOOM;
        }
        if (plant instanceof Explosive) {
            ExplosivePlantType type = ((Explosive) plant).getType();
            return type == ExplosivePlantType.POTATO_MINE
                    || type == ExplosivePlantType.PRIMAL_POTATO_MINE
                    || type == ExplosivePlantType.SQUASH
                    || type == ExplosivePlantType.TANGLE_KELP
                    || type == ExplosivePlantType.ICEBERG_LETTUCE;
        }
        if (plant instanceof Modifier) {
            ModifierPlantType type = ((Modifier) plant).getType();
            return type == ModifierPlantType.TORCHWOOD
                    || type == ModifierPlantType.HYPNO_SHROOM
                    || type == ModifierPlantType.LILY_PAD;
        }
        if (plant instanceof Shooter) {
            return ((Shooter) plant).getType() != ShooterPlantType.APPEASE_MINT;
        }
        if (plant instanceof Lobber) {
            return ((Lobber) plant).getType() != LobberPlantType.ARMA_MINT;
        }
        if (plant instanceof StrikeThrough) {
            return ((StrikeThrough) plant).getType() != StrikeThroughPlantType.PIERCE_MINT;
        }
        if (plant instanceof Homing) {
            return ((Homing) plant).getType() != HomingPlantType.CAT_TAIL_MINT;
        }
        if (plant instanceof Melee) {
            return ((Melee) plant).getType() != MeleePlantType.ENFORCE_MINT;
        }
        return plant instanceof Wallnut
                && ((Wallnut) plant).getType() != WallnutPlantType.REINFORCE_MINT;
    }
}
