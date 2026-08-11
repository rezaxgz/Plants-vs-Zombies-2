package io.github.Plants_Vs_Zombies_2.model.game.entities.plants.lobber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantCategory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.LobbedProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.ChillEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.DamageEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.FireEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.ProjectileEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.StunEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;

public class Lobber extends BasePlant {
    private static final double TIMER_EPSILON = 0.000001;
    private static final int PLANT_FOOD_DAMAGE_MULTIPLIER = 5;

    private final LobberPlantType type;
    private final Random random;

    private double secondsSinceLastAttack;
    private boolean familyBoostActivated;
    private boolean familyBoostPending;
    private boolean plantFoodPending;
    private boolean plantFoodUsed;

    public Lobber() {
        this(LobberPlantType.CABBAGE_PULT, 1, null);
    }

    public Lobber(LobberPlantType type, EntityPosition entityPosition) {
        this(type, 1, entityPosition);
    }

    public Lobber(LobberPlantType type, int level, EntityPosition entityPosition) {
        this(type, level, entityPosition, new Random());
    }

    Lobber(LobberPlantType type, int level, EntityPosition entityPosition,
            Random random) {
        super(requireType(type).getDisplayName(), PlantCategory.LOBBER,
                type.getTags(), level, type.getCost(level), type.getBaseHP(level),
                type.getDamage(level), entityPosition);
        LobberPlantType.validateLevel(level);
        if (random == null) {
            throw new IllegalArgumentException("random cannot be null");
        }
        this.type = type;
        this.random = random;
    }

    private static LobberPlantType requireType(LobberPlantType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        return type;
    }

    @Override
    public void update(float deltaSeconds) {
        if (isRemoved()) {
            return;
        }
        super.update(deltaSeconds);
        if (type.getBehavior() == LobberBehavior.FAMILY_BOOST) {
            activateFamilyBoost();
            return;
        }
        float interval = type.getActionIntervalSeconds(getLevel());
        secondsSinceLastAttack = Math.min(interval, secondsSinceLastAttack + deltaSeconds);
    }

    private void activateFamilyBoost() {
        if (!familyBoostActivated) {
            familyBoostActivated = true;
            familyBoostPending = true;
        }
    }

    public boolean isReadyToAttack() {
        float interval = type.getActionIntervalSeconds(getLevel());
        return !isRemoved() && type.getBehavior() != LobberBehavior.FAMILY_BOOST
                && interval > 0.0f && secondsSinceLastAttack + TIMER_EPSILON >= interval;
    }

    public LobbedProjectile shoot(Zombie target) {
        if (!isReadyToAttack() || target == null || target.isDead()) {
            return null;
        }
        secondsSinceLastAttack = 0.0;
        return createNormalProjectile(target);
    }

    private LobbedProjectile createNormalProjectile(Zombie target) {
        switch (type.getBehavior()) {
            case SINGLE_TARGET:
                return createProjectile(target, normalDamageEffects(getDamage()),
                        Collections.emptyList(), 0.0);
            case KERNEL:
                return createKernelProjectile(target, random.nextDouble() < getButterChance());
            case AREA:
                return createProjectile(target, normalDamageEffects(getDamage()),
                        normalDamageEffects(getSplashDamage()),
                        LobberPlantType.SPLASH_RADIUS_TILES);
            case ICE_AREA:
                return createProjectile(target, icyEffects(getDamage()),
                        icyEffects(getSplashDamage()), LobberPlantType.SPLASH_RADIUS_TILES);
            case FIRE_AREA:
                return createProjectile(target, fireEffects(getDamage()),
                        fireEffects(getSplashDamage()), LobberPlantType.SPLASH_RADIUS_TILES);
            case FAMILY_BOOST:
                return null;
            default:
                throw new IllegalStateException("Unknown lobber behavior: " + type.getBehavior());
        }
    }

    private LobbedProjectile createKernelProjectile(Zombie target, boolean butter) {
        if (!butter) {
            return createProjectile(target, normalDamageEffects(getDamage()),
                    Collections.emptyList(), 0.0);
        }
        List<ProjectileEffect> effects = new ArrayList<>();
        effects.add(new DamageEffect(getButterDamage()));
        effects.add(new StunEffect(LobberPlantType.BUTTER_STUN_SECONDS));
        return createProjectile(target, effects, Collections.emptyList(), 0.0);
    }

