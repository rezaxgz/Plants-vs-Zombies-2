package model.game.entities.zombies;

import model.Constants;
import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.plants.BasePlant;
import model.game.entities.zombies.armor.Armor;
import model.game.entities.zombies.armor.ArmorType;
import model.game.entities.zombies.abilities.*;
import model.game.entities.zombies.movement.MovementBehavior;
import model.game.entities.zombies.attack.AttackBehavior;
import model.game.entities.zombies.movement.BasicZombieMovement;
import model.game.entities.zombies.attack.PlantEatingAttack;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Zombie class with support for armor, abilities, and special
 * behaviors.
 */
public class Zombie extends Entity {
    public static final double ATTACK_REACH = 0.15;
    public static final double DEFAULT_SPEED = 0.185;

    private ZombieType type;
    private int waveNumber;
    private int hitPoints;
    private int maximumHitPoints;
    private int lane;
    private double columnPosition;
    private boolean reachedHouse;
    private boolean dead;
    private boolean deathReported;

    private Armor armor;
    private List<ZombieAbility> abilities;
    private MovementBehavior movementBehavior;
    private AttackBehavior attackBehavior;

    // Special state flags
    private boolean chilled;
    private double chilledDuration;
    private boolean frozen;
    private double frozenDuration;
    private boolean enraged;
    private boolean flying;
    private boolean submerged;

    public Zombie(ZombieType type, int waveNumber, int lane, double columnPosition) {
        super(new EntityPosition(lane, (int) columnPosition));
        this.type = type;
        this.waveNumber = waveNumber;
        this.lane = lane;
        this.columnPosition = columnPosition;
        this.hitPoints = type.getHitpoints();
        this.maximumHitPoints = type.getHitpoints();
        this.reachedHouse = false;
        this.dead = false;
        this.deathReported = false;
        this.abilities = new ArrayList<>();

        // Initialize armor if applicable
        if (type.getDefaultArmor() != null && type.getDefaultArmor() != ArmorType.NONE) {
            this.armor = new Armor(type.getDefaultArmor());
        }

        // Initialize abilities from specs
        initializeAbilities(type.getAbilitySpecs());

        // Default behaviors
        this.movementBehavior = new BasicZombieMovement(Constants.BASIC_ZOMBIE_MOVEMENT_SPEED);
        this.attackBehavior = new PlantEatingAttack(type.getEatDPS());
    }

    private void initializeAbilities(String[] abilitySpecs) {
        if (abilitySpecs == null)
            return;

        for (String spec : abilitySpecs) {
            ZombieAbility ability = parseAbility(spec);
            if (ability != null) {
                abilities.add(ability);
            }
        }
    }

