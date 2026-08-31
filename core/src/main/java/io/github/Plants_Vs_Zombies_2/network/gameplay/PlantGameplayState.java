package io.github.Plants_Vs_Zombies_2.network.gameplay;

import java.util.Objects;

/** Sanitized transport state for one catalog plant. */
public final class PlantGameplayState {
    private final String name;
    private final boolean unlocked;
    private final int level;
    private final int cards;

    public PlantGameplayState(String name, boolean unlocked, int level, int cards) {
        this.name = name;
        this.unlocked = unlocked;
        this.level = level;
        this.cards = cards;
    }

    public String getName() { return name; }
    public boolean isUnlocked() { return unlocked; }
    public int getLevel() { return level; }
    public int getCards() { return cards; }

    @Override public boolean equals(Object other) {
        if (!(other instanceof PlantGameplayState value)) return false;
        return unlocked == value.unlocked && level == value.level
                && cards == value.cards && Objects.equals(name, value.name);
    }

    @Override public int hashCode() {
        return Objects.hash(name, unlocked, level, cards);
    }
}
