package model.game;

import model.game.entities.zombies.ZombieType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a wave of zombies with support for all zombie types.
 */
public class ZombieWave {
    private final List<ZombieType> zombieTypes;
    private final int difficulty;
    private final boolean finalWave;
    private final int maximumHealth;
    private int remainingHealth;

    public ZombieWave(List<ZombieType> zombieTypes, int difficulty, boolean finalWave) {
        this.zombieTypes = Collections.unmodifiableList(new ArrayList<>(zombieTypes));
        this.difficulty = difficulty;
        this.finalWave = finalWave;
        this.maximumHealth = calculateTotalHealth();
        this.remainingHealth = maximumHealth;
    }

    private int calculateTotalHealth() {
        int total = 0;
        for (ZombieType type : zombieTypes) {
            total += type.getHitpoints();
            if (type.getDefaultArmor() != null) {
                total += type.getDefaultArmor().getBaseHealth();
            }
        }
        return total;
    }

    public static ZombieWave basicWave(int difficulty, boolean finalWave) {
        List<ZombieType> types = new ArrayList<>();
        int remaining = difficulty;

        // Add flag zombie for final wave
        if (finalWave) {
            types.add(ZombieType.FLAG);
            remaining -= ZombieType.FLAG.getWavePointCost();
        }

        // Fill with basic zombies
        while (remaining >= ZombieType.BASIC.getWavePointCost()) {
            types.add(ZombieType.BASIC);
            remaining -= ZombieType.BASIC.getWavePointCost();
        }

        return new ZombieWave(types, difficulty, finalWave);
    }

    /**
     * Create a themed wave for a specific world/chapter.
     */
    public static ZombieWave themedWave(String chapter, int difficulty, boolean finalWave) {
        List<ZombieType> types = new ArrayList<>();
        int remaining = difficulty;

        // Add flag for final wave
        if (finalWave) {
            types.add(getFlagZombie(chapter));
            remaining -= getFlagZombie(chapter).getWavePointCost();
        }

        // Fill with chapter-appropriate zombies
        ZombieType[] chapterZombies = getChapterZombies(chapter);

        while (remaining > 0) {
            ZombieType chosen = chapterZombies[(int)(Math.random() * chapterZombies.length)];
            if (chosen.getWavePointCost() <= remaining) {
                types.add(chosen);
                remaining -= chosen.getWavePointCost();
            } else {
                break;
            }
        }

        return new ZombieWave(types, difficulty, finalWave);
    }

    private static ZombieType getFlagZombie(String chapter) {
        switch (chapter.toLowerCase()) {
            case "egypt": return ZombieType.FLAG;
            case "iceage": return ZombieType.FLAG;
            case "beach": return ZombieType.FLAG;
            case "dark": return ZombieType.FLAG;
            default: return ZombieType.FLAG;
        }
    }

    private static ZombieType[] getChapterZombies(String chapter) {
        switch (chapter.toLowerCase()) {
            case "egypt":
                return new ZombieType[]{
                    ZombieType.MUMMY, ZombieType.MUMMY_CONEHEAD,
                    ZombieType.MUMMY_BUCKETHEAD, ZombieType.RA,
                    ZombieType.EXPLORER, ZombieType.TOMB_RAISER,
                    ZombieType.CAMEL
                };
            case "iceage":
                return new ZombieType[]{
                    ZombieType.ICEAGE, ZombieType.ICEAGE_CONEHEAD,
                    ZombieType.ICEAGE_BUCKETHEAD, ZombieType.ICEAGE_BLOCKHEAD,
                    ZombieType.HUNTER, ZombieType.TROGLOBITE,
                    ZombieType.DODO, ZombieType.WEASEL_HOARDER
                };
            case "beach":
                return new ZombieType[]{
                    ZombieType.BEACH, ZombieType.BEACH_CONEHEAD,
                    ZombieType.BEACH_BUCKETHEAD, ZombieType.SNORKEL,
                    ZombieType.SURFER, ZombieType.FISHERMAN,
                    ZombieType.OCTOPUS, ZombieType.FAST_SWIMMER
                };
            case "dark":
                return new ZombieType[]{
                    ZombieType.DARK, ZombieType.DARK_CONEHEAD,
                    ZombieType.DARK_BUCKETHEAD, ZombieType.DARK_SHOULDER_ARMOR,
                    ZombieType.DARK_BRICKHEAD, ZombieType.WIZARD,
                    ZombieType.JUGGLER, ZombieType.DARK_KING
                };
            default:
                return new ZombieType[]{ZombieType.BASIC, ZombieType.CONEHEAD, ZombieType.BUCKETHEAD};
        }
    }

    public ZombieWave copy() {
        return new ZombieWave(
                zombieTypes, difficulty, finalWave);
    }

    public List<ZombieType> getZombieTypes() { return zombieTypes; }
    public int getDifficulty() { return difficulty; }
    public boolean isFinalWave() { return finalWave; }
    public int getMaximumHealth() { return maximumHealth; }
    public int getRemainingHealth() { return remainingHealth; }

    public void recordDamage(int damage) {
        remainingHealth = Math.max(0, remainingHealth - damage);
    }

    public boolean isDamagedEnough(double threshold) {
        return remainingHealth * 4 <= maximumHealth * (4 - threshold);
    }
}
