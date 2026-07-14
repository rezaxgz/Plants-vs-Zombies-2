package model.game.entities.zombies;

import model.game.entities.zombies.armor.ArmorType;

/**
 * Complete enum of all zombie types in the game.
 * Each type defines base stats, armor, abilities, and special behaviors.
 */
public enum ZombieType {
    // === CORE ZOMBIES (All Chapters) ===
    BASIC("ZombieTutorialDefault", 190, 0.185, 100, 100, 1000,
            ArmorType.NONE, null, false),
    CONEHEAD("ZombieTutorialArmor1Default", 190, 0.185, 100, 200, 3000,
            ArmorType.CONE, null, false),
    BUCKETHEAD("ZombieTutorialArmor2Default", 190, 0.185, 100, 400, 4000,
            ArmorType.BUCKET, null, false),
    BRICKHEAD("ZombieTutorialArmor4Default", 190, 0.185, 100, 700, 3000,
            ArmorType.BRICK, null, false),
    GARGANTUAR("ZombieGargantuarBasic", 3600, 0.24, 0, 1500, 3000,
            ArmorType.NONE, new String[] { "SmashAbility:1500", "ImpThrowAbility:0.5:imp" }, true),
    IMP("ZombieTutorialImpDefault", 190, 0.22, 100, 100, 1000,
            ArmorType.NONE, null, false),
    FLAG("ZombieTutorialFlagDefault", 190, 0.185, 100, 100, 1000,
            ArmorType.NONE, null, false),

    // === ANCIENT EGYPT ===
    MUMMY("ZombieMummyDefault", 190, 0.185, 100, 100, 1000,
            ArmorType.NONE, null, false),
    MUMMY_CONEHEAD("ZombieMummyArmor1Default", 190, 0.185, 100, 200, 3000,
            ArmorType.CONE, null, false),
    MUMMY_BUCKETHEAD("ZombieMummyArmor2Default", 190, 0.185, 100, 400, 4000,
            ArmorType.BUCKET, null, false),
    MUMMY_BRICKHEAD("ZombieMummyArmor4Default", 190, 0.185, 100, 700, 3000,
            ArmorType.BRICK, null, false),
    PHARAOH("ZombiePharaohDefault", 490, 0.12, 100, 300, 2000,
            ArmorType.SARCOPHAGUS, new String[] { "PharaohSpeedAbility:0.3" }, false),
    RA("ZombieRaDefault", 190, 0.2, 100, 100, 700,
            ArmorType.NONE, new String[] { "SunStealAbility:250" }, false),
    EXPLORER("ZombieExplorerDefault", 250, 0.25, 100, 250, 3000,
            ArmorType.NONE, new String[] { "TorchAbility:37" }, false),
    TOMB_RAISER("ZombieTombRaiserDefault", 380, 0.185, 100, 300, 2000,
            ArmorType.NONE, new String[] { "TombSummonAbility:5:2:6" }, false),
    CAMEL("ZombieCamelDefault", 570, 0.185, 100, 300, 2000,
            ArmorType.NONE, new String[] { "CamelSegmentAbility:3" }, false),
    EGYPT_GARGANTUAR("ZombieEgyptGargantuar", 3600, 0.24, 0, 1500, 3000,
            ArmorType.NONE, new String[] { "SmashAbility:1500", "ImpThrowAbility:0.5:egypt_imp" }, true),
    EGYPT_IMP("ZombieEgyptImpDefault", 190, 0.22, 100, 100, 1000,
            ArmorType.NONE, null, false),

    // === FROSTBITE CAVES ===
    ICEAGE("ZombieIceageDefault", 190, 0.185, 100, 100, 1000,
            ArmorType.NONE, null, false),
    ICEAGE_CONEHEAD("ZombieIceageArmor1Default", 190, 0.185, 100, 200, 3000,
            ArmorType.CONE, null, false),
    ICEAGE_BUCKETHEAD("ZombieIceageArmor2Default", 190, 0.185, 100, 400, 4000,
            ArmorType.BUCKET, null, false),
    ICEAGE_BLOCKHEAD("ZombieIceageArmor3Default", 490, 0.185, 100, 500, 3500,
            ArmorType.ICE_BLOCK, new String[] { "ChillOnHitAbility" }, false),
    HUNTER("ZombieIceAgeHunter", 700, 0.12, 100, 500, 3500,
            ArmorType.NONE, new String[] { "SnowballThrowAbility:3:4:1" }, false),
    TROGLOBITE("ZombieIceAgeTroglobite", 470, 0.185, 100, 600, 3500,
            ArmorType.NONE, new String[] { "IceBlockPushAbility:3" }, false),
    DODO("ZombieIceAgeDodo", 490, 0.3, 100, 600, 3500,
            ArmorType.NONE, new String[] { "FlyAbility:2:0.1" }, false),
    WEASEL_HOARDER("ZombieWeaselHoarderDefault", 350, 0.185, 100, 400, 3000,
            ArmorType.NONE, new String[] { "WeaselReleaseAbility:12" }, false),
    WEASEL("ZombieWeaselDefault", 65, 0.4, 50, 50, 500,
            ArmorType.NONE, null, false),
    ICEAGE_GARGANTUAR("ZombieIceAgeGargantuar", 3600, 0.24, 0, 1500, 3000,
            ArmorType.NONE, new String[] { "SmashAbility:1500", "ImpThrowAbility:0.5:iceage_imp" }, true),
    ICEAGE_IMP("ZombieIceageImpDefault", 190, 0.22, 100, 100, 1000,
            ArmorType.NONE, null, false),