    private ZombieAbility parseAbility(String spec) {
        if (spec == null)
            return null;
        String[] parts = spec.split(":");
        String abilityName = parts[0];

        switch (abilityName) {
            case "SmashAbility":
                return new SmashAbility(Integer.parseInt(parts[1]));
            case "ImpThrowAbility":
                return new ImpThrowAbility(Double.parseDouble(parts[1]), parts[2]);
            case "SunStealAbility":
                return new SunStealAbility(Integer.parseInt(parts[1]));
            case "TorchAbility":
                return new TorchAbility(Double.parseDouble(parts[1]));
            case "TombSummonAbility":
                return new TombSummonAbility(
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Double.parseDouble(parts[3]));
            case "FlyAbility":
                return new FlyAbility(Integer.parseInt(parts[1]), Double.parseDouble(parts[2]));
            case "SnowballThrowAbility":
                return new SnowballThrowAbility(
                        Integer.parseInt(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]));
            case "IceBlockPushAbility":
                return new IceBlockPushAbility(Integer.parseInt(parts[1]));
            case "FishingHookAbility":
                return new FishingHookAbility(
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]),
                        Double.parseDouble(parts[4]));
            case "OctopusThrowAbility":
                return new OctopusThrowAbility();
            case "WizardSpellAbility":
                return new WizardSpellAbility();
            case "JuggleAbility":
                return new JuggleAbility(Integer.parseInt(parts[1]), Double.parseDouble(parts[2]));
            case "KingBuffAbility":
                return new KingBuffAbility(
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]));
            case "LaserBeamAbility":
                return new LaserBeamAbility(
                        Integer.parseInt(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]));
            case "LaunchAbility":
                return new LaunchAbility(
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]));
            case "PianoCrushAbility":
                return new PianoCrushAbility(Double.parseDouble(parts[1]));
            case "EnrageAbility":
                return new EnrageAbility(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
            default:
                return null;
        }
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);

        // Update status effects
        if (chilled) {
            chilledDuration -= deltaSeconds;
            if (chilledDuration <= 0)
                chilled = false;
        }
        if (frozen) {
            frozenDuration -= deltaSeconds;
            if (frozenDuration <= 0) {
                frozen = false;
                chilled = true;
                chilledDuration = 10; // Chill after freeze wears off
            }
        }

        // Update abilities
        for (ZombieAbility ability : abilities) {
            ability.update(deltaSeconds);
        }
    }

    /**
     * Apply damage to this zombie. Armor absorbs damage first.
     */
    public void takeDamage(int damage) {
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
        if (armor != null && armor.isDestroyed() && armor.getType() == ArmorType.NEWSPAPER) {
            for (ZombieAbility ability : abilities) {
                if (ability instanceof EnrageAbility) {
                    ((EnrageAbility) ability).tryUse(this, null);
                    enraged = true;
                }
            }
        }
    }

    public void heal(int amount) {
        hitPoints = Math.min(hitPoints + amount, maximumHitPoints);
    }

    public void kill() {
        this.dead = true;
    }

    public boolean isDead() {
        return dead || hitPoints <= 0;
    }

    public void markReachedHouse() {
        this.reachedHouse = true;
    }

    public boolean hasReachedHouse() {
        return reachedHouse;
    }

    public void markDeathReported() {
        this.deathReported = true;
    }

    public boolean isDeathReported() {
        return deathReported;
    }

    // Movement
    public void moveToLane(int newLane) {
        this.lane = newLane;
        updatePosition();
    }

    public void moveTo(double newColumn) {
        this.columnPosition = newColumn;
        updatePosition();
    }

    private void updatePosition() {
        setEntityPosition(new EntityPosition(lane, (int) Math.floor(columnPosition)));
    }

    // Getters
    public ZombieType getType() {
        return type;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public int getHitPoints() {
        return hitPoints;
    }

    public int getMaximumHitPoints() {
        return maximumHitPoints;
    }

    public int getLane() {
        return lane;
    }

    public double getColumnPosition() {
        return columnPosition;
    }

    public Armor getArmor() {
        return armor;
    }

    public List<ZombieAbility> getAbilities() {
        return abilities;
    }

    public MovementBehavior getMovementBehavior() {
        return movementBehavior;
    }

    public AttackBehavior getAttackBehavior() {
        return attackBehavior;
    }

    public boolean isChilled() {
        return chilled;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isEnraged() {
        return enraged;
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isSubmerged() {
        return submerged;
    }

    public void setChilled(double duration) {
        this.chilled = true;
        this.chilledDuration = duration;
    }

    public void setFrozen(double duration) {
        this.frozen = true;
        this.frozenDuration = duration;
        this.chilled = false;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
    }

    public void setSubmerged(boolean submerged) {
        this.submerged = submerged;
    }

    /**
     * Get effective speed (modified by chill/freeze/enrage).
     */
    public double getEffectiveSpeed() {
        double speed = type.getSpeed();
        if (frozen)
            return 0;
        if (chilled)
            speed *= 0.5;
        if (enraged) {
            for (ZombieAbility ability : abilities) {
                if (ability instanceof EnrageAbility) {
                    speed *= ((EnrageAbility) ability).getEnragedSpeedScale();
                }
            }
        }
        return speed;
    }

    /**
     * Get effective eat DPS (modified by enrage).
     */
    public int getEffectiveEatDPS() {
        int dps = type.getEatDPS();
        if (enraged) {
            for (ZombieAbility ability : abilities) {
                if (ability instanceof EnrageAbility) {
                    dps *= ((EnrageAbility) ability).getEnragedDamageScale();
                }
            }
        }
        return dps;
    }

    public String getName() {
        return type.getAlias(); // or type.name() for the enum name
    }

    public void move(float deltaSeconds, double minimumColumn) {
        movementBehavior.move(this, deltaSeconds, minimumColumn);
    }

    public void eat(BasePlant plant, float deltaSeconds) {
        attackBehavior.attack(this, plant, deltaSeconds);
    }
}
