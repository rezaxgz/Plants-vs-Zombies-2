package io.github.some_example_name.model.game.entities.plants.modifier;

import java.util.Set;

import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.plants.PlantCategory;
import io.github.some_example_name.model.game.entities.plants.PlantTag;
import io.github.some_example_name.model.game.entities.plants.explosive.Explosive;
import io.github.some_example_name.model.game.entities.plants.homing.Homing;
import io.github.some_example_name.model.game.entities.plants.lobber.Lobber;
import io.github.some_example_name.model.game.entities.plants.melee.Melee;
import io.github.some_example_name.model.game.entities.plants.shooter.Shooter;
import io.github.some_example_name.model.game.entities.plants.strikeThrough.StrikeThrough;
import io.github.some_example_name.model.game.entities.plants.sunProducer.SunProducer;
import io.github.some_example_name.model.game.entities.plants.wallnut.Wallnut;
import io.github.some_example_name.model.game.entities.zombies.Zombie;

public class Modifier extends BasePlant {
    private final ModifierPlantType type;
    private final BasePlant imitatedPlant;
    private final float imitatedRechargeSeconds;

    private boolean blueFlame;
    private boolean hypnoPlantFoodArmed;
    private boolean familyBoostPending;
    private boolean familyBoostActivated;
    private boolean deathAreaEffectPending;
    private boolean plantFoodUsed;
    private int lilyPadCopiesPending;

    public Modifier() {
        this(ModifierPlantType.TORCHWOOD, 1, null);
    }

    public Modifier(ModifierPlantType type, EntityPosition entityPosition) {
        this(type, 1, entityPosition);
    }

    public Modifier(ModifierPlantType type, int level, EntityPosition entityPosition) {
        this(type, level, entityPosition, null);
    }

    public Modifier(ModifierPlantType type, int level, EntityPosition entityPosition,
            BasePlant imitatedPlant) {
        super(buildName(requireType(type), imitatedPlant), PlantCategory.MODIFIER,
                resolveTags(type, imitatedPlant), level,
                resolveCost(type, level, imitatedPlant),
                resolveHitPoints(type, level, imitatedPlant),
                type.getDamage(level), entityPosition);
        ModifierPlantType.validateLevel(level);
        if (type != ModifierPlantType.IMITATER && imitatedPlant != null) {
            throw new IllegalArgumentException("only Imitater can contain a copied plant");
        }
        if (type == ModifierPlantType.IMITATER && imitatedPlant == null) {
            this.imitatedRechargeSeconds = 0.0f;
        } else if (type == ModifierPlantType.IMITATER) {
            this.imitatedRechargeSeconds = type.getImitatedRechargeSeconds(
                    level, resolveRechargeSeconds(imitatedPlant));
        } else {
            this.imitatedRechargeSeconds = type.getRechargeSeconds(level);
        }
        this.type = type;
        this.imitatedPlant = imitatedPlant;
    }

    private static ModifierPlantType requireType(ModifierPlantType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        return type;
    }

    private static String buildName(ModifierPlantType type, BasePlant imitatedPlant) {
        if (type == ModifierPlantType.IMITATER && imitatedPlant != null) {
            return type.getDisplayName() + " (" + imitatedPlant.getName() + ")";
        }
        return type.getDisplayName();
    }

    private static Set<PlantTag> resolveTags(ModifierPlantType type,
            BasePlant imitatedPlant) {
        if (type == ModifierPlantType.IMITATER && imitatedPlant != null) {
            return imitatedPlant.getTags();
        }
        return type.getTags();
    }

    private static int resolveCost(ModifierPlantType type, int level,
            BasePlant imitatedPlant) {
        if (type == ModifierPlantType.IMITATER && imitatedPlant != null) {
            return type.getImitatedCost(level, imitatedPlant.getCost());
        }
        return type.getCost(level);
    }

    private static int resolveHitPoints(ModifierPlantType type, int level,
            BasePlant imitatedPlant) {
        if (type == ModifierPlantType.IMITATER && imitatedPlant != null) {
            return imitatedPlant.getBaseHP();
        }
        return type.getBaseHP(level);
    }

