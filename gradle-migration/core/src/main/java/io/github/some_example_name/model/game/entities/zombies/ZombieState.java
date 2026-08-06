package io.github.some_example_name.model.game.entities.zombies;

import java.util.ArrayList;
import java.util.List;

import io.github.some_example_name.model.game.entities.Entity;
import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.zombies.abilities.*;
import io.github.some_example_name.model.game.entities.zombies.armor.Armor;
import io.github.some_example_name.model.game.entities.zombies.armor.ArmorType;
import io.github.some_example_name.model.game.entities.zombies.attack.AttackBehavior;
import io.github.some_example_name.model.game.entities.zombies.attack.PlantEatingAttack;
import io.github.some_example_name.model.game.entities.zombies.movement.BasicZombieMovement;
import io.github.some_example_name.model.game.entities.zombies.movement.MovementBehavior;

abstract class ZombieState extends Entity {
    public static final double ATTACK_REACH = 0.15;
    public static final double DEFAULT_SPEED = 0.185;
    public static final int DEFAULT_FROZEN_SHELL_HIT_POINTS = 600;

    ZombieType type;
    int waveNumber;
    int hitPoints;
    int maximumHitPoints;
    int lane;
    double columnPosition;
    boolean reachedHouse;
    boolean dead;
    boolean deathReported;
    boolean deathDropsProcessed;
    final boolean glowing;

    Armor armor;
    List<ZombieAbility> abilities;
    MovementBehavior movementBehavior;
    AttackBehavior attackBehavior;

    // Special state flags
    boolean chilled;
    double chilledDuration;
    boolean frozen;
    double frozenDuration;
    boolean enraged;
    boolean stunned;
    double stunnedDuration;
    boolean flying;
    boolean submerged;
    boolean hypnotized;
    boolean chapterColdImmune;
    int frozenShellHitPoints;
    int frozenShellMaximumHitPoints;
    int lastTriggeredSliderColumn = Integer.MIN_VALUE;
    double frozenShellMeltRemainder;
    double hypnotizedDamageMultiplier = 1.0;
    int alliedAttackDpsOverride;
    double pendingZombieAttackDamage;
    int difficultyLevel = 3;
    int poisonDamagePerTick;
    double poisonTickIntervalSeconds;
    double poisonDurationSeconds;
    double poisonTickTimerSeconds;

    protected ZombieState(ZombieType type, int waveNumber, int lane,
            double columnPosition, boolean glowing) {
        super(new EntityPosition(lane, (int) columnPosition));
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        this.type = type;
        this.waveNumber = waveNumber;
        this.lane = lane;
        this.columnPosition = columnPosition;
        this.hitPoints = type.getHitpoints();
        this.maximumHitPoints = type.getHitpoints();
        this.reachedHouse = false;
        this.dead = false;
        this.deathReported = false;
        this.deathDropsProcessed = false;
        this.glowing = glowing;
        this.abilities = new ArrayList<>();

        // Initialize armor if applicable
        if (type.getDefaultArmor() != null && type.getDefaultArmor() != ArmorType.NONE) {
            this.armor = new Armor(type.getDefaultArmor());
        }

        // Initialize abilities from specs
        initializeAbilities(type.getAbilitySpecs());

        // Default behaviors
        this.movementBehavior = new BasicZombieMovement(type.getSpeed());
        this.attackBehavior = new PlantEatingAttack(type.getEatDPS());
    }

    final Zombie asZombie() {
        return (Zombie) this;
    }

    final void updateEntity(float deltaSeconds) {
        super.update(deltaSeconds);
    }

    abstract void initializeAbilities(String[] abilitySpecs);

    abstract void updatePoison(float deltaSeconds);

    abstract void reconfigureType(ZombieType newType);
}
