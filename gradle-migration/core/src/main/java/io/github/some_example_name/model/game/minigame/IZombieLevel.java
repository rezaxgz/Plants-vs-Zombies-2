package io.github.some_example_name.model.game.minigame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.some_example_name.model.game.entities.zombies.ZombieType;

/**
 * Three progressively harder I, Zombie stages.
 */
public enum IZombieLevel {
    FIRST_BITE(
            1, "First Bite", 3, 10,
            cards(
                    card(ZombieType.BASIC, 25),
                    card(ZombieType.CONEHEAD, 50),
                    card(ZombieType.BUCKETHEAD, 75),
                    card(ZombieType.IMP, 30),
                    card(ZombieType.NEWSPAPER, 65))),
    PYRAMID_PLUNDER(
            2, "Pyramid Plunder", 3, 14,
            cards(
                    card(ZombieType.MUMMY, 30),
                    card(ZombieType.MUMMY_CONEHEAD, 55),
                    card(ZombieType.MUMMY_BUCKETHEAD, 80),
                    card(ZombieType.CAMEL, 90),
                    card(ZombieType.EXPLORER, 70))),
    FROZEN_FEAST(
            3, "Frozen Feast", 3, 18,
            cards(
                    card(ZombieType.ICEAGE, 35),
                    card(ZombieType.ICEAGE_CONEHEAD, 60),
                    card(ZombieType.ICEAGE_BUCKETHEAD, 90),
                    card(ZombieType.DODO, 85),
                    card(ZombieType.JUGGLER, 100)));

    public static final int LEVEL_COUNT = 3;

    private static final List<String> PLANT_POOL = List.of(
            "Peashooter",
            "Sunflower",
            "Wall-nut",
            "Potato Mine",
            "Cabbage-pult");

    static {
        validateLevelDefinitions();
    }

    private final int number;
    private final String name;
    private final int redLineColumn;
    private final int plantCount;
    private final List<IZombieCard> zombieCards;

    IZombieLevel(int number, String name,
            int redLineColumn, int plantCount,
            List<IZombieCard> zombieCards) {
        this.number = number;
        this.name = name;
        this.redLineColumn = redLineColumn;
        this.plantCount = plantCount;
        this.zombieCards = Collections.unmodifiableList(
                new ArrayList<>(zombieCards));
    }

    private static IZombieCard card(
            ZombieType type, int cost) {
        return new IZombieCard(type, cost);
    }

    private static List<IZombieCard> cards(
            IZombieCard... cards) {
        return List.of(cards);
    }

    private static void validateLevelDefinitions() {
        Set<ZombieType> distinctTypes = new HashSet<>();
        for (IZombieLevel level : values()) {
            if (level.zombieCards.size() != 5) {
                throw new IllegalStateException(
                        "every I, Zombie level must have five cards");
            }
            for (IZombieCard card : level.zombieCards) {
                distinctTypes.add(card.getType());
            }
        }
        if (distinctTypes.size() < 10) {
            throw new IllegalStateException(
                    "I, Zombie levels need at least ten zombie types");
        }
    }

    public static IZombieLevel find(int number) {
        for (IZombieLevel level : values()) {
            if (level.number == number) {
                return level;
            }
        }
        return null;
    }

    public IZombieCard findCard(String requestedType) {
        for (IZombieCard card : zombieCards) {
            if (card.matches(requestedType)) {
                return card;
            }
        }
        return null;
    }

    public int getMinimumZombieCost() {
        int minimum = Integer.MAX_VALUE;
        for (IZombieCard card : zombieCards) {
            minimum = Math.min(minimum, card.getCost());
        }
        return minimum;
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public int getRedLineColumn() {
        return redLineColumn;
    }

    public int getPlantCount() {
        return plantCount;
    }

    public List<IZombieCard> getZombieCards() {
        return zombieCards;
    }

    public List<String> getPlantPool() {
        return PLANT_POOL;
    }
}
