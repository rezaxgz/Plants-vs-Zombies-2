package io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.abilities;

import java.util.List;
import java.util.Locale;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;

/**
 * Immutable action/reinforcement data for each Zomboss machine.
 */
enum ZombossProfile {
    EGYPT("egypt"),
    ICEAGE("iceage"),
    BEACH("beach"),
    DARK("dark"),
    // Kept for compatibility with the Phase-1 enum entries.
    PIRATE("pirate"),
    COWBOY("cowboy");

    enum Action {
        MOVE,
        SPAWN,
        RUSH,
        ROCKET,
        IMP_CANNON,
        FIRE_BREATH,
        FIREBALLS,
        ICY_WIND,
        FREEZE_COLUMN,
        BABY_SHARK,
        TURBINE
    }

    private final String id;

    ZombossProfile(String id) {
        this.id = id;
    }

    static ZombossProfile parse(String name) {
        if (name == null) {
            throw new IllegalArgumentException(
                    "Zomboss world cannot be null");
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        for (ZombossProfile profile : values()) {
            if (profile.id.equals(normalized)) {
                return profile;
            }
        }
        throw new IllegalArgumentException(
                "Unknown Zomboss world: " + name);
    }

    int phaseFor(Zombie zomboss) {
        if (zomboss == null || zomboss.getMaximumHitPoints() <= 0) {
            return 1;
        }
        int hitPoints = Math.max(0, zomboss.getHitPoints());
        int maximum = zomboss.getMaximumHitPoints();
        if ((long) hitPoints * 3 > (long) maximum * 2) {
            return 1;
        }
        if ((long) hitPoints * 3 > maximum) {
            return 2;
        }
        return 3;
    }

    double cooldownFor(int phase) {
        switch (phase) {
            case 1:
                return 7.0;
            case 2:
                return 6.0;
            case 3:
                return 5.0;
            default:
                throw new IllegalArgumentException(
                        "Zomboss phase must be 1, 2, or 3");
        }
    }

    List<Action> actionsFor(int phase) {
        switch (this) {
            case EGYPT:
                return List.of(
                        Action.MOVE,
                        Action.SPAWN,
                        Action.RUSH,
                        Action.ROCKET);
            case ICEAGE:
                return List.of(
                        Action.ROCKET,
                        Action.ICY_WIND,
                        Action.FREEZE_COLUMN);
            case BEACH:
                return List.of(
                        Action.MOVE,
                        Action.SPAWN,
                        Action.BABY_SHARK,
                        Action.TURBINE);
            case DARK:
                return List.of(
                        Action.MOVE,
                        Action.SPAWN,
                        Action.FIRE_BREATH,
                        Action.FIREBALLS);
            case PIRATE:
                return pirateActions(phase);
            case COWBOY:
                return cowboyActions(phase);
            default:
                throw new IllegalStateException(
                        "Unhandled Zomboss profile");
        }
    }

    private static List<Action> pirateActions(int phase) {
        if (phase == 1) {
            return List.of(Action.MOVE, Action.SPAWN, Action.IMP_CANNON);
        }
        if (phase == 2) {
            return List.of(Action.MOVE, Action.SPAWN, Action.RUSH);
        }
        return List.of(Action.SPAWN, Action.RUSH, Action.IMP_CANNON);
    }

    private static List<Action> cowboyActions(int phase) {
        if (phase == 1) {
            return List.of(Action.MOVE, Action.SPAWN, Action.ROCKET);
        }
        return List.of(Action.SPAWN, Action.RUSH, Action.ROCKET);
    }

    List<ZombieType> minionsFor(int phase) {
        switch (this) {
            case EGYPT:
                return egyptMinions(phase);
            case BEACH:
                return beachMinions(phase);
            case DARK:
                return darkMinions(phase);
            case PIRATE:
                return pirateMinions(phase);
            case COWBOY:
                return cowboyMinions(phase);
            case ICEAGE:
                return List.of();
            default:
                throw new IllegalStateException(
                        "Unhandled Zomboss profile");
        }
    }

    private static List<ZombieType> egyptMinions(int phase) {
        if (phase == 1) {
            return List.of(ZombieType.MUMMY, ZombieType.MUMMY_CONEHEAD);
        }
        if (phase == 2) {
            return List.of(
                    ZombieType.MUMMY_BUCKETHEAD,
                    ZombieType.RA,
                    ZombieType.EXPLORER,
                    ZombieType.PHARAOH);
        }
        return List.of(
                ZombieType.TOMB_RAISER,
                ZombieType.CAMEL,
                ZombieType.EGYPT_GARGANTUAR);
    }

    private static List<ZombieType> beachMinions(int phase) {
        if (phase == 1) {
            return List.of(ZombieType.BEACH, ZombieType.BEACH_CONEHEAD);
        }
        if (phase == 2) {
            return List.of(
                    ZombieType.BEACH_BUCKETHEAD,
                    ZombieType.SNORKEL,
                    ZombieType.SURFER);
        }
        return List.of(
                ZombieType.FISHERMAN,
                ZombieType.OCTOPUS,
                ZombieType.BEACH_GARGANTUAR);
    }

    private static List<ZombieType> pirateMinions(int phase) {
        if (phase == 1) {
            return List.of(ZombieType.BASIC, ZombieType.CONEHEAD, ZombieType.IMP);
        }
        if (phase == 2) {
            return List.of(
                    ZombieType.CONEHEAD,
                    ZombieType.BUCKETHEAD,
                    ZombieType.ROLLER_BARREL);
        }
        return List.of(
                ZombieType.BUCKETHEAD,
                ZombieType.ROLLER_BARREL,
                ZombieType.GARGANTUAR);
    }

    private static List<ZombieType> cowboyMinions(int phase) {
        if (phase == 1) {
            return List.of(
                    ZombieType.BASIC,
                    ZombieType.CONEHEAD,
                    ZombieType.PROSPECTOR);
        }
        if (phase == 2) {
            return List.of(
                    ZombieType.BUCKETHEAD,
                    ZombieType.PROSPECTOR,
                    ZombieType.PIANO);
        }
        return List.of(
                ZombieType.ALL_STAR,
                ZombieType.PIANO,
                ZombieType.GARGANTUAR);
    }

    private static List<ZombieType> darkMinions(int phase) {
        if (phase == 1) {
            return List.of(ZombieType.DARK, ZombieType.DARK_CONEHEAD);
        }
        if (phase == 2) {
            return List.of(
                    ZombieType.DARK_BUCKETHEAD,
                    ZombieType.WIZARD,
                    ZombieType.JUGGLER);
        }
        return List.of(
                ZombieType.DARK_KING,
                ZombieType.DARK_GARGANTUAR,
                ZombieType.DRAGON_IMP);
    }

    boolean canMoveBetweenLanes() {
        return this != ICEAGE;
    }

    boolean canSummonNormalZombies() {
        return this != ICEAGE;
    }

    double rushMinimumColumn(Board board) {
        // Drive the Egypt rush all the way to the front of the lawn so the
        // stomp visibly reaches the first columns before the boss retreats.
        return 0.0;
    }

    static double homeColumn(Board board) {
        return Math.max(1.0, board.getNumberOfColumns() - 2.0);
    }
}