    // === BIG WAVE BEACH ===
    BEACH("ZombieBeachDefault", 190, 0.185, 100, 100, 1000,
            ArmorType.NONE, null, false),
    BEACH_CONEHEAD("ZombieBeachArmor1Default", 190, 0.185, 100, 200, 3000,
            ArmorType.CONE, null, false),
    BEACH_BUCKETHEAD("ZombieBeachArmor2Default", 190, 0.185, 100, 400, 4000,
            ArmorType.BUCKET, null, false),
    SNORKEL("ZombieBeachSnorkel", 350, 0.185, 100, 200, 3000,
            ArmorType.NONE, new String[] { "SubmergeAbility" }, false),
    SURFER("ZombieBeachSurfer", 500, 0.4, 100, 400, 3000,
            ArmorType.SURFBOARD, new String[] { "SurfAbility:0.4" }, false),
    FISHERMAN("ZombieBeachFisherman", 1000, 0.185, 100, 700, 2500,
            ArmorType.NONE, new String[] { "FishingHookAbility:0.08:0.3:2.5:0.05" }, false),
    OCTOPUS("ZombieBeachOctopus", 910, 0.12, 100, 900, 3500,
            ArmorType.NONE, new String[] { "OctopusThrowAbility" }, false),
    BEACH_GARGANTUAR("ZombieBeachGargantuar", 3600, 0.24, 0, 1500, 3000,
            ArmorType.NONE, new String[] { "SmashAbility:1500", "ImpThrowAbility:0.5:beach_imp" }, true),
    BEACH_IMP("ZombieBeachImpDefault", 190, 0.22, 100, 100, 1000,
            ArmorType.NONE, null, false),
    FAST_SWIMMER("ZombieBeachFastSwimmer", 190, 0.3, 100, 150, 1500,
            ArmorType.NONE, new String[] { "FastSwimAbility" }, false),

    // === DARK AGES ===
    DARK("ZombieDarkDefault", 190, 0.185, 100, 100, 1000,
            ArmorType.NONE, null, false),
    DARK_CONEHEAD("ZombieDarkArmor1Default", 190, 0.185, 100, 200, 3000,
            ArmorType.CONE, null, false),
    DARK_BUCKETHEAD("ZombieDarkArmor2Default", 190, 0.185, 100, 400, 4000,
            ArmorType.BUCKET, null, false),
    DARK_SHOULDER_ARMOR("ZombieDarkArmor3Default", 190, 0.185, 100, 550, 4500,
            ArmorType.SHOULDER_ARMOR, new String[] { "CrownArmorAbility:1600" }, false),
    DARK_BRICKHEAD("ZombieDarkArmor4Default", 190, 0.185, 100, 700, 3000,
            ArmorType.BRICK, null, false),
    WIZARD("ZombieWizardDefault", 490, 0.12, 100, 800, 3500,
            ArmorType.NONE, new String[] { "WizardSpellAbility" }, false),
    JUGGLER("ZombieDarkJugglerDefault", 420, 0.2, 100, 450, 3500,
            ArmorType.NONE, new String[] { "JuggleAbility:1000:120" }, false),
    DARK_KING("ZombieDarkKing", 1000, 0.185, 100, 750, 2000,
            ArmorType.NONE, new String[] { "KingBuffAbility:4:3:2.5" }, false),
    DARK_GARGANTUAR("ZombieDarkGargantuar", 3600, 0.24, 0, 1500, 3000,
            ArmorType.NONE, new String[] { "SmashAbility:1500", "ImpThrowAbility:0.5:dark_imp" }, true),
    DARK_IMP("ZombieDarkImpDefault", 190, 0.22, 100, 150, 2000,
            ArmorType.NONE, null, false),
    DRAGON_IMP("ZombieDarkImpDragonDefault", 190, 0.185, 100, 150, 2000,
            ArmorType.NONE, new String[] { "FireImmunityAbility" }, false),

