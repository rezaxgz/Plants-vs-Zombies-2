package io.github.some_example_name.model.game.entities.zombies;

import java.util.List;

import io.github.some_example_name.model.game.DifficultyRules;
import io.github.some_example_name.model.game.entities.zombies.abilities.*;
import io.github.some_example_name.model.game.entities.zombies.armor.Armor;
import io.github.some_example_name.model.game.entities.zombies.armor.ArmorType;
import io.github.some_example_name.model.game.entities.zombies.attack.AttackBehavior;
import io.github.some_example_name.model.game.entities.zombies.movement.MovementBehavior;

abstract class ZombieStatusLogic extends ZombieDamageLogic {
    protected ZombieStatusLogic(ZombieType type, int waveNumber, int lane, double columnPosition, boolean glowing) {
        super(type, waveNumber, lane, columnPosition, glowing);
    }

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

    public boolean canReceiveArmor() {
        return !isDead() && (armor == null || armor.isDestroyed());
    }

    public boolean equipArmor(ArmorType armorType) {
        if (armorType == null || armorType == ArmorType.NONE
                || !canReceiveArmor()) {
            return false;
        }
        armor = new Armor(armorType);
        return true;
    }

    public List<ZombieAbility> getAbilities() {
        return abilities;
    }

    public int getActiveCamelSegments() {
        for (ZombieAbility ability : abilities) {
            if (ability instanceof CamelSegmentAbility) {
                return ((CamelSegmentAbility) ability).getCurrentSegments();
            }
        }
        return 1;
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

    public boolean isStunned() {
        return stunned;
    }

    public void applyStun(double duration) {
        if (!Double.isFinite(duration) || duration < 0.0) {
            throw new IllegalArgumentException("duration must be finite and non-negative");
        }
        if (duration == 0.0) {
            return;
        }
        stunned = true;
        stunnedDuration = Math.max(stunnedDuration, duration);
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isSubmerged() {
        return submerged;
    }

    public void setChilled(double duration) {
        asZombie().chilled = true;
        asZombie().chilledDuration = duration;
    }

    public void applyChill(double duration) {
        if (!Double.isFinite(duration) || duration < 0.0) {
            throw new IllegalArgumentException("duration must be finite and non-negative");
        }
        if (isColdImmune()) {
            return;
        }
        setChilled(Math.max(chilledDuration, duration));
        for (ZombieAbility ability : abilities) {
            if (ability instanceof TorchAbility) {
                ((TorchAbility) ability).extinguish();
            }
        }
    }

    public void setChapterColdImmune(boolean chapterColdImmune) {
        asZombie().chapterColdImmune = chapterColdImmune;
        if (chapterColdImmune) {
            clearColdEffects();
        }
    }

    public void encaseInIce() {
        encaseInIce(DEFAULT_FROZEN_SHELL_HIT_POINTS);
    }

    public void encaseInIce(int hitPoints) {
        if (hitPoints <= 0) {
            throw new IllegalArgumentException(
                    "frozen shell hit points must be positive");
        }
        frozenShellHitPoints = hitPoints;
        frozenShellMaximumHitPoints = hitPoints;
        frozenShellMeltRemainder = 0.0;
        clearColdEffects();
    }

    public boolean damageFrozenShell(int damage, boolean fireDamage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        if (!isEncasedInIce()) {
            return false;
        }
        int appliedDamage = fireDamage
                ? frozenShellHitPoints
                : Math.max(1, damage);
        frozenShellHitPoints = Math.max(0,
                frozenShellHitPoints - appliedDamage);
        if (frozenShellHitPoints == 0) {
            frozenShellMeltRemainder = 0.0;
            return true;
        }
        return false;
    }

    public boolean meltFrozenShell(double damage) {
        if (!Double.isFinite(damage) || damage < 0.0) {
            throw new IllegalArgumentException(
                    "melt damage must be finite and non-negative");
        }
        if (!isEncasedInIce() || damage == 0.0) {
            return false;
        }
        frozenShellMeltRemainder += damage;
        int wholeDamage = (int) Math.floor(frozenShellMeltRemainder);
        if (wholeDamage == 0) {
            return false;
        }
        frozenShellMeltRemainder -= wholeDamage;
        return damageFrozenShell(wholeDamage, false);
    }

    public boolean isEncasedInIce() {
        return frozenShellHitPoints > 0;
    }

    public int getFrozenShellHitPoints() {
        return Math.max(0, frozenShellHitPoints);
    }

    public int getFrozenShellMaximumHitPoints() {
        return Math.max(0, frozenShellMaximumHitPoints);
    }

    public boolean markSliderTriggered(int column) {
        if (lastTriggeredSliderColumn == column) {
            return false;
        }
        lastTriggeredSliderColumn = column;
        return true;
    }

    public void clearSliderTrigger() {
        lastTriggeredSliderColumn = Integer.MIN_VALUE;
    }

    public void applyFireDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        clearColdEffects();
        for (ZombieAbility ability : abilities) {
            if (ability instanceof TorchAbility) {
                ((TorchAbility) ability).ignite();
            }
        }
        if (!isFireImmune()) {
            takeDamage(damage);
        }
    }

    public void clearColdEffects() {
        chilled = false;
        chilledDuration = 0.0;
        frozen = false;
        frozenDuration = 0.0;
    }

    public boolean isFireImmune() {
        return type == ZombieType.DRAGON_IMP;
    }

    public boolean isColdImmune() {
        if (chapterColdImmune) {
            return true;
        }
        switch (type) {
            case ICEAGE:
            case ICEAGE_CONEHEAD:
            case ICEAGE_BUCKETHEAD:
            case ICEAGE_BLOCKHEAD:
            case HUNTER:
            case TROGLOBITE:
            case DODO:
            case WEASEL_HOARDER:
            case WEASEL:
            case ICEAGE_GARGANTUAR:
            case ICEAGE_IMP:
                return true;
            default:
                return false;
        }
    }

    public void setFrozen(double duration) {
        applyFreeze(duration);
    }

    public void applyFreeze(double duration) {
        if (!Double.isFinite(duration) || duration < 0.0) {
            throw new IllegalArgumentException("duration must be finite and non-negative");
        }
        if (isColdImmune()) {
            return;
        }
        frozen = true;
        frozenDuration = Math.max(frozenDuration, duration);
        chilled = false;
        chilledDuration = 0.0;
        for (ZombieAbility ability : abilities) {
            if (ability instanceof TorchAbility) {
                ((TorchAbility) ability).extinguish();
            }
        }
    }

    public void setFlying(boolean flying) {
        asZombie().flying = flying;
    }

    public void setSubmerged(boolean submerged) {
        asZombie().submerged = submerged;
    }

    public double getEffectiveSpeed() {
        double speed = type.getSpeed();
        for (ZombieAbility ability : abilities) {
            if (ability instanceof PharaohSpeedAbility) {
                speed = ((PharaohSpeedAbility) ability).getEffectiveSpeed(speed);
            } else if (ability instanceof SurfAbility) {
                speed = ((SurfAbility) ability).getEffectiveSpeed(speed);
            } else if (ability instanceof FastSwimAbility) {
                speed = ((FastSwimAbility) ability).getEffectiveSpeed(speed);
            } else if (ability instanceof JuggleAbility) {
                speed *= ((JuggleAbility) ability).getSpeedMultiplier();
            }
        }
        if (frozen || stunned || isEncasedInIce())
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
        for (ZombieAbility ability : abilities) {
            if (ability instanceof TackleAbility) {
                speed *= ((TackleAbility) ability).getSpeedMultiplier();
            }
        }
        return speed;
    }

    public int getEffectiveEatDPS() {
        if (frozen || stunned || isEncasedInIce()) {
            return 0;
        }
        int dps = hypnotized && alliedAttackDpsOverride > 0
                ? alliedAttackDpsOverride
                : type.getEatDPS();
        dps = Math.max(1, (int) Math.round(
                dps * DifficultyRules.forLevel(difficultyLevel)
                        .getZombieDamageMultiplier()));
        if (chilled) {
            dps = (int) Math.floor(dps * 0.5);
        }
        if (enraged) {
            for (ZombieAbility ability : abilities) {
                if (ability instanceof EnrageAbility) {
                    double scale = ((EnrageAbility) ability).getEnragedDamageScale();
                    dps = (int) Math.floor(dps * scale);
                }
            }
        }
        if (hypnotized) {
            dps = (int) Math.floor(dps * hypnotizedDamageMultiplier);
        }
        return dps;
    }

    public double getChilledDuration() {
        return Math.max(0.0, chilledDuration);
    }

    public double getFrozenDuration() {
        return Math.max(0.0, frozenDuration);
    }

    public double getStunnedDuration() {
        return Math.max(0.0, stunnedDuration);
    }

    public double getPoisonDurationSeconds() {
        return Math.max(0.0, poisonDurationSeconds);
    }

    public int getPoisonDamagePerTick() {
        return poisonDamagePerTick;
    }

    public boolean isHypnotized() {
        return hypnotized;
    }

    public void hypnotize() {
        hypnotize(1.0, 1.0);
    }

    public void hypnotize(double healthMultiplier, double damageMultiplier) {
        if (isDead() || type.isBoss()) {
            return;
        }
        if (!Double.isFinite(healthMultiplier) || healthMultiplier < 1.0
                || !Double.isFinite(damageMultiplier) || damageMultiplier < 1.0) {
            throw new IllegalArgumentException("hypnosis multipliers must be finite and at least 1");
        }
        if (!hypnotized && healthMultiplier > 1.0) {
            maximumHitPoints = Math.max(1,
                    (int) Math.round(maximumHitPoints * healthMultiplier));
            hitPoints = Math.max(1, (int) Math.round(hitPoints * healthMultiplier));
        }
        hypnotized = true;
        hypnotizedDamageMultiplier = Math.max(
                hypnotizedDamageMultiplier, damageMultiplier);
        reachedHouse = false;
        submerged = false;
    }

    public void transformIntoAlliedGargantuar(double healthMultiplier,
            double damageMultiplier) {
        if (isDead() || type.isBoss()) {
            return;
        }
        reconfigureType(ZombieType.GARGANTUAR);
        alliedAttackDpsOverride = 1500;
        hypnotize(healthMultiplier, damageMultiplier);
    }
}
