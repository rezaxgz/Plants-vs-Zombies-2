package model.game.minigame;

import java.util.Locale;

/**
 * The three Wall-nuts available in Wall-nut Bowling.
 */
public enum BowlingWallnutType {
    NORMAL("Bowling Wall-nut", "N", 1.60),
    EXPLOSIVE("Explode-o-nut", "E", 1.55),
    LARGE("Giant Wall-nut", "L", 1.35);

    private final String displayName;
    private final String mapSymbol;
    private final double speedTilesPerSecond;

    BowlingWallnutType(String displayName, String mapSymbol,
            double speedTilesPerSecond) {
        this.displayName = displayName;
        this.mapSymbol = mapSymbol;
        this.speedTilesPerSecond = speedTilesPerSecond;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMapSymbol() {
        return mapSymbol;
    }

    public double getSpeedTilesPerSecond() {
        return speedTilesPerSecond;
    }

    public static BowlingWallnutType find(String requestedName) {
        if (requestedName == null) {
            return null;
        }
        String normalized = requestedName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        for (BowlingWallnutType type : values()) {
            String display = type.displayName.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]", "");
            if (normalized.equals(display)
                    || normalized.equals(type.name()
                            .toLowerCase(Locale.ROOT))) {
                return type;
            }
        }
        if (normalized.equals("wallnut")
                || normalized.equals("bowlingwallnut")) {
            return NORMAL;
        }
        if (normalized.equals("explodeonut")
                || normalized.equals("explosivewallnut")) {
            return EXPLOSIVE;
        }
        if (normalized.equals("largewallnut")
                || normalized.equals("giantwallnut")) {
            return LARGE;
        }
        return null;
    }
}
