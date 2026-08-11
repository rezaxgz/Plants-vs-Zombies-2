package io.github.Plants_Vs_Zombies_2.model.game.minigame;

import java.util.Locale;

import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;

/**
 * One player-selectable zombie and its sun price in I, Zombie.
 */
public final class IZombieCard {
    private final ZombieType type;
    private final int cost;

    public IZombieCard(ZombieType type, int cost) {
        if (type == null || type.isBoss() || cost <= 0) {
            throw new IllegalArgumentException(
                    "I, Zombie card values are invalid");
        }
        this.type = type;
        this.cost = cost;
    }

    public boolean matches(String requestedType) {
        if (requestedType == null) {
            return false;
        }
        String normalized = normalize(requestedType);
        return normalized.equals(normalize(type.name()))
                || normalized.equals(normalize(type.getAlias()));
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    public ZombieType getType() {
        return type;
    }

    public int getCost() {
        return cost;
    }
}
