package io.github.Plants_Vs_Zombies_2.model.game.entities.plants;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.model.game.entities.Entity;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;

public abstract class BasePlant extends Entity {
    public static final int MAX_FREEZE_LEVEL = 3;
    public static final int ICE_SHELL_HIT_POINTS = 600;
    public static final double ICE_WARMING_DAMAGE_PER_SECOND = 60.0;
    /** @deprecated Use {@link #MAX_FREEZE_LEVEL}. */
    @Deprecated
    public static final int ICE_HITS_TO_FREEZE = MAX_FREEZE_LEVEL;
    /** @deprecated Ice durability is now measured in hit points. */
    @Deprecated
    public static final int DEFAULT_ICE_LAYER_HITS = ICE_SHELL_HIT_POINTS;
    public static final int DEFAULT_OCTOPUS_HITS = 3;

    private final String name;
    private final PlantCategory category;
    private final Set<PlantTag> tags;
    private final int level;
    private final int cost;
    private final int baseHP;
    private final int damage;
    private int currentHP;
    private int freezeLevel;
    private int iceShellHitPoints;
    private double iceMeltRemainder;
    private boolean frozenByIce;
    private int octopusHitsRemaining;
    private boolean coveredByOctopus;
    private boolean transformedToSheep;

    protected BasePlant(PlantCategory category) {
        this(null, category, Collections.emptySet(), 1, 0, 0, 0, null);
    }

    protected BasePlant(String name, PlantCategory category, Set<PlantTag> tags,
            int level, int cost, int baseHP, int damage, EntityPosition entityPosition) {
        super(entityPosition);
        if (category == null) {
            throw new IllegalArgumentException("category cannot be null");
        }
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        if (cost < 0 || baseHP < 0 || damage < 0) {
            throw new IllegalArgumentException("plant numeric values cannot be negative");
        }

        this.name = name;
        this.category = category;
        this.tags = immutableTags(tags);
        this.level = level;
        this.cost = cost;
        this.baseHP = baseHP;
        this.damage = damage;
        this.currentHP = baseHP;
    }

    private static Set<PlantTag> immutableTags(Set<PlantTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(tags));
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
    }

    public String getName() {
        return name;
    }

    public PlantCategory getCategory() {
        return category;
    }

    public Set<PlantTag> getTags() {
        return tags;
    }

    public int getLevel() {
        return level;
    }

    public int getCost() {
        return cost;
    }

    public int getBaseHP() {
        return baseHP;
    }

    public int getDamage() {
        return damage;
    }

    public float getRechargeSeconds() {
        return 0.0f;
    }

    public void takeDamage(int damageAmount) {
        if (damageAmount < 0) {
            throw new IllegalArgumentException("damageAmount cannot be negative");
        }
        currentHP = Math.max(0, currentHP - damageAmount);
        if (currentHP == 0) {
            markForRemoval();
        }
    }

    public boolean applyIceHit() {
        return increaseFreezeLevel();
    }

    public boolean increaseFreezeLevel() {
        if (isDestroyed() || frozenByIce) {
            return false;
        }
        freezeLevel = Math.min(MAX_FREEZE_LEVEL, freezeLevel + 1);
        if (freezeLevel < MAX_FREEZE_LEVEL) {
            return false;
        }
        frozenByIce = true;
        iceShellHitPoints = ICE_SHELL_HIT_POINTS;
        iceMeltRemainder = 0.0;
        return true;
    }

    public boolean damageIce(int damage) {
        return damageIce(damage, false);
    }

    public boolean damageIce(int damage, boolean fireDamage) {
        if (damage < 0) {
            throw new IllegalArgumentException(
                    "ice damage cannot be negative");
        }
        if (!frozenByIce || (damage == 0 && !fireDamage)) {
            return false;
        }
        if (fireDamage) {
            clearIce();
            return true;
        }
        iceShellHitPoints = Math.max(0, iceShellHitPoints - damage);
        if (iceShellHitPoints == 0) {
            clearIce();
            return true;
        }
        return false;
    }

    public boolean meltIce(double damage) {
        if (!Double.isFinite(damage) || damage < 0.0) {
            throw new IllegalArgumentException(
                    "ice melt damage must be finite and non-negative");
        }
        if (!frozenByIce || damage == 0.0) {
            return false;
        }
        iceMeltRemainder += damage;
        int wholeDamage = (int) Math.floor(iceMeltRemainder);
        if (wholeDamage <= 0) {
            return false;
        }
        iceMeltRemainder -= wholeDamage;
        return damageIce(wholeDamage, false);
    }

    public void clearIce() {
        frozenByIce = false;
        freezeLevel = 0;
        iceShellHitPoints = 0;
        iceMeltRemainder = 0.0;
    }

    public boolean attachOctopus() {
        if (isDestroyed() || coveredByOctopus) {
            return false;
        }
        coveredByOctopus = true;
        octopusHitsRemaining = DEFAULT_OCTOPUS_HITS;
        return true;
    }

    public boolean damageOctopus(int hits) {
        if (hits < 0) {
            throw new IllegalArgumentException(
                    "octopus damage hits cannot be negative");
        }
        if (!coveredByOctopus || hits == 0) {
            return false;
        }
        octopusHitsRemaining = Math.max(
                0, octopusHitsRemaining - hits);
        if (octopusHitsRemaining == 0) {
            clearOctopus();
            return true;
        }
        return false;
    }

    public void clearOctopus() {
        coveredByOctopus = false;
        octopusHitsRemaining = 0;
    }

    public boolean isCoveredByOctopus() {
        return coveredByOctopus;
    }

    public int getOctopusHitsRemaining() {
        return octopusHitsRemaining;
    }

    public boolean transformToSheep() {
        if (isDestroyed() || isDisabled()) {
            return false;
        }
        transformedToSheep = true;
        return true;
    }

    public boolean restoreFromSheep() {
        if (!transformedToSheep) {
            return false;
        }
        transformedToSheep = false;
        return true;
    }

    public boolean isTransformedToSheep() {
        return transformedToSheep;
    }

    public boolean isDisabled() {
        return frozenByIce || coveredByOctopus || transformedToSheep;
    }

    public boolean isFrozen() {
        return frozenByIce;
    }

    public int getIceHitCount() {
        return freezeLevel;
    }

    public int getFreezeLevel() {
        return freezeLevel;
    }

    public int getIceLayerHitsRemaining() {
        return iceShellHitPoints;
    }

    public int getIceShellHitPoints() {
        return iceShellHitPoints;
    }

    public int getIceShellMaximumHitPoints() {
        return ICE_SHELL_HIT_POINTS;
    }

    public boolean hasTag(PlantTag tag) {
        return tag != null && tags.contains(tag);
    }

    public boolean isDestroyed() {
        return currentHP <= 0;
    }

    public void heal(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        currentHP = Math.min(baseHP, currentHP + amount);
    }

    public void restoreHealth() {
        currentHP = baseHP;
    }

    public int getCurrentHP() {
        return currentHP;
    }
}
