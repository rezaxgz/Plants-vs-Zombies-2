package io.github.Plants_Vs_Zombies_2.network.gameplay;

import java.util.Objects;

/** Sanitized transport state for one zombie collection entry. */
public final class ZombieGameplayState {
    private final String name;
    private final boolean unlocked;

    public ZombieGameplayState(String name, boolean unlocked) {
        this.name = name;
        this.unlocked = unlocked;
    }

    public String getName() { return name; }
    public boolean isUnlocked() { return unlocked; }

    @Override public boolean equals(Object other) {
        return other instanceof ZombieGameplayState value
                && unlocked == value.unlocked && Objects.equals(name, value.name);
    }

    @Override public int hashCode() { return Objects.hash(name, unlocked); }
}
