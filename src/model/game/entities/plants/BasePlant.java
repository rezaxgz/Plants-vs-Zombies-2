package model.game.entities.plants;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import model.game.entities.Entity;
import model.game.entities.EntityPosition;

public abstract class BasePlant extends Entity {
    public static final int ICE_HITS_TO_FREEZE = 3;
    public static final int DEFAULT_ICE_LAYER_HITS = 3;
    public static final int DEFAULT_OCTOPUS_HITS = 3;

    private final String name;
    private final PlantCategory category;
    private final Set<PlantTag> tags;
    private final int level;
    private final int cost;
    private final int baseHP;
    private final int damage;
    private int currentHP;
    private int iceHitCount;
    private int iceLayerHitsRemaining;
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
        if (isDestroyed() || frozenByIce) {
            return false;
        }
        iceHitCount++;
        if (iceHitCount < ICE_HITS_TO_FREEZE) {
            return false;
        }
        frozenByIce = true;
        iceLayerHitsRemaining = DEFAULT_ICE_LAYER_HITS;
        return true;
    }

    public boolean damageIce(int hits) {
        if (hits < 0) {
            throw new IllegalArgumentException("ice damage hits cannot be negative");
        }
        if (!frozenByIce || hits == 0) {
            return false;
        }
        iceLayerHitsRemaining = Math.max(0, iceLayerHitsRemaining - hits);
        if (iceLayerHitsRemaining == 0) {
            clearIce();
            return true;
        }
        return false;
    }

    public void clearIce() {
        frozenByIce = false;
        iceHitCount = 0;
        iceLayerHitsRemaining = 0;
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
        return iceHitCount;
    }

    public int getIceLayerHitsRemaining() {
        return iceLayerHitsRemaining;
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
