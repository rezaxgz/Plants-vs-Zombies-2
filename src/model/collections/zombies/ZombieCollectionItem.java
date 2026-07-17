package model.collections.zombies;

import java.util.Arrays;
import java.util.List;

import model.game.entities.zombies.ZombieType;
import model.game.entities.zombies.armor.ArmorType;

public class ZombieCollectionItem {
    private final ZombieType type;
    private boolean unlocked;

    ZombieCollectionItem(ZombieType type) {
        if (type == null) {
            throw new IllegalArgumentException("zombie type cannot be null");
        }
        this.type = type;
    }

    public String getName() {
        return type.getAlias();
    }

    public String getTypeName() {
        return type.name();
    }

    public int getHitpoints() {
        return type.getHitpoints();
    }

    public double getSpeed() {
        return type.getSpeed();
    }

    public int getEatDPS() {
        return type.getEatDPS();
    }

    public int getWavePointCost() {
        return type.getWavePointCost();
    }

    public int getWeight() {
        return type.getWeight();
    }

    public ArmorType getDefaultArmor() {
        return type.getDefaultArmor();
    }

    public List<String> getAbilities() {
        String[] abilities = type.getAbilitySpecs();
        return abilities == null ? List.of() : List.copyOf(Arrays.asList(abilities));
    }

    public boolean isLarge() {
        return type.isLarge();
    }

    public boolean isBoss() {
        return type.isBoss();
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }
}