    private static float resolveRechargeSeconds(BasePlant plant) {
        if (plant instanceof Shooter) {
            return ((Shooter) plant).getRechargeSeconds();
        }
        if (plant instanceof SunProducer) {
            return ((SunProducer) plant).getRechargeSeconds();
        }
        if (plant instanceof Wallnut) {
            return ((Wallnut) plant).getRechargeSeconds();
        }
        if (plant instanceof Explosive) {
            return ((Explosive) plant).getRechargeSeconds();
        }
        if (plant instanceof Melee) {
            return ((Melee) plant).getRechargeSeconds();
        }
        if (plant instanceof Lobber) {
            return ((Lobber) plant).getRechargeSeconds();
        }
        if (plant instanceof StrikeThrough) {
            return ((StrikeThrough) plant).getRechargeSeconds();
        }
        if (plant instanceof Homing) {
            return ((Homing) plant).getRechargeSeconds();
        }
        if (plant instanceof Modifier) {
            return ((Modifier) plant).getRechargeSeconds();
        }
        return 0.0f;
    }

    @Override
    public void update(float deltaSeconds) {
        if (isRemoved()) {
            return;
        }
        super.update(deltaSeconds);
        if (type.getBehavior() == ModifierBehavior.FAMILY_BOOST
                && !familyBoostActivated) {
            familyBoostActivated = true;
            familyBoostPending = true;
        }
    }

    @Override
    public void takeDamage(int damageAmount) {
        boolean wasDestroyed = isDestroyed();
        super.takeDamage(damageAmount);
        if (!wasDestroyed && isDestroyed()
                && type == ModifierPlantType.TORCHWOOD
                && type.hasDeathAreaEffect(getLevel())) {
            deathAreaEffectPending = true;
        }
    }

    public void usePlantFood() {
        if (isRemoved() || type == ModifierPlantType.ENCHANT_MINT) {
            return;
        }
        plantFoodUsed = true;
        switch (type.getBehavior()) {
            case TORCHWOOD:
                blueFlame = true;
                break;
            case HYPNO_SHROOM:
                hypnoPlantFoodArmed = true;
                break;
            case LILY_PAD:
                lilyPadCopiesPending += ModifierPlantType.LILY_PAD_PLANT_FOOD_COPIES;
                break;
            case IMITATER:
            case FAMILY_BOOST:
                break;
            default:
                throw new IllegalStateException("Unknown modifier behavior: "
                        + type.getBehavior());
        }
    }

    public boolean onEatenBy(Zombie zombie) {
        if (zombie == null || type != ModifierPlantType.HYPNO_SHROOM
                || !isDestroyed() || zombie.isDead()) {
            return false;
        }
        double healthMultiplier = type.getHypnotizedHealthMultiplier(getLevel());
        double damageMultiplier = type.getHypnotizedDamageMultiplier(getLevel());
        if (hypnoPlantFoodArmed) {
            zombie.transformIntoAlliedGargantuar(healthMultiplier, damageMultiplier);
        } else {
            zombie.hypnotize(healthMultiplier, damageMultiplier);
        }
        return true;
    }

    public int getTorchwoodDamageMultiplier() {
        return blueFlame ? 3 : 2;
    }

    public boolean hasBlueFlame() {
        return blueFlame;
    }

    public boolean isTorchwood() {
        return type == ModifierPlantType.TORCHWOOD;
    }

    public boolean isLilyPad() {
        return type == ModifierPlantType.LILY_PAD;
    }

    public boolean isImitater() {
        return type == ModifierPlantType.IMITATER;
    }

    public BasePlant getImitatedPlant() {
        return imitatedPlant;
    }

    public boolean hasValidImitatedPlant() {
        return imitatedPlant != null;
    }

    public boolean appliesPlantFoodOnEntrance() {
        return type == ModifierPlantType.IMITATER
                && type.appliesPlantFoodOnEntrance(getLevel());
    }

    public boolean drainFamilyBoostPending() {
        boolean result = familyBoostPending;
        familyBoostPending = false;
        return result;
    }

    public boolean drainDeathAreaEffectPending() {
        boolean result = deathAreaEffectPending;
        deathAreaEffectPending = false;
        return result;
    }

    public int drainLilyPadCopiesPending() {
        int result = lilyPadCopiesPending;
        lilyPadCopiesPending = 0;
        return result;
    }

    public float getRechargeSeconds() {
        return imitatedRechargeSeconds;
    }

    public float getFamilyBoostDurationSeconds() {
        return type.getFamilyBoostDurationSeconds(getLevel());
    }

    public boolean resetsFamilyCooldowns() {
        return type.resetsFamilyCooldowns(getLevel());
    }

    public boolean wasPlantFoodUsed() {
        return plantFoodUsed;
    }

    public ModifierPlantType getType() {
        return type;
    }
}
