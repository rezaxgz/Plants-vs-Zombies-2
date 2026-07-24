package model.game.entities.zombies;

import model.game.DifficultyRules;
import model.game.entities.other.PushedObstacle;
import model.game.entities.plants.BasePlant;
import model.game.entities.zombies.armor.Armor;
import model.game.entities.zombies.armor.ArmorType;
import model.game.entities.zombies.movement.BasicZombieMovement;
import model.game.entities.zombies.attack.PlantEatingAttack;
import java.util.ArrayList;

abstract class ZombieCombatLogic extends ZombieStatusLogic {
    protected ZombieCombatLogic(ZombieType type, int waveNumber, int lane, double columnPosition, boolean glowing) {
        super(type, waveNumber, lane, columnPosition, glowing);
    }

    void reconfigureType(ZombieType newType) {
        if (newType == null) {
            throw new IllegalArgumentException("newType cannot be null");
        }
        type = newType;
        maximumHitPoints = newType.getHitpoints();
        hitPoints = maximumHitPoints;
        armor = null;
        if (newType.getDefaultArmor() != null
                && newType.getDefaultArmor() != ArmorType.NONE) {
            armor = new Armor(newType.getDefaultArmor());
        }
        DifficultyRules rules =
                DifficultyRules.forLevel(difficultyLevel);
        maximumHitPoints = Math.max(1,
                (int) Math.round(maximumHitPoints
                        * rules.getZombieHealthMultiplier()));
        hitPoints = maximumHitPoints;
        if (armor != null) {
            armor.rescaleHealth(1.0,
                    rules.getZombieHealthMultiplier());
        }
        abilities = new ArrayList<>();
        initializeAbilities(newType.getAbilitySpecs());
        movementBehavior = new BasicZombieMovement(newType.getSpeed());
        attackBehavior = new PlantEatingAttack(newType.getEatDPS());
        dead = false;
        deathReported = false;
        reachedHouse = false;
        pendingZombieAttackDamage = 0.0;
        clearColdEffects();
        stunned = false;
        stunnedDuration = 0.0;
        poisonDamagePerTick = 0;
        poisonDurationSeconds = 0.0;
        poisonTickTimerSeconds = 0.0;
    }

    public boolean hasMagnetizableArmor() {
        return armor != null && armor.isMagnetizable();
    }

    public boolean removeMagnetizableArmor() {
        return armor != null && armor.removeByMagnet();
    }

    public int getCurrentDurability() {
        int durability = hitPoints;
        if (armor != null && !armor.isDestroyed()) {
            durability += armor.getCurrentHealth();
        }
        return durability;
    }

    public void moveRight(float deltaSeconds, double maximumColumn) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f
                || !Double.isFinite(maximumColumn)) {
            throw new IllegalArgumentException("movement values are invalid");
        }
        double distance = getEffectiveSpeed() * deltaSeconds;
        moveTo(Math.min(maximumColumn, columnPosition + distance));
    }

    public void attackZombie(Zombie target, float deltaSeconds) {
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null");
        }
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException(
                    "deltaSeconds must be finite and non-negative");
        }
        if (isDead() || target.isDead() || target == asZombie()) {
            return;
        }
        pendingZombieAttackDamage += getEffectiveEatDPS() * deltaSeconds;
        int damage = (int) pendingZombieAttackDamage;
        if (damage > 0) {
            target.takeDamage(damage);
            pendingZombieAttackDamage -= damage;
        }
    }

    public void attackObstacle(PushedObstacle obstacle,
            float deltaSeconds) {
        if (obstacle == null) {
            throw new IllegalArgumentException(
                    "obstacle cannot be null");
        }
        if (!Float.isFinite(deltaSeconds)
                || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException(
                    "deltaSeconds must be finite and non-negative");
        }
        if (isDead() || obstacle.isDestroyed()) {
            return;
        }

        pendingZombieAttackDamage +=
                getEffectiveEatDPS() * deltaSeconds;
        int damage = (int) pendingZombieAttackDamage;
        if (damage > 0) {
            obstacle.takeDamage(damage);
            pendingZombieAttackDamage -= damage;
        }
    }

    public String getName() {
        return type.getAlias(); // or type.name() for the enum name
    }

    public void move(float deltaSeconds, double minimumColumn) {
        movementBehavior.move(asZombie(), deltaSeconds, minimumColumn);
    }

    public void eat(BasePlant plant, float deltaSeconds) {
        attackBehavior.attack(asZombie(), plant, deltaSeconds);
    }
}