    public LobbedProjectile createPlantFoodProjectile(Zombie target) {
        if (target == null || target.isDead() || type == LobberPlantType.ARMA_MINT) {
            return null;
        }
        int directDamage = getDamage() * PLANT_FOOD_DAMAGE_MULTIPLIER;
        int splashDamage = getSplashDamage() * PLANT_FOOD_DAMAGE_MULTIPLIER;
        switch (type.getBehavior()) {
            case SINGLE_TARGET:
                return createProjectile(target, normalDamageEffects(directDamage),
                        Collections.emptyList(), 0.0);
            case KERNEL:
                return createKernelProjectile(target, true);
            case AREA:
                return createProjectile(target, normalDamageEffects(directDamage),
                        normalDamageEffects(splashDamage), LobberPlantType.SPLASH_RADIUS_TILES);
            case ICE_AREA:
                return createProjectile(target, icyEffects(directDamage),
                        icyEffects(splashDamage), LobberPlantType.SPLASH_RADIUS_TILES);
            case FIRE_AREA:
                return createProjectile(target, fireEffects(directDamage),
                        fireEffects(splashDamage), LobberPlantType.SPLASH_RADIUS_TILES);
            case FAMILY_BOOST:
                return null;
            default:
                throw new IllegalStateException("Unknown lobber behavior: " + type.getBehavior());
        }
    }

    private LobbedProjectile createProjectile(Zombie target,
            List<ProjectileEffect> directEffects,
            List<ProjectileEffect> splashEffects, double splashRadius) {
        if (getEntityPosition() == null) {
            return null;
        }
        return new LobbedProjectile(getName(), getEntityPosition().getRow(),
                getEntityPosition().getColumn(), target, directEffects,
                splashEffects, splashRadius);
    }

    private static List<ProjectileEffect> normalDamageEffects(int damage) {
        return Collections.singletonList(new DamageEffect(Math.max(0, damage)));
    }

    private static List<ProjectileEffect> fireEffects(int damage) {
        return Collections.singletonList(new FireEffect(Math.max(0, damage)));
    }

    private static List<ProjectileEffect> icyEffects(int damage) {
        List<ProjectileEffect> effects = new ArrayList<>();
        effects.add(new DamageEffect(Math.max(0, damage)));
        effects.add(new ChillEffect(LobberPlantType.WINTER_CHILL_SECONDS));
        return effects;
    }

    public void usePlantFood() {
        if (isRemoved() || type == LobberPlantType.ARMA_MINT) {
            return;
        }
        plantFoodUsed = true;
        plantFoodPending = true;
    }

    public boolean drainPlantFoodPending() {
        boolean pending = plantFoodPending;
        plantFoodPending = false;
        return pending;
    }

    public boolean drainFamilyBoostPending() {
        boolean pending = familyBoostPending;
        familyBoostPending = false;
        return pending;
    }

    public void resetActionTimer() {
        secondsSinceLastAttack = type.getActionIntervalSeconds(getLevel());
    }

    public int getPlantFoodTargetCount() {
        switch (type) {
            case CABBAGE_PULT:
                return 5;
            case KERNEL_PULT:
                return Integer.MAX_VALUE;
            case MELON_PULT:
            case WINTER_MELON:
            case PEPPER_PULT:
                return 3;
            case ARMA_MINT:
                return 0;
            default:
                throw new IllegalStateException("Unknown lobber type: " + type);
        }
    }

    public LobberPlantType getType() {
        return type;
    }

    public float getActionIntervalSeconds() {
        return type.getActionIntervalSeconds(getLevel());
    }

    public float getRechargeSeconds() {
        return type.getRechargeSeconds(getLevel());
    }

    public int getButterDamage() {
        return type.getButterDamage(getLevel());
    }

    public double getButterChance() {
        return type.getButterChance(getLevel());
    }

    public int getSplashDamage() {
        return type.getSplashDamage(getLevel());
    }

    public int getWarmthRadius() {
        return type.getWarmthRadius(getLevel());
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
}
