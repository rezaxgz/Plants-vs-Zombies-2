package io.github.some_example_name.model.game.entities.zombies;

import io.github.some_example_name.model.game.entities.zombies.abilities.*;

abstract class ZombieAbilityLogic extends ZombieState {
    protected ZombieAbilityLogic(ZombieType type, int waveNumber, int lane, double columnPosition, boolean glowing) {
        super(type, waveNumber, lane, columnPosition, glowing);
    }

    void initializeAbilities(String[] abilitySpecs) {
        if (abilitySpecs == null)
            return;

        for (String spec : abilitySpecs) {
            ZombieAbility ability = parseAbility(spec);
            if (ability != null) {
                abilities.add(ability);
            }
        }
    }

    ZombieAbility parseAbility(String spec) {
        if (spec == null)
            return null;
        String[] parts = spec.split(":");
        String abilityName = parts[0];

        switch (abilityName) {
            case "SmashAbility":
                return new SmashAbility(Integer.parseInt(parts[1]));
            case "ImpThrowAbility":
                return new ImpThrowAbility(Double.parseDouble(parts[1]), parts[2]);
            case "PharaohSpeedAbility":
                return new PharaohSpeedAbility(Double.parseDouble(parts[1]));
            case "SunStealAbility":
                return new SunStealAbility(Integer.parseInt(parts[1]));
            case "TorchAbility":
                return new TorchAbility(Double.parseDouble(parts[1]));
            case "TombSummonAbility":
                return new TombSummonAbility(
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Double.parseDouble(parts[3]));
            case "CamelSegmentAbility":
                return new CamelSegmentAbility(Integer.parseInt(parts[1]));
            case "FlyAbility":
                return new FlyAbility(Integer.parseInt(parts[1]), Double.parseDouble(parts[2]));
            case "ChillOnHitAbility":
                return new ChillOnHitAbility();
            case "SnowballThrowAbility":
                return new SnowballThrowAbility(
                        Integer.parseInt(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]));
            case "IceBlockPushAbility":
                return new IceBlockPushAbility(Integer.parseInt(parts[1]));
            case "WeaselReleaseAbility":
                return new WeaselReleaseAbility(Integer.parseInt(parts[1]));
            case "SubmergeAbility":
                return new SubmergeAbility();
            case "SurfAbility":
                return new SurfAbility(Double.parseDouble(parts[1]));
            case "FastSwimAbility":
                return new FastSwimAbility();
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
            case "ArcadePushAbility":
                return new ArcadePushAbility();
            case "BarrelPushAbility":
                return new BarrelPushAbility(Integer.parseInt(parts[1]));
            case "UmbrellaBounceAbility":
                return new UmbrellaBounceAbility();
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
            case "TackleAbility":
                return new TackleAbility(
                        Integer.parseInt(parts[1]),
                        Double.parseDouble(parts[2]));
            case "ZombossAbility":
                return new ZombossAbility(parts[1]);
            default:
                return null;
        }
    }

    public void update(float deltaSeconds) {
        updateEntity(deltaSeconds);

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
        if (stunned) {
            stunnedDuration -= deltaSeconds;
            if (stunnedDuration <= 0.0) {
                stunned = false;
                stunnedDuration = 0.0;
            }
        }
        updatePoison(deltaSeconds);

        // Update abilities
        for (ZombieAbility ability : abilities) {
            ability.update(deltaSeconds);
            if (ability instanceof PharaohSpeedAbility) {
                ability.tryUse(asZombie(), null);
            }
        }
        synchronizeCamelSegments();
    }

    void synchronizeCamelSegments() {
        for (ZombieAbility ability : abilities) {
            if (ability instanceof CamelSegmentAbility) {
                ability.tryUse(asZombie(), null);
            }
        }
    }
}