    // === OTHER SPECIAL ZOMBIES ===
    ALL_STAR("ZombieModernAllStarDefault", 1100, 0.16, 100, 1000, 3500,
            ArmorType.NONE, new String[] { "TackleAbility:1500:0.5" }, false),
    ARCADE("ZombieEightiesArcade", 490, 0.19, 100, 600, 1000,
            ArmorType.NONE, new String[] { "ArcadePushAbility" }, false),
    LOST_CITY_JANE("ZombieLostCityJaneDefault", 350, 0.25, 100, 200, 3000,
            ArmorType.NONE, new String[] { "UmbrellaBounceAbility" }, false),
    CRYSTAL_SKULL("ZombieCrystalSkullDefault", 250, 0.185, 100, 500, 3000,
            ArmorType.NONE, new String[] { "LaserBeamAbility:4001:220:5" }, false),
    PROSPECTOR("ZombieProspectorDefault", 190, 0.16, 100, 200, 3000,
            ArmorType.NONE, new String[] { "LaunchAbility:10:1.5:250" }, false),
    PIANO("ZombiePianoDefault", 840, 0.12, 4000, 450, 2000,
            ArmorType.NONE, new String[] { "PianoCrushAbility:0.4" }, false),
    NEWSPAPER("ZombieModernNewspaperDefault", 460, 0.22, 200, 700, 4000,
            ArmorType.NEWSPAPER, new String[] { "EnrageAbility:4:4" }, false),
    PET("ZombiePetDefault", 300, 0.1, 100, 100, 1000,
            ArmorType.NONE, null, false),

    // === ZOMBOSS ===
    ZOMBOSS_EGYPT("ZombieZombossMechEgypt", 4000, 0.1, 0, 0, 0,
            ArmorType.NONE, new String[] { "ZombossAbility:egypt" }, true),
    ZOMBOSS_PIRATE("ZombieZombossMechPirate", 5500, 0.1, 0, 0, 0,
            ArmorType.NONE, new String[] { "ZombossAbility:pirate" }, true),
    ZOMBOSS_COWBOY("ZombieZombossMechCowboy", 6500, 0.1, 0, 0, 0,
            ArmorType.NONE, new String[] { "ZombossAbility:cowboy" }, true),
    ZOMBOSS_DARK("ZombieZombossMechDark", 7000, 0.1, 0, 0, 0,
            ArmorType.NONE, new String[] { "ZombossAbility:dark" }, true);

    private final String alias;
    private final int hitpoints;
    private final double speed;
    private final int eatDPS;
    private final int wavePointCost;
    private final int weight;
    private final ArmorType defaultArmor;
    private final String[] abilitySpecs;
    private final boolean isLarge;

    ZombieType(String alias, int hitpoints, double speed, int eatDPS,
            int wavePointCost, int weight, ArmorType defaultArmor,
            String[] abilitySpecs, boolean isLarge) {
        this.alias = alias;
        this.hitpoints = hitpoints;
        this.speed = speed;
        this.eatDPS = eatDPS;
        this.wavePointCost = wavePointCost;
        this.weight = weight;
        this.defaultArmor = defaultArmor;
        this.abilitySpecs = abilitySpecs;
        this.isLarge = isLarge;
    }

    public String getAlias() {
        return alias;
    }

    public int getHitpoints() {
        return hitpoints;
    }

    public double getSpeed() {
        return speed;
    }

    public int getEatDPS() {
        return eatDPS;
    }

    public int getWavePointCost() {
        return wavePointCost;
    }

    public int getWeight() {
        return weight;
    }

    public ArmorType getDefaultArmor() {
        return defaultArmor;
    }

    public String[] getAbilitySpecs() {
        return abilitySpecs;
    }

    public boolean isLarge() {
        return isLarge;
    }

    /**
     * Find a zombie type by its alias name.
     */
    public static ZombieType findByAlias(String alias) {
        for (ZombieType type : values()) {
            if (type.alias.equalsIgnoreCase(alias)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find a zombie type by its enum name.
     */
    public static ZombieType findByName(String name) {
        for (ZombieType type : values()) {
            if (type.name().equalsIgnoreCase(name) ||
                    type.alias.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
