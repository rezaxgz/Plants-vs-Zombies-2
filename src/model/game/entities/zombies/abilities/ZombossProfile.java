package model.game.entities.zombies.abilities;

import java.util.List;
import java.util.Locale;

import model.game.Board;
import model.game.entities.zombies.ZombieType;

/**
 * Immutable phase and reinforcement data for each Zomboss machine.
 */
enum ZombossProfile {
    EGYPT("egypt", 14500, 6500),
    PIRATE("pirate", 21000, 11000),
    COWBOY("cowboy", 19000, 9000),
    DARK("dark", 20000, 11000);

    enum Action {
        MOVE,
        SPAWN,
        RUSH,
        ROCKET,
        IMP_CANNON,
        FIRE_BREATH,
        FIREBALLS
    }

    private final String id;
    private final int phaseOneThreshold;
    private final int phaseTwoThreshold;

    ZombossProfile(String id, int phaseOneThreshold,
            int phaseTwoThreshold) {
        this.id = id;
        this.phaseOneThreshold = phaseOneThreshold;
        this.phaseTwoThreshold = phaseTwoThreshold;
    }

    static ZombossProfile parse(String name) {
        if (name == null) {
            throw new IllegalArgumentException(
                    "Zomboss world cannot be null");
        }
        String normalized = name.trim()
                .toLowerCase(Locale.ROOT);
        for (ZombossProfile profile : values()) {
            if (profile.id.equals(normalized)) {
                return profile;
            }
        }
        throw new IllegalArgumentException(
                "Unknown Zomboss world: " + name);
    }

    int phaseFor(int hitPoints) {
        if (hitPoints > phaseOneThreshold) {
            return 1;
        }
        if (hitPoints > phaseTwoThreshold) {
            return 2;
        }
        return 3;
    }

    double cooldownFor(int phase) {
        switch (phase) {
            case 1:
                return 5.0;
            case 2:
                return 4.0;
            case 3:
                return 3.0;
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
            case PIRATE:
                return pirateActions(phase);
            case COWBOY:
                return cowboyActions(phase);
            case DARK:
                return List.of(
                        Action.MOVE,
                        Action.SPAWN,
                        Action.FIRE_BREATH,
                        Action.FIREBALLS);
            default:
                throw new IllegalStateException(
                        "Unhandled Zomboss profile");
        }
    }

    private static List<Action> pirateActions(int phase) {
        if (phase == 1) {
            return List.of(
                    Action.MOVE,
                    Action.SPAWN,
                    Action.IMP_CANNON);
        }
        if (phase == 2) {
            return List.of(
                    Action.MOVE,
                    Action.SPAWN,
                    Action.RUSH);
        }
        return List.of(
                Action.SPAWN,
                Action.RUSH,
                Action.IMP_CANNON);
    }

    private static List<Action> cowboyActions(int phase) {
        if (phase == 1) {
            return List.of(
                    Action.MOVE,
                    Action.SPAWN,
                    Action.ROCKET);
        }
        return List.of(
                Action.SPAWN,
                Action.RUSH,
                Action.ROCKET);
    }

    List<ZombieType> minionsFor(int phase) {
        switch (this) {
            case EGYPT:
                return egyptMinions(phase);
            case PIRATE:
                return pirateMinions(phase);
            case COWBOY:
                return cowboyMinions(phase);
            case DARK:
                return darkMinions(phase);
            default:
                throw new IllegalStateException(
                        "Unhandled Zomboss profile");
        }
    }

    private static List<ZombieType> egyptMinions(int phase) {
        if (phase == 1) {
            return List.of(
                    ZombieType.MUMMY,
                    ZombieType.MUMMY_CONEHEAD);
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

    private static List<ZombieType> pirateMinions(int phase) {
        if (phase == 1) {
            return List.of(
                    ZombieType.BASIC,
                    ZombieType.CONEHEAD,
                    ZombieType.IMP);
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
            return List.of(
                    ZombieType.DARK,
                    ZombieType.DARK_CONEHEAD);
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

    double minimumColumn(Board board) {
        if (this == DARK) {
            return Math.min(
                    1.0,
                    board.getNumberOfColumns() - 1.0);
        }
        return Math.max(
                1.0,
                board.getNumberOfColumns() - 4.0);
    }

    static double maximumColumn(Board board) {
        return Math.max(
                1.0,
                board.getNumberOfColumns() - 2.0);
    }
}