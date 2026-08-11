package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies;

import io.github.Plants_Vs_Zombies_2.model.game.DifficultyRules;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities.*;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.armor.ArmorType;

abstract class ZombieDamageLogic extends ZombieAbilityLogic {
    protected ZombieDamageLogic(ZombieType type, int waveNumber, int lane, double columnPosition, boolean glowing) {
        super(type, waveNumber, lane, columnPosition, glowing);
    }

    public void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        if (dead)
            return;

        int remainingDamage = damage;

        // Apply damage to armor first
        if (armor != null && !armor.isDestroyed()) {
            remainingDamage = armor.takeDamage(damage);
        }

        // Apply remaining damage to base health
        if (remainingDamage > 0) {
            hitPoints -= remainingDamage;
            if (hitPoints <= 0) {
                hitPoints = 0;
                kill();
            }
        }

        // Check for enrage trigger (newspaper destroyed)
        if (!dead && armor != null && armor.isDestroyed()
                && armor.getType() == ArmorType.NEWSPAPER) {
            for (ZombieAbility ability : abilities) {
                if (ability instanceof EnrageAbility
                        && ability.tryUse(asZombie(), null)) {
                    enraged = true;
                }
            }
        }
        synchronizeCamelSegments();
    }

    public void takeDirectDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        if (dead || damage == 0) {
            return;
        }
        hitPoints = Math.max(0, hitPoints - damage);
        if (hitPoints == 0) {
            kill();
        }
        synchronizeCamelSegments();
    }

    public void applyPoison(int damagePerTick, double tickIntervalSeconds,
            double durationSeconds) {
        if (damagePerTick < 0 || !Double.isFinite(tickIntervalSeconds)
                || tickIntervalSeconds <= 0.0 || !Double.isFinite(durationSeconds)
                || durationSeconds < 0.0) {
            throw new IllegalArgumentException("poison values are invalid");
        }
        poisonDamagePerTick = Math.max(poisonDamagePerTick, damagePerTick);
        poisonTickIntervalSeconds = tickIntervalSeconds;
        poisonDurationSeconds = Math.max(poisonDurationSeconds, durationSeconds);
    }

    void updatePoison(float deltaSeconds) {
        if (dead || poisonDurationSeconds <= 0.0 || poisonDamagePerTick <= 0) {
            return;
        }
        double activeSeconds = Math.min(deltaSeconds, poisonDurationSeconds);
        poisonDurationSeconds = Math.max(0.0, poisonDurationSeconds - deltaSeconds);
        poisonTickTimerSeconds += activeSeconds;
        while (!dead && poisonTickTimerSeconds + 0.000001 >= poisonTickIntervalSeconds) {
            poisonTickTimerSeconds -= poisonTickIntervalSeconds;
            takeDirectDamage(poisonDamagePerTick);
        }
        if (poisonDurationSeconds <= 0.0) {
            poisonDamagePerTick = 0;
            poisonTickTimerSeconds = 0.0;
        }
    }

    public void heal(int amount) {
        hitPoints = Math.min(hitPoints + amount, maximumHitPoints);
    }

    public void applyDifficulty(int newDifficultyLevel) {
        DifficultyRules oldRules = DifficultyRules.forLevel(difficultyLevel);
        DifficultyRules newRules = DifficultyRules.forLevel(newDifficultyLevel);
        double oldHealth = oldRules.getZombieHealthMultiplier();
        double newHealth = newRules.getZombieHealthMultiplier();
        double healthRatio = maximumHitPoints == 0
                ? 0.0
                : (double) hitPoints / maximumHitPoints;
        maximumHitPoints = Math.max(1,
                (int) Math.round(maximumHitPoints
                        / oldHealth * newHealth));
        hitPoints = Math.max(0,
                (int) Math.round(maximumHitPoints * healthRatio));
        if (armor != null) {
            armor.rescaleHealth(oldHealth, newHealth);
        }
        difficultyLevel = newDifficultyLevel;
    }

    public void kill() {
        asZombie().dead = true;
    }

    public boolean isDead() {
        return dead || hitPoints <= 0;
    }

    public void markReachedHouse() {
        asZombie().reachedHouse = true;
    }

    public boolean hasReachedHouse() {
        return reachedHouse;
    }

    public void markDeathReported() {
        asZombie().deathReported = true;
    }

    public boolean isDeathReported() {
        return deathReported;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public boolean areDeathDropsProcessed() {
        return deathDropsProcessed;
    }

    public void markDeathDropsProcessed() {
        deathDropsProcessed = true;
    }

    public void moveToLane(int newLane) {
        asZombie().lane = newLane;
        updatePosition();
    }

    public void moveTo(double newColumn) {
        asZombie().columnPosition = newColumn;
        updatePosition();
    }

    void updatePosition() {
        setEntityPosition(new EntityPosition(lane, (int) Math.floor(columnPosition)));
    }
}
